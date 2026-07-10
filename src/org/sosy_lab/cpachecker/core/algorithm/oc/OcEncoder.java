// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.cpa.oc.OcExplorationRegistry;
import org.sosy_lab.cpachecker.cpa.oc.ThreadInstance;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Turns the exploration registry into the ordering-consistency SMT encoding: per-instance
 * linearized program order with create/join cross edges, full event guards (creation guard of the
 * instance and path guard), read-from and write-serialization Booleans with their value
 * constraints, mutual-exclusion constraints for critical sections, and the error property.
 */
final class OcEncoder {

  /** A read-from pair with its Boolean selector variable. */
  record RfPair(MemoryEvent write, MemoryEvent read, BooleanFormula variable) {}

  /** An unordered same-location write pair with one selector per direction. */
  record WsPair(
      MemoryEvent write1, MemoryEvent write2, BooleanFormula var12, BooleanFormula var21) {}

  /**
   * A critical section: a LOCK event and its end — the matching UNLOCK event below it, or the last
   * event of a path that ends (e.g. at the error location) while still holding the lock.
   */
  record Section(MemoryEvent lock, MemoryEvent unlock) {}

  /**
   * Two same-mutex sections of different instances; {@code var12} orders section 1 completely
   * before section 2 (unlock1 before lock2), {@code var21} the other way.
   */
  record CsPair(Section section1, Section section2, BooleanFormula var12, BooleanFormula var21) {}

  /**
   * A create/join ordering edge that only holds when its create/join event is enabled (the same
   * thread instance may be created or joined from mutually exclusive branches, so these edges must
   * not be unconditional).
   */
  record CrossPoEdge(int from, int to, int guardEventId) {}

  private final OcExplorationRegistry registry;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final boolean withOrderingBooleans;

  private final ImmutableList<MemoryEvent> events;
  private final BooleanFormula[] fullGuards;
  private final List<int[]> poEdges = new ArrayList<>();
  private final List<CrossPoEdge> crossPoEdges = new ArrayList<>();
  private final BitSet[] definiteReach;
  private final ImmutableList<RfPair> rfPairs;
  private final ImmutableList<WsPair> wsPairs;
  private final ImmutableList<CsPair> csPairs;

  OcEncoder(
      OcExplorationRegistry pRegistry, FormulaManagerView pFmgr, boolean pWithOrderingBooleans)
      throws CPAException {
    registry = pRegistry;
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    withOrderingBooleans = pWithOrderingBooleans;
    events = pRegistry.getEvents();

    ImmutableListMultimap<Integer, MemoryEvent> childrenByParent = buildChildren();
    ImmutableMap<Integer, List<MemoryEvent>> linearizations = buildLinearizations(childrenByParent);
    buildPoEdges(linearizations, childrenByParent);
    definiteReach = computeReachability(buildDefiniteOrderEdges(childrenByParent));
    checkProgramOrderAcyclic();
    fullGuards = computeFullGuards();
    rfPairs = buildRfPairs();
    wsPairs = withOrderingBooleans ? buildWsPairs() : ImmutableList.of();
    csPairs = buildCsPairs(childrenByParent);
  }

  ImmutableList<MemoryEvent> getEvents() {
    return events;
  }

  /**
   * The real program-order DAG edges (predecessor to event), as opposed to {@link #getPoEdges()}
   * which are consecutive pairs of an arbitrary linearization and therefore also connect
   * mutually-exclusive branch events. The consistency checker must use these so it never treats two
   * events that are never co-enabled as ordered.
   */
  List<int[]> getProgramOrderDagEdges() {
    List<int[]> edges = new ArrayList<>();
    for (MemoryEvent event : events) {
      for (int predecessorId : registry.getPoPredecessors().get(event.id())) {
        edges.add(new int[] {predecessorId, event.id()});
      }
    }
    return edges;
  }

  List<int[]> getPoEdges() {
    return poEdges;
  }

  List<CrossPoEdge> getCrossPoEdges() {
    return crossPoEdges;
  }

  BooleanFormula getFullGuard(int pEventId) {
    return fullGuards[pEventId];
  }

  BooleanFormula getFullGuard(MemoryEvent pEvent) {
    return fullGuards[pEvent.id()];
  }

  ImmutableList<RfPair> getRfPairs() {
    return rfPairs;
  }

  ImmutableList<WsPair> getWsPairs() {
    return wsPairs;
  }

  ImmutableList<CsPair> getCsPairs() {
    return csPairs;
  }

  /** Whether pFirst precedes pSecond in every execution in which both are enabled. */
  boolean poBefore(MemoryEvent pFirst, MemoryEvent pSecond) {
    return definiteReach[pFirst.id()].get(pSecond.id());
  }

  /**
   * A join completes only if the joined instance terminated normally (THREAD_EXIT); paths that
   * abort the program or are cut at the loop bound have no such event and cannot be joined. An
   * ERROR in the joined instance also satisfies the join: the run is already violating, so the
   * (hypothetical) continuation must not make the model inconsistent.
   *
   * <p>These are COMPLETION facts and must be asserted only for the violation check. The unwinding
   * assertion asks about executions that were deliberately cut short; assuming there that joined
   * threads finished within the bound would contradict the cut guards by construction and unsoundly
   * "prove" the bound sufficient.
   */
  List<BooleanFormula> getJoinConstraints() {
    List<BooleanFormula> constraints = new ArrayList<>();
    ImmutableListMultimap<Integer, MemoryEvent> exitsByInstance =
        events.stream()
            .filter(e -> e.kind() == EventKind.THREAD_EXIT || e.kind() == EventKind.ERROR)
            .collect(
                ImmutableListMultimap.toImmutableListMultimap(MemoryEvent::instanceId, e -> e));
    for (MemoryEvent join : events) {
      if (join.kind() == EventKind.JOIN) {
        List<BooleanFormula> exitGuards =
            exitsByInstance.get(join.otherInstanceId()).stream()
                .map(this::getFullGuard)
                .collect(ImmutableList.toImmutableList());
        constraints.add(bfmgr.implication(getFullGuard(join), bfmgr.or(exitGuards)));
      }
    }
    return constraints;
  }

  /** The error property: some error event is enabled. Asserted only for the violation check. */
  BooleanFormula getErrorConstraint() {
    List<BooleanFormula> errorGuards = new ArrayList<>();
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.ERROR) {
        errorGuards.add(getFullGuard(event));
      }
    }
    return bfmgr.or(errorGuards);
  }

  /**
   * An unordered pair of conflicting accesses of different thread instances to the same memory cell,
   * at least one of them a write. These are the candidates for a data race; {@link
   * #getDataRaceConstraint} asks whether any of them can occur without an ordering between them.
   */
  record RacePair(MemoryEvent access1, MemoryEvent access2) {}

  /** All conflicting cross-instance access pairs (see {@link RacePair}). */
  ImmutableList<RacePair> getRacePairs() {
    List<MemoryEvent> accesses = new ArrayList<>();
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.READ || event.kind() == EventKind.WRITE) {
        accesses.add(event);
      }
    }
    ImmutableList.Builder<RacePair> result = ImmutableList.builder();
    for (int i = 0; i < accesses.size(); i++) {
      for (int j = i + 1; j < accesses.size(); j++) {
        MemoryEvent a = accesses.get(i);
        MemoryEvent b = accesses.get(j);
        if (a.instanceId() != b.instanceId()
            && (a.kind() == EventKind.WRITE || b.kind() == EventKind.WRITE)
            && cellKey(a).equals(cellKey(b))
            // a data race needs at least one non-atomic access; two _Atomic accesses never race
            && !(registry.isAtomicAccess(a.id()) && registry.isAtomicAccess(b.id()))) {
          result.add(new RacePair(a, b));
        }
      }
    }
    return result.build();
  }

  /**
   * The data-race property (CLOCKS encoding only): some conflicting cross-instance pair (see {@link
   * #getRacePairs}) can occur at adjacent clock values, i.e. with no event forced between them. Two
   * conflicting accesses that must be ordered — because they are mutex- or atomic-block-protected
   * (the critical-section constraints then push a LOCK/UNLOCK between them) or otherwise
   * happens-before ordered — cannot become adjacent, so they are not flagged. Reads reading directly
   * from a concurrent write, however, stay adjacent and are (correctly) a race. Asserted, together
   * with the clock constraints, only for the violation check.
   */
  BooleanFormula getDataRaceConstraint() {
    var imgr = fmgr.getIntegerFormulaManager();
    IntegerFormula one = imgr.makeNumber(1);
    List<BooleanFormula> disjuncts = new ArrayList<>();
    for (RacePair pair : getRacePairs()) {
      IntegerFormula clock1 = imgr.makeVariable("__oc_clk_" + pair.access1().id());
      IntegerFormula clock2 = imgr.makeVariable("__oc_clk_" + pair.access2().id());
      BooleanFormula adjacent =
          bfmgr.or(
              imgr.equal(clock1, imgr.add(clock2, one)),
              imgr.equal(clock2, imgr.add(clock1, one)));
      BooleanFormula both =
          bfmgr.and(getFullGuard(pair.access1()), getFullGuard(pair.access2()));
      if (pair.access1().isRegionAccess()) {
        both = bfmgr.and(both, sameAddress(pair.access1(), pair.access2()));
      }
      disjuncts.add(bfmgr.and(both, adjacent));
    }
    return bfmgr.or(disjuncts);
  }

  /**
   * Guards of the loop-bound cut points; if none of them is feasible, no real execution was cut and
   * a safe verdict is sound despite the structural truncation (unwinding assertion).
   */
  ImmutableList<BooleanFormula> getTruncationGuards() {
    ImmutableList.Builder<BooleanFormula> guards = ImmutableList.builder();
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.TRUNCATED) {
        guards.add(getFullGuard(event));
      }
    }
    return guards.build();
  }

  /** Constraints shared by both solving modes. */
  List<BooleanFormula> getBaseConstraints() {
    List<BooleanFormula> constraints = new ArrayList<>(registry.getPathConstraints());

    for (RfPair rf : rfPairs) {
      BooleanFormula sameValue = fmgr.makeEqual(rf.write().variable(), rf.read().variable());
      BooleanFormula condition =
          bfmgr.and(sameValue, getFullGuard(rf.write()), getFullGuard(rf.read()));
      if (rf.write().isRegionAccess()) {
        condition = bfmgr.and(condition, sameAddress(rf.write(), rf.read()));
      }
      constraints.add(bfmgr.implication(rf.variable(), condition));
    }

    ImmutableListMultimap<Integer, RfPair> rfByRead =
        rfPairs.stream()
            .collect(ImmutableListMultimap.toImmutableListMultimap(p -> p.read().id(), p -> p));
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.READ) {
        List<BooleanFormula> candidates =
            rfByRead.get(event.id()).stream()
                .map(RfPair::variable)
                .collect(ImmutableList.toImmutableList());
        constraints.add(bfmgr.implication(getFullGuard(event), bfmgr.or(candidates)));
      }
    }

    // all allocation bases (address-taken variables, heap cells) are pairwise distinct
    ImmutableList<Formula> bases = registry.getAddressBases();
    for (int i = 0; i < bases.size(); i++) {
      for (int j = i + 1; j < bases.size(); j++) {
        constraints.add(bfmgr.not(fmgr.makeEqual(bases.get(i), bases.get(j))));
      }
    }

    if (withOrderingBooleans) {
      for (WsPair ws : wsPairs) {
        BooleanFormula bothEnabled =
            bfmgr.and(getFullGuard(ws.write1()), getFullGuard(ws.write2()));
        if (ws.write1().isRegionAccess()) {
          bothEnabled = bfmgr.and(bothEnabled, sameAddress(ws.write1(), ws.write2()));
        }
        constraints.add(bfmgr.implication(ws.var12(), bothEnabled));
        constraints.add(bfmgr.implication(ws.var21(), bothEnabled));
        constraints.add(bfmgr.implication(bothEnabled, bfmgr.or(ws.var12(), ws.var21())));
      }
      for (CsPair cs : csPairs) {
        constraints.add(
            bfmgr.implication(
                cs.var12(),
                bfmgr.and(
                    getFullGuard(cs.section1().unlock()), getFullGuard(cs.section2().lock()))));
        constraints.add(
            bfmgr.implication(
                cs.var21(),
                bfmgr.and(
                    getFullGuard(cs.section2().unlock()), getFullGuard(cs.section1().lock()))));
        constraints.add(
            bfmgr.implication(
                bfmgr.and(
                    getFullGuard(cs.section1().unlock()), getFullGuard(cs.section2().unlock())),
                bfmgr.or(cs.var12(), cs.var21())));
      }
    }
    return constraints;
  }

  /** Eager integer-clock ordering constraints (used by the CLOCKS mode). */
  List<BooleanFormula> getClockConstraints() {
    var imgr = fmgr.getIntegerFormulaManager();
    IntegerFormula[] clocks = new IntegerFormula[events.size()];
    for (MemoryEvent event : events) {
      clocks[event.id()] = imgr.makeVariable("__oc_clk_" + event.id());
    }

    List<BooleanFormula> constraints = new ArrayList<>();
    for (int[] edge : poEdges) {
      constraints.add(imgr.lessThan(clocks[edge[0]], clocks[edge[1]]));
    }
    for (CrossPoEdge cross : crossPoEdges) {
      constraints.add(
          bfmgr.implication(
              fullGuards[cross.guardEventId()],
              imgr.lessThan(clocks[cross.from()], clocks[cross.to()])));
    }

    ImmutableListMultimap<Object, MemoryEvent> writesByCell = writesByCell();
    for (RfPair rf : rfPairs) {
      constraints.add(
          bfmgr.implication(
              rf.variable(), imgr.lessThan(clocks[rf.write().id()], clocks[rf.read().id()])));
      for (MemoryEvent other : writesByCell.get(cellKey(rf.write()))) {
        if (other.id() == rf.write().id()) {
          continue;
        }
        // no other enabled write to the same cell may fall between an rf-related write and read
        BooleanFormula premise = bfmgr.and(rf.variable(), getFullGuard(other));
        if (rf.write().isRegionAccess()) {
          premise = bfmgr.and(premise, sameAddress(other, rf.write()));
        }
        constraints.add(
            bfmgr.implication(
                premise,
                bfmgr.or(
                    imgr.lessThan(clocks[other.id()], clocks[rf.write().id()]),
                    imgr.lessThan(clocks[rf.read().id()], clocks[other.id()]))));
      }
    }

    for (CsPair cs : csPairs) {
      constraints.add(
          bfmgr.implication(
              bfmgr.and(getFullGuard(cs.section1().unlock()), getFullGuard(cs.section2().unlock())),
              bfmgr.or(
                  imgr.lessThan(
                      clocks[cs.section1().unlock().id()], clocks[cs.section2().lock().id()]),
                  imgr.lessThan(
                      clocks[cs.section2().unlock().id()], clocks[cs.section1().lock().id()]))));
    }
    return constraints;
  }

  /**
   * Program-order successor multimap, built by inverting the authoritative predecessor multimap of
   * the registry (an event right after a DAG merge point has several predecessors, and thus is a
   * successor of several parents).
   */
  private ImmutableListMultimap<Integer, MemoryEvent> buildChildren() {
    ImmutableListMultimap.Builder<Integer, MemoryEvent> result = ImmutableListMultimap.builder();
    for (Map.Entry<Integer, Integer> entry : registry.getPoPredecessors().entries()) {
      result.put(entry.getValue(), events.get(entry.getKey()));
    }
    return result.build();
  }

  /**
   * A deterministic total order per instance, computed as a Kahn topological sort of the instance's
   * program-order DAG: nodes with no remaining predecessors become ready, and the smallest-id ready
   * event is always picked next. DAG-parallel events of one instance have mutually exclusive
   * guards, so any topological linearization is a sound total program order.
   */
  private ImmutableMap<Integer, List<MemoryEvent>> buildLinearizations(
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    ImmutableSetMultimap<Integer, Integer> predecessors = registry.getPoPredecessors();
    ImmutableMap.Builder<Integer, List<MemoryEvent>> result = ImmutableMap.builder();
    for (ThreadInstance instance : registry.getInstances()) {
      List<MemoryEvent> instanceEvents =
          events.stream()
              .filter(e -> e.instanceId() == instance.getId())
              .collect(ImmutableList.toImmutableList());
      Map<Integer, Integer> indegree = new HashMap<>();
      PriorityQueue<MemoryEvent> ready =
          new PriorityQueue<>(Comparator.comparingInt(MemoryEvent::id));
      for (MemoryEvent event : instanceEvents) {
        int degree = predecessors.get(event.id()).size();
        indegree.put(event.id(), degree);
        if (degree == 0) {
          ready.add(event);
        }
      }
      List<MemoryEvent> linear = new ArrayList<>();
      while (!ready.isEmpty()) {
        MemoryEvent event = ready.poll();
        linear.add(event);
        for (MemoryEvent child : pChildren.get(event.id())) {
          int remaining = indegree.merge(child.id(), -1, Integer::sum);
          if (remaining == 0) {
            ready.add(child);
          }
        }
      }
      result.put(instance.getId(), linear);
    }
    return result.buildOrThrow();
  }

  /**
   * Builds the per-instance linearization edges (consecutive pairs, used only by the eager CLOCKS
   * encoding) and the create/join cross edges.
   *
   * <p>A create/join cross edge must connect to <em>every</em> root (respectively sink) of the
   * other instance's real program-order DAG, not just one of them: a DAG merge can be missed (the
   * merge operator never merges into an already-expanded state, so two branches that reconverge at
   * the same program point after unequal exploration depths — e.g. the arms of a nested ternary —
   * can end up as siblings instead of merging), leaving the instance with several mutually exclusive
   * terminal (or initial) events. Anchoring the cross edge on a single, arbitrarily chosen
   * linearization endpoint (as opposed to all of them) would silently drop the ordering edge
   * whenever a model's actually-enabled root/sink is a different one: the checker builds its
   * happens-before graph only from enabled events, so with no edge at all from the created/joined
   * instance to the create/join event, a required order (e.g. "the joined thread's write happens
   * before the joining read") becomes unreachable in the graph and a real conflict goes undetected.
   * The eager CLOCKS encoding does not strictly need this (its unconditional per-instance clock
   * chain already relates every sibling event, enabled or not, so the ordering leaks through
   * transitively), but asserting it there too is sound and harmless (redundant, already implied).
   */
  private void buildPoEdges(
      ImmutableMap<Integer, List<MemoryEvent>> pLinearizations,
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    for (List<MemoryEvent> linear : pLinearizations.values()) {
      for (int i = 0; i + 1 < linear.size(); i++) {
        poEdges.add(new int[] {linear.get(i).id(), linear.get(i + 1).id()});
      }
    }
    ImmutableSetMultimap<Integer, Integer> predecessors = registry.getPoPredecessors();
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.CREATE || event.kind() == EventKind.JOIN) {
        List<MemoryEvent> other = pLinearizations.get(event.otherInstanceId());
        if (other == null || other.isEmpty()) {
          continue;
        }
        if (event.kind() == EventKind.CREATE) {
          for (MemoryEvent root : other) {
            if (predecessors.get(root.id()).isEmpty()) {
              crossPoEdges.add(new CrossPoEdge(event.id(), root.id(), event.id()));
            }
          }
        } else {
          for (MemoryEvent sink : other) {
            if (pChildren.get(sink.id()).isEmpty()) {
              crossPoEdges.add(new CrossPoEdge(sink.id(), event.id(), event.id()));
            }
          }
        }
      }
    }
  }

  /**
   * Edges of the "definite" order, which holds in every execution in which both endpoints are
   * enabled: the program-order DAG edges within an instance, the single create event before all
   * roots of an instance that has exactly one create event (instances with several creates only
   * lose optional rf/ws pruning, not correctness), and all sinks of an instance before every join
   * of that instance.
   */
  private List<int[]> buildDefiniteOrderEdges(
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    List<int[]> edges = new ArrayList<>();
    ImmutableSetMultimap<Integer, Integer> predecessors = registry.getPoPredecessors();
    for (MemoryEvent event : events) {
      for (int predecessorId : predecessors.get(event.id())) {
        edges.add(new int[] {predecessorId, event.id()});
      }
    }
    for (ThreadInstance instance : registry.getInstances()) {
      List<MemoryEvent> instanceEvents =
          events.stream()
              .filter(e -> e.instanceId() == instance.getId())
              .collect(ImmutableList.toImmutableList());
      if (instanceEvents.isEmpty()) {
        continue;
      }
      if (instance.getCreateEventIds().size() == 1) {
        int createEventId = instance.getCreateEventIds().get(0);
        for (MemoryEvent root : instanceEvents) {
          if (predecessors.get(root.id()).isEmpty()) {
            edges.add(new int[] {createEventId, root.id()});
          }
        }
      }
      for (MemoryEvent sink : instanceEvents) {
        if (pChildren.get(sink.id()).isEmpty()) {
          for (MemoryEvent join : events) {
            if (join.kind() == EventKind.JOIN && join.otherInstanceId() == instance.getId()) {
              edges.add(new int[] {sink.id(), join.id()});
            }
          }
        }
      }
    }
    return edges;
  }

  private BitSet[] computeReachability(List<int[]> pEdges) {
    int n = events.size();
    BitSet[] reach = new BitSet[n];
    for (int i = 0; i < n; i++) {
      reach[i] = new BitSet(n);
    }
    for (int[] edge : pEdges) {
      reach[edge[0]].set(edge[1]);
    }
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int[] edge : pEdges) {
        BitSet source = reach[edge[0]];
        int before = source.cardinality();
        source.or(reach[edge[1]]);
        if (source.cardinality() != before) {
          changed = true;
        }
      }
    }
    return reach;
  }

  private void checkProgramOrderAcyclic() throws CPAException {
    for (MemoryEvent event : events) {
      if (definiteReach[event.id()].get(event.id())) {
        throw new CPAException("program order of the ordering-consistency encoding is cyclic");
      }
    }
  }

  private BooleanFormula[] computeFullGuards() {
    ImmutableList<ThreadInstance> instances = registry.getInstances();
    BooleanFormula[] creationGuards = new BooleanFormula[instances.size()];
    BooleanFormula[] guards = new BooleanFormula[events.size()];
    for (ThreadInstance instance : instances) {
      if (instance.getId() == ThreadInstance.MAIN_INSTANCE_ID) {
        creationGuards[instance.getId()] = bfmgr.makeTrue();
      } else {
        List<BooleanFormula> createGuards = new ArrayList<>();
        for (int createEventId : instance.getCreateEventIds()) {
          MemoryEvent create = registry.getEvent(createEventId);
          // creators always have smaller instance ids, so their guard is already computed
          createGuards.add(bfmgr.and(creationGuards[create.instanceId()], create.pathGuard()));
        }
        creationGuards[instance.getId()] = bfmgr.or(createGuards);
      }
      for (MemoryEvent event : events) {
        if (event.instanceId() == instance.getId()) {
          guards[event.id()] = bfmgr.and(creationGuards[instance.getId()], event.pathGuard());
        }
      }
    }
    return guards;
  }

  /**
   * The key under which events may access the same memory cell: the variable itself for accesses
   * that can never be aliased, the whole alias region otherwise (there, address-equality
   * constraints decide cell identity precisely).
   */
  static Object cellKey(MemoryEvent pEvent) {
    return pEvent.isRegionAccess() ? pEvent.regionId() : pEvent.memoryLocation();
  }

  /**
   * Same-cell test of two events of the same region: equal object base and equal byte offset. Bases
   * are pairwise distinct so base equality is exact; region events always carry a concrete offset
   * (zero when the access has none), so both components are compared directly.
   */
  BooleanFormula sameAddress(MemoryEvent pFirst, MemoryEvent pSecond) {
    BooleanFormula sameBase = fmgr.makeEqual(pFirst.addressTerm(), pSecond.addressTerm());
    if (pFirst.fill() || pSecond.fill()) {
      // a fill write covers every offset of its base, so only the base has to match
      return sameBase;
    }
    return bfmgr.and(sameBase, fmgr.makeEqual(pFirst.offsetTerm(), pSecond.offsetTerm()));
  }

  private ImmutableListMultimap<Object, MemoryEvent> writesByCell() {
    return events.stream()
        .filter(e -> e.kind() == EventKind.WRITE)
        .collect(ImmutableListMultimap.toImmutableListMultimap(OcEncoder::cellKey, e -> e));
  }

  private ImmutableList<RfPair> buildRfPairs() {
    ImmutableListMultimap<Object, MemoryEvent> writes = writesByCell();
    ImmutableList.Builder<RfPair> result = ImmutableList.builder();
    for (MemoryEvent read : events) {
      if (read.kind() != EventKind.READ) {
        continue;
      }
      for (MemoryEvent write : writes.get(cellKey(read))) {
        if (definiteReach[read.id()].get(write.id())) {
          continue; // the write is in program order after the read
        }
        result.add(
            new RfPair(write, read, bfmgr.makeVariable("__oc_rf_" + write.id() + "_" + read.id())));
      }
    }
    return result.build();
  }

  private ImmutableList<WsPair> buildWsPairs() {
    ImmutableListMultimap<Object, MemoryEvent> writes = writesByCell();
    ImmutableList.Builder<WsPair> result = ImmutableList.builder();
    for (Map.Entry<Object, List<MemoryEvent>> entry : Multimaps.asMap(writes).entrySet()) {
      List<MemoryEvent> sameLocation = entry.getValue();
      for (int i = 0; i < sameLocation.size(); i++) {
        for (int j = i + 1; j < sameLocation.size(); j++) {
          MemoryEvent w1 = sameLocation.get(i);
          MemoryEvent w2 = sameLocation.get(j);
          if (w1.instanceId() == w2.instanceId()
              || definiteReach[w1.id()].get(w2.id())
              || definiteReach[w2.id()].get(w1.id())) {
            continue;
          }
          result.add(
              new WsPair(
                  w1,
                  w2,
                  bfmgr.makeVariable("__oc_ws_" + w1.id() + "_" + w2.id()),
                  bfmgr.makeVariable("__oc_ws_" + w2.id() + "_" + w1.id())));
        }
      }
    }
    return result.build();
  }

  private ImmutableList<CsPair> buildCsPairs(
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    List<Section> sections = new ArrayList<>();
    for (MemoryEvent lock : events) {
      if (lock.kind() != EventKind.LOCK) {
        continue;
      }
      // find the matching unlock (same mutex, nesting depth zero) reachable from the lock in the
      // DAG; a path that ends while still holding the lock keeps it to its last event. Nesting
      // depth at a DAG node is path-independent (states with different lock depths never merge),
      // so a plain visited set (one visit per node) suffices instead of per-path revisits.
      record Item(MemoryEvent event, int depth) {}
      Deque<Item> queue = new ArrayDeque<>();
      Set<Integer> visited = new HashSet<>();
      for (MemoryEvent child : pChildren.get(lock.id())) {
        if (visited.add(child.id())) {
          queue.addLast(new Item(child, 1));
        }
      }
      if (pChildren.get(lock.id()).isEmpty()) {
        sections.add(new Section(lock, lock));
      }
      while (!queue.isEmpty()) {
        Item item = queue.pollFirst();
        int depth = item.depth();
        MemoryEvent event = item.event();
        if (lock.mutexId().equals(event.mutexId())) {
          if (event.kind() == EventKind.LOCK) {
            depth++;
          } else if (event.kind() == EventKind.UNLOCK) {
            depth--;
            if (depth == 0) {
              sections.add(new Section(lock, event));
              continue; // this branch is done
            }
          }
        }
        List<MemoryEvent> children = pChildren.get(event.id());
        if (children.isEmpty()) {
          sections.add(new Section(lock, event));
        }
        for (MemoryEvent child : children) {
          if (visited.add(child.id())) {
            queue.addLast(new Item(child, depth));
          }
        }
      }
    }

    ImmutableList.Builder<CsPair> result = ImmutableList.builder();
    for (int i = 0; i < sections.size(); i++) {
      for (int j = i + 1; j < sections.size(); j++) {
        Section s1 = sections.get(i);
        Section s2 = sections.get(j);
        if (!s1.lock().mutexId().equals(s2.lock().mutexId())
            || s1.lock().instanceId() == s2.lock().instanceId()) {
          continue;
        }
        if (registry.isReadLock(s1.lock().id()) && registry.isReadLock(s2.lock().id())) {
          // two read-locked (rwlock rdlock) sections of the same lock may overlap; only pairs
          // involving a write-locked section exclude each other
          continue;
        }
        // The selector atom names must include the section-END (unlock) ids, not just the
        // lock ids: a single lock can yield multiple mutually-exclusive Section objects (e.g.
        // an internal branch or an early error-exit before the matching unlock). If two such
        // sections of the same lock-pair shared an atom name, getBaseConstraints() would force
        // that atom to imply BOTH sections' (mutually exclusive) unlock guards, making cs-total
        // unsatisfiable and the whole query wrongly UNSAT (reported as safe/TRUE).
        result.add(
            new CsPair(
                s1,
                s2,
                bfmgr.makeVariable(
                    "__oc_cs_"
                        + s1.lock().id()
                        + "_"
                        + s1.unlock().id()
                        + "__"
                        + s2.lock().id()
                        + "_"
                        + s2.unlock().id()),
                bfmgr.makeVariable(
                    "__oc_cs_"
                        + s2.lock().id()
                        + "_"
                        + s2.unlock().id()
                        + "__"
                        + s1.lock().id()
                        + "_"
                        + s1.unlock().id())));
      }
    }
    return result.build();
  }
}

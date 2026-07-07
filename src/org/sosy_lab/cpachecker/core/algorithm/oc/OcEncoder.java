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
import com.google.common.collect.Multimaps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Map;
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
    buildPoEdges(linearizations);
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

    List<BooleanFormula> errorGuards = new ArrayList<>();
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.ERROR) {
        errorGuards.add(getFullGuard(event));
      }
    }
    constraints.add(bfmgr.or(errorGuards));

    // a join completes only if the joined instance terminated normally (THREAD_EXIT); paths that
    // abort the program or are cut at the loop bound have no such event and cannot be joined.
    // An ERROR in the joined instance also satisfies the join: the run is already violating, so
    // the (hypothetical) continuation must not make the model inconsistent.
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

  private ImmutableListMultimap<Integer, MemoryEvent> buildChildren() {
    return events.stream()
        .filter(e -> e.poParentId() != MemoryEvent.NO_EVENT)
        .collect(ImmutableListMultimap.toImmutableListMultimap(MemoryEvent::poParentId, e -> e));
  }

  private ImmutableMap<Integer, List<MemoryEvent>> buildLinearizations(
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    ImmutableMap.Builder<Integer, List<MemoryEvent>> result = ImmutableMap.builder();
    for (ThreadInstance instance : registry.getInstances()) {
      List<MemoryEvent> roots =
          events.stream()
              .filter(
                  e -> e.instanceId() == instance.getId() && e.poParentId() == MemoryEvent.NO_EVENT)
              .collect(ImmutableList.toImmutableList());
      List<MemoryEvent> linear = new ArrayList<>();
      Deque<MemoryEvent> stack = new ArrayDeque<>();
      roots.reversed().forEach(stack::push);
      while (!stack.isEmpty()) {
        MemoryEvent event = stack.pop();
        linear.add(event);
        List<MemoryEvent> children = pChildren.get(event.id());
        for (int i = children.size() - 1; i >= 0; i--) {
          stack.push(children.get(i));
        }
      }
      result.put(instance.getId(), linear);
    }
    return result.buildOrThrow();
  }

  private void buildPoEdges(ImmutableMap<Integer, List<MemoryEvent>> pLinearizations) {
    for (List<MemoryEvent> linear : pLinearizations.values()) {
      for (int i = 0; i + 1 < linear.size(); i++) {
        poEdges.add(new int[] {linear.get(i).id(), linear.get(i + 1).id()});
      }
    }
    for (MemoryEvent event : events) {
      if (event.kind() == EventKind.CREATE || event.kind() == EventKind.JOIN) {
        List<MemoryEvent> other = pLinearizations.get(event.otherInstanceId());
        if (other == null || other.isEmpty()) {
          continue;
        }
        if (event.kind() == EventKind.CREATE) {
          crossPoEdges.add(new CrossPoEdge(event.id(), other.get(0).id(), event.id()));
        } else {
          crossPoEdges.add(
              new CrossPoEdge(other.get(other.size() - 1).id(), event.id(), event.id()));
        }
      }
    }
  }

  /**
   * Edges of the "definite" order, which holds in every execution in which both endpoints are
   * enabled: the exploration-tree order within an instance, everything up to the common ancestor of
   * all creates of an instance before all its events, and all events of an instance before every
   * join of that instance.
   */
  private List<int[]> buildDefiniteOrderEdges(
      ImmutableListMultimap<Integer, MemoryEvent> pChildren) {
    List<int[]> edges = new ArrayList<>();
    for (MemoryEvent event : events) {
      if (event.poParentId() != MemoryEvent.NO_EVENT) {
        edges.add(new int[] {event.poParentId(), event.id()});
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
      if (!instance.getCreateEventIds().isEmpty()) {
        int commonAncestor = -1;
        for (int createEventId : instance.getCreateEventIds()) {
          commonAncestor =
              commonAncestor < 0
                  ? createEventId
                  : commonTreeAncestor(commonAncestor, createEventId);
          if (commonAncestor < 0) {
            break;
          }
        }
        if (commonAncestor >= 0) {
          for (MemoryEvent root : instanceEvents) {
            if (root.poParentId() == MemoryEvent.NO_EVENT) {
              edges.add(new int[] {commonAncestor, root.id()});
            }
          }
        }
      }
      for (MemoryEvent leaf : instanceEvents) {
        if (pChildren.get(leaf.id()).isEmpty()) {
          for (MemoryEvent join : events) {
            if (join.kind() == EventKind.JOIN && join.otherInstanceId() == instance.getId()) {
              edges.add(new int[] {leaf.id(), join.id()});
            }
          }
        }
      }
    }
    return edges;
  }

  /** Deepest common ancestor-or-self of two events in their instance's exploration tree. */
  private int commonTreeAncestor(int pFirst, int pSecond) {
    BitSet ancestors = new BitSet(events.size());
    for (int node = pFirst; node != MemoryEvent.NO_EVENT; node = events.get(node).poParentId()) {
      ancestors.set(node);
    }
    for (int node = pSecond; node != MemoryEvent.NO_EVENT; node = events.get(node).poParentId()) {
      if (ancestors.get(node)) {
        return node;
      }
    }
    return -1;
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

  /** Address equality of two events of the same region. */
  BooleanFormula sameAddress(MemoryEvent pFirst, MemoryEvent pSecond) {
    return fmgr.makeEqual(pFirst.addressTerm(), pSecond.addressTerm());
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
      // find the matching unlock (same mutex, nesting depth zero) on every path below the lock;
      // a path that ends while still holding the lock keeps it to its last event
      record Item(MemoryEvent event, int depth) {}
      Deque<Item> stack = new ArrayDeque<>();
      for (MemoryEvent child : pChildren.get(lock.id())) {
        stack.push(new Item(child, 1));
      }
      if (pChildren.get(lock.id()).isEmpty()) {
        sections.add(new Section(lock, lock));
      }
      while (!stack.isEmpty()) {
        Item item = stack.pop();
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
          stack.push(new Item(child, depth));
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
        result.add(
            new CsPair(
                s1,
                s2,
                bfmgr.makeVariable("__oc_cs_" + s1.lock().id() + "_" + s2.lock().id()),
                bfmgr.makeVariable("__oc_cs_" + s2.lock().id() + "_" + s1.lock().id())));
      }
    }
    return result.build();
  }
}

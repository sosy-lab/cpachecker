// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.CsPair;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.RfPair;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.WsPair;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;

/**
 * Checks whether a model of the base encoding describes a consistent execution: builds the event
 * graph (program order plus the read-from, write-serialization, and critical-section edges whose
 * selector is true in the model) and closes it under the derivation rules — write-serialization: (w
 * rf-> r) and (w' before r) and w' enabled implies (w' before w); from-read: (w rf-> r) and (w
 * before w') and w' enabled implies (r before w'); "before" is reachability in the edge graph, so
 * transitivity needs no own edges. A cycle is an inconsistency; its "reason" (the conjunction of
 * selector variables and guards of the edges it consists of) is returned as a conflict to be
 * excluded.
 */
final class ConsistencyChecker {

  /** One edge of the event graph; a null reason means the edge always holds (program order). */
  private record Edge(int from, int to, @Nullable BooleanFormula reason) {}

  private final OcEncoder encoder;
  private final BooleanFormulaManagerView bfmgr;
  private final int eventCount;
  private final List<List<Edge>> outgoing;
  private final Set<Long> presentEdges = new HashSet<>();
  private BitSet[] reachable;

  private ConsistencyChecker(OcEncoder pEncoder, BooleanFormulaManagerView pBfmgr) {
    encoder = pEncoder;
    bfmgr = pBfmgr;
    eventCount = pEncoder.getEvents().size();
    outgoing = new ArrayList<>(eventCount);
    for (int i = 0; i < eventCount; i++) {
      outgoing.add(new ArrayList<>());
    }
  }

  /** Returns the reasons of all inconsistencies of the model, or an empty list if consistent. */
  static List<BooleanFormula> findConflicts(
      OcEncoder pEncoder, Model pModel, BooleanFormulaManagerView pBfmgr)
      throws InterruptedException {
    ConsistencyChecker checker = new ConsistencyChecker(pEncoder, pBfmgr);
    return checker.run(pModel);
  }

  private List<BooleanFormula> run(Model pModel) {
    boolean[] enabled = new boolean[eventCount];
    for (MemoryEvent event : encoder.getEvents()) {
      enabled[event.id()] = isTrue(pModel.evaluate(encoder.getFullGuard(event)));
    }

    for (int[] edge : encoder.getPoEdges()) {
      addEdge(new Edge(edge[0], edge[1], null));
    }
    // create/join ordering holds only when the creating/joining event is enabled
    for (OcEncoder.CrossPoEdge cross : encoder.getCrossPoEdges()) {
      addEdge(new Edge(cross.from(), cross.to(), encoder.getFullGuard(cross.guardEventId())));
    }
    List<RfPair> activeRf = new ArrayList<>();
    for (RfPair rf : encoder.getRfPairs()) {
      if (isTrue(pModel.evaluate(rf.variable()))) {
        addEdge(new Edge(rf.write().id(), rf.read().id(), rf.variable()));
        activeRf.add(rf);
      }
    }
    for (WsPair ws : encoder.getWsPairs()) {
      if (isTrue(pModel.evaluate(ws.var12()))) {
        addEdge(new Edge(ws.write1().id(), ws.write2().id(), ws.var12()));
      }
      if (isTrue(pModel.evaluate(ws.var21()))) {
        addEdge(new Edge(ws.write2().id(), ws.write1().id(), ws.var21()));
      }
    }
    for (CsPair cs : encoder.getCsPairs()) {
      if (isTrue(pModel.evaluate(cs.var12()))) {
        addEdge(new Edge(cs.section1().unlock().id(), cs.section2().lock().id(), cs.var12()));
      }
      if (isTrue(pModel.evaluate(cs.var21()))) {
        addEdge(new Edge(cs.section2().unlock().id(), cs.section1().lock().id(), cs.var21()));
      }
    }

    ImmutableListMultimap<Integer, MemoryEvent> sameCellWrites = sameCellWrites();
    Object[] addressValues = evaluateAddresses(pModel);

    boolean changed = true;
    while (changed) {
      changed = false;
      computeReachability();
      for (RfPair rf : activeRf) {
        MemoryEvent writeEvent = rf.write();
        int write = writeEvent.id();
        int read = rf.read().id();
        for (MemoryEvent other : sameCellWrites.get(write)) {
          if (!enabled[other.id()]) {
            continue;
          }
          BooleanFormula sideCondition = encoder.getFullGuard(other);
          if (writeEvent.isRegionAccess()) {
            // in the aliasing regime, "same cell" additionally means equal addresses
            if (addressValues[other.id()] == null
                || !addressValues[other.id()].equals(addressValues[write])) {
              continue;
            }
            sideCondition = bfmgr.and(sideCondition, encoder.sameAddress(other, writeEvent));
          }
          if (reachable[other.id()].get(read) && !hasEdge(other.id(), write)) {
            addEdge(
                new Edge(
                    other.id(),
                    write,
                    bfmgr.and(rf.variable(), pathReason(other.id(), read), sideCondition)));
            changed = true;
          }
          if (reachable[write].get(other.id()) && !hasEdge(read, other.id())) {
            addEdge(
                new Edge(
                    read,
                    other.id(),
                    bfmgr.and(rf.variable(), pathReason(write, other.id()), sideCondition)));
            changed = true;
          }
        }
      }
    }

    Set<BooleanFormula> conflicts = new LinkedHashSet<>();
    for (int event = 0; event < eventCount; event++) {
      if (reachable[event].get(event)) {
        conflicts.add(cycleReason(event));
      }
    }
    return new ArrayList<>(conflicts);
  }

  private void addEdge(Edge pEdge) {
    if (presentEdges.add(key(pEdge.from(), pEdge.to()))) {
      outgoing.get(pEdge.from()).add(pEdge);
    }
  }

  private boolean hasEdge(int pFrom, int pTo) {
    return presentEdges.contains(key(pFrom, pTo));
  }

  private long key(int pFrom, int pTo) {
    return (long) pFrom * eventCount + pTo;
  }

  private void computeReachability() {
    reachable = new BitSet[eventCount];
    for (int start = 0; start < eventCount; start++) {
      BitSet visited = new BitSet(eventCount);
      Deque<Integer> worklist = new ArrayDeque<>();
      for (Edge edge : outgoing.get(start)) {
        if (!visited.get(edge.to())) {
          visited.set(edge.to());
          worklist.push(edge.to());
        }
      }
      while (!worklist.isEmpty()) {
        int node = worklist.pop();
        for (Edge edge : outgoing.get(node)) {
          if (!visited.get(edge.to())) {
            visited.set(edge.to());
            worklist.push(edge.to());
          }
        }
      }
      reachable[start] = visited;
    }
  }

  /**
   * Conjunction of the reasons of the edges of one path from pFrom to pTo (there must be one).
   * Reasons of derived edges already contain the reasons of the paths they were derived from.
   */
  private BooleanFormula pathReason(int pFrom, int pTo) {
    Edge[] parent = new Edge[eventCount];
    Deque<Integer> worklist = new ArrayDeque<>();
    BitSet visited = new BitSet(eventCount);
    for (Edge edge : outgoing.get(pFrom)) {
      if (!visited.get(edge.to())) {
        visited.set(edge.to());
        parent[edge.to()] = edge;
        worklist.add(edge.to());
      }
    }
    while (!worklist.isEmpty()) {
      int node = worklist.poll();
      if (node == pTo) {
        break;
      }
      for (Edge edge : outgoing.get(node)) {
        if (!visited.get(edge.to())) {
          visited.set(edge.to());
          parent[edge.to()] = edge;
          worklist.add(edge.to());
        }
      }
    }

    List<BooleanFormula> reasons = new ArrayList<>();
    int node = pTo;
    do {
      Edge edge = parent[node];
      if (edge.reason() != null) {
        reasons.add(edge.reason());
      }
      node = edge.from();
    } while (node != pFrom);
    return bfmgr.and(reasons);
  }

  /** The reason of a (non-empty) cycle from pEvent back to itself. */
  private BooleanFormula cycleReason(int pEvent) {
    return pathReason(pEvent, pEvent);
  }

  private ImmutableListMultimap<Integer, MemoryEvent> sameCellWrites() {
    ImmutableListMultimap.Builder<Integer, MemoryEvent> result = ImmutableListMultimap.builder();
    List<MemoryEvent> writes =
        encoder.getEvents().stream().filter(e -> e.kind() == EventKind.WRITE).toList();
    for (MemoryEvent write : writes) {
      for (MemoryEvent other : writes) {
        if (other.id() != write.id() && OcEncoder.cellKey(write).equals(OcEncoder.cellKey(other))) {
          result.put(write.id(), other);
        }
      }
    }
    return result.build();
  }

  /** The model's (base, offset) value of every region event's address, by event id (else null). */
  private Object[] evaluateAddresses(Model pModel) {
    Object[] values = new Object[eventCount];
    for (MemoryEvent event : encoder.getEvents()) {
      if (event.isRegionAccess()) {
        Object base = pModel.evaluate(event.addressTerm());
        Object offset = event.offsetTerm() == null ? null : pModel.evaluate(event.offsetTerm());
        values[event.id()] = List.of(base == null ? "?" : base, offset == null ? 0 : offset);
      }
    }
    return values;
  }

  private static boolean isTrue(Boolean pValue) {
    return pValue != null && pValue;
  }
}

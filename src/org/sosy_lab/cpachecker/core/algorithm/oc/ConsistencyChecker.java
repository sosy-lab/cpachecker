// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.oc;

import com.google.common.collect.ImmutableListMultimap;
import java.math.BigInteger;
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

    // Only enabled events are part of this model's execution; edges touching a disabled event are
    // not real happens-before and must not enter the graph. Every edge's reason must fully imply
    // its presence (including the enabled-ness of its endpoints), so the conflict clause it feeds
    // excludes only models that genuinely contain the cycle.
    for (int[] edge : encoder.getProgramOrderDagEdges()) {
      if (enabled[edge[0]] && enabled[edge[1]]) {
        addEdge(new Edge(edge[0], edge[1], guardsOf(edge[0], edge[1])));
      }
    }
    // create/join ordering holds only when the creating/joining event is enabled
    for (OcEncoder.CrossPoEdge cross : encoder.getCrossPoEdges()) {
      if (enabled[cross.from()] && enabled[cross.to()]) {
        addEdge(
            new Edge(
                cross.from(),
                cross.to(),
                bfmgr.and(
                    encoder.getFullGuard(cross.guardEventId()),
                    guardsOf(cross.from(), cross.to()))));
      }
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
    BigInteger[] addressValues = evaluateAddresses(pModel);

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
            if (!sameCellInModel(other, writeEvent, addressValues)) {
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

  /** The presence condition of a program-order edge: both of its endpoints are enabled. */
  private BooleanFormula guardsOf(int pFrom, int pTo) {
    return bfmgr.and(encoder.getFullGuard(pFrom), encoder.getFullGuard(pTo));
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

  /**
   * The model's full byte address ({@code base + offset}) of every region event, by event id, or
   * null if it did not evaluate to a concrete integer. This mirrors {@link OcEncoder#sameAddress}:
   * in the flat memory layout a cell is identified by one address, so an interior access whose
   * address is a combined {@code base + offset} is compared on the same footing as a base/offset
   * pair.
   */
  private BigInteger[] evaluateAddresses(Model pModel) {
    BigInteger[] values = new BigInteger[eventCount];
    for (MemoryEvent event : encoder.getEvents()) {
      if (event.isRegionAccess()) {
        Object base = pModel.evaluate(event.addressTerm());
        Object offset = event.offsetTerm() == null ? null : pModel.evaluate(event.offsetTerm());
        if (base instanceof BigInteger baseValue) {
          BigInteger offsetValue = offset instanceof BigInteger o ? o : BigInteger.ZERO;
          values[event.id()] = baseValue.add(offsetValue);
        }
      }
    }
    return values;
  }

  /**
   * Whether two region writes touch the same cell in the model. A fill write stands for the whole
   * object {@code [base, base + fillSize)}, so it is the same cell as any access whose full address
   * falls in that range; two ordinary accesses match iff their full addresses are equal.
   */
  private boolean sameCellInModel(
      MemoryEvent pFirst, MemoryEvent pSecond, BigInteger[] pAddresses) {
    BigInteger a = pAddresses[pFirst.id()];
    BigInteger b = pAddresses[pSecond.id()];
    if (a == null || b == null) {
      return false;
    }
    if (pFirst.fill()) {
      return covers(a, pFirst.fillSizeBytes(), b);
    }
    if (pSecond.fill()) {
      return covers(b, pSecond.fillSizeBytes(), a);
    }
    return a.equals(b);
  }

  /**
   * Whether {@code pAddress} is in the fill's covered range {@code [fillBase, fillBase + size)}.
   */
  private static boolean covers(BigInteger pFillBase, long pSize, BigInteger pAddress) {
    return pAddress.compareTo(pFillBase) >= 0
        && pAddress.compareTo(pFillBase.add(BigInteger.valueOf(pSize))) < 0;
  }

  private static boolean isTrue(Boolean pValue) {
    return pValue != null && pValue;
  }
}

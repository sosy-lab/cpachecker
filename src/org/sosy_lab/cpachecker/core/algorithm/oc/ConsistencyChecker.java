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
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.CsPair;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.PoEdge;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.RfPair;
import org.sosy_lab.cpachecker.core.algorithm.oc.OcEncoder.WsPair;
import org.sosy_lab.cpachecker.cpa.oc.EventKind;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;

/**
 * Checks whether a model of the base encoding describes a consistent execution.
 *
 * <p>The check proceeds in two steps:
 *
 * <ul>
 *   <li>Build the event graph from the edges that this model selects:
 *       <ul>
 *         <li>program order, which holds unconditionally between two events of the same thread;
 *         <li>read-from, write-serialization, and critical-section edges, each of them included
 *             only if its selector variable evaluates to true in the model.
 *       </ul>
 *   <li>Close the graph under the derivation rules. Here "{@code x} before {@code y}" means that
 *       {@code y} is reachable from {@code x} in the graph, so transitivity needs no edges of its
 *       own:
 *       <ul>
 *         <li>write-serialization: {@code w rf-> r} and {@code w' before r} and {@code w'} enabled
 *             implies {@code w' before w};
 *         <li>from-read: {@code w rf-> r} and {@code w before w'} and {@code w'} enabled implies
 *             {@code r before w'}.
 *       </ul>
 * </ul>
 *
 * <p>A cycle in the closed graph is an inconsistency. Its <em>reason</em> — the conjunction of the
 * selector variables and guards of the edges the cycle consists of — is returned as a conflict to
 * be excluded from the following solver queries.
 */
final class ConsistencyChecker {

  /**
   * One edge of the event graph. {@code reasons} are the conditions under which the edge is
   * present, to be read conjunctively; an empty list means the edge always holds.
   */
  record Edge(int from, int to, ImmutableList<BooleanFormula> reasons) {}

  private final OcEncoder encoder;
  private final int eventCount;
  private final List<List<Edge>> outgoing;
  private final Set<Long> presentEdges = new HashSet<>();
  private BitSet[] reachable;

  private ConsistencyChecker(OcEncoder pEncoder) {
    encoder = pEncoder;
    eventCount = pEncoder.getEvents().size();
    outgoing = new ArrayList<>(eventCount);
    for (int i = 0; i < eventCount; i++) {
      outgoing.add(new ArrayList<>());
    }
  }

  /**
   * The cycles of the closed event graph, each as the list of edges it consists of, or an empty
   * list if the model is consistent. The caller turns a cycle into a conflict clause by conjoining
   * the {@link Edge#reasons()} of its edges; this class needs no solver context of its own.
   */
  static ImmutableList<ImmutableList<Edge>> findCycles(OcEncoder pEncoder, Model pModel) {
    ConsistencyChecker checker = new ConsistencyChecker(pEncoder);
    return checker.run(pModel);
  }

  private ImmutableList<ImmutableList<Edge>> run(Model pModel) {
    ImmutableSet<MemoryEvent> enabled = encoder.enabledIn(pModel);

    // Only enabled events are part of this model's execution; edges touching a disabled event are
    // not real happens-before and must not enter the graph. Every edge's reason must fully imply
    // its presence (including the enabled-ness of its endpoints), so the conflict clause it feeds
    // excludes only models that genuinely contain the cycle.
    for (PoEdge edge : encoder.getProgramOrderDagEdges()) {
      if (enabled.contains(edge.from()) && enabled.contains(edge.to())) {
        int from = edge.from().id();
        int to = edge.to().id();
        addEdge(new Edge(from, to, guardsOf(from, to)));
      }
    }
    // create/join ordering holds only when the creating/joining event is enabled
    for (OcEncoder.CrossPoEdge cross : encoder.getCrossPoEdges()) {
      int from = cross.from().id();
      int to = cross.to().id();
      if (enabled.contains(cross.from()) && enabled.contains(cross.to())) {
        addEdge(
            new Edge(
                from,
                to,
                ImmutableList.<BooleanFormula>builder()
                    .add(encoder.getFullGuard(cross.guardEvent()))
                    .addAll(guardsOf(from, to))
                    .build()));
      }
    }
    List<RfPair> activeRf = new ArrayList<>();
    for (RfPair rf : encoder.getRfPairs()) {
      if (isTrue(pModel.evaluate(rf.variable()))) {
        addEdge(new Edge(rf.write().id(), rf.read().id(), ImmutableList.of(rf.variable())));
        activeRf.add(rf);
      }
    }
    for (WsPair ws : encoder.getWsPairs()) {
      if (isTrue(pModel.evaluate(ws.var12()))) {
        addEdge(new Edge(ws.write1().id(), ws.write2().id(), ImmutableList.of(ws.var12())));
      }
      if (isTrue(pModel.evaluate(ws.var21()))) {
        addEdge(new Edge(ws.write2().id(), ws.write1().id(), ImmutableList.of(ws.var21())));
      }
    }
    for (CsPair cs : encoder.getCsPairs()) {
      if (isTrue(pModel.evaluate(cs.var12()))) {
        addEdge(
            new Edge(
                cs.section1().unlock().id(),
                cs.section2().lock().id(),
                ImmutableList.of(cs.var12())));
      }
      if (isTrue(pModel.evaluate(cs.var21()))) {
        addEdge(
            new Edge(
                cs.section2().unlock().id(),
                cs.section1().lock().id(),
                ImmutableList.of(cs.var21())));
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
          if (!enabled.contains(other)) {
            continue;
          }
          ImmutableList.Builder<BooleanFormula> sideBuilder = ImmutableList.builder();
          sideBuilder.add(encoder.getFullGuard(other));
          if (writeEvent.isRegionAccess()) {
            // in the aliasing regime, "same cell" additionally means equal addresses
            if (!sameCellInModel(other, writeEvent, addressValues)) {
              continue;
            }
            sideBuilder.add(encoder.sameAddress(other, writeEvent));
          }
          ImmutableList<BooleanFormula> sideCondition = sideBuilder.build();
          if (reachable[other.id()].get(read) && !hasEdge(other.id(), write)) {
            addEdge(
                new Edge(
                    other.id(),
                    write,
                    ImmutableList.<BooleanFormula>builder()
                        .add(rf.variable())
                        .addAll(pathReasons(other.id(), read))
                        .addAll(sideCondition)
                        .build()));
            changed = true;
          }
          if (reachable[write].get(other.id()) && !hasEdge(read, other.id())) {
            addEdge(
                new Edge(
                    read,
                    other.id(),
                    ImmutableList.<BooleanFormula>builder()
                        .add(rf.variable())
                        .addAll(pathReasons(write, other.id()))
                        .addAll(sideCondition)
                        .build()));
            changed = true;
          }
        }
      }
    }

    Set<ImmutableList<BooleanFormula>> seen = new LinkedHashSet<>();
    ImmutableList.Builder<ImmutableList<Edge>> cycles = ImmutableList.builder();
    for (int event = 0; event < eventCount; event++) {
      if (reachable[event].get(event)) {
        ImmutableList<Edge> cycle = path(event, event);
        if (seen.add(reasonsOf(cycle))) {
          cycles.add(cycle);
        }
      }
    }
    return cycles.build();
  }

  /** The conditions of all edges of {@code pEdges}, to be read conjunctively. */
  static ImmutableList<BooleanFormula> reasonsOf(List<Edge> pEdges) {
    return pEdges.stream()
        .flatMap(edge -> edge.reasons().stream())
        .collect(ImmutableList.toImmutableList());
  }

  /** The presence condition of a program-order edge: both of its endpoints are enabled. */
  private ImmutableList<BooleanFormula> guardsOf(int pFrom, int pTo) {
    return ImmutableList.of(encoder.getFullGuard(pFrom), encoder.getFullGuard(pTo));
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
  private ImmutableList<BooleanFormula> pathReasons(int pFrom, int pTo) {
    return reasonsOf(path(pFrom, pTo));
  }

  /** The edges of one path from pFrom to pTo (there must be one). */
  private ImmutableList<Edge> path(int pFrom, int pTo) {
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

    List<Edge> edges = new ArrayList<>();
    int node = pTo;
    do {
      Edge edge = parent[node];
      edges.add(edge);
      node = edge.from();
    } while (node != pFrom);
    return ImmutableList.copyOf(edges).reverse();
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

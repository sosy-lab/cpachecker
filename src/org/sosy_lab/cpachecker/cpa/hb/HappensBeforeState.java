// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * We store the following information about each state: * set of reads per memory address * set of
 * (still visible) writes per memory address
 */
record HappensBeforeState(
    Map<Integer, Pair<LocationState, CallstackState>> threads,
    Map<String, Integer> cssaCounters,
    int prevActiveThread,
    int nextActiveThread,
    int eid,
    Map<Integer, MemoryEvent> lastEvent,
    ExecutionGraph lastG,
    ExecutionGraph g)
    implements AbstractState, AbstractStateWithLocations, Graphable {

  static HappensBeforeState empty() {
    return new HappensBeforeState(
        ImmutableMap.of(),
        ImmutableMap.of(),
        -1,
        0,
        0,
        ImmutableMap.of(),
        ExecutionGraph.empty(),
        ExecutionGraph.empty());
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return ImmutableList.<CFANode>builder()
        .addAll(
            nextActiveThread >= 0
                ? threads.get(nextActiveThread).getFirstNotNull().getLocationNodes()
                : ImmutableList.of())
        .addAll(
            prevActiveThread >= 0
                ? threads.get(prevActiveThread).getFirstNotNull().getLocationNodes()
                : ImmutableList.of())
        .build();
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    if (g.pendingRf().isEmpty()) {
      return nextActiveThread >= 0
          ? StreamSupport.stream(
                  threads.get(nextActiveThread).getFirstNotNull().getOutgoingEdges().spliterator(),
                  false)
              .map(edge -> HappensBeforeEdgeTools.clone(edge, nextActiveThread, cssaCounters))
              .collect(ImmutableList.toImmutableList())
          : ImmutableList.of();
    } else {
      return ImmutableList.of(
          HappensBeforeEdgeTools.createAssume(
              threads.get(nextActiveThread).getFirstNotNull().getLocationNode(), g));
    }
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    if (lastG.pendingRf().isEmpty()) {
      return prevActiveThread >= 0
          ? StreamSupport.stream(
                  threads.get(prevActiveThread).getFirstNotNull().getIncomingEdges().spliterator(),
                  false)
              .map(edge -> HappensBeforeEdgeTools.clone(edge, prevActiveThread, cssaCounters))
              .collect(ImmutableList.toImmutableList())
          : ImmutableList.of();
    } else {
      return ImmutableList.of(
          HappensBeforeEdgeTools.createAssume(
              threads.get(nextActiveThread).getFirstNotNull().getLocationNode(), lastG));
    }
  }

  @Override
  public String toDOTLabel() {
    final var threadsPrint = ImmutableMap.<Integer, CFANode>builder();
    threads.forEach(
        (pInteger, pLocationStateCallstackStatePair) ->
            threadsPrint.put(
                pInteger, pLocationStateCallstackStatePair.getFirstNotNull().getLocationNode()));
    return "%s\\nnext: %s\\nExecGraph: %s".formatted(threads, nextActiveThread, g.toDOTLabel());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  // copy()-like methods

  Collection<HappensBeforeState> addRead(final int thread, final CVariableDeclaration var) {
    final var newMemEvent =
        new MemoryEvent(MemoryEventType.READ, var, thread, eid, lastEvent.get(thread));
    final var newLastEvent =
        ImmutableMap.<Integer, MemoryEvent>builder()
            .putAll(lastEvent)
            .put(thread, newMemEvent)
            .buildKeepingLast();
    return g.addRead(newMemEvent).stream()
        .map(
            it ->
                new HappensBeforeState(
                    threads,
                    cssaCounters,
                    prevActiveThread,
                    nextActiveThread,
                    eid + 1,
                    newLastEvent,
                    g,
                    it))
        .toList();
  }

  Collection<HappensBeforeState> addWrite(final int thread, final CVariableDeclaration var) {
    final var newMemEvent =
        new MemoryEvent(MemoryEventType.WRITE, var, thread, eid, lastEvent.get(thread));
    final var newLastEvent =
        ImmutableMap.<Integer, MemoryEvent>builder()
            .putAll(lastEvent)
            .put(thread, newMemEvent)
            .buildKeepingLast();
    return g.addWrite(newMemEvent).stream()
        .map(
            it ->
                new HappensBeforeState(
                    threads,
                    cssaCounters,
                    prevActiveThread,
                    nextActiveThread,
                    eid + 1,
                    newLastEvent,
                    g,
                    it))
        .toList();
  }

  HappensBeforeState updateThread(
      final int idx,
      final LocationState pLocationState,
      final CallstackState pCallstackState,
      final int nextIdx,
      final Map<String, Integer> pCssaCounters) {
    final var newThreads =
        ImmutableMap.<Integer, Pair<LocationState, CallstackState>>builder()
            .putAll(threads)
            .put(idx, Pair.of(pLocationState, pCallstackState))
            .buildKeepingLast();
    return new HappensBeforeState(newThreads, pCssaCounters, idx, nextIdx, eid, lastEvent, g, g);
  }

  HappensBeforeState addThread(
      final int threadId,
      final int parentThreadId,
      final LocationState pLocationState,
      final CallstackState pCallstackState) {
    final var newThreads =
        ImmutableMap.<Integer, Pair<LocationState, CallstackState>>builder()
            .putAll(threads)
            .put(threadId, Pair.of(pLocationState, pCallstackState))
            .buildKeepingLast();
    if (lastEvent.get(parentThreadId) != null) {
      final var newLastEvent =
          ImmutableMap.<Integer, MemoryEvent>builder()
              .putAll(lastEvent)
              .put(threadId, lastEvent.get(parentThreadId))
              .buildKeepingLast();
      return new HappensBeforeState(
          newThreads, cssaCounters, prevActiveThread, nextActiveThread, eid, newLastEvent, g, g);
    } else {
      return new HappensBeforeState(
          newThreads, cssaCounters, prevActiveThread, nextActiveThread, eid, lastEvent, g, g);
    }
  }

  HappensBeforeState clearPending() {
    return new HappensBeforeState(
        threads,
        cssaCounters,
        prevActiveThread,
        nextActiveThread,
        eid,
        lastEvent,
        lastG,
        g.clearPending());
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import static org.sosy_lab.cpachecker.cpa.hb.HappensBeforeUtils.extendMapOfSets;
import static org.sosy_lab.cpachecker.cpa.hb.HappensBeforeUtils.subtractMapOfSets;
import static org.sosy_lab.cpachecker.util.CFAUtils.allEnteringEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.allLeavingEdges;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * We store the following information about each state:
 *   * set of revisitable reads per memory address
 *   * set of writes per memory address
 */

record HappensBeforeState(
    Map<MemoryLocation, Set<MemoryEvent>> revisitableReads,
    Map<MemoryLocation, Set<MemoryEvent>> writes,
    Map<Integer, Pair<LocationState, CallstackState>> threads,
    int prevActiveThread,
    int nextActiveThread
) implements AbstractState, AbstractStateWithLocations, Graphable {

  static HappensBeforeState empty() {
    return new HappensBeforeState(
        ImmutableMap.of(),
        ImmutableMap.of(),
        ImmutableMap.of(),
        -1,
        0
    );
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return ImmutableList.<CFANode>builder()
        .addAll(nextActiveThread >= 0 ? threads.get(nextActiveThread).getFirstNotNull().getLocationNodes() : List.of())
        .addAll(prevActiveThread >= 0 ? threads.get(prevActiveThread).getFirstNotNull().getLocationNodes() : List.of())
        .build();
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return nextActiveThread >= 0
        ? StreamSupport.stream(
                threads.get(nextActiveThread).getFirstNotNull().getOutgoingEdges().spliterator(),
                false)
            .map(edge -> EdgeCloningUtils.clone(edge, nextActiveThread))
            .collect(Collectors.toList())
        : Collections.emptyList();
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    return prevActiveThread >= 0
           ? StreamSupport.stream(
            threads.get(prevActiveThread).getFirstNotNull().getIncomingEdges().spliterator(),
            false)
               .map(edge -> EdgeCloningUtils.clone(edge, prevActiveThread))
               .collect(Collectors.toList())
           : Collections.emptyList();
  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  // copy()-like methods

  public HappensBeforeState addRevisitableRead(final MemoryLocation var, final MemoryEvent event) {
    final var newRevisitableReads = extendMapOfSets(revisitableReads, var, event);
    return new HappensBeforeState(newRevisitableReads, writes, threads, prevActiveThread, nextActiveThread);
  }

  public HappensBeforeState removeRevisitableRead(final MemoryLocation var, final MemoryEvent event) {
    final var newRevisitableReads = subtractMapOfSets(revisitableReads, var, event);
    return new HappensBeforeState(newRevisitableReads, writes, threads, prevActiveThread, nextActiveThread);
  }

  public HappensBeforeState addWrite(final MemoryLocation var, final MemoryEvent event) {
    final var newWrites = extendMapOfSets(writes, var, event);
    return new HappensBeforeState(revisitableReads, newWrites, threads, prevActiveThread, nextActiveThread);
  }

  public HappensBeforeState updateThread(final int idx, final LocationState pLocationState, final CallstackState pCallstackState, final int nextIdx) {
    final var newThreads = ImmutableMap.<Integer, Pair<LocationState, CallstackState>>builder().putAll(threads).put(idx, Pair.of(pLocationState, pCallstackState)).buildKeepingLast();
    return new HappensBeforeState(revisitableReads, writes, newThreads, idx, nextIdx);
  }

  public HappensBeforeState addThread(final int idx, final LocationState pLocationState, final CallstackState pCallstackState) {
    final var newThreads = ImmutableMap.<Integer, Pair<LocationState, CallstackState>>builder().putAll(threads).put(idx, Pair.of(pLocationState, pCallstackState)).buildKeepingLast();
    return new HappensBeforeState(revisitableReads, writes, newThreads, prevActiveThread, nextActiveThread);
  }
}

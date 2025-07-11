// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static org.sosy_lab.cpachecker.util.CFAUtils.allLeavingEdges;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.Pair;

public record OrderingConsistencyState(
    Optional<Integer> pid,
    ImmutableMap<Integer, OrderingConsistencyThreadState> waitingThreads,
    int uniqueCounter
) implements AbstractState, AbstractStateWithLocations, Graphable {
  static OrderingConsistencyState empty() {
    return new OrderingConsistencyState(Optional.empty(), ImmutableMap.of(), -1);
  }

  boolean canMerge(OrderingConsistencyState other) {
    return pid == other.pid && waitingThreads.equals(other.waitingThreads);
  }

  Optional<Pair<Integer, OrderingConsistencyThreadState>> nextThreadToStep() {
    final Predicate<OrderingConsistencyThreadState> hasNext =
        state ->
            !allLeavingEdges(state.pLocationState().getLocationNode()).isEmpty();

    // if the current pid can continue, we keep it
    if(pid.isPresent() && hasNext.test(waitingThreads.get(pid.get()))) {
      return Optional.of(Pair.of(pid.orElseThrow(), waitingThreads.get(pid.get())));
    }
    // otherwise, we choose the first suitable candidate (doesn't matter which)
    for (Entry<Integer, OrderingConsistencyThreadState> entry : waitingThreads.entrySet()) {
      final int nextPid = entry.getKey();
      final OrderingConsistencyThreadState threadState = entry.getValue();
      if(hasNext.test(threadState)) {
        return Optional.of(Pair.of(nextPid, threadState));
      }
    }
    return Optional.empty();
  }

  OrderingConsistencyState addNewThread(LocationState pInitialLoc, CallstackState pInitialStack) {
    final int newPid = uniqueCounter + 1;
    final ImmutableMap<Integer, OrderingConsistencyThreadState> newWaiting =
        ImmutableMap.<Integer, OrderingConsistencyThreadState>builder().putAll(waitingThreads).put(newPid, new OrderingConsistencyThreadState(pInitialLoc, pInitialStack)).buildKeepingLast();
    return new OrderingConsistencyState(pid, newWaiting, newPid);
  }

  public OrderingConsistencyState stepThread(int pPid, LocationState pNextLoc, CallstackState pNextStack, int newCounter) {
    assert waitingThreads.containsKey(pPid) : "waitingThreads must contain pid to step " + pPid;
    final ImmutableMap<Integer, OrderingConsistencyThreadState> newWaiting =
        ImmutableMap.<Integer, OrderingConsistencyThreadState>builder().putAll(waitingThreads).put(pPid, new OrderingConsistencyThreadState(pNextLoc, pNextStack)).buildKeepingLast();
    return new OrderingConsistencyState(Optional.of(pPid), newWaiting, newCounter);
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return pid.map(
        pid -> waitingThreads.get(pid).pLocationState()
            .getLocationNodes()).orElseGet(ImmutableList::of);
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return nextThreadToStep().map(pair -> {
      final var ret = ImmutableList.<CFAEdge>builder();
      for (CFAEdge outgoingEdge : pair.getSecondNotNull().pLocationState().getOutgoingEdges()) {
        ret.add(EdgeCloner.clone(outgoingEdge, pair.getFirstNotNull(), uniqueCounter));
      }
      return ret.build();
    }).orElseGet(ImmutableList::of);
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    // TODO: how to parameterize EdgeTools::clone() here?
    return pid.map(pid -> waitingThreads.get(pid).pLocationState().getIncomingEdges()).orElseGet(ImmutableList::of);
  }

  @Override
  public String toDOTLabel() {
    return "pid: %s\\nwaiting threads: %s".formatted(pid, waitingThreads.entrySet());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}

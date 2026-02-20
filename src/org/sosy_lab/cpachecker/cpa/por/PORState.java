// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class PORState implements AbstractState, AbstractStateWithLocations, Graphable {

  private final ImmutableMap<Integer, PORThreadState> threads;

  /** Transient mapping from cloned outgoing edges to their originating thread PID. */
  private final Map<CFAEdge, Integer> edgePidMap = new HashMap<>();

  PORState(ImmutableMap<Integer, PORThreadState> threads) {
    this.threads = threads;
  }

  static PORState empty() {
    return new PORState(ImmutableMap.of());
  }

  public ImmutableMap<Integer, PORThreadState> threads() {
    return threads;
  }

  boolean canMerge(PORState other) {
    return threads.equals(other.threads);
  }

  PORState addNewThread(
      LocationState pInitialLoc,
      CallstackState pInitialStack,
      PathFormula pEmptyFormula,
      Optional<MemoryEvent> pHbBeforeEvent) {
    final int newPid = threads.size();
    final List<MemoryEvent> eventList =
        pHbBeforeEvent
            .map(pMemoryEvent -> ImmutableList.of(pMemoryEvent))
            .orElse(ImmutableList.of());
    final ImmutableMap<Integer, PORThreadState> newThreads =
        ImmutableMap.<Integer, PORThreadState>builder()
            .putAll(threads)
            .put(
                newPid,
                new PORThreadState(pInitialLoc, pInitialStack, pEmptyFormula, eventList))
            .buildKeepingLast();
    return new PORState(newThreads);
  }

  public PORState stepThread(
      int pPid,
      LocationState pNextLoc,
      CallstackState pNextStack,
      PathFormula pNextFormula,
      List<MemoryEvent> pAccesses) {
    assert threads.containsKey(pPid) : "threads must contain pid to step " + pPid;
    final ImmutableMap.Builder<Integer, PORThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pPid) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(
        pPid, new PORThreadState(pNextLoc, pNextStack, pNextFormula, pAccesses));
    return new PORState(newThreads.buildKeepingLast());
  }

  /**
   * Returns the thread PID that produced the given cloned outgoing edge. This mapping is populated
   * during {@link #getOutgoingEdges()}.
   */
  Integer getEdgePid(CFAEdge edge) {
    return edgePidMap.get(edge);
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    for (PORThreadState threadState : threads.values()) {
      for (CFANode node : threadState.pLocationState().getLocationNodes()) {
        nodes.add(node);
      }
    }
    return nodes.build();
  }

  /**
   * Returns outgoing edges for <b>all</b> threads that have leaving edges, exploring all possible
   * interleavings.
   */
  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    edgePidMap.clear();
    ImmutableList.Builder<CFAEdge> ret = ImmutableList.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      int pid = entry.getKey();
      PORThreadState threadState = entry.getValue();
      if (!threadState.pLocationState().getLocationNode().getAllLeavingEdges().isEmpty()) {
        for (CFAEdge outgoingEdge : threadState.pLocationState().getOutgoingEdges()) {
          CFAEdge cloned = PorEdgeCloner.clone(outgoingEdge, pid, this);
          edgePidMap.put(cloned, pid);
          ret.add(cloned);
        }
      }
    }
    return ret.build();
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    for (PORThreadState threadState : threads.values()) {
      for (CFAEdge edge : threadState.pLocationState().getIncomingEdges()) {
        edges.add(edge);
      }
    }
    return edges.build();
  }

  @Override
  public String toDOTLabel() {
    return "threads: %s".formatted(threads.entrySet());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PORState other)) {
      return false;
    }
    return Objects.equals(threads, other.threads);
  }

  @Override
  public int hashCode() {
    return threads.hashCode();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.oc.MemoryEvent;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class PORState implements AbstractState, AbstractStateWithLocations,
                                 AbstractStateWithLocation, Graphable {

  private final ImmutableMap<Integer, PORThreadState> threads;

  /**
   * Transient mapping from cloned outgoing edges to their originating thread PID.
   */
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
      PathFormula pEmptyFormula) {
    final int newPid = threads.size();
    final ImmutableMap<Integer, PORThreadState> newThreads =
        ImmutableMap.<Integer, PORThreadState>builder()
            .putAll(threads)
            .put(
                newPid,
                new PORThreadState(pInitialLoc, pInitialStack, pEmptyFormula))
            .buildKeepingLast();
    return new PORState(newThreads);
  }

  public PORState stepThread(
      int pPid,
      LocationState pNextLoc,
      CallstackState pNextStack,
      PathFormula pNextFormula) {
    assert threads.containsKey(pPid) : "threads must contain pid to step " + pPid;
    final ImmutableMap.Builder<Integer, PORThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pPid) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(
        pPid, new PORThreadState(pNextLoc, pNextStack, pNextFormula));
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


  // needed, otherwise cannot sort states (gives exception).
  @Override
  public CFANode getLocationNode() {
    return getLocationNodes().iterator().next();
  }

  /**
   * Returns outgoing edges for <b>all</b> threads that have leaving edges, exploring all possible
   * interleavings.
   */
  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return getAllThreadOutgoingEdges();
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


  // POR algorithm

  private ImmutableCollection<CFAEdge> getAllThreadOutgoingEdges() {
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

  private ImmutableCollection<CFAEdge> getMinimalSourceSet() {
    ImmutableCollection<CFAEdge> minimalSourceSet = null;
    final var allOutgoingEdges = getAllThreadOutgoingEdges();
    final var sourceSetFirstActions = getSourceSetFirstActions(allOutgoingEdges);
    for (final var firstActions : sourceSetFirstActions) {
      final var sourceSet = calculateSourceSet(allOutgoingEdges, firstActions);
      if (minimalSourceSet == null || sourceSet.size() < minimalSourceSet.size()) {
        minimalSourceSet = sourceSet;
      }
    }
    return minimalSourceSet;
  }

  private ImmutableCollection<ImmutableCollection<CFAEdge>> getSourceSetFirstActions(Iterable<CFAEdge> allOutgoingEdges) {
    final var enabledThreads = threads.entrySet().stream()
        .filter(entry -> !entry.getValue().pLocationState().getLocationNode().getAllLeavingEdges()
            .isEmpty())
        .map(Entry::getKey)
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(enabledThreads);
    final ImmutableList.Builder<ImmutableCollection<CFAEdge>> sourceSetFirstActions =
        ImmutableList.builder();
    for (final var pid : enabledThreads) {
      final ImmutableList.Builder<CFAEdge> firstActions = ImmutableList.builder();
      for (final var edge : allOutgoingEdges) {
        if (pid.equals(edgePidMap.get(edge))) {
          firstActions.add(edge);
        }
      }
      // TODO check mutex blocks
      sourceSetFirstActions.add(firstActions.build());
    }
    return sourceSetFirstActions.build();
  }

  private ImmutableCollection<CFAEdge> calculateSourceSet(
      ImmutableCollection<CFAEdge> allOutgoingEdges,
      ImmutableCollection<CFAEdge> firstActions) {
    final var sourceSet = new ArrayList<CFAEdge>();
    final var otherEdges = new ArrayList<CFAEdge>();
    for (final var edge : allOutgoingEdges) {
      if (firstActions.contains(edge)) {
        if (edge.getPredecessor().isLoopStart()) {
          return allOutgoingEdges;
        }
        sourceSet.add(edge);
      } else {
        otherEdges.add(edge);
      }
    }

    var addedNewEdge = true;
    while (addedNewEdge) {
      addedNewEdge = false;
      final ImmutableSet.Builder<CFAEdge> edgesToRemove = ImmutableSet.builder();
      for (final var edge : otherEdges) {
        if (sourceSet.stream().anyMatch(s -> dependent(s, edge))) {
          if (edge.getPredecessor().isLoopStart()) {
            return allOutgoingEdges;
          }
          sourceSet.add(edge);
          edgesToRemove.add(edge);
          addedNewEdge = true;
        }
      }
      otherEdges.removeAll(edgesToRemove.build());
    }

    return ImmutableList.copyOf(sourceSet);
  }

  private boolean dependent(CFAEdge sourceSetEdge, CFAEdge edge) {
    if (edgePidMap.get(sourceSetEdge).equals(edgePidMap.get(edge))) {
      return true;
    }

    final var sourceSetVars = getUsedGlobalVars(sourceSetEdge);
    final var influencedVars = getInfluencedGlobalVars(edge);
    if (intersects(sourceSetVars, influencedVars)) {
      return true;
    }

    // TODO handle pointers
    // first simple solution could be that we return true if any dereference is involved

    return false;
  }

  private Collection<?> getDirectlyUsedGlobalVars(CFAEdge edge) {
    // TODO implement
    // collect directly used vars by the cfa edge
    return ImmutableList.of();
  }

  private Collection<?> getUsedGlobalVars(CFAEdge edge) {
    // collect directly used vars by the cfa edge
    // plus continue to successor edges until the current thread obtains any mutexes
    return getDirectlyUsedGlobalVars(edge); // TODO handle mutexes
  }

  private Collection<?> getInfluencedGlobalVars(CFAEdge edge) {
    // get vars of all edges statically reachable in the cfa from the given cfa edge
    // at thread start edges, also process all edges reachable from the started thread's initial location
    return getVarsWithTraversal(edge);
  }

  private Collection<?> getVarsWithTraversal(CFAEdge startEdge) {
    final var vars = new ArrayList<>();
    final var exploredEdges = new ArrayList<CFAEdge>();
    final var edgesToExplore = new ArrayList<>(List.of(startEdge));
    while (!edgesToExplore.isEmpty()) {
      final var edge = edgesToExplore.removeFirst();
      exploredEdges.add(edge);
      vars.addAll(getDirectlyUsedGlobalVars(edge));
      final var successorEdges = getSuccessorEdges(edge);
      for (final var successorEdge : successorEdges) {
        if (!exploredEdges.contains(successorEdge)) {
          edgesToExplore.add(successorEdge);
        }
      }
    }
    return vars;
  }

  private Iterable<CFAEdge> getSuccessorEdges(CFAEdge edge) {
    final var allLeavingEdges = edge.getSuccessor().getAllLeavingEdges();
    final var startedThreadEdges = new ArrayList<CFAEdge>();
    for (final var leavingEdge : allLeavingEdges) {
      // TODO if leaving edge starts a new thread, add the first edges of the started thread as well
    }
    return allLeavingEdges.append(startedThreadEdges);
  }

  private boolean intersects(Iterable<?> i1, Iterable<?> i2) {
    for (var o1 : i1) {
      for (var o2 : i2) {
        if (o1.equals(o2)) {
          return true;
        }
      }
    }
    return false;
  }
}

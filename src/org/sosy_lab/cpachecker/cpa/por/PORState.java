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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PORState implements AbstractState, AbstractStateWithLocations,
                                 AbstractStateWithLocation, Graphable {

  private final CFA cfa;

  private final ImmutableMap<Integer, PORThreadState> threads;

  /**
   * Transient mapping from cloned outgoing edges to their originating thread PID.
   */
  private final Map<CFAEdge, Integer> edgePidMap = new HashMap<>();

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true));

  private Iterable<CFAEdge> sourceSet = null;

  PORState(CFA pCfa, ImmutableMap<Integer, PORThreadState> threads) {
    cfa = pCfa;
    this.threads = threads;
  }

  static PORState empty() {
    return new PORState(null, ImmutableMap.of());
  }

  public ImmutableMap<Integer, PORThreadState> threads() {
    return threads;
  }

  boolean canMerge(PORState other) {
    return threads.equals(other.threads);
  }

  PORState addNewThread(
      CFA cfa,
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
    return new PORState(cfa, newThreads);
  }

  public PORState stepThread(
      CFA cfa,
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
    return new PORState(cfa, newThreads.buildKeepingLast());
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
    if (sourceSet == null) {
      sourceSet = getMinimalSourceSet();
    }
    return sourceSet;
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
    ImmutableCollection<CFAEdge> minimalSourceSet = ImmutableList.of();
    final var allOutgoingEdges = getAllThreadOutgoingEdges();
    final var sourceSetFirstActions = getSourceSetFirstActions(allOutgoingEdges);
    for (final var firstActions : sourceSetFirstActions) {
      final var sourceSet = calculateSourceSet(allOutgoingEdges, firstActions);
      if (minimalSourceSet.isEmpty() || sourceSet.size() < minimalSourceSet.size()) {
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

    final var sourceSetMemLocs = getUsedGlobalVars(sourceSetEdge);
    final var influencedMemLocs = getInfluencedGlobalVars(edge);
    return intersect(sourceSetMemLocs, influencedMemLocs);
  }

  private EdgeDefUseData getDirectlyUsedGlobalVars(CFAEdge edge) {
    // collect directly used vars by the cfa edge
    return memoryAccessExtractor.extract(edge);
  }

  private EdgeDefUseData getUsedGlobalVars(CFAEdge edge) {
    // collect directly used vars by the cfa edge
    // plus continue to successor edges until the current thread obtains any mutexes
    return getDirectlyUsedGlobalVars(edge); // TODO handle mutexes
  }

  private EdgeDefUseData getInfluencedGlobalVars(CFAEdge edge) {
    // get vars of all edges statically reachable in the cfa from the given cfa edge
    // at thread start edges, also process all edges reachable from the started thread's initial location
    return getVarsWithTraversal(edge);
  }

  private EdgeDefUseData getVarsWithTraversal(CFAEdge startEdge) {
    var uses = EdgeDefUseData.empty();
    final var exploredEdges = new ArrayList<CFAEdge>();
    final var edgesToExplore = new ArrayList<>(List.of(startEdge));
    while (!edgesToExplore.isEmpty()) {
      final var edge = edgesToExplore.removeFirst();
      exploredEdges.add(edge);
      uses = uses.merge(getDirectlyUsedGlobalVars(edge));
      final var successorEdges = getSuccessorEdges(edge);
      for (final var successorEdge : successorEdges) {
        if (!exploredEdges.contains(successorEdge)) {
          edgesToExplore.add(successorEdge);
        }
      }
    }
    return uses;
  }

  private Iterable<CFAEdge> getSuccessorEdges(CFAEdge edge) {
    final var allLeavingEdges = edge.getSuccessor().getAllLeavingEdges();
    final var startedThreadEdges = new ArrayList<CFAEdge>();
    for (final var leavingEdge : allLeavingEdges) {
      if (leavingEdge instanceof AStatementEdge statementEdge) {
        if (statementEdge.getStatement() instanceof AFunctionCall functionCall) {
          if (functionCall.getFunctionCallExpression()
              .getFunctionNameExpression() instanceof AIdExpression functionName) {
            if ("pthread_create".equals(functionName.getName())) {
              final var params = functionCall.getFunctionCallExpression().getParameterExpressions();
              final String startedFunctionName =
                  ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand()).getName();
              final CFANode initialNode = cfa.getFunctionHead(startedFunctionName);
              for (CFAEdge initialEdge : initialNode.getAllLeavingEdges()) {
                startedThreadEdges.add(initialEdge);
              }
            }
          }
        }
      }
    }
    return allLeavingEdges.append(startedThreadEdges);
  }

  private boolean intersect(EdgeDefUseData access1, EdgeDefUseData access2) {
    // dependence if the same memory location is accessed and at least one of them writes it
    if (access1.getDefs().isEmpty() && access1.getPointeeDefs().isEmpty() &&
        access2.getDefs().isEmpty() && access2.getPointeeDefs().isEmpty()) {
      return false;
    }
    // TODO properly handle pointers
    if (!access1.getPointeeDefs().isEmpty() || !access1.getPointeeUses().isEmpty()
        || !access2.getPointeeDefs().isEmpty() || !access2.getPointeeUses().isEmpty()) {
      return true;
    }
    return intersect(access1.getDefs(), access2.getUses())
        || intersect(access1.getUses(), access2.getDefs())
        || intersect(access1.getDefs(), access2.getDefs());
  }

  private boolean intersect(Iterable<MemoryLocation> access1, Iterable<MemoryLocation> access2) {
    for (var o1 : access1) {
      for (var o2 : access2) {
        if (o1.getExtendedQualifiedName().equals(o2.getExtendedQualifiedName())) {
          return true;
        }
      }
    }
    return false;
  }
}

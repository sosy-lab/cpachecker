// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithThreads;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PORState
    implements AbstractState, AbstractStateWithLocations, AbstractStateWithThreads, Graphable {

  private final CFA cfa;

  private final ImmutableMap<Integer, PORThreadState> threads;

  private final ImmutableMap<String, Integer> threadHandles;

  /**
   * Transient mapping from cloned outgoing edges to their originating thread PID.
   */
  private final IdentityHashMap<CFAEdge, Integer> edgePidMap = new IdentityHashMap<>();

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  private final MutexState mutexState;

  private Collection<CFAEdge> sourceSet = null;

  PORState(
      CFA pCfa,
      ImmutableMap<Integer, PORThreadState> pThreads,
      ImmutableMap<String, Integer> pThreadHandles,
      MutexState pMutexState) {
    cfa = pCfa;
    threads = pThreads;
    threadHandles = pThreadHandles;
    mutexState = pMutexState;
  }

  static PORState empty(CFA pCfa) {
    return new PORState(pCfa, ImmutableMap.of(), ImmutableMap.of(), MutexState.EMPTY);
  }

  public MutexState getMutexState() {
    return mutexState;
  }

  public ImmutableMap<Integer, PORThreadState> threads() {
    return threads;
  }

  boolean canMerge(PORState other) {
    return threads.equals(other.threads) && mutexState.equals(other.mutexState);
  }

  PORState addNewThread(
      String handle,
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
    // if handle is null, we are adding the initial thread, so do not add to threadHandles
    final ImmutableMap<String, Integer> newThreadHandles =
        handle == null ? threadHandles :
        ImmutableMap.<String, Integer>builder()
            .putAll(threadHandles)
            .put(handle, newPid)
            .buildKeepingLast();
    return new PORState(cfa, newThreads, newThreadHandles, mutexState);
  }

  PORState joinThread(String handle) {
    final Integer pidToRemove = canJoin(handle, true);
    if (pidToRemove == null) {
      return null;
    }

    final ImmutableMap<Integer, PORThreadState> newThreads = threads.entrySet().stream()
        .filter(e -> !e.getKey().equals(pidToRemove))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    final ImmutableMap<String, Integer> newThreadHandles = threadHandles.entrySet().stream()
        .filter(e -> !e.getValue().equals(pidToRemove))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    return new PORState(cfa, newThreads, newThreadHandles, mutexState);
  }

  private Integer canJoin(String handle, boolean throwIfThreadNotFound) {
    final Integer pidToRemove = threadHandles.get(handle);
    var threadState = threads.get(pidToRemove);
    if (pidToRemove == null || threadState == null) {
      if (throwIfThreadNotFound) {
        throw new IllegalArgumentException(
            "No thread with handle " + handle + ". Undefined behavior or unsupported case.");
      } else {
        return null;
      }
    }

    if (threadState.pLocationState().getOutgoingEdges().iterator().hasNext()) {
      return null;
    }

    return pidToRemove;
  }

  public PORState stepThread(
      int pPid,
      LocationState pNextLoc,
      CallstackState pNextStack,
      PathFormula pNextFormula) {
    return stepThread(pPid, pNextLoc, pNextStack, pNextFormula, mutexState);
  }

  public PORState stepThread(
      int pPid,
      LocationState pNextLoc,
      CallstackState pNextStack,
      PathFormula pNextFormula,
      MutexState pMutexState) {
    assert threads.containsKey(pPid) : "threads must contain pid to step " + pPid;
    final ImmutableMap.Builder<Integer, PORThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pPid) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(
        pPid, new PORThreadState(pNextLoc, pNextStack, pNextFormula));
    return new PORState(cfa, newThreads.buildKeepingLast(), threadHandles, pMutexState);
  }

  /**
   * Returns the thread PID that produced the given cloned outgoing edge. This mapping is populated
   * during {@link #getOutgoingEdges()}.
   */
  Integer getEdgePid(CFAEdge edge) {
    return edgePidMap.get(edge);
  }

  @Override
  public CFAEdge getNextBasicBlockEdge(CFAEdge previousEdge) {
    Integer pid = PorEdgeCloner.getPid(previousEdge);
    if (pid == null) {
      throw new IllegalArgumentException("Thread could not be found for the edge: " + previousEdge);
    }
    CFAEdge successor = previousEdge.getSuccessor().getLeavingEdge(0);
    CFAEdge cloned = PorEdgeCloner.clone(successor, pid, this);
    edgePidMap.put(cloned, pid);
    return cloned;
  }

  @Override
  public int getNumberOfActiveThreads() {
    return threads.size();
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
  public @Nullable List<CFAEdge> getEdgesToChild(AbstractStateWithLocations pChild) {
    if (pChild instanceof PORState child) {
      PORThreadState parentThreadState = null;
      PORThreadState childThreadState = null;
      for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
        int threadId = entry.getKey();
        PORThreadState currentParentState = entry.getValue();
        PORThreadState currentChildState = child.threads().get(threadId);
        if (currentChildState != null && !currentParentState.pLocationState().getLocationNode()
            .equals(currentChildState.pLocationState().getLocationNode())) {
          if (parentThreadState != null) {
            // more than one thread has a different location in the child,
            // we cannot determine a unique path of edges to the child
            return null;
          }
          parentThreadState = currentParentState;
          childThreadState = currentChildState;
        }
      }

      if (parentThreadState == null) {
        if (threads.keySet().equals(child.threads().keySet())) {
          // all threads have the same location in parent and child, so the path is empty
          return ImmutableList.of();
        } else {
          // weird case: some new/destroyed threads but no other stepping thread,
          // we cannot determine a path of edges to the child
          return null;
        }
      }

      return parentThreadState.pLocationState().getEdgesToChild(childThreadState.pLocationState());
    }
    return null;
  }

  @Override
  public String toDOTLabel() {
    return "[" + threads.keySet().stream().sorted()
        .map(e -> e + ": " + threads.get(e).pLocationState().getLocationNode()).collect(
            Collectors.joining(", ")) + "] " + mutexState;
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
    return Objects.equals(threads, other.threads)
        && Objects.equals(mutexState, other.mutexState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, mutexState);
  }

  private ImmutableCollection<CFAEdge> getAllThreadOutgoingEdges() {
    edgePidMap.clear();
    ImmutableList.Builder<CFAEdge> ret = ImmutableList.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      int pid = entry.getKey();
      PORThreadState threadState = entry.getValue();
      if (!threadState.pLocationState().getLocationNode().getAllLeavingEdges().isEmpty()) {
        for (CFAEdge outgoingEdge : threadState.pLocationState().getOutgoingEdges()) {
          CFAEdge cloned = PorEdgeCloner.clone(outgoingEdge, pid, this);

          // Mutex lock filtering: if this edge is a lock call and the mutex is held by another
          // thread, this thread is blocked and cannot proceed along this edge.
          String lockMutex = MutexFunctions.getLockMutexName(cloned);
          if (lockMutex != null && mutexState.isLockedByOther(lockMutex, pid)) {
            continue;
          }

          // shortcut for pthread_join edges: only include them if the joined thread has already terminated
          if (cloned instanceof AStatementEdge statementEdge
              && statementEdge.getStatement() instanceof AFunctionCall functionCall
              && functionCall.getFunctionCallExpression()
              .getFunctionNameExpression() instanceof AIdExpression functionName
              && "pthread_join".equals(functionName.getName())) {
            final var params =
                functionCall.getFunctionCallExpression().getParameterExpressions();
            checkState(params.size() == 2, "Malformed pthread_join (not 2 params): %s",
                functionCall);
            final var handleParam = params.get(0);
            checkState(handleParam instanceof CUnaryExpression cUnaryExpression
                    && cUnaryExpression.getOperator() == UnaryOperator.AMPER
                    && cUnaryExpression.getOperand() instanceof CIdExpression,
                "Malformed/unsupported pthread_join (Thread handle not unary expression with variable reference): %s",
                handleParam);
            final var handleName =
                ((CIdExpression) ((CUnaryExpression) handleParam).getOperand()).getDeclaration()
                    .getQualifiedName();

            if (canJoin(handleName, false) == null) {
              continue;
            }
          }

          edgePidMap.put(cloned, pid);
          ret.add(cloned);
        }
      }
    }
    return ret.build();
  }

  // POR algorithm

  Collection<CFAEdge> getSourceSet(Precision precision) {
    if (sourceSet == null) {
      ImmutableCollection<CFAEdge> minimalSourceSet = ImmutableList.of();
      final var allOutgoingEdges = getAllThreadOutgoingEdges();
      final var sourceSetFirstActions = getSourceSetFirstActions(allOutgoingEdges);
      for (final var firstActions : sourceSetFirstActions) {
        final var sourceSet = calculateSourceSet(allOutgoingEdges, firstActions);
        if (minimalSourceSet.isEmpty() || sourceSet.size() < minimalSourceSet.size()) {
          minimalSourceSet = sourceSet;
        }
      }
      sourceSet = minimalSourceSet;
    }
    return sourceSet;
  }

  private ImmutableCollection<ImmutableCollection<CFAEdge>> getSourceSetFirstActions(Iterable<CFAEdge> allOutgoingEdges) {
    final var enabledThreads = StreamSupport.stream(allOutgoingEdges.spliterator(), false)
        .map(e -> edgePidMap.get(e))
        .distinct()
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
      // Skip threads whose first actions are all mutex lock calls on already-held mutexes,
      // since those threads are effectively blocked and need not seed a source set.
      final ImmutableCollection<CFAEdge> built = firstActions.build();
      boolean allBlocked = !built.isEmpty() && built.stream().allMatch(e -> {
        String lockMutex = MutexFunctions.getLockMutexName(e);
        return lockMutex != null && mutexState.isLocked(lockMutex);
      });
      if (!allBlocked) {
        sourceSetFirstActions.add(built);
      }
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
    // Collect directly used vars by the CFA edge.
    // If this edge acquires a mutex, only the directly used vars matter (the mutex protects
    // subsequent accesses, so we stop expanding at the lock boundary).
    if (MutexFunctions.isLockCall(edge)) {
      return getDirectlyUsedGlobalVars(edge);
    }
    return getDirectlyUsedGlobalVars(edge);
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
              for (CFAEdge initialEdge : initialNode.getLeavingEdges()) {
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

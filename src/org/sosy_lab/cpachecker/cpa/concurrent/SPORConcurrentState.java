// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.ThreadId;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexHandle;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A static partial order reduction algorithm. A similar approach is described <a
 * href="https://link.springer.com/chapter/10.1007/978-3-319-63121-9_26">here</a>.
 */
class SPORConcurrentState extends ConcurrentState {

  private final GlobalMemoryAccesses.Extractor memoryAccessExtractor =
      new GlobalMemoryAccesses.Extractor();

  private ImmutableCollection<CFAEdge> sourceSet = null;

  SPORConcurrentState(
      AbstractState pWrappedState,
      ConcurrentCfaClones pCfaClones,
      LogManager pLogger,
      ImmutableMap<ThreadId, ThreadState> pThreads,
      ImmutableSet<ThreadId> pLiveThreadIds,
      ImmutableMap<String, ThreadId> pHandleToLastCreatedThreadId,
      Random pRandom) {
    super(
        pWrappedState,
        pCfaClones,
        pLogger,
        pThreads,
        pLiveThreadIds,
        pHandleToLastCreatedThreadId,
        pRandom);
  }

  @Override
  protected ConcurrentState update(
      AbstractState pWrappedState,
      ImmutableMap<ThreadId, ThreadState> pThreads,
      ImmutableSet<ThreadId> pLiveThreadIds,
      ImmutableMap<String, ThreadId> pHandleToLastCreatedThreadId) {
    return new SPORConcurrentState(
        pWrappedState,
        cfaClones,
        logger,
        pThreads,
        pLiveThreadIds,
        pHandleToLastCreatedThreadId,
        random);
  }

  /**
   * Calculates a source set of enabled edges that are sufficient to explore to cover all possible
   * behaviors of the program.
   */
  @Override
  public ImmutableCollection<CFAEdge> getEdgesToExplore(
      ConcurrentPrecision precision, BasicBlockAggregator basicBlock) throws CPATransferException {
    if (sourceSet == null) {
      ImmutableCollection<CFAEdge> minimalSourceSet = ImmutableList.of();
      final var allOutgoingEdges = getOutgoingEdges();
      final var sourceSetFirstActions = getSourceSetFirstActions(allOutgoingEdges);
      for (final var firstActions : sourceSetFirstActions) {
        final var currentSourceSet =
            calculateSourceSet(allOutgoingEdges, firstActions, precision, basicBlock);
        if (minimalSourceSet.isEmpty() || currentSourceSet.size() < minimalSourceSet.size()) {
          minimalSourceSet = currentSourceSet;
        }
      }
      sourceSet = minimalSourceSet;
    } else {
      MutexState mutexState =
          AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
      if (mutexState != null) {
        mutexState.addEdgePids(edgeThreadIds);
      }
    }
    return sourceSet;
  }

  private ImmutableCollection<ImmutableCollection<CFAEdge>> getSourceSetFirstActions(
      Iterable<CFAEdge> allOutgoingEdges) {
    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    final var enabledThreads =
        Streams.stream(allOutgoingEdges)
            .map(this::getEdgeThreadId)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(enabledThreads, random);
    final ImmutableList.Builder<ImmutableCollection<CFAEdge>> sourceSetFirstActions =
        ImmutableList.builder();
    for (final var threadId : enabledThreads) {
      final Collection<CFAEdge> firstActions = new ArrayList<>();
      for (final var edge : allOutgoingEdges) {
        if (threadId.equals(edgeThreadIds.get(edge))) {
          firstActions.add(edge);
        }
      }
      boolean allBlocked =
          !firstActions.isEmpty()
              && mutexState != null
              && firstActions.stream()
                  .allMatch(
                      e -> {
                        Optional<MutexLock> lockMutex = MutexFunctions.getLockMutex(e);
                        return lockMutex.isPresent()
                            && mutexState.isMutexBlockedFor(lockMutex.orElseThrow(), threadId);
                      });
      if (!allBlocked) {
        sourceSetFirstActions.add(ImmutableList.copyOf(firstActions));
      }
    }
    return sourceSetFirstActions.build();
  }

  private ImmutableCollection<CFAEdge> calculateSourceSet(
      ImmutableCollection<CFAEdge> allOutgoingEdges,
      ImmutableCollection<CFAEdge> firstActions,
      ConcurrentPrecision precision,
      BasicBlockAggregator basicBlock)
      throws CPATransferException {
    final var currentSourceSet = new ArrayList<CFAEdge>();
    final var otherEdges = new ArrayList<CFAEdge>();
    for (final var edge : allOutgoingEdges) {
      if (firstActions.contains(edge)) {
        if (isLoopHead(edge)) {
          return allOutgoingEdges;
        }
        currentSourceSet.add(edge);
      } else {
        otherEdges.add(edge);
      }
    }

    var addedNewEdge = true;
    while (addedNewEdge) {
      addedNewEdge = false;
      final ImmutableSet.Builder<CFAEdge> edgesToRemove = ImmutableSet.builder();
      for (final var edge : otherEdges) {
        boolean isDependent = false;
        for (final var sourceSetEdge : currentSourceSet) {
          if (dependent(sourceSetEdge, edge, precision, basicBlock)) {
            isDependent = true;
            break;
          }
        }
        if (isDependent) {
          if (isLoopHead(edge)) {
            return allOutgoingEdges;
          }
          currentSourceSet.add(edge);
          edgesToRemove.add(edge);
          addedNewEdge = true;
        }
      }
      otherEdges.removeAll(edgesToRemove.build());
    }

    return ImmutableList.copyOf(currentSourceSet);
  }

  private boolean dependent(
      CFAEdge sourceSetEdge,
      CFAEdge edge,
      ConcurrentPrecision precision,
      BasicBlockAggregator basicBlock)
      throws CPATransferException {
    if (edgeThreadIds.get(sourceSetEdge).equals(edgeThreadIds.get(edge))) {
      return true;
    }

    final var sourceSetMemLocs = getUsedGlobalVars(sourceSetEdge, basicBlock);
    final var influencedMemLocs = getInfluencedGlobalVars(edge);
    return intersect(sourceSetMemLocs, influencedMemLocs, precision);
  }

  private boolean isLoopHead(CFAEdge edge) {
    return cfa.getAllLoopHeads()
        .map(loopHeads -> loopHeads.contains(edge.getPredecessor().getOriginal()))
        .orElse(false);
  }

  private GlobalMemoryAccesses getDirectlyUsedGlobalVars(CFAEdge edge) {
    return memoryAccessExtractor.extract(edge);
  }

  private GlobalMemoryAccesses getUsedGlobalVars(CFAEdge edge, BasicBlockAggregator basicBlock)
      throws CPATransferException {
    // collect directly used vars by the cfa edge
    // plus continue to successor edges until the current thread obtains any mutexes
    BiPredicate<CFAEdge, MutexState> goFurther;
    if (basicBlock != null && basicBlock.isValidMultiEdgeStart(edge.getPredecessor())) {
      goFurther =
          (pCFAEdge, pMutexState) ->
              basicBlock.isValidMultiEdgeComponent(edge.getPredecessor(), pCFAEdge);
    } else {
      goFurther = (pCFAEdge, pMutexState) -> false;
    }

    final MutexState initialMutexState;
    final ThreadId threadId;
    if (MutexFunctions.isLockCall(edge)) {
      MutexState currentMutexState =
          AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
      MutexState currentInitialMutexState = MutexState.EMPTY;
      if (currentMutexState != null) {
        for (MutexHandle initializedMutex : currentMutexState.getInitializedMutexes()) {
          currentInitialMutexState = currentInitialMutexState.withInit(initializedMutex);
        }
      }
      initialMutexState = currentInitialMutexState;
      threadId = getEdgeThreadId(edge);
      final BiPredicate<CFAEdge, MutexState> originalGoFurther = goFurther;
      goFurther =
          (pCFAEdge, pMutexState) ->
              (pMutexState != null
                      && (!pMutexState.getLockedMutexes().isEmpty()
                          || pMutexState.getAtomicHolder().equals(Optional.of(threadId))))
                  || originalGoFurther.test(pCFAEdge, pMutexState);
    } else {
      initialMutexState = null;
      threadId = null;
    }

    return getVarsWithTraversal(edge, goFurther, initialMutexState, threadId, false);
  }

  private GlobalMemoryAccesses getInfluencedGlobalVars(CFAEdge edge) throws CPATransferException {
    return getVarsWithTraversal(edge, (e, s) -> true, null, null, true);
  }

  private GlobalMemoryAccesses getVarsWithTraversal(
      CFAEdge startEdge,
      BiPredicate<CFAEdge, MutexState> goFurther,
      MutexState initialMutexState,
      ThreadId threadId,
      boolean visitStartedThreadFunction)
      throws CPATransferException {
    GlobalMemoryAccesses uses = GlobalMemoryAccesses.EMPTY;
    final List<CFAEdge> exploredEdges = new ArrayList<>();
    final List<Pair<CFAEdge, MutexState>> toExplore =
        new ArrayList<>(ImmutableList.of(Pair.of(startEdge, initialMutexState)));
    while (!toExplore.isEmpty()) {
      final Pair<CFAEdge, MutexState> exploring = toExplore.removeFirst();
      final CFAEdge edge = exploring.getFirst();
      MutexState mutexState = exploring.getSecond();
      exploredEdges.add(edge);
      uses = uses.merge(getDirectlyUsedGlobalVars(edge));
      if (mutexState != null) {
        Optional<MutexState> result = mutexState.update(edge, threadId, null);
        if (result.isEmpty()) {
          continue;
        }
        mutexState = result.orElseThrow();
      }
      if (goFurther.test(edge, mutexState)) {
        for (final var successorEdge : getSuccessorEdges(edge, visitStartedThreadFunction)) {
          if (!exploredEdges.contains(successorEdge)) {
            toExplore.add(Pair.of(successorEdge, mutexState));
          }
        }
      }
    }
    return uses;
  }

  private Iterable<CFAEdge> getSuccessorEdges(CFAEdge edge, boolean visitStartedThreadFunction)
      throws CPATransferException {
    final var allLeavingEdges = edge.getSuccessor().getAllLeavingEdges();
    if (!visitStartedThreadFunction) {
      return allLeavingEdges;
    }

    final var startedThreadEdges = new ArrayList<CFAEdge>();
    // The started thread's body must be reachable from the create edge ITSELF, not only from an
    // edge whose successor performs the create. Checking just the leaving edges misses exactly one
    // case -- the edge the traversal STARTS at -- and that is the case that matters: the influenced
    // set of a `pthread_create` edge (getInfluencedGlobalVars) then comes back EMPTY, so the create
    // looks independent of everything the spawned thread touches. POR concludes it never has to
    // interpose the create between two conflicting accesses of another thread, and silently drops
    // every schedule in which the new thread races with them (a real wrong-TRUE: pthread-theta
    // unwind2, where f2's `limit = lim` must fall between f1's `limit = lim` and `bound = limit`).
    for (final var leavingEdge : Iterables.concat(ImmutableList.of(edge), allLeavingEdges)) {
      if (leavingEdge instanceof AStatementEdge statementEdge) {
        if (statementEdge.getStatement() instanceof AFunctionCall functionCall) {
          if (functionCall.getFunctionCallExpression().getFunctionNameExpression()
              instanceof AIdExpression functionName) {
            if ("pthread_create".equals(functionName.getName())) {
              final var params = functionCall.getFunctionCallExpression().getParameterExpressions();
              final String startedFunctionName = ThreadFunctions.extractCreateFunctionName(params);
              final CFANode initialNode = cfa.getFunctionHead(startedFunctionName);
              initialNode.getLeavingEdges().copyInto(startedThreadEdges);
            }
          }
        }
      }
    }
    return allLeavingEdges.append(startedThreadEdges);
  }

  private boolean intersect(
      GlobalMemoryAccesses access1, GlobalMemoryAccesses access2, ConcurrentPrecision precision) {
    if (!access1.hasWrites() && !access2.hasWrites()) {
      return false;
    }
    if (access1.accessesThroughPointer() || access2.accessesThroughPointer()) {
      return true;
    }
    return intersect(access1.writes(), access2.reads(), precision)
        || intersect(access1.reads(), access2.writes(), precision)
        || intersect(access1.writes(), access2.writes(), precision);
  }

  private boolean intersect(
      Iterable<MemoryLocation> access1,
      Iterable<MemoryLocation> access2,
      ConcurrentPrecision precision) {
    for (var o1 : access1) {
      for (var o2 : access2) {
        if (o1.getExtendedQualifiedName().equals(o2.getExtendedQualifiedName())) {
          if (!precision.canIgnoreVariable(o1)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}

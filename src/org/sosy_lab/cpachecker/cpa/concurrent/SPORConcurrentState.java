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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A static partial order reduction algorithm. A similar approach is described
 * <a href="https://link.springer.com/chapter/10.1007/978-3-319-63121-9_26">here</a>.
 */
class SPORConcurrentState extends ConcurrentState {

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  private ImmutableCollection<CFAEdge> sourceSet = null;

  SPORConcurrentState(
      AbstractState pWrappedState,
      CFA pCfa,
      LogManager pLogger,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints,
      Random pRandom) {
    super(pWrappedState, pCfa, pLogger, pThreads, pLivePids, pHandleHints, pRandom);
  }

  @Override
  protected ConcurrentState update(
      AbstractState pWrappedState,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints) {
    return new SPORConcurrentState(pWrappedState, cfa, logger, pThreads, pLivePids, pHandleHints, random);
  }

  /**
   * Calculates a source set of enabled edges that are sufficient to explore to cover all possible
   * behaviors of the program.
   */
  @Override
  public ImmutableCollection<CFAEdge> getEdgesToExplore(
      ConcurrentPrecision precision,
      BasicBlockAggregator basicBlock) throws CPATransferException {
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
        mutexState.addEdgePids(edgePidMap);
      }
    }
    return sourceSet;
  }

  private ImmutableCollection<ImmutableCollection<CFAEdge>> getSourceSetFirstActions(
      Iterable<CFAEdge> allOutgoingEdges) {
    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    final var enabledThreads =
        Streams.stream(allOutgoingEdges)
            .map(e -> getEdgePid(e))
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(enabledThreads, random);
    final ImmutableList.Builder<ImmutableCollection<CFAEdge>> sourceSetFirstActions =
        ImmutableList.builder();
    for (final var pid : enabledThreads) {
      final Collection<CFAEdge> firstActions = new ArrayList<>();
      for (final var edge : allOutgoingEdges) {
        if (pid.equals(edgePidMap.get(edge))) {
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
                    return lockMutex.isPresent() && mutexState.isMutexBlockedFor(lockMutex.get(),
                        pid);
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
        if (edge.getPredecessor().isLoopStart()) {
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
          if (edge.getPredecessor().isLoopStart()) {
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
      CFAEdge sourceSetEdge, CFAEdge edge, ConcurrentPrecision precision, BasicBlockAggregator basicBlock)
      throws CPATransferException {
    if (edgePidMap.get(sourceSetEdge).equals(edgePidMap.get(edge))) {
      return true;
    }

    final var sourceSetMemLocs = getUsedGlobalVars(sourceSetEdge, basicBlock);
    final var influencedMemLocs = getInfluencedGlobalVars(edge);
    return intersect(sourceSetMemLocs, influencedMemLocs, precision);
  }

  private EdgeDefUseData getDirectlyUsedGlobalVars(CFAEdge edge) {
    return memoryAccessExtractor.extract(edge);
  }

  private EdgeDefUseData getUsedGlobalVars(CFAEdge edge, BasicBlockAggregator basicBlock)
      throws CPATransferException {
    // collect directly used vars by the cfa edge
    // plus continue to successor edges until the current thread obtains any mutexes
    BiPredicate<CFAEdge, MutexState> goFurther;
    if (basicBlock != null && basicBlock.isValidMultiEdgeStart(edge.getPredecessor())) {
      goFurther =
          (pCFAEdge, pMutexState) -> basicBlock.isValidMultiEdgeComponent(edge.getPredecessor(),
              pCFAEdge);
    } else {
      goFurther = (pCFAEdge, pMutexState) -> false;
    }

    final MutexState initialMutexState;
    final Integer pid;
    if (MutexFunctions.isLockCall(edge)) {
      MutexState currentMutexState =
          AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
      MutexState currentInitialMutexState = MutexState.EMPTY;
      if (currentMutexState != null) {
        for (String initializedMutex : currentMutexState.getInitializedMutexes()) {
          currentInitialMutexState = currentInitialMutexState.withInit(initializedMutex);
        }
      }
      initialMutexState = currentInitialMutexState;
      pid = getEdgePid(edge);
      final BiPredicate<CFAEdge, MutexState> originalGoFurther = goFurther;
      goFurther = (pCFAEdge, pMutexState) ->
          (pMutexState != null && (!pMutexState.getLockedMutexes().isEmpty()
              || pid.equals(pMutexState.getAtomicHolder())))
              || originalGoFurther.test(pCFAEdge, pMutexState);
    } else {
      initialMutexState = null;
      pid = null;
    }

    return getVarsWithTraversal(edge, goFurther, initialMutexState, pid, false);
  }

  private EdgeDefUseData getInfluencedGlobalVars(CFAEdge edge) throws CPATransferException {
    return getVarsWithTraversal(edge, (e, s) -> true, null, null, true);
  }

  private EdgeDefUseData getVarsWithTraversal(
      CFAEdge startEdge,
      BiPredicate<CFAEdge, MutexState> goFurther,
      MutexState initialMutexState,
      Integer pid,
      boolean visitStartedThreadFunction) throws CPATransferException {
    EdgeDefUseData uses = EdgeDefUseData.empty();
    final List<CFAEdge> exploredEdges = new ArrayList<>();
    final List<Pair<CFAEdge, MutexState>> toExplore =
        new ArrayList<>(List.of(Pair.of(startEdge, initialMutexState)));
    while (!toExplore.isEmpty()) {
      final Pair<CFAEdge, MutexState> exploring = toExplore.removeFirst();
      final CFAEdge edge = exploring.getFirst();
      MutexState mutexState = exploring.getSecond();
      exploredEdges.add(edge);
      uses = uses.merge(getDirectlyUsedGlobalVars(edge));
      if (mutexState != null) {
        Optional<MutexState> result = mutexState.update(edge, pid, null);
        if (result.isEmpty()) {
          continue;
        }
        mutexState = result.get();
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
      EdgeDefUseData access1, EdgeDefUseData access2, ConcurrentPrecision precision) {
    if (access1.getDefs().isEmpty()
        && access1.getPointeeDefs().isEmpty()
        && access2.getDefs().isEmpty()
        && access2.getPointeeDefs().isEmpty()) {
      return false;
    }
    if (!access1.getPointeeDefs().isEmpty()
        || !access1.getPointeeUses().isEmpty()
        || !access2.getPointeeDefs().isEmpty()
        || !access2.getPointeeUses().isEmpty()) {
      return true;
    }
    return intersect(access1.getDefs(), access2.getUses(), precision)
        || intersect(access1.getUses(), access2.getDefs(), precision)
        || intersect(access1.getDefs(), access2.getDefs(), precision);
  }

  private boolean intersect(
      Iterable<MemoryLocation> access1, Iterable<MemoryLocation> access2, ConcurrentPrecision precision) {
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

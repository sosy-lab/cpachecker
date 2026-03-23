// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.por.ThreadFunctions.extractJoinHandle;

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
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithThreads;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PORState
    extends AbstractSingleWrapperState
    implements AbstractState, AbstractStateWithLocations,
               AbstractStateWithThreads, Graphable {

  private final Random random = new Random();

  private final CFA cfa;

  private final ImmutableMap<Integer, PORThreadState> threads;

  private final ImmutableMap<String, Integer> threadHandles;

  /**
   * Transient mapping from cloned outgoing edges to their originating thread PID.
   */
  private final IdentityHashMap<CFAEdge, Integer> edgePidMap = new IdentityHashMap<>();

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  private Collection<CFAEdge> sourceSet = null;

  PORState(
      AbstractState pWrappedState,
      CFA pCfa,
      ImmutableMap<Integer, PORThreadState> pThreads,
      ImmutableMap<String, Integer> pThreadHandles) {
    super(pWrappedState);

    cfa = pCfa;
    threads = pThreads;
    threadHandles = pThreadHandles;
  }

  static PORState empty(AbstractState pWrappedInitialState, CFA pCfa) {
    return new PORState(pWrappedInitialState, pCfa, ImmutableMap.of(), ImmutableMap.of());
  }

  public ImmutableMap<Integer, PORThreadState> threads() {
    return threads;
  }

  boolean canMerge(PORState other) {
    return threads.equals(other.threads) && threadHandles.equals(other.threadHandles);
  }

  PORState addNewThread(
      String handle,
      LocationState pInitialLoc,
      CallstackState pInitialStack) {
    final int newPid = threads.size();
    final ImmutableMap<Integer, PORThreadState> newThreads =
        ImmutableMap.<Integer, PORThreadState>builder()
            .putAll(threads)
            .put(newPid, new PORThreadState(pInitialLoc, pInitialStack))
            .buildKeepingLast();
    final ImmutableMap<String, Integer> newThreadHandles =
        handle == null
        ? threadHandles
        : ImmutableMap.<String, Integer>builder()
            .putAll(threadHandles)
            .put(handle, newPid)
            .buildKeepingLast();
    return new PORState(getWrappedState(), cfa, newThreads, newThreadHandles);
  }

  PORState joinThread(String handle) {
    final Integer pidToRemove = canJoin(handle, true);
    if (pidToRemove == null) {
      return null;
    }

    final ImmutableMap<Integer, PORThreadState> newThreads =
        threads.entrySet().stream()
            .filter(e -> !e.getKey().equals(pidToRemove))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    final ImmutableMap<String, Integer> newThreadHandles =
        threadHandles.entrySet().stream()
            .filter(e -> !e.getValue().equals(pidToRemove))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    return new PORState(getWrappedState(), cfa, newThreads, newThreadHandles);
  }

  private Integer canJoin(String handle, boolean throwIfThreadNotFound) {
    final Integer pidToRemove = threadHandles.get(handle);
    var threadState = pidToRemove != null ? threads.get(pidToRemove) : null;
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
      CallstackState pNextStack) {
    assert threads.containsKey(pPid) : "threads must contain pid to step " + pPid;
    final ImmutableMap.Builder<Integer, PORThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pPid) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(pPid, new PORThreadState(pNextLoc, pNextStack));
    return new PORState(getWrappedState(), cfa, newThreads.buildKeepingLast(), threadHandles);
  }

  public PORState exitThread(int pPid, LocationStateFactory pLocationStateFactory) {
    PORThreadState threadState = threads.get(pPid);
    assert threadState != null : "threads must contain pid to exit " + pPid;
    CFANode currentNode = threadState.pLocationState().getLocationNode();
    // Resolve to original CFA node to find exit node in the original CFA
    CFANode originalCurrentNode = PorEdgeCloner.getOriginalNode(currentNode);
    String function = originalCurrentNode.getFunctionName();

    CFANode originalExitNode = cfa.nodes().stream().filter(
        n -> n instanceof FunctionExitNode && function.equals(n.getFunctionName())
            && n.getNumLeavingEdges() == 0).findAny().orElseThrow();
    // Get the cloned exit node for this thread
    CFANode clonedExitNode = PorEdgeCloner.getClonedNode(originalExitNode, pPid, cfa);
    LocationState exitLocationState = pLocationStateFactory.getState(clonedExitNode);

    CFANode clonedFunctionHead =
        PorEdgeCloner.getClonedNode(cfa.getFunctionHead(function), pPid, cfa);
    CallstackState exitCallstackState =
        new CallstackState(null, function, clonedFunctionHead);

    return stepThread(pPid, exitLocationState, exitCallstackState);
  }

  PORState withWrappedState(AbstractState pWrappedState) {
    return new PORState(pWrappedState, cfa, threads, threadHandles);
  }

  /**
   * Returns the thread PID that produced the given cloned outgoing edge. This mapping is populated
   * during {@link #getOutgoingEdges()}.
   */
  public Integer getEdgePid(CFAEdge edge) {
    return edgePidMap.get(edge);
  }

  public CFAEdge getNextBasicBlockEdge(int pid) {
    var threadState = threads.get(pid);
    if (threadState == null) {
      throw new IllegalArgumentException("No thread with pid " + pid);
    }

    CFANode locationNode = threadState.pLocationState().getLocationNode();
    CFANode clonedNode = PorEdgeCloner.getClonedNode(locationNode, pid, cfa);
    var leavingEdges = clonedNode.getLeavingEdges();
    assert leavingEdges.size() == 1 : "Expected exactly one leaving edge for basic block stepping";
    CFAEdge cloned = leavingEdges.get(0);
    edgePidMap.put(cloned, pid);

    MutexState mutexState =
        AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    if (mutexState != null) {
      mutexState.addEdgePids(edgePidMap);
    }

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
        if (currentChildState != null
            && !currentParentState
            .pLocationState()
            .getLocationNode()
            .equals(currentChildState.pLocationState().getLocationNode())) {
          if (parentThreadState != null) {
            // Multiple threads changed: collect edges from all changed threads
            ImmutableList.Builder<CFAEdge> allEdges = ImmutableList.builder();
            for (Entry<Integer, PORThreadState> entry2 : threads.entrySet()) {
              int tid = entry2.getKey();
              PORThreadState pState = entry2.getValue();
              PORThreadState cState = child.threads().get(tid);
              if (cState != null
                  && !pState.pLocationState().getLocationNode()
                  .equals(cState.pLocationState().getLocationNode())) {
                var edges = pState.pLocationState().getEdgesToChild(cState.pLocationState());
                if (edges != null) {
                  allEdges.addAll(edges);
                }
              }
            }
            return allEdges.build();
          }
          parentThreadState = currentParentState;
          childThreadState = currentChildState;
        }
      }

      if (parentThreadState == null) {
        if (threads.keySet().equals(child.threads().keySet())) {
          return ImmutableList.of();
        } else {
          // Thread set changed (thread created/destroyed) but no location changed.
          // Find a thread that exists in parent whose location edges lead to the child.
          for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
            int threadId = entry.getKey();
            PORThreadState pState = entry.getValue();
            PORThreadState cState = child.threads().get(threadId);
            if (cState != null) {
              var edges = pState.pLocationState().getEdgesToChild(cState.pLocationState());
              if (edges != null && !edges.isEmpty()) {
                return edges;
              }
            }
          }
          return ImmutableList.of();
        }
      }

      return parentThreadState
          .pLocationState()
          .getEdgesToChild(childThreadState.pLocationState());
    }
    return null;
  }

  @Override
  public String toDOTLabel() {
    return "["
        + threads.keySet().stream()
        .sorted()
        .map(e -> e + ": " + threads.get(e).pLocationState().getLocationNode())
        .collect(Collectors.joining(", "))
        + "]";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return true;
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
        && Objects.equals(threadHandles, other.threadHandles)
        && Objects.equals(getWrappedState(), other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, threadHandles, getWrappedState());
  }

  private ImmutableCollection<CFAEdge> getAllThreadOutgoingEdges() {
    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    edgePidMap.clear();
    ImmutableList.Builder<CFAEdge> ret = ImmutableList.builder();
    for (Entry<Integer, PORThreadState> entry : threads.entrySet()) {
      int pid = entry.getKey();
      PORThreadState threadState = entry.getValue();

      // Atomic block filtering: if another thread holds the atomic block, this thread is blocked.
      Integer atomicHolder = mutexState != null ? mutexState.getAtomicHolder() : null;
      if (atomicHolder != null && atomicHolder != pid) {
        continue;
      }

      CFANode locationNode = threadState.pLocationState().getLocationNode();
      CFANode clonedNode = PorEdgeCloner.getClonedNode(locationNode, pid, cfa);
      for (CFAEdge cloned : clonedNode.getLeavingEdges()) {

        if (mutexState != null) {
          // Mutex lock filtering: if this edge is a lock call and the mutex is held by another
          // thread, this thread is blocked and cannot proceed along this edge.
          MutexLock lockMutex = MutexFunctions.getLockMutex(cloned);
          if (lockMutex != null && mutexState.isMutexBlockedFor(lockMutex, pid)) {
            continue;
          }
        }

        // Shortcut for pthread_join: only include if the joined thread has terminated
        if (cloned instanceof AStatementEdge statementEdge
            && statementEdge.getStatement() instanceof AFunctionCall functionCall
            && functionCall
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            instanceof AIdExpression functionName
            && "pthread_join".equals(functionName.getName())) {
          final var params =
              functionCall.getFunctionCallExpression().getParameterExpressions();
          checkState(
              params.size() == 2, "Malformed pthread_join (not 2 params): %s", functionCall);
          final var handleName = extractJoinHandle(params);

          if (canJoin(handleName, false) == null) {
            continue;
          }
        }

        edgePidMap.put(cloned, pid);
        ret.add(cloned);
      }
    }

    if (mutexState != null) {
      mutexState.addEdgePids(edgePidMap);
    }

    return ret.build();
  }

  // POR algorithm

  Collection<CFAEdge> getSourceSet(PORPrecision precision, BasicBlockAggregator basicBlock) {
    if (sourceSet == null) {
      ImmutableCollection<CFAEdge> minimalSourceSet = ImmutableList.of();
      final var allOutgoingEdges = getAllThreadOutgoingEdges();
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
        StreamSupport.stream(allOutgoingEdges.spliterator(), false)
            .map(e -> edgePidMap.get(e))
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
          !firstActions.isEmpty() && mutexState != null
              && firstActions.stream()
              .allMatch(
                  e -> {
                    MutexLock lockMutex = MutexFunctions.getLockMutex(e);
                    return lockMutex != null && mutexState.isMutexBlockedFor(lockMutex, pid);
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
      PORPrecision precision,
      BasicBlockAggregator basicBlock) {
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
        if (currentSourceSet.stream().anyMatch(s -> dependent(s, edge, precision, basicBlock))) {
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
      CFAEdge sourceSetEdge,
      CFAEdge edge,
      PORPrecision precision,
      BasicBlockAggregator basicBlock) {
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

  private EdgeDefUseData getUsedGlobalVars(CFAEdge edge, BasicBlockAggregator basicBlock) {
    // collect directly used vars by the cfa edge
    // plus continue to successor edges until the current thread obtains any mutexes
    Predicate<CFAEdge> goFurther;
    if (basicBlock != null && basicBlock.isValidMultiEdgeStart(edge.getPredecessor())) {
      goFurther = pCFAEdge -> basicBlock.isValidMultiEdgeComponent(edge.getPredecessor(), pCFAEdge);
    } else {
      goFurther = pCFAEdge -> false;
    }

    if (MutexFunctions.isLockCall(edge)) {
      MutexState currentMutexState =
          AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
      MutexState currentInitialMutexState = MutexState.EMPTY;
      if (currentMutexState != null) {
        for (String initializedMutex : currentMutexState.getInitializedMutexes()) {
          currentInitialMutexState = currentInitialMutexState.withInit(initializedMutex);
        }
      }
      final MutexState finalCurrentInitialMutexState = currentInitialMutexState;
      final Integer pid = getEdgePid(edge);
      final Predicate<CFAEdge> originalGoFurther = goFurther;
      goFurther = new Predicate<>() {

        private MutexState mutexState = finalCurrentInitialMutexState;

        @Override
        public boolean test(CFAEdge pCFAEdge) {
          mutexState = mutexState.update(pCFAEdge, pid);
          return (mutexState != null && !mutexState.getLockedMutexes().isEmpty())
              || originalGoFurther.test(pCFAEdge);
        }
      };
    }

    return getVarsWithTraversal(edge, goFurther, false);
  }

  private EdgeDefUseData getInfluencedGlobalVars(CFAEdge edge) {
    return getVarsWithTraversal(edge, e -> true, true);
  }

  private EdgeDefUseData getVarsWithTraversal(
      CFAEdge startEdge,
      Predicate<CFAEdge> goFurther,
      boolean visitStartedThreadFunction) {
    var uses = EdgeDefUseData.empty();
    final var exploredEdges = new ArrayList<CFAEdge>();
    final var edgesToExplore = new ArrayList<>(List.of(startEdge));
    while (!edgesToExplore.isEmpty()) {
      final var edge = edgesToExplore.removeFirst();
      exploredEdges.add(edge);
      uses = uses.merge(getDirectlyUsedGlobalVars(edge));
      if (goFurther.test(edge)) {
        for (final var successorEdge : getSuccessorEdges(edge, visitStartedThreadFunction)) {
          if (!exploredEdges.contains(successorEdge)) {
            edgesToExplore.add(successorEdge);
          }
        }
      }
    }
    return uses;
  }

  private Iterable<CFAEdge> getSuccessorEdges(CFAEdge edge, boolean visitStartedThreadFunction) {
    final var allLeavingEdges = edge.getSuccessor().getAllLeavingEdges();
    if (!visitStartedThreadFunction) {
      return allLeavingEdges;
    }

    final var startedThreadEdges = new ArrayList<CFAEdge>();
    for (final var leavingEdge : allLeavingEdges) {
      if (leavingEdge instanceof AStatementEdge statementEdge) {
        if (statementEdge.getStatement() instanceof AFunctionCall functionCall) {
          if (functionCall
              .getFunctionCallExpression()
              .getFunctionNameExpression()
              instanceof AIdExpression functionName) {
            if ("pthread_create".equals(functionName.getName())) {
              final var params =
                  functionCall.getFunctionCallExpression().getParameterExpressions();
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

  private boolean intersect(
      EdgeDefUseData access1,
      EdgeDefUseData access2,
      PORPrecision precision) {
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
      Iterable<MemoryLocation> access1,
      Iterable<MemoryLocation> access2,
      PORPrecision precision) {
    for (var o1 : access1) {
      for (var o2 : access2) {
        if (o1.getExtendedQualifiedName().equals(o2.getExtendedQualifiedName())) {
          return !precision.canIgnoreVariable(o1);
        }
      }
    }
    return false;
  }
}

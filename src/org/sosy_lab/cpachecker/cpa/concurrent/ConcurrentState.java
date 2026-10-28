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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithThreads;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.composite.BasicBlockAggregator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ConcurrentState extends AbstractSingleWrapperState
    implements AbstractState, AbstractStateWithLocations, AbstractStateWithThreads, Graphable {

  /**
   * Shared by reference across every {@link ConcurrentState} of one analysis run (including across
   * CEGAR refinement rounds), so successive shuffles draw fresh values instead of each state
   * restarting the sequence; only the initial seed is fixed, for reproducibility.
   */
  protected final Random random;

  protected final CFA cfa;

  protected final LogManager logger;

  protected final ImmutableMap<Integer, ThreadState> threads;

  /** Thread instances created so far along this path and not yet joined away. */
  protected final ImmutableSet<Integer> liveThreadIds;

  /**
   * Maps a thread handle variable's qualified name to the ID of the thread most recently created
   * through it by {@code pthread_create(&name, ...)}; only plain variables are tracked, not array
   * elements, struct fields, etc. It is not a cache for merging: it is a shortcut for resolving
   * which thread a later {@code pthread_join(name, ...)} refers to. For example, after {@code
   * pthread_create(&t, ...)} created thread 1, the map holds {@code t -> 1}, so {@code
   * pthread_join(t, ...)} joins thread 1 directly (see {@link ConcurrentTransferRelation}'s join
   * dispatch) instead of branching over all live threads and filtering each candidate with a
   * synthetic assumption in the wrapped analysis. If the handle has no entry, the general
   * candidate-branching is used.
   *
   * <p>The map decides which thread a join targets, so it is part of {@link #equals}, {@link
   * #hashCode}, merging and coverage: two states with different maps must not be merged or covered
   * by each other.
   */
  protected final ImmutableMap<String, Integer> handleToLastCreatedThreadId;

  /** Transient mapping from cloned outgoing edges to their originating thread ID. */
  protected final IdentityHashMap<CFAEdge, Integer> edgeThreadIds = new IdentityHashMap<>();

  protected ConcurrentState(
      AbstractState pWrappedState,
      CFA pCfa,
      LogManager pLogger,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLiveThreadIds,
      ImmutableMap<String, Integer> pHandleToLastCreatedThreadId,
      Random pRandom) {
    super(pWrappedState);

    cfa = pCfa;
    logger = pLogger;
    threads = pThreads;
    liveThreadIds = pLiveThreadIds;
    handleToLastCreatedThreadId = pHandleToLastCreatedThreadId;
    random = pRandom;
  }

  protected ConcurrentState update(
      AbstractState pWrappedState,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLiveThreadIds,
      ImmutableMap<String, Integer> pHandleToLastCreatedThreadId) {
    return new ConcurrentState(
        pWrappedState, cfa, logger, pThreads, pLiveThreadIds, pHandleToLastCreatedThreadId, random);
  }

  public ImmutableMap<Integer, ThreadState> threads() {
    return threads;
  }

  public ImmutableSet<Integer> liveThreadIds() {
    return liveThreadIds;
  }

  /** Compatibility accessor for witness export; prefer {@link #liveThreadIds()} in the CPA. */
  public ImmutableSet<Integer> livePids() {
    return liveThreadIds;
  }

  /**
   * The ID of the thread most recently created through the given handle variable, if tracked (see
   * {@link #handleToLastCreatedThreadId}), or empty.
   */
  public OptionalInt getLastCreatedThreadId(String qualifiedName) {
    Integer hint = handleToLastCreatedThreadId.get(qualifiedName);
    return hint == null ? OptionalInt.empty() : OptionalInt.of(hint);
  }

  /**
   * Returns the thread ID the given node was cloned for, or empty if the node is not a cloned POR
   * node. Since the CFA is cloned per thread, a cloned node uniquely identifies the thread it
   * belongs to. The ID is assigned in creation order with the main thread having ID 0, which
   * matches the thread IDs used in the witnesses.
   */
  public static OptionalInt getThreadIdForClonedNode(CFANode pNode) {
    return pNode.getCloneThreadId();
  }

  boolean canMerge(ConcurrentState other) {
    return threads.equals(other.threads)
        && liveThreadIds.equals(other.liveThreadIds)
        && handleToLastCreatedThreadId.equals(other.handleToLastCreatedThreadId);
  }

  /**
   * Adds a new thread instance. {@code pIsJoinable} is false only for the main thread (created
   * synthetically at analysis start, not via a real {@code pthread_create}): nothing can join it,
   * so it is not a join candidate. {@code pHandleQualifiedName}, if given, records this thread ID
   * as the thread created through that variable (see {@link #handleToLastCreatedThreadId}) — last
   * write wins, matching ordinary variable semantics if the same storage is reused for a later
   * create.
   */
  ConcurrentState addNewThread(
      boolean pIsJoinable, Optional<String> pHandleQualifiedName, ThreadState pInitialThreadState) {
    final int newThreadId = threads.size();
    final ImmutableMap<Integer, ThreadState> newThreads =
        ImmutableMap.<Integer, ThreadState>builder()
            .putAll(threads)
            .put(newThreadId, pInitialThreadState)
            .buildKeepingLast();
    final ImmutableSet<Integer> newLiveThreadIds =
        pIsJoinable
            ? ImmutableSet.<Integer>builder().addAll(liveThreadIds).add(newThreadId).build()
            : liveThreadIds;
    final ImmutableMap<String, Integer> newHandleToLastCreatedThreadId =
        pHandleQualifiedName
            .map(
                pS ->
                    ImmutableMap.<String, Integer>builder()
                        .putAll(handleToLastCreatedThreadId)
                        .put(pS, newThreadId)
                        .buildKeepingLast())
            .orElse(handleToLastCreatedThreadId);
    return update(getWrappedState(), newThreads, newLiveThreadIds, newHandleToLastCreatedThreadId);
  }

  /**
   * Joins the given candidate thread instance (one of {@link #liveThreadIds}), blocking (returning
   * empty) until it has finished. Which candidate a given {@code pthread_join} call actually
   * targets is resolved by the transfer relation — either directly, via {@link
   * #getLastCreatedThreadId} for the common case of a plain handle variable, or by branching over
   * every live candidate and keeping only the ones the wrapped analysis finds feasible (see
   * ConcurrentTransferRelation's join dispatch) — not by this method.
   */
  Optional<ConcurrentState> joinThread(int pThreadId) {
    if (!canJoin(pThreadId)) {
      return Optional.empty();
    }

    final ImmutableMap<Integer, ThreadState> newThreads =
        threads.entrySet().stream()
            .filter(e -> e.getKey() != pThreadId)
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    final ImmutableSet<Integer> newLiveThreadIds =
        liveThreadIds.stream()
            .filter(threadId -> threadId != pThreadId)
            .collect(ImmutableSet.toImmutableSet());
    return Optional.of(
        update(getWrappedState(), newThreads, newLiveThreadIds, handleToLastCreatedThreadId));
  }

  private boolean canJoin(int pThreadId) {
    var threadState = threads.get(pThreadId);
    return threadState != null && !threadState.getOutgoingEdges().iterator().hasNext();
  }

  /**
   * Whether a {@code pthread_join} call could actually proceed from this state right now — must
   * mirror {@link ConcurrentTransferRelation}'s join dispatch exactly (see the call site's
   * comment).
   */
  private boolean isJoinCurrentlyEnabled(AFunctionCall pJoinCall) {
    var params = pJoinCall.getFunctionCallExpression().getParameterExpressions();
    if (!params.isEmpty() && params.getFirst() instanceof CExpression handle) {
      Optional<String> handleKey = ThreadFunctions.canonicalHandleLvalueKey(handle);
      if (handleKey.isPresent()) {
        Integer hint = handleToLastCreatedThreadId.get(handleKey.orElseThrow());
        if (hint != null && liveThreadIds.contains(hint)) {
          return canJoin(hint);
        }
      }
    }
    return liveThreadIds.stream().anyMatch(this::canJoin);
  }

  public ConcurrentState stepThread(int pThreadId, ThreadState pNextThreadState) {
    assert threads.containsKey(pThreadId) : "threads must contain thread ID to step " + pThreadId;
    final ImmutableMap.Builder<Integer, ThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pThreadId) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(pThreadId, pNextThreadState);
    return update(
        getWrappedState(),
        newThreads.buildKeepingLast(),
        liveThreadIds,
        handleToLastCreatedThreadId);
  }

  ConcurrentState withWrappedState(AbstractState pWrappedState) {
    return update(pWrappedState, threads, liveThreadIds, handleToLastCreatedThreadId);
  }

  /**
   * Returns the thread ID that produced the given cloned outgoing edge. This mapping is populated
   * during {@link #getOutgoingEdges()}.
   */
  public Integer getEdgeThreadId(CFAEdge edge) {
    return edgeThreadIds.get(edge);
  }

  public CFAEdge getNextBasicBlockEdge(int threadId) {
    var threadState = threads.get(threadId);
    if (threadState == null) {
      throw new IllegalArgumentException("No thread with ID " + threadId);
    }

    CFANode locationNode = threadState.getLocationNode();
    CFANode clonedNode = ConcurrentEdgeCloner.getClonedNode(locationNode, threadId, cfa);
    var leavingEdges = clonedNode.getLeavingEdges();
    assert leavingEdges.size() == 1 : "Expected exactly one leaving edge for basic block stepping";
    CFAEdge cloned = leavingEdges.get(0);
    edgeThreadIds.put(cloned, threadId);

    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    if (mutexState != null) {
      mutexState.addEdgePids(edgeThreadIds);
    }

    return cloned;
  }

  @Override
  public int getNumberOfActiveThreads() {
    return threads.size();
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    ImmutableSet.Builder<CFANode> nodes = ImmutableSet.builder();
    for (ThreadState threadState : threads.values()) {
      for (CFANode node : threadState.getLocationNodes()) {
        nodes.add(node);
      }
    }
    return nodes.build();
  }

  @Override
  public Object getPartitionKey() {
    Object wrappedPartitionKey = super.getPartitionKey();
    if (wrappedPartitionKey == null) {
      return getLocationNodes();
    }
    return ImmutableList.of(getLocationNodes(), wrappedPartitionKey);
  }

  @Override
  public ImmutableList<CFAEdge> getOutgoingEdges() {
    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    edgeThreadIds.clear();
    ImmutableList.Builder<CFAEdge> ret = ImmutableList.builder();
    for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
      int threadId = entry.getKey();
      ThreadState threadState = entry.getValue();

      // Atomic block filtering: if another thread holds the atomic block, this thread is blocked.
      OptionalInt atomicHolder =
          mutexState != null ? mutexState.getAtomicHolder() : OptionalInt.empty();
      if (atomicHolder.isPresent() && atomicHolder.orElseThrow() != threadId) {
        continue;
      }

      CFANode locationNode = threadState.getLocationNode();
      CFANode clonedNode = ConcurrentEdgeCloner.getClonedNode(locationNode, threadId, cfa);
      for (CFAEdge cloned : clonedNode.getLeavingEdges()) {

        if (mutexState != null) {
          // Mutex lock filtering: if this edge is a lock call and the mutex is held by another
          // thread, this thread is blocked and cannot proceed along this edge.
          Optional<MutexLock> lockMutex = MutexFunctions.getLockMutex(cloned);
          if (lockMutex.isPresent()
              && mutexState.isMutexBlockedFor(lockMutex.orElseThrow(), threadId)) {
            continue;
          }
        }

        // Shortcut for pthread_join: only include if it could actually proceed right now. This
        // must be checked here, not just left to the transfer relation's join dispatch as POR
        // decides which edges are "enabled" from this list alone, before any edge is actually
        // processed.
        if (cloned instanceof AStatementEdge statementEdge
            && statementEdge.getStatement() instanceof AFunctionCall functionCall
            && functionCall.getFunctionCallExpression().getFunctionNameExpression()
                instanceof AIdExpression functionName
            && ThreadFunctions.isJoinFunction(functionName.getName())
            && !isJoinCurrentlyEnabled(functionCall)) {
          continue;
        }
        edgeThreadIds.put(cloned, threadId);
        ret.add(cloned);
      }
    }

    if (mutexState != null) {
      mutexState.addEdgePids(edgeThreadIds);
    }

    return ret.build();
  }

  /**
   * Returns the edges to explore from this state. This implementation explores all outgoing edges;
   * {@link SPORConcurrentState} overrides it to return only the edges selected by the partial-order
   * reduction, for which it needs both parameters and can fail.
   */
  @SuppressWarnings("unused") // parameters and exception exist for the overriding implementation
  ImmutableCollection<CFAEdge> getEdgesToExplore(
      ConcurrentPrecision precision, BasicBlockAggregator basicBlock) throws CPATransferException {
    return getOutgoingEdges();
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    for (ThreadState threadState : threads.values()) {
      for (CFAEdge edge : threadState.getIncomingEdges()) {
        edges.add(edge);
      }
    }
    return edges.build();
  }

  @Override
  public List<CFAEdge> getEdgesToChild(AbstractStateWithLocations pChild) {
    if (pChild instanceof ConcurrentState child) {
      ThreadState parentThreadState = null;
      ThreadState childThreadState = null;
      for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
        int threadId = entry.getKey();
        ThreadState currentParentState = entry.getValue();
        ThreadState currentChildState = child.threads().get(threadId);
        if (currentChildState != null
            && !currentParentState.getLocationNode().equals(currentChildState.getLocationNode())) {
          if (parentThreadState != null) {
            // Multiple threads changed: collect edges from all changed threads
            ImmutableList.Builder<CFAEdge> allEdges = ImmutableList.builder();
            for (Entry<Integer, ThreadState> entry2 : threads.entrySet()) {
              int tid = entry2.getKey();
              ThreadState pState = entry2.getValue();
              ThreadState cState = child.threads().get(tid);
              if (cState != null && !pState.getLocationNode().equals(cState.getLocationNode())) {
                allEdges.addAll(pState.getEdgesToChild(cState));
              }
            }
            return allEdges.build();
          }
          parentThreadState = currentParentState;
          childThreadState = currentChildState;
        }
      }

      if (parentThreadState == null || childThreadState == null) {
        if (!threads.keySet().equals(child.threads().keySet())) {
          // Thread set changed (thread created/destroyed) but no location changed.
          // Find a thread that exists in parent whose location edges lead to the child.
          for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
            int threadId = entry.getKey();
            ThreadState pState = entry.getValue();
            ThreadState cState = child.threads().get(threadId);
            if (cState != null) {
              List<CFAEdge> edges = pState.getEdgesToChild(cState);
              if (!edges.isEmpty()) {
                return edges;
              }
            }
          }
        }
        return ImmutableList.of();
      }

      return parentThreadState.getEdgesToChild(childThreadState);
    }
    return ImmutableList.of();
  }

  @Override
  public String toDOTLabel() {
    return "["
        + threads.keySet().stream()
            .sorted()
            .map(e -> e + ": " + threads.get(e).getLocationNode())
            .collect(Collectors.joining(", "))
        + ((CompositeState) getWrappedState()).toDOTLabel()
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
    return o instanceof ConcurrentState other
        && Objects.equals(threads, other.threads)
        && Objects.equals(liveThreadIds, other.liveThreadIds)
        && Objects.equals(handleToLastCreatedThreadId, other.handleToLastCreatedThreadId)
        && Objects.equals(getWrappedState(), other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, liveThreadIds, handleToLastCreatedThreadId, getWrappedState());
  }
}

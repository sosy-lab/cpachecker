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
import org.checkerframework.checker.nullness.qual.Nullable;
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
  protected final ImmutableSet<Integer> livePids;

  /**
   * Fast-path hint from a handle variable's qualified name to the pid it was last assigned by
   * {@code pthread_create(&name, ...)}, for the common case where the handle is a plain variable
   * (not an array element, struct field, ...). Excluded from {@link #equals}/{@link #hashCode} — it
   * is pure optimization, never load-bearing for correctness (see {@link
   * ConcurrentTransferRelation}'s join dispatch, which falls back to the general
   * candidate-branching mechanism whenever no hint applies), so two states that agree on everything
   * else may still merge/cover regardless of it.
   */
  protected final ImmutableMap<String, Integer> handleHints;

  /** Transient mapping from cloned outgoing edges to their originating thread PID. */
  protected final IdentityHashMap<CFAEdge, Integer> edgePidMap = new IdentityHashMap<>();

  protected ConcurrentState(
      AbstractState pWrappedState,
      CFA pCfa,
      LogManager pLogger,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints,
      Random pRandom) {
    super(pWrappedState);

    cfa = pCfa;
    logger = pLogger;
    threads = pThreads;
    livePids = pLivePids;
    handleHints = pHandleHints;
    random = pRandom;
  }

  protected ConcurrentState update(
      AbstractState pWrappedState,
      ImmutableMap<Integer, ThreadState> pThreads,
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints) {
    return new ConcurrentState(
        pWrappedState, cfa, logger, pThreads, pLivePids, pHandleHints, random);
  }

  public ImmutableMap<Integer, ThreadState> threads() {
    return threads;
  }

  public ImmutableSet<Integer> livePids() {
    return livePids;
  }

  /**
   * The fast-path candidate hint for a handle's qualified name, if any (see {@link #handleHints}),
   * or null.
   */
  public @Nullable Integer getHandleHint(String qualifiedName) {
    return handleHints.get(qualifiedName);
  }

  /**
   * Returns the thread ID (PID) the given node was cloned for, or empty if the node is not a cloned
   * POR node. Since the CFA is cloned per thread, a cloned node uniquely identifies the thread it
   * belongs to. The PID is assigned in creation order with the main thread having PID 0, which
   * matches the thread IDs used in the witnesses.
   */
  public static OptionalInt getThreadIdForClonedNode(CFANode pNode) {
    return ConcurrentEdgeCloner.getThreadIdForNode(pNode);
  }

  boolean canMerge(ConcurrentState other) {
    return threads.equals(other.threads) && livePids.equals(other.livePids);
  }

  /**
   * Adds a new thread instance. {@code pAddToLivePids} is false only for the main thread (created
   * synthetically at analysis start, not via a real {@code pthread_create}): nothing can join it,
   * so it was never made a join candidate under the old handle-name scheme either. {@code
   * pHandleQualifiedName}, if given, records this pid as the fast-path join candidate for that
   * variable (see {@link #handleHints}) — last write wins, matching ordinary variable semantics if
   * the same storage is reused for a later create.
   */
  ConcurrentState addNewThread(
      boolean pAddToLivePids,
      Optional<String> pHandleQualifiedName,
      ThreadState pInitialThreadState) {
    final int newPid = threads.size();
    final ImmutableMap<Integer, ThreadState> newThreads =
        ImmutableMap.<Integer, ThreadState>builder()
            .putAll(threads)
            .put(newPid, pInitialThreadState)
            .buildKeepingLast();
    final ImmutableSet<Integer> newLivePids =
        pAddToLivePids
            ? ImmutableSet.<Integer>builder().addAll(livePids).add(newPid).build()
            : livePids;
    final ImmutableMap<String, Integer> newHandleHints =
        pHandleQualifiedName
            .map(
                pS ->
                    ImmutableMap.<String, Integer>builder()
                        .putAll(handleHints)
                        .put(pS, newPid)
                        .buildKeepingLast())
            .orElse(handleHints);
    return update(getWrappedState(), newThreads, newLivePids, newHandleHints);
  }

  /**
   * Joins the given candidate thread instance (one of {@link #livePids}), blocking (returning null)
   * until it has finished. Which candidate a given {@code pthread_join} call actually targets is
   * resolved by the transfer relation — either directly, via {@link #getHandleHint} for the common
   * case of a plain handle variable, or by branching over every live candidate and keeping only the
   * ones the wrapped analysis finds feasible (see ConcurrentTransferRelation's join dispatch) — not
   * by this method.
   */
  Optional<ConcurrentState> joinThread(int pPid) {
    if (!canJoin(pPid)) {
      return Optional.empty();
    }

    final ImmutableMap<Integer, ThreadState> newThreads =
        threads.entrySet().stream()
            .filter(e -> e.getKey() != pPid)
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    final ImmutableSet<Integer> newLivePids =
        livePids.stream().filter(pid -> pid != pPid).collect(ImmutableSet.toImmutableSet());
    return Optional.of(update(getWrappedState(), newThreads, newLivePids, handleHints));
  }

  private boolean canJoin(int pPid) {
    var threadState = threads.get(pPid);
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
        Integer hint = handleHints.get(handleKey.get());
        if (hint != null && livePids.contains(hint)) {
          return canJoin(hint);
        }
      }
    }
    return livePids.stream().anyMatch(this::canJoin);
  }

  public ConcurrentState stepThread(int pPid, ThreadState pNextThreadState) {
    assert threads.containsKey(pPid) : "threads must contain pid to step " + pPid;
    final ImmutableMap.Builder<Integer, ThreadState> newThreads = ImmutableMap.builder();
    for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
      if (entry.getKey() != pPid) {
        newThreads.put(entry.getKey(), entry.getValue());
      }
    }
    newThreads.put(pPid, pNextThreadState);
    return update(getWrappedState(), newThreads.buildKeepingLast(), livePids, handleHints);
  }

  ConcurrentState withWrappedState(AbstractState pWrappedState) {
    return update(pWrappedState, threads, livePids, handleHints);
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

    CFANode locationNode = threadState.getLocationNode();
    CFANode clonedNode = ConcurrentEdgeCloner.getClonedNode(locationNode, pid, cfa);
    var leavingEdges = clonedNode.getLeavingEdges();
    assert leavingEdges.size() == 1 : "Expected exactly one leaving edge for basic block stepping";
    CFAEdge cloned = leavingEdges.get(0);
    edgePidMap.put(cloned, pid);

    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
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
    return getLocationNodes();
  }

  @Override
  public ImmutableList<CFAEdge> getOutgoingEdges() {
    MutexState mutexState = AbstractStates.extractStateByType(getWrappedState(), MutexState.class);
    edgePidMap.clear();
    ImmutableList.Builder<CFAEdge> ret = ImmutableList.builder();
    for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
      int pid = entry.getKey();
      ThreadState threadState = entry.getValue();

      // Atomic block filtering: if another thread holds the atomic block, this thread is blocked.
      Integer atomicHolder = mutexState != null ? mutexState.getAtomicHolder() : null;
      if (atomicHolder != null && atomicHolder != pid) {
        continue;
      }

      CFANode locationNode = threadState.getLocationNode();
      CFANode clonedNode = ConcurrentEdgeCloner.getClonedNode(locationNode, pid, cfa);
      for (CFAEdge cloned : clonedNode.getLeavingEdges()) {

        if (mutexState != null) {
          // Mutex lock filtering: if this edge is a lock call and the mutex is held by another
          // thread, this thread is blocked and cannot proceed along this edge.
          Optional<MutexLock> lockMutex = MutexFunctions.getLockMutex(cloned);
          if (lockMutex.isPresent() && mutexState.isMutexBlockedFor(lockMutex.get(), pid)) {
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
        edgePidMap.put(cloned, pid);
        ret.add(cloned);
      }
    }

    if (mutexState != null) {
      mutexState.addEdgePids(edgePidMap);
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
  public @Nullable List<CFAEdge> getEdgesToChild(AbstractStateWithLocations pChild) {
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
                var edges = pState.getEdgesToChild(cState);
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

      if (parentThreadState == null || childThreadState == null) {
        if (!threads.keySet().equals(child.threads().keySet())) {
          // Thread set changed (thread created/destroyed) but no location changed.
          // Find a thread that exists in parent whose location edges lead to the child.
          for (Entry<Integer, ThreadState> entry : threads.entrySet()) {
            int threadId = entry.getKey();
            ThreadState pState = entry.getValue();
            ThreadState cState = child.threads().get(threadId);
            if (cState != null) {
              var edges = pState.getEdgesToChild(cState);
              if (edges != null && !edges.isEmpty()) {
                return edges;
              }
            }
          }
        }
        return ImmutableList.of();
      }

      return parentThreadState.getEdgesToChild(childThreadState);
    }
    return null;
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
        && Objects.equals(livePids, other.livePids)
        && Objects.equals(getWrappedState(), other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, livePids, getWrappedState());
  }
}

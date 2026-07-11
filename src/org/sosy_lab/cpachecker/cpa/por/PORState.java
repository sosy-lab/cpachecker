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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
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
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock;
import org.sosy_lab.cpachecker.cpa.mutex.MutexState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PORState
    extends AbstractSingleWrapperState
    implements AbstractState, AbstractStateWithLocations,
               AbstractStateWithThreads, Graphable {

  /**
   * Shared by reference across every {@link PORState} of one analysis run (including across
   * CEGAR refinement rounds), so successive shuffles draw fresh values instead of each state
   * restarting the sequence; only the initial seed is fixed, for reproducibility.
   */
  private final Random random;

  private final CFA cfa;

  private final ImmutableMap<Integer, PORThreadState> threads;

  /** Thread instances created so far along this path and not yet joined away. */
  private final ImmutableSet<Integer> livePids;

  /**
   * Fast-path hint from a handle variable's qualified name to the pid it was last assigned by
   * {@code pthread_create(&name, ...)}, for the common case where the handle is a plain variable
   * (not an array element, struct field, ...). Excluded from {@link #equals}/{@link #hashCode} —
   * it is pure optimization, never load-bearing for correctness (see {@link
   * PORTransferRelation}'s join dispatch, which falls back to the general candidate-branching
   * mechanism whenever no hint applies), so two states that agree on everything else may still
   * merge/cover regardless of it.
   */
  private final ImmutableMap<String, Integer> handleHints;

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
      ImmutableSet<Integer> pLivePids,
      ImmutableMap<String, Integer> pHandleHints,
      Random pRandom) {
    super(pWrappedState);

    cfa = pCfa;
    threads = pThreads;
    livePids = pLivePids;
    handleHints = pHandleHints;
    random = pRandom;
  }

  static PORState empty(AbstractState pWrappedInitialState, CFA pCfa, Random pRandom) {
    return new PORState(
        pWrappedInitialState, pCfa, ImmutableMap.of(), ImmutableSet.of(), ImmutableMap.of(),
        pRandom);
  }

  public ImmutableMap<Integer, PORThreadState> threads() {
    return threads;
  }

  public ImmutableSet<Integer> livePids() {
    return livePids;
  }

  /** The fast-path candidate hint for a handle's qualified name, if any (see {@link
   * #handleHints}), or null. */
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
    return PorEdgeCloner.getThreadIdForNode(pNode);
  }

  boolean canMerge(PORState other) {
    return threads.equals(other.threads) && livePids.equals(other.livePids);
  }

  /**
   * Adds a new thread instance. {@code pAddToLivePids} is false only for the main thread (created
   * synthetically at analysis start, not via a real {@code pthread_create}): nothing can join it,
   * so it was never made a join candidate under the old handle-name scheme either.
   * {@code pHandleQualifiedName}, if given, records this pid as the fast-path join candidate for
   * that variable (see {@link #handleHints}) — last write wins, matching ordinary variable
   * semantics if the same storage is reused for a later create.
   */
  PORState addNewThread(
      boolean pAddToLivePids,
      @Nullable String pHandleQualifiedName,
      LocationState pInitialLoc,
      CallstackState pInitialStack) {
    final int newPid = threads.size();
    final ImmutableMap<Integer, PORThreadState> newThreads =
        ImmutableMap.<Integer, PORThreadState>builder()
            .putAll(threads)
            .put(newPid, new PORThreadState(pInitialLoc, pInitialStack))
            .buildKeepingLast();
    final ImmutableSet<Integer> newLivePids =
        pAddToLivePids
        ? ImmutableSet.<Integer>builder().addAll(livePids).add(newPid).build()
        : livePids;
    final ImmutableMap<String, Integer> newHandleHints =
        pHandleQualifiedName == null
        ? handleHints
        : ImmutableMap.<String, Integer>builder()
            .putAll(handleHints)
            .put(pHandleQualifiedName, newPid)
            .buildKeepingLast();
    return new PORState(getWrappedState(), cfa, newThreads, newLivePids, newHandleHints, random);
  }

  /**
   * Joins the given candidate thread instance (one of {@link #livePids}), blocking (returning
   * null) until it has finished. Which candidate a given {@code pthread_join} call actually
   * targets is resolved by the transfer relation — either directly, via {@link #getHandleHint} for
   * the common case of a plain handle variable, or by branching over every live candidate and
   * keeping only the ones the wrapped analysis finds feasible (see PORTransferRelation's join
   * dispatch) — not by this method.
   */
  PORState joinThread(int pPid) {
    if (!canJoin(pPid)) {
      return null;
    }

    final ImmutableMap<Integer, PORThreadState> newThreads =
        threads.entrySet().stream()
            .filter(e -> e.getKey() != pPid)
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    final ImmutableSet<Integer> newLivePids =
        livePids.stream().filter(pid -> pid != pPid).collect(ImmutableSet.toImmutableSet());
    return new PORState(getWrappedState(), cfa, newThreads, newLivePids, handleHints, random);
  }

  private boolean canJoin(int pPid) {
    var threadState = threads.get(pPid);
    return threadState != null && !threadState.pLocationState().getOutgoingEdges().iterator().hasNext();
  }

  /**
   * Whether a {@code pthread_join} call could actually proceed from this state right now — must
   * mirror {@link PORTransferRelation}'s join dispatch exactly (see the call site's comment).
   */
  private boolean isJoinCurrentlyEnabled(AFunctionCall pJoinCall) {
    var params = pJoinCall.getFunctionCallExpression().getParameterExpressions();
    if (!params.isEmpty() && params.get(0) instanceof CExpression handle) {
      String handleKey = ThreadFunctions.canonicalHandleLvalueKey(handle);
      if (handleKey != null) {
        Integer hint = handleHints.get(handleKey);
        if (hint != null && livePids.contains(hint)) {
          return canJoin(hint);
        }
      }
    }
    return livePids.stream().anyMatch(this::canJoin);
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
    return new PORState(
        getWrappedState(), cfa, newThreads.buildKeepingLast(), livePids, handleHints, random);
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
    return new PORState(pWrappedState, cfa, threads, livePids, handleHints, random);
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
    ImmutableSet.Builder<CFANode> nodes = ImmutableSet.builder();
    for (PORThreadState threadState : threads.values()) {
      for (CFANode node : threadState.pLocationState().getLocationNodes()) {
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
  public Iterable<CFAEdge> getOutgoingEdges() {
    try {
      return getAllThreadOutgoingEdges();
    } catch (CPATransferException e) {
      // getOutgoingEdges() is only used for post-hoc graph/witness display of states that were
      // already successfully explored via getSourceSet(), so reaching an unsupported construct
      // here would indicate a real bug rather than an expected occurrence.
      throw new IllegalStateException(e);
    }
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
    if (!(o instanceof PORState other)) {
      return false;
    }
    return Objects.equals(threads, other.threads)
        && Objects.equals(livePids, other.livePids)
        && Objects.equals(getWrappedState(), other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, livePids, getWrappedState());
  }

  private ImmutableCollection<CFAEdge> getAllThreadOutgoingEdges() throws CPATransferException {
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

        // Shortcut for pthread_join: only include if it could actually proceed right now. This
        // must be checked here, not just left to the transfer relation's join dispatch: the
        // source-set/persistent-set reduction decides which edges are "enabled" from this list
        // alone, before any edge is actually processed. A join edge that is offered here but
        // turns out blocked makes the reduction pick it as a (spuriously) sufficient persistent
        // set on its own, discarding the other threads' genuinely enabled edges from this state —
        // a real interleaving is then lost, not just a schedule this edge happens not to take.
        // Mirrors the transfer relation's join dispatch exactly: if the handle is a plain variable
        // with a definite fast-path candidate (see PORState#handleHints), only that candidate's
        // completion matters (the dispatch only ever tries that one, no fallback); otherwise any
        // live candidate finishing is enough, matching the general candidate-branching path.
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

  // POR algorithm

  Collection<CFAEdge> getSourceSet(PORPrecision precision, BasicBlockAggregator basicBlock)
      throws CPATransferException {
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

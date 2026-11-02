// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_JOIN;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.extractParamName;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.getLockId;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.isLastNodeOfThread;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessViolationV2Parser;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** This immutable state represents a location state combined with a callstack state. */
public class ThreadingState
    implements AbstractState,
        AbstractStateWithAssumptions,
        AbstractStateWithLocations,
        Graphable,
        Partitionable,
        AbstractQueryableState {

  private static final String PROPERTY_DEADLOCK = "deadlock";

  /**
   * Marks a thread that was created but whose identifier in the witness is not known yet, cf.
   * {@link #threadIdsForWitness}. It never matches the current thread, because identifiers of a
   * witness are never negative.
   */
  private static final int AWAITING_WITNESS_THREAD_ID = -1;

  /**
   * Marks a thread that the witness does not refer to, because it did not pass a waypoint for the
   * creation of that thread, cf. {@link #threadIdsForWitness}.
   */
  private static final int NO_WITNESS_THREAD_ID = -2;

  static final int MIN_THREAD_NUM = 0;

  // String :: identifier for the thread TODO change to object or memory-location
  // CallstackState +  LocationState :: thread-position
  private final PersistentMap<String, ThreadState> threads;

  // String :: lock-id  -->  String :: thread-id
  private final PersistentMap<String, String> locks;

  // String :: lock-id of a mutex of type PTHREAD_MUTEX_RECURSIVE
  //   -->  Integer :: how often its holder locked it again
  private final PersistentMap<String, Integer> recursiveMutexes;

  // String :: identifier of a mutex attribute of type PTHREAD_MUTEX_RECURSIVE
  private final ImmutableSet<String> recursiveMutexAttributes;

  // String :: thread-id of a thread that runs the init routine of pthread_once  -->  OnceRun
  private final PersistentMap<String, OnceRun> onceRunners;

  // String :: identifier of a pthread_once_t whose init routine has finished
  private final ImmutableSet<String> finishedOnces;

  // String :: identifier of a pthread_barrier_t  -->  Barrier
  private final PersistentMap<String, Barrier> barriers;

  /**
   * Assumptions about the program state that hold after the edge that produced this state, cf.
   * {@link #getAssumptions()}. They are not part of {@link #equals}, because the other CPAs already
   * restrict their states with them.
   */
  private final ImmutableList<CExpression> assumptions;

  /**
   * Thread-id of last active thread that produced this exact {@link ThreadingState}. This value
   * should only be set in {@link ThreadingTransferRelation#getAbstractSuccessorsForEdge} and must
   * be deleted in {@link ThreadingTransferRelation#strengthen}, e.g. set to {@code null}. It is not
   * considered to be part of any 'full' abstract state, but serves as intermediate flag to have
   * information for the strengthening process.
   */
  @Nullable private final String activeThread;

  /**
   * This functioncall was called when creating this thread. This value should only be set in {@link
   * ThreadingTransferRelation#getAbstractSuccessorsForEdge} when creating a new thread, i.e., only
   * for the first state in the new thread, which is directly after the function call.
   *
   * <p>It must be deleted in {@link ThreadingTransferRelation#strengthen}, e.g. set to {@code
   * null}. It is not considered to be part of any 'full' abstract state, but serves as intermediate
   * flag to have information for the strengthening process.
   */
  @Nullable private final FunctionCallEdge entryFunction;

  /**
   * This map contains the mapping of threadIds to the unique identifier used for witness
   * validation. Without a witness that refers to threads, it should always be empty. It is filled
   * by {@link ThreadingTransferRelation#strengthen}.
   *
   * <p>Besides the identifiers of the witness, which are never negative, the map also holds the two
   * markers {@link #AWAITING_WITNESS_THREAD_ID} and {@link #NO_WITNESS_THREAD_ID}. Entries are
   * never removed, not even when the thread they belong to terminates, so that a thread created
   * afterwards cannot inherit the identifier of a thread that has already run.
   */
  private final PersistentMap<String, Integer> threadIdsForWitness;

  public ThreadingState() {
    threads = PathCopyingPersistentTreeMap.of();
    locks = PathCopyingPersistentTreeMap.of();
    recursiveMutexes = PathCopyingPersistentTreeMap.of();
    recursiveMutexAttributes = ImmutableSet.of();
    onceRunners = PathCopyingPersistentTreeMap.of();
    finishedOnces = ImmutableSet.of();
    barriers = PathCopyingPersistentTreeMap.of();
    assumptions = ImmutableList.of();
    activeThread = null;
    entryFunction = null;
    threadIdsForWitness = PathCopyingPersistentTreeMap.of();
  }

  private ThreadingState(
      PersistentMap<String, ThreadState> pThreads,
      PersistentMap<String, String> pLocks,
      PersistentMap<String, Integer> pRecursiveMutexes,
      ImmutableSet<String> pRecursiveMutexAttributes,
      PersistentMap<String, OnceRun> pOnceRunners,
      ImmutableSet<String> pFinishedOnces,
      PersistentMap<String, Barrier> pBarriers,
      ImmutableList<CExpression> pAssumptions,
      String pActiveThread,
      FunctionCallEdge entryFunction,
      PersistentMap<String, Integer> pThreadIdsForWitness) {
    threads = pThreads;
    locks = pLocks;
    recursiveMutexes = pRecursiveMutexes;
    recursiveMutexAttributes = pRecursiveMutexAttributes;
    onceRunners = pOnceRunners;
    finishedOnces = pFinishedOnces;
    barriers = pBarriers;
    assumptions = pAssumptions;
    activeThread = pActiveThread;
    this.entryFunction = entryFunction;
    threadIdsForWitness = pThreadIdsForWitness;
  }

  private ThreadingState withThreads(PersistentMap<String, ThreadState> pThreads) {
    return new ThreadingState(
        pThreads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  private ThreadingState withLocks(
      PersistentMap<String, String> pLocks, PersistentMap<String, Integer> pRecursiveMutexes) {
    return new ThreadingState(
        threads,
        pLocks,
        pRecursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  private ThreadingState withRecursiveMutexAttributes(
      ImmutableSet<String> pRecursiveMutexAttributes) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        pRecursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  private ThreadingState withOnces(
      PersistentMap<String, OnceRun> pOnceRunners, ImmutableSet<String> pFinishedOnces) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        pOnceRunners,
        pFinishedOnces,
        barriers,
        assumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  private ThreadingState withBarriers(PersistentMap<String, Barrier> pBarriers) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        pBarriers,
        assumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  private ThreadingState withThreadIdsForWitness(
      PersistentMap<String, Integer> pThreadIdsForWitness) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        activeThread,
        entryFunction,
        pThreadIdsForWitness);
  }

  public ThreadingState addThreadAndCopy(
      String id, int num, AbstractState stack, AbstractState loc) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(!threads.containsKey(id), "thread already exists");
    return withThreads(threads.putAndCopy(id, new ThreadState(loc, stack, num)));
  }

  public ThreadingState updateLocationAndCopy(String id, AbstractState stack, AbstractState loc) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(threads.containsKey(id), "updating non-existing thread");
    return withThreads(
        threads.putAndCopy(id, new ThreadState(loc, stack, threads.get(id).getNum())));
  }

  /** Removes the thread. If it ran the init routine of a pthread_once, the routine is finished. */
  public ThreadingState removeThreadAndCopy(String id) {
    Preconditions.checkNotNull(id);
    checkState(threads.containsKey(id), "leaving non-existing thread: %s", id);
    ThreadingState result = withThreads(threads.removeAndCopy(id));
    OnceRun onceRun = onceRunners.get(id);
    if (onceRun == null) {
      return result;
    }
    return result.withOnces(
        onceRunners.removeAndCopy(id),
        ImmutableSet.<String>builder().addAll(finishedOnces).add(onceRun.onceId()).build());
  }

  public Set<String> getThreadIds() {
    return threads.keySet();
  }

  public AbstractState getThreadCallstack(String id) {
    return Preconditions.checkNotNull(threads.get(id).getCallstack());
  }

  public LocationState getThreadLocation(String id) {
    return (LocationState) Preconditions.checkNotNull(threads.get(id).getLocation());
  }

  Set<Integer> getThreadNums() {
    SequencedSet<Integer> result = new LinkedHashSet<>();
    for (ThreadState ts : threads.values()) {
      result.add(ts.getNum());
    }
    Preconditions.checkState(result.size() == threads.size());
    return result;
  }

  int getSmallestMissingThreadNum() {
    int num = MIN_THREAD_NUM;
    // TODO loop is not efficient for big number of threads
    final Set<Integer> threadNums = getThreadNums();
    while (threadNums.contains(num)) {
      num++;
    }
    return num;
  }

  public ThreadingState addLockAndCopy(String threadId, String lockId) {
    Preconditions.checkNotNull(lockId);
    Preconditions.checkNotNull(threadId);
    checkArgument(
        threads.containsKey(threadId),
        "blocking non-existant thread: %s with lock: %s",
        threadId,
        lockId);
    return withLocks(locks.putAndCopy(lockId, threadId), recursiveMutexes);
  }

  /** The thread locks the recursive mutex again, which it already holds. */
  public ThreadingState increaseRecursiveLockAndCopy(String threadId, String lockId) {
    checkArgument(
        hasLock(threadId, lockId) && isRecursiveMutex(lockId),
        "thread %s does not hold recursive lock %s",
        threadId,
        lockId);
    return withLocks(locks, recursiveMutexes.putAndCopy(lockId, recursiveMutexes.get(lockId) + 1));
  }

  public ThreadingState removeLockAndCopy(String threadId, String lockId) {
    Preconditions.checkNotNull(threadId);
    Preconditions.checkNotNull(lockId);
    checkArgument(
        threads.containsKey(threadId),
        "unblocking non-existant thread: %s with lock: %s",
        threadId,
        lockId);
    int count = recursiveMutexes.getOrDefault(lockId, 0);
    if (count > 0 && hasLock(threadId, lockId)) {
      return withLocks(locks, recursiveMutexes.putAndCopy(lockId, count - 1));
    }
    return withLocks(
        locks.removeAndCopy(lockId),
        count > 0 ? recursiveMutexes.putAndCopy(lockId, 0) : recursiveMutexes);
  }

  public boolean isRecursiveMutex(String lockId) {
    return recursiveMutexes.containsKey(lockId);
  }

  /** Sets whether the mutex is recursive, e.g. when it is initialized. */
  public ThreadingState setRecursiveMutexAndCopy(String lockId, boolean recursive) {
    if (recursive == isRecursiveMutex(lockId)) {
      return this;
    }
    return withLocks(
        locks,
        recursive
            ? recursiveMutexes.putAndCopy(lockId, 0)
            : recursiveMutexes.removeAndCopy(lockId));
  }

  public boolean isRecursiveMutexAttribute(String attributeId) {
    return recursiveMutexAttributes.contains(attributeId);
  }

  /** Sets whether the mutex attribute has the type PTHREAD_MUTEX_RECURSIVE. */
  public ThreadingState setRecursiveMutexAttributeAndCopy(String attributeId, boolean recursive) {
    if (recursive == isRecursiveMutexAttribute(attributeId)) {
      return this;
    }
    return withRecursiveMutexAttributes(
        recursive
            ? ImmutableSet.<String>builder()
                .addAll(recursiveMutexAttributes)
                .add(attributeId)
                .build()
            : ImmutableSet.copyOf(
                Sets.difference(recursiveMutexAttributes, ImmutableSet.of(attributeId))));
  }

  /** The thread runner runs the init routine of the pthread_once_t onceId for the thread caller. */
  public ThreadingState startOnceAndCopy(String runner, String onceId, String caller) {
    checkArgument(threads.containsKey(runner), "unknown runner thread: %s", runner);
    return withOnces(onceRunners.putAndCopy(runner, new OnceRun(onceId, caller)), finishedOnces);
  }

  public boolean isOnceFinished(String onceId) {
    return finishedOnces.contains(onceId);
  }

  public boolean isOnceRunning(String onceId) {
    return onceRunners.values().stream().anyMatch(run -> run.onceId().equals(onceId));
  }

  /** Whether the thread waits in pthread_once until the init routine has finished. */
  public boolean isWaitingForOnce(String threadId) {
    return onceRunners.values().stream().anyMatch(run -> run.caller().equals(threadId));
  }

  /**
   * Returns the thread that executes the code of the given thread in the program. For a thread that
   * runs the init routine of pthread_once, this is the thread that called pthread_once.
   */
  public String getProgramThread(String threadId) {
    OnceRun onceRun = onceRunners.get(threadId);
    return onceRun == null ? threadId : getProgramThread(onceRun.caller());
  }

  public @Nullable Barrier getBarrier(String barrierId) {
    return barriers.get(barrierId);
  }

  public ThreadingState setBarrierAndCopy(String barrierId, Barrier barrier) {
    return withBarriers(barriers.putAndCopy(barrierId, barrier));
  }

  /** returns whether any of the threads has the lock */
  public boolean hasLock(String lockId) {
    return locks.containsKey(lockId); // TODO threadId needed?
  }

  /** returns whether the given thread has the lock */
  public boolean hasLock(String threadId, String lockId) {
    return locks.containsKey(lockId) && threadId.equals(locks.get(lockId));
  }

  /** returns whether there is any lock registered for the thread. */
  public boolean hasLockForThread(String threadId) {
    return locks.containsValue(threadId);
  }

  public Set<String> getLocksForThread(String threadId) {
    return FluentIterable.from(locks.entrySet())
        .filter(entry -> entry.getValue().equals(threadId))
        .transform(Map.Entry::getKey)
        .toSet();
  }

  @Override
  public String toString() {
    return "( threads={\n"
        + Joiner.on(",\n ").withKeyValueSeparator("=").join(threads)
        + "}\n and locks={"
        + Joiner.on(",\n ").withKeyValueSeparator("=").join(locks)
        + "}"
        + (recursiveMutexes.isEmpty()
            ? ""
            : ("\n and recursive mutexes={"
                + Joiner.on(", ").withKeyValueSeparator("=").join(recursiveMutexes)
                + "}"))
        + (onceRunners.isEmpty() && finishedOnces.isEmpty()
            ? ""
            : ("\n and once runners={"
                + Joiner.on(", ").withKeyValueSeparator("=").join(onceRunners)
                + "}, finished onces="
                + finishedOnces))
        + (barriers.isEmpty()
            ? ""
            : ("\n and barriers={"
                + Joiner.on(", ").withKeyValueSeparator("=").join(barriers)
                + "}"))
        + (assumptions.isEmpty() ? "" : ("\n assuming " + assumptions))
        + (activeThread == null ? "" : ("\n produced from thread " + activeThread))
        + " \n"
        + Joiner.on(",\n ").withKeyValueSeparator("=").join(threadIdsForWitness)
        + ")";
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ThreadingState ts
        && threads.equals(ts.threads)
        && locks.equals(ts.locks)
        && recursiveMutexes.equals(ts.recursiveMutexes)
        && recursiveMutexAttributes.equals(ts.recursiveMutexAttributes)
        && onceRunners.equals(ts.onceRunners)
        && finishedOnces.equals(ts.finishedOnces)
        && barriers.equals(ts.barriers)
        && Objects.equals(activeThread, ts.activeThread)
        && threadIdsForWitness.equals(ts.threadIdsForWitness);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        activeThread,
        threadIdsForWitness);
  }

  private FluentIterable<AbstractStateWithLocations> getLocations() {
    return FluentIterable.from(threads.values())
        .transform(s -> (AbstractStateWithLocations) s.getLocation());
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return getLocations().transformAndConcat(AbstractStateWithLocations::getLocationNodes);
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    return getLocations().transformAndConcat(AbstractStateWithLocations::getOutgoingEdges);
  }

  @Override
  public Iterable<CFAEdge> getIncomingEdges() {
    return getLocations().transformAndConcat(AbstractStateWithLocations::getIncomingEdges);
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(",\n ").withKeyValueSeparator("=").appendTo(sb, threads);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public Object getPartitionKey() {
    return threads;
  }

  @Override
  public String getCPAName() {
    return "ThreadingCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (PROPERTY_DEADLOCK.equals(pProperty)) {
      try {
        return hasDeadlock();
      } catch (UnrecognizedCodeException e) {
        throw new InvalidQueryException("deadlock-check had a problem", e);
      }
    }
    if (pProperty.startsWith(AutomatonWitnessViolationV2Parser.THREAD_ID_QUERY)) {
      return checkActiveThreadHasWitnessId(
          pProperty,
          pProperty.substring(AutomatonWitnessViolationV2Parser.THREAD_ID_QUERY.length()));
    }

    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  private boolean checkActiveThreadHasWitnessId(String pProperty, String pExpectedWitnessId)
      throws InvalidQueryException {
    int expectedWitnessId;
    try {
      expectedWitnessId = Integer.parseInt(pExpectedWitnessId);
    } catch (NumberFormatException e) {
      throw new InvalidQueryException(
          "Query '" + pProperty + "' does not compare against an integer.", e);
    }
    // The active thread is only known while an edge is being handled, which is the only situation
    // in which a witness automaton evaluates its transition guards.
    checkState(
        activeThread != null, "Query '%s' is only valid while an edge is handled.", pProperty);
    // the witness names the caller of pthread_once for the steps of the init routine
    return Objects.equals(getThreadIdForWitness(getProgramThread(activeThread)), expectedWitnessId);
  }

  /**
   * check, whether one of the outgoing edges can be visited without requiring an already used lock.
   */
  private boolean hasDeadlock() throws UnrecognizedCodeException {
    FluentIterable<CFAEdge> edges = FluentIterable.from(getOutgoingEdges());

    // no need to check for existing locks after program termination -> ok

    // no need to check for existing locks after thread termination
    // -> TODO what about a missing ATOMIC_LOCK_RELEASE?

    // no need to check VERIFIER_ATOMIC, ATOMIC_LOCK or LOCAL_ACCESS_LOCK,
    // because they cannot cause deadlocks, as there is always one thread to go
    // (=> the thread that has the lock).
    // -> TODO what about a missing ATOMIC_LOCK_RELEASE?

    // no outgoing edges, i.e. program terminates -> no deadlock possible
    if (edges.isEmpty()) {
      return false;
    }

    for (CFAEdge edge : edges) {
      if (!needsAlreadyUsedLock(edge) && !isWaitingForOtherThread(edge)) {
        // edge can be visited, thus there is no deadlock
        return false;
      }
    }

    // if no edge can be visited, there is a deadlock
    return true;
  }

  /** check, if the edge required a lock, that is already used. This might cause a deadlock. */
  private boolean needsAlreadyUsedLock(CFAEdge edge) throws UnrecognizedCodeException {
    final String newLock = getLockId(edge);
    if (newLock == null || !hasLock(newLock)) {
      return false;
    }
    // the holder of a recursive mutex can acquire it again
    return !isRecursiveMutex(newLock)
        || !Iterables.contains(getThreadLocation(locks.get(newLock)).getOutgoingEdges(), edge);
  }

  /**
   * A thread might need to wait for another thread, if the other thread joins at the current edge.
   * If the other thread never exits, we have found a deadlock.
   */
  private boolean isWaitingForOtherThread(CFAEdge edge) throws UnrecognizedCodeException {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      AStatement statement = ((AStatementEdge) edge).getStatement();
      if (statement instanceof AFunctionCall aFunctionCall) {
        AExpression functionNameExp =
            aFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression aIdExpression) {
          final String functionName = aIdExpression.getName();
          if (THREAD_JOIN.equals(functionName)) {
            final String joiningThread = extractParamName(statement, 0);
            // check whether other thread is running and has at least one outgoing edge,
            // then we have to wait for it.
            if (threads.containsKey(joiningThread)
                && !isLastNodeOfThread(getThreadLocation(joiningThread).getLocationNode())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /** The init routine of the pthread_once_t onceId, which the thread caller waits for. */
  record OnceRun(String onceId, String caller) {}

  /**
   * A barrier that releases its waiting threads when capacity threads wait. The threads in released
   * can pass their pthread_barrier_wait.
   */
  record Barrier(int capacity, ImmutableSet<String> released) {

    /** The capacity of a barrier that never releases its threads, e.g. for a count of 0. */
    static final int NEVER_RELEASED = Integer.MAX_VALUE;
  }

  /** A ThreadState describes the state of a single thread. */
  private static class ThreadState {

    // String :: identifier for the thread TODO change to object or memory-location
    // CallstackState +  LocationState :: thread-position
    private final AbstractState location;
    private final CallstackStateEqualsWrapper callstack;

    // Each thread is assigned to an Integer
    // TODO do we really need this? -> needed for identification of cloned functions.
    private final int num;

    ThreadState(AbstractState pLocation, AbstractState pCallstack, int pNum) {
      location = pLocation;
      callstack = new CallstackStateEqualsWrapper((CallstackState) pCallstack);
      num = pNum;
    }

    AbstractState getLocation() {
      return location;
    }

    AbstractState getCallstack() {
      return callstack.getState();
    }

    int getNum() {
      return num;
    }

    @Override
    public String toString() {
      return location + " " + callstack + " @@ " + num;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ThreadState other
          && location.equals(other.location)
          && callstack.equals(other.callstack)
          && num == other.num;
    }

    @Override
    public int hashCode() {
      return Objects.hash(location, callstack, num);
    }
  }

  /** See {@link #activeThread}. */
  public ThreadingState withActiveThread(@Nullable String pActiveThread) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        pActiveThread,
        entryFunction,
        threadIdsForWitness);
  }

  String getActiveThread() {
    return activeThread;
  }

  /** See {@link #entryFunction}. */
  public ThreadingState withEntryFunction(@Nullable FunctionCallEdge pEntryFunction) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        assumptions,
        activeThread,
        pEntryFunction,
        threadIdsForWitness);
  }

  /** See {@link #assumptions}. */
  public ThreadingState withAssumptions(ImmutableList<CExpression> pAssumptions) {
    return new ThreadingState(
        threads,
        locks,
        recursiveMutexes,
        recursiveMutexAttributes,
        onceRunners,
        finishedOnces,
        barriers,
        pAssumptions,
        activeThread,
        entryFunction,
        threadIdsForWitness);
  }

  /**
   * Returns the assumptions of the edge that produced this state, e.g. about the count of a
   * barrier. They are set only for this edge, and the next edge removes them.
   */
  @Override
  public List<CExpression> getAssumptions() {
    return assumptions;
  }

  /** See {@link #entryFunction}. */
  @Nullable
  public FunctionCallEdge getEntryFunction() {
    return entryFunction;
  }

  private @Nullable Integer getThreadIdForWitness(String threadId) {
    Preconditions.checkNotNull(threadId);
    return threadIdsForWitness.get(threadId);
  }

  /**
   * Returns this state with the identifiers updated by which a witness refers to its threads, cf.
   * {@link #threadIdsForWitness}.
   *
   * <p>Only threads whose creation the witness passes a waypoint for have an identifier at all: the
   * automaton assigns the identifier of the created thread to its thread-id variable when it passes
   * that waypoint.
   *
   * <p>That new value is not visible on the edge of the creation itself, because the
   * CompositeTransferRelation strengthens every component state with the states from before
   * strengthening. A created thread hence only receives its identifier one edge later: if the
   * automaton has advanced its variable to an identifier that no thread holds yet, that identifier
   * belongs to the created thread, otherwise the witness does not refer to the thread at all.
   *
   * @param pAutomatonThreadId the current value of the thread-id variable of the witness automaton
   */
  ThreadingState updateThreadIdsForWitness(int pAutomatonThreadId) {
    PersistentMap<String, Integer> ids = threadIdsForWitness;

    // On the very first edge the initial thread receives the identifier the witness starts with.
    if (ids.isEmpty() && activeThread != null) {
      ids = ids.putAndCopy(activeThread, pAutomatonThreadId);
    }

    // Resolve the thread created by the previous edge, see above. At most one thread can await its
    // identifier, because at most one thread is created per edge.
    for (Entry<String, Integer> entry : threadIdsForWitness.entrySet()) {
      if (entry.getValue() == AWAITING_WITNESS_THREAD_ID) {
        ids =
            ids.putAndCopy(
                entry.getKey(),
                ids.containsValue(pAutomatonThreadId) ? NO_WITNESS_THREAD_ID : pAutomatonThreadId);
      }
    }

    // A thread created by this edge only learns its identifier on the next edge, see above.
    for (String threadId : threads.keySet()) {
      if (!ids.containsKey(threadId)) {
        ids = ids.putAndCopy(threadId, AWAITING_WITNESS_THREAD_ID);
      }
    }

    return ids == threadIdsForWitness ? this : withThreadIdsForWitness(ids);
  }
}

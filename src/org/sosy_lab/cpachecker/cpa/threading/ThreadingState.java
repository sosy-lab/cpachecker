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
import java.util.LinkedHashSet;
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
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
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
        AbstractStateWithLocations,
        Graphable,
        Partitionable,
        AbstractQueryableState {

  private static final String PROPERTY_DEADLOCK = "deadlock";

  /**
   * Marks a thread that was created but whose identifier in the witness is not known yet, cf.
   * {@link #threadIdsForWitness}. It never matches {@link #PROPERTY_ACTIVE_THREAD_WITNESS_ID},
   * because identifiers of a witness are never negative.
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
   * by {@link ThreadingTransferRelation#strengthen} and queried through {@link
   * #PROPERTY_ACTIVE_THREAD_WITNESS_ID}.
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
    activeThread = null;
    entryFunction = null;
    threadIdsForWitness = PathCopyingPersistentTreeMap.of();
  }

  private ThreadingState(
      PersistentMap<String, ThreadState> pThreads,
      PersistentMap<String, String> pLocks,
      String pActiveThread,
      FunctionCallEdge entryFunction,
      PersistentMap<String, Integer> pThreadIdsForWitness) {
    threads = pThreads;
    locks = pLocks;
    activeThread = pActiveThread;
    this.entryFunction = entryFunction;
    threadIdsForWitness = pThreadIdsForWitness;
  }

  private ThreadingState withThreads(PersistentMap<String, ThreadState> pThreads) {
    return new ThreadingState(pThreads, locks, activeThread, entryFunction, threadIdsForWitness);
  }

  private ThreadingState withLocks(PersistentMap<String, String> pLocks) {
    return new ThreadingState(threads, pLocks, activeThread, entryFunction, threadIdsForWitness);
  }

  private ThreadingState withThreadIdsForWitness(
      PersistentMap<String, Integer> pThreadIdsForWitness) {
    return new ThreadingState(threads, locks, activeThread, entryFunction, pThreadIdsForWitness);
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

  public ThreadingState removeThreadAndCopy(String id) {
    Preconditions.checkNotNull(id);
    checkState(threads.containsKey(id), "leaving non-existing thread: %s", id);
    return withThreads(threads.removeAndCopy(id));
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
    return withLocks(locks.putAndCopy(lockId, threadId));
  }

  public ThreadingState removeLockAndCopy(String threadId, String lockId) {
    Preconditions.checkNotNull(threadId);
    Preconditions.checkNotNull(lockId);
    checkArgument(
        threads.containsKey(threadId),
        "unblocking non-existant thread: %s with lock: %s",
        threadId,
        lockId);
    return withLocks(locks.removeAndCopy(lockId));
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
        && Objects.equals(activeThread, ts.activeThread)
        && threadIdsForWitness.equals(ts.threadIdsForWitness);
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, locks, activeThread, threadIdsForWitness);
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
    return Objects.equals(getThreadIdForWitness(activeThread), expectedWitnessId);
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
    return newLock != null && hasLock(newLock);
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
    return new ThreadingState(threads, locks, pActiveThread, entryFunction, threadIdsForWitness);
  }

  String getActiveThread() {
    return activeThread;
  }

  /** See {@link #entryFunction}. */
  public ThreadingState withEntryFunction(@Nullable FunctionCallEdge pEntryFunction) {
    return new ThreadingState(threads, locks, activeThread, pEntryFunction, threadIdsForWitness);
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

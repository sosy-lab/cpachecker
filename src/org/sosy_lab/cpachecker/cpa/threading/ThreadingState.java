/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.threading;

import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_JOIN;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.extractParamName;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.getLockId;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.isLastNodeOfThread;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** This immutable state represents a location state combined with a callstack state. */
public class ThreadingState implements AbstractState, AbstractStateWithLocations, Graphable, Partitionable, AbstractQueryableState {

  private static final String PROPERTY_DEADLOCK = "deadlock";

  final static int MIN_THREAD_NUM = 0;

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

  public ThreadingState() {
    this.threads = PathCopyingPersistentTreeMap.of();
    this.locks = PathCopyingPersistentTreeMap.of();
    this.activeThread = null;
  }

  private ThreadingState(
      PersistentMap<String, ThreadState> pThreads,
      PersistentMap<String, String> pLocks,
      String pActiveThread) {
    this.threads = pThreads;
    this.locks = pLocks;
    this.activeThread = pActiveThread;
  }

  public ThreadingState addThreadAndCopy(String id, int num, AbstractState stack, AbstractState loc) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(!threads.containsKey(id), "thread already exists");
    return new ThreadingState(
        threads.putAndCopy(id, new ThreadState(loc, stack, num)), locks, activeThread);
  }

  public ThreadingState updateLocationAndCopy(String id, AbstractState stack, AbstractState loc) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(threads.containsKey(id), "updating non-existing thread");
    return new ThreadingState(
        threads.putAndCopy(id, new ThreadState(loc, stack, threads.get(id).getNum())),
        locks,
        activeThread);
  }

  public ThreadingState removeThreadAndCopy(String id) {
    Preconditions.checkNotNull(id);
    Preconditions.checkState(threads.containsKey(id), "leaving non-existing thread: " + id);
    return new ThreadingState(threads.removeAndCopy(id), locks, activeThread);
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

  private Set<Integer> getThreadNums() {
    Set<Integer> result = new HashSet<>();
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
    while(threadNums.contains(num)) {
      num++;
    }
    return num;
  }

  public ThreadingState addLockAndCopy(String threadId, String lockId) {
    Preconditions.checkNotNull(lockId);
    Preconditions.checkNotNull(threadId);
    Preconditions.checkArgument(threads.containsKey(threadId), "blocking non-existant thread: " + threadId + " with lock: " + lockId);
    return new ThreadingState(threads, locks.putAndCopy(lockId, threadId), activeThread);
  }

  public ThreadingState removeLockAndCopy(String threadId, String lockId) {
    Preconditions.checkNotNull(threadId);
    Preconditions.checkNotNull(lockId);
    Preconditions.checkArgument(threads.containsKey(threadId), "unblocking non-existant thread: " + threadId + " with lock: " + lockId);
    return new ThreadingState(threads, locks.removeAndCopy(lockId), activeThread);
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

  @Override
  public String toString() {
    return "( threads={\n"
        + Joiner.on(",\n ").withKeyValueSeparator("=").join(threads)
        + "}\n and locks={"
        + Joiner.on(",\n ").withKeyValueSeparator("=").join(locks)
        + "}"
        + (activeThread == null ? "" : ("\n produced from thread " + activeThread))
        + " )";
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof ThreadingState)) {
      return false;
    }
    ThreadingState ts = (ThreadingState)other;
    return threads.equals(ts.threads)
        && locks.equals(ts.locks)
        && Objects.equals(activeThread, ts.activeThread);
  }

  @Override
  public int hashCode() {
    return Objects.hash(threads, locks, activeThread);
  }

  private FluentIterable<AbstractStateWithLocations> getLocations() {
    return FluentIterable.from(threads.values()).transform(
        s -> (AbstractStateWithLocations) s.getLocation());
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
  public Iterable<CFAEdge> getIngoingEdges() {
    return getLocations().transformAndConcat(AbstractStateWithLocations::getIngoingEdges);
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
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  /**
   * check, whether one of the outgoing edges can be visited
   * without requiring a already used lock.
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

  /** A thread might need to wait for another thread, if the other thread joins at
   * the current edge. If the other thread never exits, we have found a deadlock. */
  private boolean isWaitingForOtherThread(CFAEdge edge) throws UnrecognizedCodeException {
    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      AStatement statement = ((AStatementEdge)edge).getStatement();
      if (statement instanceof AFunctionCall) {
        AExpression functionNameExp = ((AFunctionCall)statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          final String functionName = ((AIdExpression)functionNameExp).getName();
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

    ThreadState(AbstractState pLocation, AbstractState pCallstack, int  pNum) {
      location = pLocation;
      callstack = new CallstackStateEqualsWrapper((CallstackState)pCallstack);
      num= pNum;
    }

    public AbstractState getLocation() {
      return location;
    }

    public AbstractState getCallstack() {
      return callstack.getState();
    }

    public int getNum() {
      return num;
    }

    @Override
    public String toString() {
      return location + " " + callstack + " @@ " + num;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof ThreadState)) {
        return false;
      }
      ThreadState other = (ThreadState)o;
      return location.equals(other.location) && callstack.equals(other.callstack) && num == other.num;
    }

    @Override
    public int hashCode() {
      return Objects.hash(location, callstack, num);
    }
  }

  /** @see #activeThread */
  public ThreadingState setActiveThread(String pActiveThread) {
    return new ThreadingState(threads, locks, pActiveThread);
  }

  String getActiveThread() {
    return activeThread;
  }
}

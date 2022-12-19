// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.LOCAL_ACCESS_LOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.RW_MUTEX_READLOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.RW_MUTEX_UNLOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.RW_MUTEX_WRITELOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_COND_BC;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_COND_SIGNAL;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_COND_TIMEDWAIT;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_COND_WAIT;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_JOIN;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_MUTEX_LOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_MUTEX_UNLOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_START;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.getFunctionName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.locks.ConditionVariable;
import org.sosy_lab.cpachecker.cpa.threading.locks.LockInfo;
import org.sosy_lab.cpachecker.cpa.threading.locks.LockInfo.LockType;
import org.sosy_lab.cpachecker.cpa.threading.locks.RWLock;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DataRaceTransferRelation extends SingleEdgeTransferRelation {

  // These functions need special handling that is not currently provided by the DataRaceCPA.
  // When one of these functions is encountered we are therefore unable to tell if a data race
  // is present or not, so the analysis is terminated. TODO: Add support for these functions
  private static final ImmutableSet<String> UNSUPPORTED_FUNCTIONS =
      ImmutableSet.of("pthread_rwlock_timedrdlock", "pthread_rwlock_timedwrlock");

  private final MemoryAccessExtractor memoryAccessExtractor = new MemoryAccessExtractor();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    // Can only update state with info from ThreadingCPA
    return ImmutableSet.of(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return ImmutableSet.of(pState);
    }
    DataRaceState state = (DataRaceState) pState;
    Map<String, ThreadInfo> threadInfo = state.getThreadInfo();
    ImmutableSet.Builder<DataRaceState> strengthenedStates = ImmutableSet.builder();
    ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder = ImmutableSet.builder();
    synchronizationBuilder.addAll(state.getThreadSynchronizations());

    for (ThreadingState threadingState :
        AbstractStates.projectToType(otherStates, ThreadingState.class)) {

      Set<String> threadIds = threadingState.getThreadIds();
      String activeThread = getActiveThread(cfaEdge, threadingState);
      assert Objects.equals(activeThread, threadInfo.get(activeThread).getThreadId());
      ImmutableMap<String, ThreadInfo> newThreadInfo =
          updateThreadInfo(threadInfo, threadIds, activeThread, synchronizationBuilder);

      if (newThreadInfo.values().stream().filter(i -> i.isRunning()).count() == 1) {
        // No data race possible in sequential part
        strengthenedStates.add(new DataRaceState(newThreadInfo, state.hasDataRace()));
        continue;
      }

      // Update locking related info with info from ThreadingCPA
      Set<String> activeThreadLocks = threadingState.getLocksForThread(activeThread);
      ImmutableSetMultimap<String, LockInfo> heldLocks =
          updateHeldLocks(state, threadingState, activeThread, activeThreadLocks);
      Multimap<String, LockRelease> lastReleases = state.getLastReleases();

      // Handle threading-related functions
      AFunctionCall functionCall = null;
      switch (cfaEdge.getEdgeType()) {
        case FunctionCallEdge:
          FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
          functionCall = functionCallEdge.getFunctionCall();
          break;
        case StatementEdge:
          AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
          AStatement statement = statementEdge.getStatement();
          if (statement instanceof AFunctionCallAssignmentStatement) {
            functionCall = (AFunctionCallAssignmentStatement) statement;
          } else if (statement instanceof AFunctionCallStatement) {
            functionCall = (AFunctionCallStatement) statement;
          }
          break;
        default:
          // Other edge types shouldn't contain function calls
      }
      Set<WaitInfo> newWaitInfo = new HashSet<>(state.getWaitInfo());
      if (functionCall != null) {
        lastReleases =
            handleThreadFunctions(
                state,
                threadingState,
                functionCall,
                newThreadInfo.get(activeThread),
                newWaitInfo,
                synchronizationBuilder);
      }

      // Collect new accesses
      Set<MemoryAccess> newMemoryAccesses =
          memoryAccessExtractor.getNewAccesses(
              threadInfo.get(activeThread), cfaEdge, activeThreadLocks);
      for (MemoryAccess newAccess : newMemoryAccesses) {
        if (newAccess.isOverapproximating()) {
          throw new CPATransferException("DataRaceCPA does not support pointer analysis");
        }
      }

      // Update tracked memory accesses
      ImmutableSet.Builder<MemoryAccess> memoryAccessBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<MemoryAccess> subsequentWritesBuilder =
          prepareSubsequentWritesBuilder(state, threadIds);
      for (MemoryAccess access : state.getMemoryAccesses()) {
        if (!threadIds.contains(access.getThreadId())) {
          // If the thread that made the access is no longer running,
          // then this access can not conflict with any newer accesses.
          // Therefore, we do not need to track it any longer.
          continue;
        }
        memoryAccessBuilder.add(access);

        // Add new synchronizes-with edges, if possible.
        // We do this here to avoid looping over all memory accesses a second time.
        if (!access.isWrite() || state.getAccessesWithSubsequentWrites().contains(access)) {
          continue;
        }
        for (MemoryAccess newAccess : newMemoryAccesses) {
          if (!access.mightAccessSameLocationAs(newAccess)) {
            continue;
          }
          if (newAccess.isWrite()) {
            // There is now a more recent write to the same memory location,
            // so mark the old access accordingly.
            // Other accesses currently in newMemoryAccesses that read this memory location still
            // synchronize-with the old access, so no need to break/rollback here.
            subsequentWritesBuilder.add(access);
          } else {
            if (access.getThreadId().equals(newAccess.getThreadId())) {
              // Adding synchronizes-with edge between accesses made by the same thread is
              // unnecessary, because happens-before is established anyway.
              continue;
            }
            if (!Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()) {
              // Add synchronizes-with edge:
              // An atomic write operation synchronizes-with an atomic read operation of the same
              // variable that reads the value written by the former.
              synchronizationBuilder.add(
                  new ThreadSynchronization(
                      access.getThreadId(),
                      newAccess.getThreadId(),
                      access.getAccessEpoch(),
                      newAccess.getAccessEpoch()));
            }
          }
        }
      }

      // Determine whether any of the new accesses constitutes a data race
      boolean hasDataRace = state.hasDataRace();
      Set<ThreadSynchronization> threadSynchronizations = synchronizationBuilder.build();
      for (MemoryAccess access : memoryAccessBuilder.build()) {
        if (hasDataRace) {
          // Already found a data race, no need to continue
          break;
        }
        if (access.getThreadId().equals(activeThread)) {
          // All new accesses were made by the currently active thread and accesses made by the same
          // thread are never conflicting.
          continue;
        }
        for (MemoryAccess newAccess : newMemoryAccesses) {
          if (access.mightAccessSameLocationAs(newAccess)
              && (access.isWrite() || newAccess.isWrite())
              && Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()
              && !access.happensBefore(newAccess, threadSynchronizations)) {
            // Two accesses are conflicting if:
            //   - They access the same memory location
            //   - They were made by two different threads
            //   - At least one of them is a write access
            // Two conflicting accesses constitute a data race unless:
            //   - Both accesses are performed through atomic operations, or
            //   - One access happens-before the other
            hasDataRace = true;
            break;
          }
        }
      }

      strengthenedStates.add(
          new DataRaceState(
              memoryAccessBuilder.addAll(newMemoryAccesses).build(),
              subsequentWritesBuilder.build(),
              newThreadInfo,
              threadSynchronizations,
              heldLocks,
              lastReleases,
              newWaitInfo,
              hasDataRace));
    }

    return strengthenedStates.build();
  }

  private ImmutableSet.Builder<MemoryAccess> prepareSubsequentWritesBuilder(
      DataRaceState current, Set<String> threadIds) {
    ImmutableSet.Builder<MemoryAccess> subsequentWritesBuilder = ImmutableSet.builder();
    for (MemoryAccess access : current.getAccessesWithSubsequentWrites()) {
      if (threadIds.contains(access.getThreadId())) {
        subsequentWritesBuilder.add(access);
      }
    }
    return subsequentWritesBuilder;
  }

  private ImmutableSetMultimap<String, LockInfo> updateHeldLocks(
      DataRaceState state,
      ThreadingState pThreadingState,
      String activeThread,
      Set<String> activeThreadLocks) {
    ImmutableSetMultimap.Builder<String, LockInfo> newHeldLocks = ImmutableSetMultimap.builder();
    // For locks held by the active thread use info from ThreadingCPA
    for (String lock : activeThreadLocks) {
      newHeldLocks.put(activeThread, pThreadingState.getLock(lock));
    }

    // For locks held by any other thread nothing changes
    for (Entry<String, LockInfo> entry : state.getHeldLocks().entries()) {
      if (!entry.getKey().equals(activeThread)) {
        newHeldLocks.put(entry);
      }
    }
    return newHeldLocks.build();
  }

  /**
   * Updates the currently tracked thread information with information from the ThreadingCPA.
   *
   * @param threadInfo The current map of thread ID -> ThreadInfo.
   * @param threadIds The IDs of currently existing thread as obtained from the ThreadingCPA.
   * @param activeThread The ID of the current active thread.
   * @return The new map of thread ID -> ThreadInfo objects.
   */
  private ImmutableMap<String, ThreadInfo> updateThreadInfo(
      Map<String, ThreadInfo> threadInfo,
      Set<String> threadIds,
      String activeThread,
      ImmutableSet.Builder<ThreadSynchronization> threadSynchronizations) {
    Set<String> added = Sets.difference(threadIds, threadInfo.keySet());
    assert added.size() < 2 : "Multiple thread creations in same step not supported";
    Set<String> removed = Sets.difference(threadInfo.keySet(), threadIds);
    assert !removed.contains(activeThread) : "Thread active after join";

    ImmutableMap.Builder<String, ThreadInfo> threadsBuilder = ImmutableMap.builder();
    if (!added.isEmpty()) {
      String threadId = added.iterator().next();
      ThreadInfo addedThreadInfo;
      if (threadInfo.containsKey(threadId)) {
        addedThreadInfo = new ThreadInfo(threadId, threadInfo.get(threadId).getEpoch() + 1, true);
      } else {
        addedThreadInfo = new ThreadInfo(threadId, 0, true);
      }
      threadsBuilder.put(threadId, addedThreadInfo);
      threadSynchronizations.add(
          new ThreadSynchronization(
              activeThread,
              threadId,
              threadInfo.get(activeThread).getEpoch() + 1,
              addedThreadInfo.getEpoch()));
    }
    for (Entry<String, ThreadInfo> entry : threadInfo.entrySet()) {
      if (entry.getKey().equals(activeThread)) {
        threadsBuilder.put(
            activeThread, new ThreadInfo(activeThread, entry.getValue().getEpoch() + 1, true));
      } else if (removed.contains(entry.getKey())) {
        threadsBuilder.put(
            entry.getKey(), new ThreadInfo(entry.getKey(), entry.getValue().getEpoch(), false));
      } else if (!added.contains(entry.getKey())) {
        threadsBuilder.put(entry);
      }
    }
    return threadsBuilder.buildOrThrow();
  }

  private Multimap<String, LockRelease> handleThreadFunctions(
      DataRaceState state,
      ThreadingState threadingState,
      AFunctionCall pFunctionCall,
      ThreadInfo pActiveThreadInfo,
      Set<WaitInfo> pWaitInfo,
      ImmutableSet.Builder<ThreadSynchronization> threadSynchronizations)
      throws CPATransferException {
    String activeThread = pActiveThreadInfo.getThreadId();
    int epoch = pActiveThreadInfo.getEpoch();

    String functionName = getFunctionName(pFunctionCall);
    if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
      throw new CPATransferException("DataRaceCPA does not support function " + functionName);
    }

    Map<String, ThreadInfo> threadInfo = new HashMap<>(state.getThreadInfo());
    for (Entry<String, ThreadInfo> entry : threadInfo.entrySet()) {
      if (entry.getKey().equals(activeThread)) {
        threadInfo.put(
            activeThread, new ThreadInfo(activeThread, entry.getValue().getEpoch() + 1, true));
      }
    }

    Multimap<String, LockRelease> newReleases = LinkedHashMultimap.create();
    newReleases.putAll(state.getLastReleases());

    switch (functionName) {
      case THREAD_START:
        {
          AExpression threadExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String newThreadId = ((CUnaryExpression) threadExpression).getOperand().toString();

          ThreadInfo addedThreadInfo;
          if (state.getThreadInfo().containsKey(newThreadId)) {
            addedThreadInfo =
                new ThreadInfo(
                    newThreadId, state.getThreadInfo().get(newThreadId).getEpoch() + 1, true);
          } else {
            addedThreadInfo = new ThreadInfo(newThreadId, 0, true);
          }
          threadInfo.put(newThreadId, addedThreadInfo);

          // Thread creation synchronizes-with the created thread
          threadSynchronizations.add(
              new ThreadSynchronization(
                  activeThread,
                  newThreadId,
                  state.getThreadInfo().get(activeThread).getEpoch() + 1,
                  addedThreadInfo.getEpoch()));
          break;
        }
      case THREAD_JOIN:
        {
          AExpression threadExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String joinedThreadId = ((CUnaryExpression) threadExpression).getOperand().toString();

          threadInfo.put(
              joinedThreadId,
              new ThreadInfo(joinedThreadId, threadInfo.get(joinedThreadId).getEpoch(), false));
          break;
        }
      case THREAD_MUTEX_LOCK:
        {
          AExpression lockExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String lockId = ((CUnaryExpression) lockExpression).getOperand().toString();

          Collection<LockRelease> lastReleases = newReleases.get(lockId);
          assert lastReleases.size() < 2 : "Expected at most one last release for regular mutex";
          for (LockRelease lastRelease : lastReleases) {
            if (lastRelease.getThreadId().equals(activeThread)) {
              // synchronizes-with is unnecessary if acquire and release are done by the same thread
              continue;
            }

            // A lock release synchronizes-with the next acquisition of that lock
            threadSynchronizations.add(
                new ThreadSynchronization(
                    lastRelease.getThreadId(), activeThread, lastRelease.getAccessEpoch(), epoch));
          }
          break;
        }
      case RW_MUTEX_READLOCK:
        {
          AExpression lockExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String lockId = ((CUnaryExpression) lockExpression).getOperand().toString();

          Collection<LockRelease> lastReleases = newReleases.get(lockId);
          for (LockRelease lastRelease : lastReleases) {
            assert lastRelease instanceof RWLockRelease;
            if (lastRelease.getThreadId().equals(activeThread)) {
              continue;
            }

            // - Reader Acquire synchronizes-with the last writer release
            // - Reader Acquire does NOT synchronize-with ANY reader release directly
            if (((RWLockRelease) lastRelease).isWriteRelease()) {
              threadSynchronizations.add(
                  new ThreadSynchronization(
                      lastRelease.getThreadId(),
                      activeThread,
                      lastRelease.getAccessEpoch(),
                      epoch));
            }
          }
          break;
        }
      case RW_MUTEX_WRITELOCK:
        {
          AExpression lockExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String lockId = ((CUnaryExpression) lockExpression).getOperand().toString();

          ImmutableList<LockRelease> lastReleases = ImmutableList.copyOf(newReleases.get(lockId));
          for (LockRelease lastRelease : lastReleases) {
            assert lastRelease instanceof RWLockRelease;
            if (lastRelease.getThreadId().equals(activeThread)) {
              continue;
            }

            // - Writer Acquire synchronizes-with the last writer release
            // - Writer Acquire synchronizes-with EVERY reader release since the last writer release
            threadSynchronizations.add(
                new ThreadSynchronization(
                    lastRelease.getThreadId(), activeThread, lastRelease.getAccessEpoch(), epoch));

            if (!((RWLockRelease) lastRelease).isWriteRelease()) {
              // Reader releases can be removed now, because synchronizes-with is transitive
              newReleases.remove(lockId, lastRelease);
            }
          }
          break;
        }
      case THREAD_MUTEX_UNLOCK:
      case RW_MUTEX_UNLOCK:
        {
          AExpression lockExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String lockId = ((CUnaryExpression) lockExpression).getOperand().toString();

          if (lockId.equals(LOCAL_ACCESS_LOCK)) {
            // Do not track releases of local access lock,
            // as these may not be used for synchronization
            break;
          }

          LockInfo lock = null;
          for (LockInfo lockInfo : state.getHeldLocks().get(activeThread)) {
            if (lockInfo.getLockId().equals(lockId)) {
              lock = lockInfo;
              break;
            }
          }
          assert lock != null;

          if (functionName.equals(THREAD_MUTEX_UNLOCK)) {
            assert lock.getLockType() == LockType.MUTEX;
            newReleases.put(lockId, new LockRelease(lockId, activeThread, epoch));
          } else {
            assert lock.getLockType() == LockType.RW_MUTEX;
            boolean isWriteRelease = ((RWLock) lock).hasWriter();
            newReleases.put(lockId, new RWLockRelease(lockId, activeThread, epoch, isWriteRelease));
          }
          break;
        }
      case THREAD_COND_SIGNAL:
      case THREAD_COND_BC:
        {
          AExpression condVarExpression =
              pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(0);
          String condVar = ((CUnaryExpression) condVarExpression).getOperand().toString();

          Set<WaitInfo> toRemove = new HashSet<>();
          for (WaitInfo waitInfo : pWaitInfo) {
            if (waitInfo.getWaitingOn().equals(condVar)) {
              threadSynchronizations.add(
                  new ThreadSynchronization(
                      // TODO: Order correct?
                      activeThread, waitInfo.getWaitingThread(), epoch, waitInfo.getEpoch()));
              toRemove.add(waitInfo);
            }
          }
          pWaitInfo.removeAll(toRemove);
          break;
        }
      case THREAD_COND_WAIT:
      case THREAD_COND_TIMEDWAIT:
        {
          ConditionVariable condVar = threadingState.getCondVarForThread(activeThread);
          pWaitInfo.add(new WaitInfo(activeThread, condVar.getName(), epoch));
          break;
        }
      default:
        // TODO: Uncomment check once all thread functions are handled
        // if (THREAD_FUNCTIONS.contains(functionName)) {
        //  throw new AssertionError("Unhandled thread function");
        // }
        // Nothing to do
    }
    return newReleases;
  }

  /**
   * Search for the thread where the given edge is available.
   *
   * <p>This method is necessary, because neither ThreadingState::getActiveThread nor
   * ThreadingTransferRelation::getActiveThread are guaranteed to give the correct result during
   * strengthening.
   */
  private String getActiveThread(final CFAEdge cfaEdge, final ThreadingState threadingState) {
    for (String id : threadingState.getThreadIds()) {
      if (Iterables.contains(threadingState.getThreadLocation(id).getIngoingEdges(), cfaEdge)) {
        return id;
      }
    }
    throw new AssertionError("Unable to determine active thread");
  }
}

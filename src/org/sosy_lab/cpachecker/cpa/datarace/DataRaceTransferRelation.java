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
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_EXIT;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_FUNCTIONS;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_JOIN;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_MUTEX_LOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_MUTEX_TRYLOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_MUTEX_UNLOCK;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.THREAD_START;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.VERIFIER_ATOMIC_BEGIN;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.VERIFIER_ATOMIC_END;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.extractLock;
import static org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation.getFunctionName;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    ImmutableSet.Builder<DataRaceState> strengthenedStates = ImmutableSet.builder();

    for (ThreadingState threadingState :
        AbstractStates.projectToType(otherStates, ThreadingState.class)) {

      Set<String> threadIds = threadingState.getThreadIds();
      String activeThread = getActiveThread(cfaEdge, threadingState);
      ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder = ImmutableSet.builder();
      // synchronizes-with is transitive, and thus thread synchronizations should never be removed,
      // even if one of the participating threads terminates
      synchronizationBuilder.addAll(state.getThreadSynchronizations());

      // Collect new accesses
      Set<LockInfo> heldLocks =
          FluentIterable.from(threadingState.getLocksForThread(activeThread))
              .transform(id -> threadingState.getLock(id))
              .toSet();
      Set<MemoryAccess> newMemoryAccesses =
          memoryAccessExtractor.getNewAccesses(
              state.getThreadInfo().get(activeThread), cfaEdge, heldLocks);

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

      // Handle threading-related functions if necessary
      AFunctionCall functionCall = extractFunctionCall(cfaEdge);
      strengthenedStates.add(
          handleThreadFunctions(
              state,
              threadingState,
              functionCall,
              activeThread,
              synchronizationBuilder,
              subsequentWritesBuilder.build(),
              memoryAccessBuilder.build(),
              newMemoryAccesses));
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

  private AFunctionCall extractFunctionCall(CFAEdge cfaEdge) {
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
    return functionCall;
  }

  private DataRaceState handleThreadFunctions(
      DataRaceState state,
      ThreadingState threadingState,
      AFunctionCall pFunctionCall,
      String activeThread,
      ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder,
      Set<MemoryAccess> subsequentWrites,
      Set<MemoryAccess> memoryAccesses,
      Set<MemoryAccess> newMemoryAccesses)
      throws CPATransferException {
    Map<String, ThreadInfo> threadInfo = new HashMap<>(state.getThreadInfo());
    threadInfo.put(
        activeThread,
        new ThreadInfo(activeThread, threadInfo.get(activeThread).getEpoch() + 1, true));
    int epoch = threadInfo.get(activeThread).getEpoch();
    Multimap<String, LockRelease> newReleases = LinkedHashMultimap.create(state.getLastReleases());
    Set<WaitInfo> waitInfo = new HashSet<>(state.getWaitInfo());
    for (WaitInfo info : state.getWaitInfo()) {
      if (info.getWaitingThread().equals(activeThread)
          && threadingState.getCondVarForThread(activeThread) == null) {
        // This is a spurious wakeup. If the active thread had been signalled, info would have
        // already been removed.
        waitInfo.remove(info);
      }
    }

    // Apply necessary changes, depending on the called thread function
    if (pFunctionCall != null) {
      String functionName = getFunctionName(pFunctionCall);
      if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
        throw new CPATransferException("DataRaceCPA does not support function " + functionName);
      }

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
            synchronizationBuilder.add(
                new ThreadSynchronization(
                    activeThread,
                    newThreadId,
                    state.getThreadInfo().get(activeThread).getEpoch() + 1,
                    addedThreadInfo.getEpoch()));
            break;
          }
        case THREAD_MUTEX_LOCK:
        case THREAD_MUTEX_TRYLOCK:
          {
            String lockId = extractLock(pFunctionCall, threadingState).getLockId();
            assert threadingState.hasLock(lockId);

            if (functionName.equals(THREAD_MUTEX_TRYLOCK)
                && !threadingState.isLockHeld(activeThread, lockId)) {
              // Trylock did not succeed, no new synchronization
              break;
            }

            // Add synchronization with last lock release
            Collection<LockRelease> lastReleases = newReleases.get(lockId);
            assert lastReleases.size() < 2 : "Expected at most one last release for regular mutex";
            for (LockRelease lastRelease : lastReleases) {
              if (lastRelease.getThreadId().equals(activeThread)) {
                // synchronizes-with is unnecessary if acquire and release are done by the same
                // thread
                continue;
              }

              // A lock release synchronizes-with the next acquisition of that lock
              synchronizationBuilder.add(
                  new ThreadSynchronization(
                      lastRelease.getThreadId(),
                      activeThread,
                      lastRelease.getAccessEpoch(),
                      epoch));
            }
            break;
          }
        case RW_MUTEX_READLOCK:
          {
            String lockId = extractLock(pFunctionCall, threadingState).getLockId();
            assert threadingState.hasLock(lockId);

            // Add synchronization
            Collection<LockRelease> lastReleases = newReleases.get(lockId);
            for (LockRelease lastRelease : lastReleases) {
              assert lastRelease instanceof RWLockRelease;
              if (lastRelease.getThreadId().equals(activeThread)) {
                continue;
              }

              // - Reader Acquire synchronizes-with the last writer release
              // - Reader Acquire does NOT synchronize-with ANY reader release directly
              if (((RWLockRelease) lastRelease).isWriteRelease()) {
                synchronizationBuilder.add(
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
            String lockId = extractLock(pFunctionCall, threadingState).getLockId();
            assert threadingState.hasLock(lockId);

            // Add synchronization
            ImmutableList<LockRelease> lastReleases = ImmutableList.copyOf(newReleases.get(lockId));
            for (LockRelease lastRelease : lastReleases) {
              assert lastRelease instanceof RWLockRelease;
              if (lastRelease.getThreadId().equals(activeThread)) {
                continue;
              }

              // - Writer Acquire synchronizes-with the last writer release
              // - Writer Acquire synchronizes-with EVERY reader release since the last writer
              // release
              synchronizationBuilder.add(
                  new ThreadSynchronization(
                      lastRelease.getThreadId(),
                      activeThread,
                      lastRelease.getAccessEpoch(),
                      epoch));

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
            LockInfo lock = extractLock(pFunctionCall, threadingState);
            String lockId = lock.getLockId();
            assert threadingState.hasLock(lockId);

            if (lockId.equals(LOCAL_ACCESS_LOCK)) {
              // Do not track releases of local access lock,
              // as these may not be used for synchronization
              break;
            }

            if (functionName.equals(THREAD_MUTEX_UNLOCK)) {
              assert lock.getLockType() == LockType.MUTEX;
              newReleases.put(lockId, new LockRelease(lockId, activeThread, epoch));
            } else {
              assert lock.getLockType() == LockType.RW_MUTEX;
              boolean isWriteRelease = ((RWLock) lock).wasLastReleaseWriter();
              newReleases.put(
                  lockId, new RWLockRelease(lockId, activeThread, epoch, isWriteRelease));
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
            for (WaitInfo info : waitInfo) {
              if (info.getWaitingOn().equals(condVar)) {
                synchronizationBuilder.add(
                    new ThreadSynchronization(
                        activeThread, info.getWaitingThread(), epoch, info.getEpoch()));
                toRemove.add(info);
              }
            }
            waitInfo.removeAll(toRemove);
            break;
          }
        case THREAD_COND_WAIT:
        case THREAD_COND_TIMEDWAIT:
          {
            ConditionVariable condVar = threadingState.getCondVarForThread(activeThread);
            waitInfo.add(new WaitInfo(activeThread, condVar.getName(), epoch));
            break;
          }
        case THREAD_JOIN:
        case THREAD_EXIT:
        case VERIFIER_ATOMIC_BEGIN:
        case VERIFIER_ATOMIC_END:
          {
            // No special handling needed
            break;
          }
        default:
          {
            if (THREAD_FUNCTIONS.contains(functionName)) {
              throw new AssertionError("Unhandled thread function");
            }
            // Nothing to do
            break;
          }
      }
    }

    // Update terminated threads
    Set<String> threadIds = threadInfo.keySet();
    for (String threadId : threadIds) {
      if (!threadingState.getThreadIds().contains(threadId)) {
        threadInfo.put(
            threadId, new ThreadInfo(threadId, threadInfo.get(threadId).getEpoch(), false));
      }
    }

    if (threadInfo.values().stream().filter(i -> i.isRunning()).count() == 1) {
      // No data race possible in sequential part
      return new DataRaceState(threadInfo, state.hasDataRace());
    }
    // Only check this in the parallel part, because otherwise the exception will trigger even
    // because of unused stuff in the header
    for (MemoryAccess newAccess : newMemoryAccesses) {
      if (newAccess.isOverapproximating()) {
        throw new CPATransferException("DataRaceCPA does not support pointer analysis");
      }
    }

    Set<ThreadSynchronization> threadSynchronizations = synchronizationBuilder.build();
    boolean hasDataRace =
        nextHasDataRace(
            state, activeThread, memoryAccesses, newMemoryAccesses, threadSynchronizations);

    return new DataRaceState(
        Sets.union(memoryAccesses, newMemoryAccesses),
        subsequentWrites,
        threadInfo,
        threadSynchronizations,
        newReleases,
        waitInfo,
        hasDataRace);
  }

  private boolean nextHasDataRace(
      DataRaceState state,
      String activeThread,
      Set<MemoryAccess> memoryAccesses,
      Set<MemoryAccess> newMemoryAccesses,
      Set<ThreadSynchronization> threadSynchronizations) {
    // Determine whether any of the new accesses constitutes a data race
    boolean hasDataRace = state.hasDataRace();
    for (MemoryAccess access : memoryAccesses) {
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
    return hasDataRace;
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

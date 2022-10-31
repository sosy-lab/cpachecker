// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DataRaceTransferRelation extends SingleEdgeTransferRelation {

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
    Map<String, ThreadInfo> threadInfo = state.getThreadInfo();
    ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder = ImmutableSet.builder();
    synchronizationBuilder.addAll(state.getThreadSynchronizations());

    for (ThreadingState threadingState :
        AbstractStates.projectToType(otherStates, ThreadingState.class)) {

      Set<String> threadIds = threadingState.getThreadIds();
      String activeThread = getActiveThread(cfaEdge, threadingState);
      ImmutableMap<String, ThreadInfo> newThreadInfo =
          updateThreadInfo(threadInfo, threadIds, activeThread, synchronizationBuilder);

      if (newThreadInfo.values().stream().filter(i -> i.isRunning()).count() == 1) {
        // No data race possible in sequential part
        strengthenedStates.add(new DataRaceState(newThreadInfo, state.hasDataRace()));
        continue;
      }

      Set<String> locks = threadingState.getLocksForThread(activeThread);
      ImmutableSetMultimap.Builder<String, String> newHeldLocks = ImmutableSetMultimap.builder();
      ImmutableSet.Builder<LockRelease> newReleases = ImmutableSet.builder();
      updateLocks(
          state,
          locks,
          threadInfo.get(activeThread),
          newHeldLocks,
          newReleases,
          synchronizationBuilder);

      ImmutableSet.Builder<MemoryAccess> memoryAccessBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<MemoryAccess> subsequentWritesBuilder =
          prepareSubsequentWritesBuilder(state, threadIds);
      Set<MemoryAccess> newMemoryAccesses =
          memoryAccessExtractor.getNewAccesses(threadInfo.get(activeThread), cfaEdge, locks);

      for (MemoryAccess newAccess : newMemoryAccesses) {
        if (newAccess.isAmbiguous()) {
          throw new CPATransferException("DataRaceCPA does not support pointer analysis");
        }
      }

      for (MemoryAccess access : state.getMemoryAccesses()) {
        if (threadIds.contains(access.getThreadId())) {
          memoryAccessBuilder.add(access);
          if (access.isWrite() && !state.getAccessesWithSubsequentWrites().contains(access)) {
            for (MemoryAccess newAccess : newMemoryAccesses) {
              if (access.mightAccessSameLocationAs(newAccess)) {
                if (newAccess.isWrite()) {
                  subsequentWritesBuilder.add(access);
                } else if (!access.getThreadId().equals(newAccess.getThreadId())) {
                  // Unnecessary if both accesses were made by the same thread, because then
                  // happens-before is established even without synchronizes-with
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
        }
      }

      boolean hasDataRace = state.hasDataRace();
      Set<ThreadSynchronization> threadSynchronizations = synchronizationBuilder.build();
      for (MemoryAccess access : memoryAccessBuilder.build()) {
        if (hasDataRace) {
          break;
        }
        // In particular, this skips all new memory accesses
        if (access.getThreadId().equals(activeThread)) {
          continue;
        }
        for (MemoryAccess newAccess : newMemoryAccesses) {
          if (access.mightAccessSameLocationAs(newAccess)
              && (access.isWrite() || newAccess.isWrite())
              && Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()
              && !access.happensBefore(newAccess, threadSynchronizations)) {
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
              newHeldLocks.build(),
              newReleases.build(),
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

  private void updateLocks(
      DataRaceState state,
      Set<String> locks,
      ThreadInfo activeThreadInfo,
      ImmutableSetMultimap.Builder<String, String> newHeldLocks,
      ImmutableSet.Builder<LockRelease> newReleases,
      ImmutableSet.Builder<ThreadSynchronization> synchronizationBuilder) {
    String activeThread = activeThreadInfo.getThreadId();
    Set<String> updated = new HashSet<>();
    for (String lock : Sets.union(state.getLocksForThread(activeThread), locks)) {
      if (Sets.difference(locks, state.getLocksForThread(activeThread)).contains(lock)) {
        //  Lock was acquired
        LockRelease lastRelease = state.getLastReleaseForLock(lock);
        if (lastRelease != null && !lastRelease.getThreadId().equals(activeThread)) {
          synchronizationBuilder.add(
              new ThreadSynchronization(
                  lastRelease.getThreadId(),
                  activeThread,
                  lastRelease.getAccessEpoch(),
                  activeThreadInfo.getEpoch()));
        }
        updated.add(lock);
      } else if (Sets.difference(state.getLocksForThread(activeThread), locks).contains(lock)) {
        // Lock was released
        if (!lock.equals(ThreadingTransferRelation.LOCAL_ACCESS_LOCK)) {
          // Do not track releases of local access lock,
          // as these may not be used for synchronization
          newReleases.add(new LockRelease(lock, activeThread, activeThreadInfo.getEpoch()));
        }
        updated.add(lock);
        continue;
      }
      newHeldLocks.put(activeThread, lock);
    }

    for (LockRelease release : state.getLastReleases()) {
      if (!updated.contains(release.getLockId())) {
        newReleases.add(release);
      }
    }
    for (Entry<String, String> entry : state.getHeldLocks().entries()) {
      if (!entry.getKey().equals(activeThread)) {
        newHeldLocks.put(entry);
      }
    }
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

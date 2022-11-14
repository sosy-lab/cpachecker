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
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class DataRaceState implements AbstractQueryableState {

  private static final String PROPERTY_DATA_RACE = "data-race";

  private final ImmutableSet<MemoryAccess> memoryAccesses;
  private final ImmutableSet<MemoryAccess> accessesWithSubsequentWrites;
  private final ImmutableMap<String, ThreadInfo> threadInfo;
  private final ImmutableSet<ThreadSynchronization> threadSynchronizations;
  private final ImmutableSetMultimap<String, String> heldLocks;
  private final ImmutableSet<LockRelease> lastReleases;
  private final boolean hasDataRace;

  DataRaceState(Map<String, ThreadInfo> pThreadInfo, boolean pHasDataRace) {
    this(
        ImmutableSet.of(),
        ImmutableSet.of(),
        pThreadInfo,
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSet.of(),
        pHasDataRace);
  }

  DataRaceState(
      Set<MemoryAccess> pMemoryAccesses,
      Set<MemoryAccess> pAccessesWithSubsequentWrites,
      Map<String, ThreadInfo> pThreadInfo,
      Set<ThreadSynchronization> pThreadSynchronizations,
      SetMultimap<String, String> pHeldLocks,
      Set<LockRelease> pLastReleases,
      boolean pHasDataRace) {
    memoryAccesses = ImmutableSet.copyOf(pMemoryAccesses);
    accessesWithSubsequentWrites = ImmutableSet.copyOf(pAccessesWithSubsequentWrites);
    threadInfo = ImmutableMap.copyOf(pThreadInfo);
    threadSynchronizations = ImmutableSet.copyOf(pThreadSynchronizations);
    heldLocks = ImmutableSetMultimap.copyOf(pHeldLocks);
    lastReleases = ImmutableSet.copyOf(pLastReleases);
    hasDataRace = pHasDataRace;
  }

  ImmutableSet<MemoryAccess> getMemoryAccesses() {
    return memoryAccesses;
  }

  Set<MemoryAccess> getAccessesWithSubsequentWrites() {
    return accessesWithSubsequentWrites;
  }

  Map<String, ThreadInfo> getThreadInfo() {
    return threadInfo;
  }

  Set<ThreadSynchronization> getThreadSynchronizations() {
    return threadSynchronizations;
  }

  public ImmutableSetMultimap<String, String> getHeldLocks() {
    return heldLocks;
  }

  public Set<LockRelease> getLastReleases() {
    return lastReleases;
  }

  Set<String> getLocksForThread(String threadId) {
    return heldLocks.get(threadId);
  }

  @Nullable LockRelease getLastReleaseForLock(String lock) {
    for (LockRelease release : lastReleases) {
      if (release.getLockId().equals(lock)) {
        return release;
      }
    }
    return null;
  }

  boolean hasDataRace() {
    return hasDataRace;
  }

  @Override
  public String getCPAName() {
    return "DataRaceCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    if (property.equals(PROPERTY_DATA_RACE)) {
      return hasDataRace();
    }
    throw new InvalidQueryException("Invalid query: " + property);
  }

  @Override
  public String toString() {
    return memoryAccesses.size()
        + " memory access"
        + (memoryAccesses.size() == 1 ? "" : "es")
        + ", "
        + threadSynchronizations.size()
        + " thread synchronization"
        + (threadSynchronizations.size() == 1 ? "" : "s");
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof DataRaceState)) {
      return false;
    }
    DataRaceState that = (DataRaceState) pO;
    return hasDataRace == that.hasDataRace
        && memoryAccesses.equals(that.memoryAccesses)
        && accessesWithSubsequentWrites.equals(that.accessesWithSubsequentWrites)
        && threadInfo.equals(that.threadInfo)
        && threadSynchronizations.equals(that.threadSynchronizations)
        && heldLocks.equals(that.heldLocks)
        && lastReleases.equals(that.lastReleases);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        memoryAccesses,
        accessesWithSubsequentWrites,
        threadInfo,
        threadSynchronizations,
        heldLocks,
        lastReleases,
        hasDataRace);
  }
}

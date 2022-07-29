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
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class DataRaceState implements AbstractQueryableState {

  private static final String PROPERTY_DATA_RACE = "data-race";

  private final ImmutableSet<MemoryAccess> memoryAccesses;
  private final ImmutableMap<MemoryAccess, MemoryAccess> subsequentWrites;
  private final ImmutableMap<String, Integer> threadEpochs;
  private final ImmutableSet<ThreadSynchronization> threadSynchronizations;
  private final ImmutableSetMultimap<String, String> heldLocks;
  private final ImmutableSet<LockRelease> lastReleases;
  private final boolean hasDataRace;

  DataRaceState(Map<String, Integer> pThreadEpochs, boolean pHasDataRace) {
    this(
        ImmutableSet.of(),
        ImmutableMap.of(),
        pThreadEpochs,
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSet.of(),
        pHasDataRace);
  }

  DataRaceState(
      Set<MemoryAccess> pMemoryAccesses,
      Map<MemoryAccess, MemoryAccess> pSubsequentWrites,
      Map<String, Integer> pThreadEpochs,
      Set<ThreadSynchronization> pThreadSynchronizations,
      SetMultimap<String, String> pHeldLocks,
      Set<LockRelease> pLastReleases,
      boolean pHasDataRace) {
    memoryAccesses = ImmutableSet.copyOf(pMemoryAccesses);
    subsequentWrites = ImmutableMap.copyOf(pSubsequentWrites);
    threadEpochs = ImmutableMap.copyOf(pThreadEpochs);
    threadSynchronizations = ImmutableSet.copyOf(pThreadSynchronizations);
    heldLocks = ImmutableSetMultimap.copyOf(pHeldLocks);
    lastReleases = ImmutableSet.copyOf(pLastReleases);
    hasDataRace = pHasDataRace;
  }

  ImmutableSet<MemoryAccess> getMemoryAccesses() {
    return memoryAccesses;
  }

  Map<MemoryAccess, MemoryAccess> getSubsequentWrites() {
    return subsequentWrites;
  }

  Map<String, Integer> getThreadEpochs() {
    return threadEpochs;
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
}

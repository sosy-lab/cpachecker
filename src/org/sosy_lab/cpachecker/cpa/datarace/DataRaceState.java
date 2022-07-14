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
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class DataRaceState implements AbstractQueryableState {

  private static final String PROPERTY_DATA_RACE = "data-race";

  private final ImmutableSet<MemoryAccess> memoryAccesses;
  private final ImmutableMap<String, ThreadInfo> threads;
  private final ImmutableSet<ThreadSynchronization> threadSynchronizations;
  private final boolean hasDataRace;

  DataRaceState(
      Set<MemoryAccess> pMemoryAccesses,
      Map<String, ThreadInfo> pThreads,
      Set<ThreadSynchronization> pThreadSynchronizations,
      boolean pHasDataRace) {
    memoryAccesses = ImmutableSet.copyOf(pMemoryAccesses);
    threads = ImmutableMap.copyOf(pThreads);
    threadSynchronizations = ImmutableSet.copyOf(pThreadSynchronizations);
    hasDataRace = pHasDataRace;
  }

  ImmutableSet<MemoryAccess> getMemoryAccesses() {
    return memoryAccesses;
  }

  Map<String, ThreadInfo> getThreads() {
    return threads;
  }

  Set<ThreadSynchronization> getThreadSynchronizations() {
    return threadSynchronizations;
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

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
  private final ImmutableMap<MemoryAccess, MemoryAccess> subsequentWrites;
  private final ImmutableMap<String, Integer> threadEpochs;
  private final ImmutableSet<ThreadSynchronization> threadSynchronizations;
  private final boolean hasDataRace;

  DataRaceState(
      Set<MemoryAccess> pMemoryAccesses,
      Map<MemoryAccess, MemoryAccess> pSubsequentWrites,
      Map<String, Integer> pThreadEpochs,
      Set<ThreadSynchronization> pThreadSynchronizations,
      boolean pHasDataRace) {
    memoryAccesses = ImmutableSet.copyOf(pMemoryAccesses);
    subsequentWrites = ImmutableMap.copyOf(pSubsequentWrites);
    threadEpochs = ImmutableMap.copyOf(pThreadEpochs);
    threadSynchronizations = ImmutableSet.copyOf(pThreadSynchronizations);
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

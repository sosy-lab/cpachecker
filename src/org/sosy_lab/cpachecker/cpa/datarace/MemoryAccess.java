// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class MemoryAccess {

  private final String threadId;
  private final MemoryLocation memoryLocation;
  private final boolean isWrite;
  private final ImmutableSet<String> locks;
  private final CFAEdge edge;
  private final boolean hasSubsequentWrite;
  private final int accessEpoch;

  MemoryAccess(
      String pThreadId,
      MemoryLocation pMemoryLocation,
      boolean pIsWrite,
      Set<String> pLocks,
      CFAEdge pEdge,
      boolean pHasSubsequentWrite,
      int pAccessEpoch) {
    threadId = pThreadId;
    memoryLocation = pMemoryLocation;
    isWrite = pIsWrite;
    locks = ImmutableSet.copyOf(pLocks);
    edge = pEdge;
    hasSubsequentWrite = pHasSubsequentWrite;
    accessEpoch = pAccessEpoch;
  }

  String getThreadId() {
    return threadId;
  }

  MemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  boolean isWrite() {
    return isWrite;
  }

  Set<String> getLocks() {
    return locks;
  }

  boolean hasSubsequentWrite() {
    return hasSubsequentWrite;
  }

  int getAccessEpoch() {
    return accessEpoch;
  }

  MemoryAccess withSubsequentWrite() {
    return new MemoryAccess(threadId, memoryLocation, isWrite, locks, edge, true, accessEpoch);
  }

  boolean happensBefore(MemoryAccess other, Set<ThreadSynchronization> threadSynchronizations) {
    if (threadId.equals(other.threadId)) {
      return accessEpoch <= other.accessEpoch;
    }

    Set<ThreadSynchronization> relevantSynchronizations = new HashSet<>();
    boolean changed = true;
    while (changed) {
      changed = false;
      for (ThreadSynchronization synchronization : threadSynchronizations) {
        if (!relevantSynchronizations.contains(synchronization)
            && isRelevant(synchronization, relevantSynchronizations)) {
          relevantSynchronizations.add(synchronization);
          changed = true;
        }
      }
    }

    for (ThreadSynchronization synchronization : threadSynchronizations) {
      if (synchronization.getReadThread().equals(other.threadId)
          && synchronization.getReadThreadIndex() <= other.accessEpoch) {
        return true;
      }
    }
    return false;
  }

  private boolean isRelevant(
      ThreadSynchronization threadSynchronization,
      Set<ThreadSynchronization> relevantSynchronizations) {
    if (threadSynchronization.getWriteThread().equals(threadId)) {
      return threadSynchronization.getWriteThreadIndex() >= accessEpoch;
    }
    for (ThreadSynchronization relevantSynchronization : relevantSynchronizations) {
      if (relevantSynchronization.getReadThread().equals(threadSynchronization.getWriteThread())
          && relevantSynchronization.getReadThreadIndex()
              <= threadSynchronization.getWriteThreadIndex()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "MemoryAccess{threadId='"
        + threadId
        + "', memoryLocation="
        + memoryLocation
        + ", isWrite="
        + isWrite
        + ", edge="
        + edge
        + '}';
  }
}

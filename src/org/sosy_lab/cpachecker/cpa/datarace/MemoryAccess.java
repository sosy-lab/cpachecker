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
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class MemoryAccess {

  private final String threadId;
  private final int epoch;
  private final MemoryLocation memoryLocation;
  private final boolean isWrite;
  private final ImmutableSet<String> locks;
  private final CFAEdge edge;
  private final boolean hasSubsequentRead;
  private final int threadIndex;

  MemoryAccess(
      String pThreadId,
      int pEpoch,
      MemoryLocation pMemoryLocation,
      boolean pIsWrite,
      Set<String> pLocks,
      CFAEdge pEdge,
      boolean pHasSubsequentRead,
      int pThreadIndex) {
    threadId = pThreadId;
    epoch = pEpoch;
    memoryLocation = pMemoryLocation;
    isWrite = pIsWrite;
    locks = ImmutableSet.copyOf(pLocks);
    edge = pEdge;
    hasSubsequentRead = pHasSubsequentRead;
    threadIndex = pThreadIndex;
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

  boolean hasSubsequentRead() {
    return hasSubsequentRead;
  }

  int getThreadIndex() {
    return threadIndex;
  }

  MemoryAccess withSubsequentRead() {
    return new MemoryAccess(
        threadId, epoch, memoryLocation, isWrite, locks, edge, true, threadIndex);
  }

  boolean happensBefore(
      MemoryAccess other,
      Map<String, ThreadInfo> threads,
      Set<ThreadSynchronization> threadSynchronizations) {
    if (threadId.equals(other.threadId)) {
      return threadIndex <= other.threadIndex;
    }

    if (threads.containsKey(other.getThreadId())) {
      ThreadInfo ancestor = threads.get(other.getThreadId());
      while (ancestor != null) {
        ThreadInfo parent = ancestor.getParent();
        if (parent != null && parent.getName().equals(threadId)) {
          break;
        }
        ancestor = parent;
      }
      if (ancestor != null && ancestor.getCreationEpoch() > epoch) {
        return true;
      }
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
          && synchronization.getReadThreadIndex() <= other.threadIndex) {
        return true;
      }
    }
    return false;
  }

  private boolean isRelevant(
      ThreadSynchronization threadSynchronization,
      Set<ThreadSynchronization> relevantSynchronizations) {
    if (threadSynchronization.getWriteThread().equals(threadId)) {
      return threadSynchronization.getWriteThreadIndex() >= threadIndex;
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

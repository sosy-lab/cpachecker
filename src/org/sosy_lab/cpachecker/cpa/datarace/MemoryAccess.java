// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class MemoryAccess {

  private final String threadId;
  private final int epoch;
  private final MemoryLocation memoryLocation;
  private final boolean isWrite;
  private final ImmutableSet<String> locks;

  MemoryAccess(
      String pThreadId,
      int pEpoch,
      MemoryLocation pMemoryLocation,
      boolean pIsWrite,
      Set<String> pLocks) {
    threadId = pThreadId;
    epoch = pEpoch;
    memoryLocation = pMemoryLocation;
    isWrite = pIsWrite;
    locks = ImmutableSet.copyOf(pLocks);
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

  boolean happensBefore(MemoryAccess other, Map<String, ThreadInfo> threads) {
    if (threadId.equals(other.getThreadId())) {
      return true;
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
    // TODO: Check for synchronizes-with relationship? What memory model do we assume?
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
        + '}';
  }
}

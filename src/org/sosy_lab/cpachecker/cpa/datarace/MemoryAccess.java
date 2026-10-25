// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class MemoryAccess {

  private final String threadId;
  private final OverapproximatingMemoryLocation memoryLocation;
  private final boolean isWrite;
  private final ImmutableSet<String> locks;
  private final CFAEdge edge;
  private final int accessEpoch;
  // Whether a variable used to compute the address was changed after the access
  private final boolean addressMayHaveChanged;

  MemoryAccess(
      String pThreadId,
      OverapproximatingMemoryLocation pMemoryLocation,
      boolean pIsWrite,
      Set<String> pLocks,
      CFAEdge pEdge,
      int pAccessEpoch) {
    this(pThreadId, pMemoryLocation, pIsWrite, pLocks, pEdge, pAccessEpoch, false);
  }

  private MemoryAccess(
      String pThreadId,
      OverapproximatingMemoryLocation pMemoryLocation,
      boolean pIsWrite,
      Set<String> pLocks,
      CFAEdge pEdge,
      int pAccessEpoch,
      boolean pAddressMayHaveChanged) {
    threadId = pThreadId;
    memoryLocation = pMemoryLocation;
    isWrite = pIsWrite;
    locks = ImmutableSet.copyOf(pLocks);
    edge = pEdge;
    accessEpoch = pAccessEpoch;
    addressMayHaveChanged = pAddressMayHaveChanged;
  }

  /** Returns this access, marked if its address depends on one of the given changed variables. */
  MemoryAccess withChangedVariables(Set<MemoryLocation> pChangedVariables) {
    if (addressMayHaveChanged
        || Collections.disjoint(memoryLocation.getAddressVariables(), pChangedVariables)) {
      return this;
    }
    return new MemoryAccess(threadId, memoryLocation, isWrite, locks, edge, accessEpoch, true);
  }

  boolean addressMayHaveChanged() {
    return addressMayHaveChanged;
  }

  CExpression getExpression() {
    return memoryLocation.getExpression();
  }

  String getThreadId() {
    return threadId;
  }

  boolean isWrite() {
    return isWrite;
  }

  Set<String> getLocks() {
    return locks;
  }

  int getAccessEpoch() {
    return accessEpoch;
  }

  boolean isOverapproximating() {
    return memoryLocation.isAmbiguous() || !memoryLocation.isPrecise();
  }

  boolean mayBePointedTo() {
    return memoryLocation.mayBePointedTo();
  }

  CType getType() {
    return memoryLocation.getType();
  }

  boolean memoryLocationIsAtomic() {
    return memoryLocation.getType().getCanonicalType().isAtomic();
  }

  boolean mightAccessSameLocationAs(MemoryAccess other) {
    return !Sets.intersection(
            memoryLocation.getMemoryLocations(), other.memoryLocation.getMemoryLocations())
        .isEmpty();
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

    for (ThreadSynchronization synchronization : relevantSynchronizations) {
      if (synchronization.getReadThread().equals(other.threadId)
          && synchronization.getReadEpoch() <= other.accessEpoch) {
        return true;
      }
    }
    return false;
  }

  private boolean isRelevant(
      ThreadSynchronization threadSynchronization,
      Set<ThreadSynchronization> relevantSynchronizations) {
    if (threadSynchronization.getWriteThread().equals(threadId)) {
      return threadSynchronization.getWriteEpoch() >= accessEpoch;
    }
    for (ThreadSynchronization relevantSynchronization : relevantSynchronizations) {
      if (relevantSynchronization.getReadThread().equals(threadSynchronization.getWriteThread())
          && relevantSynchronization.getReadEpoch() <= threadSynchronization.getWriteEpoch()) {
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

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof MemoryAccess access
        && isWrite == access.isWrite
        && accessEpoch == access.accessEpoch
        && addressMayHaveChanged == access.addressMayHaveChanged
        && threadId.equals(access.threadId)
        && memoryLocation.equals(access.memoryLocation)
        && locks.equals(access.locks)
        && edge.equals(access.edge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        threadId, memoryLocation, isWrite, locks, edge, accessEpoch, addressMayHaveChanged);
  }
}

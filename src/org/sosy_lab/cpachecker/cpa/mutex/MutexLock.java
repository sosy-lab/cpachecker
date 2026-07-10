// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;


import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

public record MutexLock(String handle, MutexLockType type) {
  enum MutexLockType {
    READ,
    WRITE,
    BOTH,
  }

  /**
   * Whether this is a shared/read lock ({@code pthread_rwlock_rdlock}): read-locked sections of
   * the same rwlock may overlap each other, they only exclude write-locked sections.
   */
  public boolean isReadLock() {
    return type == MutexLockType.READ;
  }

  ImmutableCollection<MutexLock> getBlockingLocks() {
    if (type == MutexLockType.READ) {
      return ImmutableList.of(
          new MutexLock(handle, MutexLockType.WRITE),
          new MutexLock(handle, MutexLockType.BOTH)
      );
    }
    return ImmutableList.of(
        new MutexLock(handle, MutexLockType.READ),
        new MutexLock(handle, MutexLockType.WRITE),
        new MutexLock(handle, MutexLockType.BOTH)
    );
  }
}

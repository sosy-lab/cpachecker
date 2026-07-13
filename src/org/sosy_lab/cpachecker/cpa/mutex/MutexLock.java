// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

/**
 * A specific mutex/rwlock operation: which lock ({@code handle}, a canonical storage-location key
 * as computed by {@link MutexFunctions#extractMutexName}) and which kind of hold ({@code type}).
 *
 * <p>{@code handle} must never be {@code null}. Callers that cannot resolve a mutex expression to
 * a canonical key (see {@link MutexFunctions#extractMutexName}) must not construct a {@code
 * MutexLock} at all: treating the edge as an unrecognized/unmodelled mutex operation is the sound
 * fallback, since it only costs reduction power (POR/OC then explore more interleavings than
 * strictly necessary around that lock) and never hides a real interleaving.
 */
public record MutexLock(String handle, MutexLockType type) {
  public MutexLock {
    checkNotNull(handle);
    checkNotNull(type);
  }

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

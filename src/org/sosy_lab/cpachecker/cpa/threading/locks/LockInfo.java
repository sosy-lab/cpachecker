// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading.locks;

import java.util.Objects;

public abstract class LockInfo implements Comparable<LockInfo> {

  public enum LockType {
    MUTEX,
    RW_MUTEX
  }

  private final String lockId;
  private final LockType lockType;

  protected LockInfo(String pLockId, LockType pLockType) {
    lockId = pLockId;
    lockType = pLockType;
  }

  public String getLockId() {
    return lockId;
  }

  public LockType getLockType() {
    return lockType;
  }

  @Override
  public int compareTo(LockInfo other) {
    if (lockId.equals(other.lockId)) {
      return lockType.compareTo(other.lockType);
    }
    return lockId.compareTo(other.lockId);
  }

  @Override
  public String toString() {
    return lockId + "[" + lockType + "]";
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof LockInfo)) {
      return false;
    }
    LockInfo lockInfo = (LockInfo) pO;
    return lockId.equals(lockInfo.lockId) && lockType == lockInfo.lockType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lockId, lockType);
  }

  /** Returns whether some thread is holding the lock. */
  public abstract boolean isHeld();

  /** Returns whether the specified thread is holding the lock. */
  public abstract boolean isHeldByThread(String pThreadId);

  public abstract LockInfo acquire(String pThreadId);

  public abstract LockInfo release(String pThreadId);
}

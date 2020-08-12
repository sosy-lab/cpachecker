/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.rcucpa;

import java.util.Objects;

public class LockStateRCU {
  public enum HeldLock {
    NO_LOCK,
    READ_LOCK,
    WRITE_LOCK
  }

  private final HeldLock lockType;
  private final int readLockCount;

  private LockStateRCU(HeldLock lock, int readCount) {
    lockType = lock;
    readLockCount = readCount;
  }

  LockStateRCU() {
    this(HeldLock.NO_LOCK, 0);
  }

  LockStateRCU markRead() {
    return new LockStateRCU(HeldLock.READ_LOCK, readLockCount);
  }

  LockStateRCU markWrite() {
    return new LockStateRCU(HeldLock.WRITE_LOCK, readLockCount);
  }

  LockStateRCU clearLock() {
    return new LockStateRCU(HeldLock.NO_LOCK, readLockCount);
  }

  LockStateRCU incRCURead() {
    return new LockStateRCU(lockType, readLockCount + 1);
  }

  LockStateRCU decRCURead() {
    return new LockStateRCU(lockType, readLockCount - 1);
  }

  @Override
  public String toString() {
    return "\n Lock Type: " + lockType.name() + "\n Read Lock Count: " + readLockCount;
  }

  public LockStateRCU join(LockStateRCU other) {
    int minReadLock = Math.min(this.readLockCount, other.readLockCount);
    HeldLock lock;

    if (this.lockType == HeldLock.NO_LOCK || other.lockType == HeldLock.NO_LOCK) {
      lock = HeldLock.NO_LOCK;
    } else {
      if (this.lockType == HeldLock.WRITE_LOCK || other.lockType == HeldLock.WRITE_LOCK) {
        lock = HeldLock.WRITE_LOCK;
      } else {
        lock = HeldLock.READ_LOCK;
      }
    }
    return new LockStateRCU(lock, minReadLock);
  }

  public boolean isLessOrEqual(LockStateRCU other) {
    return readLockCount <= other.readLockCount && lockType == other.lockType;
  }

  public int compareTo(LockStateRCU other) {
    int res = lockType.compareTo(other.lockType);
    if (res != 0) {
      return res;
    }
    return readLockCount - other.readLockCount;
  }

  boolean isCompatible(LockStateRCU other) {
    boolean first = this.lockType == HeldLock.READ_LOCK &&
                    this.readLockCount > 0
                    && other.lockType == HeldLock.WRITE_LOCK;
    boolean second = this.lockType == HeldLock.WRITE_LOCK
                      && other.lockType == HeldLock.READ_LOCK
                      && other.readLockCount > 0;
    return !first && !second;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lockType, readLockCount);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    LockStateRCU that = (LockStateRCU) pO;
    return readLockCount == that.readLockCount && lockType == that.lockType;
  }
}

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

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LockStateRCU implements LatticeAbstractState<LockStateRCU>{
  @Override
  public LockStateRCU join(LockStateRCU other) {
    return null;
  }

  private LockStateRCU(HeldLock lock, int readCount) {
    lockType = lock;
    readLockCount = readCount;
  }

  @Override
  public boolean isLessOrEqual(LockStateRCU other) throws CPAException, InterruptedException {
    return readLockCount <= other.readLockCount;
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
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    LockStateRCU that = (LockStateRCU) pO;

    if (readLockCount != that.readLockCount) {
      return false;
    }
    if (lockType != that.lockType) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = lockType.hashCode();
    result = 31 * result + readLockCount;
    return result;
  }

  public enum HeldLock {
    NO_LOCK, READ_LOCK, WRITE_LOCK
  }

  private HeldLock lockType;
  private int readLockCount;

  LockStateRCU() {
    readLockCount = 0;
    lockType = HeldLock.NO_LOCK;
  }

  void markRead() {
    lockType = HeldLock.READ_LOCK;
  }

  void markWrite() {
    lockType = HeldLock.WRITE_LOCK;
  }

  void clearLock() {
    lockType = HeldLock.NO_LOCK;
  }

  void incRCURead() {
    ++readLockCount;
  }

  void decRCURead() {
    --readLockCount;
  }

  @Override
  public String toString() {
    return "\n Lock Type: " + lockType.name() +
            "\n Read Lock Count: " + readLockCount;
  }

  public static LockStateRCU copyOf(LockStateRCU other) {
    return new LockStateRCU(other.lockType, other.readLockCount);
  }

}

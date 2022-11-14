// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import java.util.Objects;

public class LockRelease {

  private final String lockId;
  private final String threadId;
  private final int accessEpoch;

  LockRelease(String pLockId, String pThreadId, int pAccessEpoch) {
    lockId = pLockId;
    threadId = pThreadId;
    accessEpoch = pAccessEpoch;
  }

  public String getLockId() {
    return lockId;
  }

  public String getThreadId() {
    return threadId;
  }

  public int getAccessEpoch() {
    return accessEpoch;
  }

  @Override
  public String toString() {
    return "LockRelease{threadId='"
        + threadId
        + "', lockId="
        + lockId
        + ", accessEpoch="
        + accessEpoch
        + '}';
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof LockRelease)) {
      return false;
    }
    LockRelease release = (LockRelease) pO;
    return accessEpoch == release.accessEpoch
        && lockId.equals(release.lockId)
        && threadId.equals(release.threadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lockId, threadId, accessEpoch);
  }
}

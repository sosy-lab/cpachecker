// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.base.Objects;

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
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    LockRelease release = (LockRelease) pO;
    return accessEpoch == release.accessEpoch
        && Objects.equal(lockId, release.lockId)
        && Objects.equal(threadId, release.threadId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(lockId, threadId, accessEpoch);
  }
}

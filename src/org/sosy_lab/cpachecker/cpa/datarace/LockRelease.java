// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

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
}

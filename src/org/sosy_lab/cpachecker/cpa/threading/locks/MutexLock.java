// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading.locks;

import com.google.common.base.Preconditions;
import java.util.Objects;

public class MutexLock extends LockInfo {

  private final String threadId;

  public MutexLock(String pLockId) {
    this(pLockId, null);
  }

  private MutexLock(String pLockId, String pThreadId) {
    super(pLockId, LockType.MUTEX);
    threadId = pThreadId;
  }

  @Override
  public boolean isHeldByThread() {
    return threadId != null;
  }

  @Override
  public boolean isHeldByThread(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    return pThreadId.equals(threadId);
  }

  @Override
  public LockInfo acquire(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkState(!isHeldByThread());
    return new MutexLock(getLockId(), pThreadId);
  }

  @Override
  public LockInfo release(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkArgument(isHeldByThread(pThreadId));
    return new MutexLock(getLockId());
  }

  @Override
  public String toString() {
    return super.toString() + (isHeldByThread() ? " held by " + threadId : "");
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof MutexLock)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    MutexLock mutexLock = (MutexLock) pO;
    return Objects.equals(threadId, mutexLock.threadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), threadId);
  }
}

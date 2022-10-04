// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading.locks;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;

public class MutexLock extends LockInfo {

  protected static final LockInfo LOCAL_ACCESS_LOCK =
      new MutexLock(ThreadingTransferRelation.LOCAL_ACCESS_LOCK);

  private final String threadId;

  public MutexLock(String pLockId) {
    this(pLockId, null);
  }

  MutexLock(String pLockId, String pThreadId) {
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
    Preconditions.checkState(threadId == null);
    return new MutexLock(getLockId(), pThreadId);
  }

  @Override
  public LockInfo release(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkArgument(pThreadId.equals(threadId));
    return new MutexLock(getLockId());
  }

  @Override
  public String toString() {
    return super.toString() + (isHeldByThread() ? " held by " + threadId : "");
  }
}

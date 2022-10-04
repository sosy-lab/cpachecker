// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public abstract class LockInfo implements Comparable<LockInfo> {

  // TODO: Add subclasses for other lock types
  public enum LockType {
    MUTEX,
    RECURSIVE_MUTEX,
    RW_MUTEX
  }

  private final String lockId;
  private final LockType lockType;

  public LockInfo(String pLockId, LockType pLockType) {
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

  public static LockInfo getLocalAccessLock() {
    return MutexLock.LOCAL_ACCESS_LOCK;
  }

  /** Returns whether some thread is holding the lock. */
  public abstract boolean isHeldByThread();

  /** Returns whether the specified thread is holding the lock. */
  public abstract boolean isHeldByThread(String pThreadId);

  public abstract LockInfo acquire(String pThreadId);

  public abstract LockInfo release(String pThreadId);
}

class MutexLock extends LockInfo {

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

class RWLock extends LockInfo {

  private final PersistentSet<String> readers;
  private final String writer;

  public RWLock(String pLockId) {
    this(pLockId, new PersistentSet<>(), null);
  }

  private RWLock(String pLockId, PersistentSet<String> pReaders, String pWriter) {
    super(pLockId, LockType.RW_MUTEX);
    readers = pReaders;
    writer = pWriter;
  }

  private RWLock withReaders(PersistentSet<String> pReaders) {
    return new RWLock(getLockId(), pReaders, writer);
  }

  private RWLock withWriter(String pWriter) {
    return new RWLock(getLockId(), readers, pWriter);
  }

  public RWLock addReader(String pReader) {
    Preconditions.checkNotNull(pReader);
    Preconditions.checkState(writer == null);
    return withReaders(readers.addAndCopy(pReader));
  }

  public boolean hasReader() {
    return !readers.isEmpty();
  }

  public RWLock removeReader(String pReader) {
    Preconditions.checkNotNull(pReader);
    Preconditions.checkArgument(readers.contains(pReader));
    return withReaders(readers.removeAndCopy(pReader));
  }

  public RWLock addWriter(String pWriter) {
    Preconditions.checkNotNull(pWriter);
    Preconditions.checkState(writer == null && readers.isEmpty());
    return withWriter(pWriter);
  }

  public boolean hasWriter() {
    return writer != null;
  }

  public RWLock removeWriter(String pWriter) {
    Preconditions.checkNotNull(pWriter);
    Preconditions.checkArgument(pWriter.equals(writer));
    return withWriter(null);
  }

  @Override
  public boolean isHeldByThread() {
    return writer != null || !readers.isEmpty();
  }

  @Override
  public boolean isHeldByThread(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    return pThreadId.equals(writer) || readers.contains(pThreadId);
  }

  @Override
  public LockInfo acquire(String pThreadId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LockInfo release(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    Preconditions.checkArgument(isHeldByThread(pThreadId));
    if (pThreadId.equals(writer)) {
      return removeWriter(pThreadId);
    }
    return removeReader(pThreadId);
  }

  @Override
  public String toString() {
    return super.toString()
        + (hasReader() ? " with readers " + readers : "")
        + (hasWriter() ? " with writer " + writer : "");
  }
}

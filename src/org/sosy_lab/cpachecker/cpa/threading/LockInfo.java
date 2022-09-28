// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public abstract class LockInfo implements Comparable<LockInfo> {

  // TODO: Add subclasses for other lock types
  public enum LockType {
    MUTEX,
    RECURSIVE_MUTEX,
    RW_MUTEX,
    LOCAL_ACCESS_LOCK
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
    assert writer == null;
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
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading.locks;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class RWLock extends LockInfo {

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

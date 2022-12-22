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
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class RWLock extends LockInfo {

  private final PersistentSet<String> readers;
  private final String writer;
  private final boolean lastReleaseWasWriter;

  public RWLock(String pLockId) {
    this(pLockId, new PersistentSet<>(), null, false);
  }

  private RWLock(
      String pLockId,
      PersistentSet<String> pReaders,
      String pWriter,
      boolean pLastReleaseWasWriter) {
    super(pLockId, LockType.RW_MUTEX);
    readers = pReaders;
    writer = pWriter;
    lastReleaseWasWriter = pLastReleaseWasWriter;
  }

  public RWLock addReader(String pReader) {
    Preconditions.checkNotNull(pReader);
    Preconditions.checkState(writer == null);
    return new RWLock(getLockId(), readers.addAndCopy(pReader), writer, lastReleaseWasWriter);
  }

  public boolean hasReader() {
    return !readers.isEmpty();
  }

  private RWLock removeReader(String pReader) {
    Preconditions.checkNotNull(pReader);
    Preconditions.checkArgument(readers.contains(pReader));
    return new RWLock(getLockId(), readers.removeAndCopy(pReader), writer, false);
  }

  public RWLock addWriter(String pWriter) {
    Preconditions.checkNotNull(pWriter);
    Preconditions.checkState(writer == null && readers.isEmpty());
    return new RWLock(getLockId(), readers, pWriter, lastReleaseWasWriter);
  }

  public boolean hasWriter() {
    return writer != null;
  }

  private RWLock removeWriter(String pWriter) {
    Preconditions.checkNotNull(pWriter);
    Preconditions.checkArgument(pWriter.equals(writer));
    return new RWLock(getLockId(), readers, null, true);
  }

  public boolean wasLastReleaseWriter() {
    return lastReleaseWasWriter;
  }

  @Override
  public boolean isHeld() {
    return writer != null || !readers.isEmpty();
  }

  @Override
  public boolean isHeldByThread(String pThreadId) {
    Preconditions.checkNotNull(pThreadId);
    return pThreadId.equals(writer) || readers.contains(pThreadId);
  }

  @Override
  public LockInfo acquire(String pThreadId) {
    throw new UnsupportedOperationException(
        "RWLock does not support generic acquire, use addReader/addWriter instead");
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

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof RWLock)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    RWLock rwLock = (RWLock) pO;
    return readers.equals(rwLock.readers) && Objects.equals(writer, rwLock.writer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), readers, writer);
  }
}

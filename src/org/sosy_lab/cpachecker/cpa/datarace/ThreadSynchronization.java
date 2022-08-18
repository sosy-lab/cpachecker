// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.base.Objects;

public class ThreadSynchronization {

  private final String writeThread;
  private final String readThread;
  private final int writeEpoch;
  private final int readEpoch;

  ThreadSynchronization(String pWriteThread, String pReadThread, int pWriteEpoch, int pReadEpoch) {
    writeThread = pWriteThread;
    readThread = pReadThread;
    writeEpoch = pWriteEpoch;
    readEpoch = pReadEpoch;
  }

  public String getWriteThread() {
    return writeThread;
  }

  public String getReadThread() {
    return readThread;
  }

  public int getWriteEpoch() {
    return writeEpoch;
  }

  public int getReadEpoch() {
    return readEpoch;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ThreadSynchronization that = (ThreadSynchronization) pO;
    return writeEpoch == that.writeEpoch
        && readEpoch == that.readEpoch
        && Objects.equal(writeThread, that.writeThread)
        && Objects.equal(readThread, that.readThread);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(writeThread, readThread, writeEpoch, readEpoch);
  }
}

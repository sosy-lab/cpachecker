// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import java.util.Objects;

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
    if (!(pO instanceof ThreadSynchronization)) {
      return false;
    }
    ThreadSynchronization that = (ThreadSynchronization) pO;
    return getWriteEpoch() == that.getWriteEpoch()
        && getReadEpoch() == that.getReadEpoch()
        && getWriteThread().equals(that.getWriteThread())
        && getReadThread().equals(that.getReadThread());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getWriteThread(), getReadThread(), getWriteEpoch(), getReadEpoch());
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

public class ThreadSynchronization {

  private final String writeThread;
  private final String readThread;
  private final int writeEpoch;
  private final int readEpoch;

  ThreadSynchronization(
      String pWriteThread, String pReadThread, int pWriteEpoch, int pReadEpoch) {
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
}

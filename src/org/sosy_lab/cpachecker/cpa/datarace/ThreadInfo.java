// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

public class ThreadInfo {

  private final String threadId;
  private final int epoch;
  private final boolean running;

  ThreadInfo(String pThreadId, int pEpoch, boolean pRunning) {
    threadId = pThreadId;
    epoch = pEpoch;
    running = pRunning;
  }

  public String getThreadId() {
    return threadId;
  }

  public int getEpoch() {
    return epoch;
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public String toString() {
    return "ThreadInfo{threadId='" + threadId + "', epoch=" + epoch + ", running=" + running + '}';
  }
}

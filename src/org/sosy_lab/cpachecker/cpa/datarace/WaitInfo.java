// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

public class WaitInfo {

  // Thread id of the waiting thread
  private final String waitingThread;
  // ID of the lock or condition variable waited on
  private final String waitingOn;
  // Thread epoch when the waiting thread started to wait
  private final int epoch;

  public WaitInfo(String pWaitingThread, String pWaitingOn, int pEpoch) {
    waitingThread = pWaitingThread;
    waitingOn = pWaitingOn;
    epoch = pEpoch;
  }

  public String getWaitingThread() {
    return waitingThread;
  }

  public String getWaitingOn() {
    return waitingOn;
  }

  public int getEpoch() {
    return epoch;
  }
}

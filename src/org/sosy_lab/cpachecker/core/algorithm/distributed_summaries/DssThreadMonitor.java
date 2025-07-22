// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.util.List;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;

public class DssThreadMonitor extends Thread {

  private static final String THREAD_NAME = "dss-monitor";

  private final List<Thread> threadsToMonitor;
  private final DssConnection connection;
  private final DssMessageFactory messageFactory;

  public DssThreadMonitor(
      List<Thread> pThreadsToMonitor,
      DssMessageFactory pMessageFactory,
      DssConnection pConnection) {
    super(THREAD_NAME);
    threadsToMonitor = pThreadsToMonitor;
    connection = pConnection;
    messageFactory = pMessageFactory;
  }

  @Override
  public void run() {
    while (true) {
      boolean allWaiting =
          threadsToMonitor.stream()
              .allMatch(
                  t -> {
                    Thread.State state = t.getState();
                    return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
                  });

      if (allWaiting) {
        connection
            .getBroadcaster()
            .broadcastToAll(messageFactory.createDssResultMessage(THREAD_NAME, Result.TRUE));
        break;
      }

      try {
        Thread.sleep(1); // 1 millisecond
      } catch (InterruptedException e) {
        connection
            .getBroadcaster()
            .broadcastToAll(messageFactory.createDssExceptionMessage(THREAD_NAME, e));
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}

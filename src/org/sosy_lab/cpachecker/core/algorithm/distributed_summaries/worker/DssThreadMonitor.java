// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;

public class DssThreadMonitor extends Thread {

  private static final String THREAD_NAME = "dss-monitor";

  private final List<Thread> threadsToMonitor;
  private final DssConnection connection;
  private final DssMessageFactory messageFactory;

  public static Set<String> active = ConcurrentHashMap.newKeySet();

  public DssThreadMonitor(
      List<Thread> pThreadsToMonitor,
      DssMessageFactory pMessageFactory,
      DssConnection pConnection) {
    super(THREAD_NAME);
    threadsToMonitor = pThreadsToMonitor;
    connection = pConnection;
    messageFactory = pMessageFactory;
  }

  public static void add(String id) {
    active.add(id);
  }

  public static void remove(String id) {
    active.remove(id);
  }

  @Override
  public void run() {
    while (true) {
      boolean allWaiting =
          threadsToMonitor.stream()
              .allMatch(
                  t ->
                      t.getState() == Thread.State.WAITING
                          || t.getState() == Thread.State.TIMED_WAITING);

      if (allWaiting && connection.getBroadcaster().isEmpty() && active.isEmpty()) {
        connection
            .getBroadcaster()
            .broadcastToAll(messageFactory.createDssResultMessage(THREAD_NAME, Result.TRUE));
        return;
      }

      try {
        Thread.sleep(10);
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

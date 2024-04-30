// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

public class FixpointNotifier {

  private final BlockSummaryConnection connection;
  private final int connections;
  private final ConcurrentHashMap<String, String> waiting;
  private static FixpointNotifier instance;

  private FixpointNotifier(BlockSummaryConnection pConnection, int pConnections) {
    connection = pConnection;
    connections = pConnections;
    waiting = new ConcurrentHashMap<>();
  }

  public static void init(BlockSummaryConnection connection, int connections) {
    if (instance != null) {
      throw new IllegalStateException("FixPointNotifier already initialized");
    }
    instance = new FixpointNotifier(connection, connections);
  }

  public static FixpointNotifier getInstance() {
    if (instance == null) {
      throw new IllegalStateException("FixPointNotifier not initialized");
    }
    return instance;
  }

  public void waiting(String id) throws InterruptedException {
    waiting.put(id, id);
    if (waiting.size() == connections) {
      connection.write(BlockSummaryMessage.newResultMessage("root", 0, Result.TRUE));
    }
  }

  public void active(String id) {
    waiting.remove(id);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;

public class DssFixpointNotifier {

  private final DssConnection connection;
  private final int connections;
  private final ConcurrentHashMap<String, String> waiting;
  private static DssFixpointNotifier instance;
  private final DssMessageFactory messageFactory;

  private DssFixpointNotifier(
      DssConnection pConnection,
      int pConnections,
      DssMessageFactory pMessageFactory) {
    messageFactory = pMessageFactory;
    connection = pConnection;
    connections = pConnections;
    waiting = new ConcurrentHashMap<>();
  }

  public static void init(
      DssMessageFactory pMessageFactory,
      DssConnection connection,
      int connections) {
    // checkState(instance == null, "FixPointNotifier already initialized");
    instance = new DssFixpointNotifier(connection, connections, pMessageFactory);
  }

  public static DssFixpointNotifier getInstance() {
    checkState(instance != null, "FixPointNotifier not initialized");
    return instance;
  }

  public void waiting(String id) throws InterruptedException {
    waiting.put(id, id);
    if (waiting.size() == connections) {
      connection.write(messageFactory.newResultMessage("root", 0, Result.TRUE));
    }
  }

  public void active(String id) {
    waiting.remove(id);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class ResultWorker extends Worker {

  private final Set<String> staleIds;

  private final int numberWorkers;

  ResultWorker(
      LogManager pLogger,
      int pNumWorkers) {
    super(pLogger);
    staleIds = new HashSet<>();
    numberWorkers = pNumWorkers;
  }

  @Override
  public Message nextMessage() throws InterruptedException {
    return connection.read();
  }

  @Override
  public Message processMessage(Message pMessage)
      throws InterruptedException, CPAException, IOException, SolverException {
    /*switch (pMessage.getType()) {
      case FOUND_RESULT:
      case ERROR:
        shutdown();
      case EMPTY:
        return Message.noResponse();
      case PRECONDITION:
      case POSTCONDITION:
        staleIds.remove(pMessage.getUniqueBlockId());
        return Message.noResponse();
      case STALE:
        if (Boolean.parseBoolean(pMessage.getPayload())) {
          staleIds.add(nextMessage().getUniqueBlockId());
        }
        finished = staleIds.size() == numberWorkers;
        if (finished) {
          return Message.newResultMessage("collector", 0, Result.TRUE);
        }
        return Message.newStaleMessage("collector", false);
      default:
        throw new AssertionError("MessageType " + pMessage.getType() + " unknown");
    }*/
    if (pMessage.getType() == MessageType.STALE) {
      if (Boolean.parseBoolean(pMessage.getPayload())) {
        staleIds.add(pMessage.getUniqueBlockId());
      } else {
        staleIds.remove(pMessage.getUniqueBlockId());
      }
    }
    finished = staleIds.size() == numberWorkers;
    if (finished) {
      return Message.newResultMessage("main", 0, Result.TRUE);
    }
    return Message.noResponse();
  }

  public void shutdown() throws IOException {
    connection.close();
    Thread.currentThread().interrupt();
  }

  @Override
  public void run() {
    try {
      while (!finished) {
        broadcast(processMessage(nextMessage()));
      }
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.log(Level.SEVERE, pE);
    }
  }
}

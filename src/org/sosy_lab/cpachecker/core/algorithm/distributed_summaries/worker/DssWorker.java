// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class DssWorker implements DssActor {

  private final DssMessageFactory messageFactory;
  private final LogManager logger;
  private final String id;

  private final StatCounter receivedMessages;
  private final StatCounter sentMessages;

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  protected DssWorker(String pId, DssMessageFactory pMessageFactory, LogManager pLogger) {
    id = pId;
    receivedMessages = new StatCounter(pId + " received messages");
    sentMessages = new StatCounter(pId + " sent messages");
    messageFactory = pMessageFactory;
    logger = pLogger;
  }

  @Override
  public void broadcast(Collection<DssMessage> pMessage) throws InterruptedException {
    // pMessage.forEach(m -> logger.log(Level.INFO, m));
    for (DssMessage message : pMessage) {
      sentMessages.inc();
      getConnection().write(message);
    }
  }

  void broadcastOrLogException(Collection<DssMessage> pMessage) {
    try {
      broadcast(pMessage);
    } catch (InterruptedException e) {
      logger.logfException(
          Level.SEVERE, e, "Broadcasting %s messages interrupted unexpectedly.", pMessage);
    }
  }

  @Override
  public void run() {
    final DssConnection connection = getConnection();
    try (connection) {
      while (!shutdownRequested()) {
        broadcast(processMessage(nextMessage()));
        receivedMessages.inc();
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
      }
    } catch (CPAException | InterruptedException | IOException | SolverException e) {
      logger.logfException(
          Level.SEVERE, e, "%s faced a problem while processing messages.", getId());
      broadcastOrLogException(ImmutableList.of(messageFactory.newErrorMessage(getId(), e)));
    } finally {
      logger.logf(Level.INFO, "Worker %s finished and shuts down.", id);
    }
  }

  @Override
  public final String getId() {
    return id;
  }

  int getReceivedMessages() {
    return receivedMessages.getUpdateCount();
  }

  int getSentMessages() {
    return sentMessages.getUpdateCount();
  }
}

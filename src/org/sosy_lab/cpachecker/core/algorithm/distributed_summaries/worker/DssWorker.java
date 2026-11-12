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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class DssWorker implements DssActor {

  private final DssMessageFactory messageFactory;
  protected final LogManager logger;
  private final String id;
  private boolean interrupted;

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  protected DssWorker(String pId, DssMessageFactory pMessageFactory, LogManager pLogger) {
    id = pId;
    messageFactory = pMessageFactory;
    logger = pLogger;
  }

  @Override
  public void broadcast(Collection<DssMessage> pMessage) throws InterruptedException {
    // pMessage.forEach(m -> logger.log(Level.INFO, m));
    for (DssMessage message : pMessage) {
      getConnection().getBroadcaster().broadcastToAll(message);
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

  /**
   * Whether this actor stopped because it was interrupted, e.g. by a shutdown request, rather than
   * because it was done.
   */
  protected final boolean wasInterrupted() {
    return interrupted;
  }

  @Override
  public void run() {
    if (shutdownRequested()) {
      return;
    }
    final DssConnection connection = getConnection();
    try (connection) {
      while (!shutdownRequested()) {
        broadcast(processMessage(nextMessage()));
      }
    } catch (InterruptedException e) {
      // An interrupt is how a shutdown request reaches an actor blocked on its queue.
      interrupted = true;
      logger.logf(Level.WARNING, "%s was interrupted, most likely by a shutdown request.", getId());
      // The other actors are still blocked on their queues and have to be told to stop.
      broadcastOrLogException(
          ImmutableList.of(messageFactory.createDssExceptionMessage(getId(), e)));
    } catch (CPAException | IOException | SolverException e) {
      logger.logfException(
          Level.SEVERE, e, "%s faced a problem while processing messages.", getId());
      broadcastOrLogException(
          ImmutableList.of(messageFactory.createDssExceptionMessage(getId(), e)));
    } finally {
      logger.logf(Level.INFO, "Worker %s finished and shuts down.", id);
    }
  }

  protected DssMessageFactory getMessageFactory() {
    return messageFactory;
  }

  @Override
  public final String getId() {
    return id;
  }
}

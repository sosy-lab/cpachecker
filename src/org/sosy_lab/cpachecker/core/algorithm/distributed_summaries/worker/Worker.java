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
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class Worker implements Runnable, StatisticsProvider {

  protected final LogManager logger;
  protected final String id;
  protected final AnalysisOptions analysisOptions;
  protected final Connection connection;

  protected boolean finished;

  protected static final WorkerStatistics stats = new WorkerStatistics();

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  protected Worker(String pId, Connection pConnection, AnalysisOptions pOptions)
      throws InvalidConfigurationException {
    id = pId;
    analysisOptions = pOptions;
    logger = BasicLogManager.create(pOptions.getParentConfig()).withComponentName(pId);
    connection = pConnection;
  }

  /**
   * Get the next message from the connection. Note that the connection requires a blocking read()
   *
   * @return the current message to be processed
   * @throws InterruptedException thrown if thread is interrupted
   */
  public Message nextMessage() throws InterruptedException {
    return connection.read();
  }

  public abstract Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, IOException, SolverException, CPAException;

  protected void broadcast(Collection<Message> pMessage) throws InterruptedException {
    Objects.requireNonNull(connection, "Connection cannot be null.");
    pMessage.forEach(
        m -> {
          logger.log(Level.ALL, m);
          stats.sentMessages.inc();
        });
    for (Message message : pMessage) {
      connection.write(message);
    }
  }

  protected void broadcastOrLogException(Collection<Message> pMessage) {
    try {
      broadcast(pMessage);
    } catch (InterruptedException pE) {
      logger.logException(Level.SEVERE, pE, "Broadcasting messages interrupted unexpectedly.");
    }
  }

  @Override
  public void run() {
    try (connection) {
      while (!finished) {
        broadcast(processMessage(nextMessage()));
        finished |= Thread.currentThread().isInterrupted();
      }
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.logfException(
          Level.SEVERE, pE, "%s faced a problem while processing messages.", getId());
      broadcastOrLogException(ImmutableList.of(Message.newErrorMessage(getId(), pE)));
    } finally {
      logger.logf(Level.INFO, "Worker %s stopped working.", id);
    }
  }

  public synchronized void shutdown() {
    finished = true;
  }

  public boolean hasPendingMessages() {
    return !connection.isEmpty();
  }

  public final String getId() {
    return id;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

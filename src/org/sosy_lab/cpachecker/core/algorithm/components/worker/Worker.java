// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class Worker implements Runnable, StatisticsProvider {

  protected final LogManager logger;
  protected final String id;
  protected final AnalysisOptions analysisOptions;

  protected Connection connection;
  protected boolean finished;

  protected final static WorkerStatistics stats = new WorkerStatistics();

  /**
   * Abstract definition of a Worker.
   * All workers enter the same routine of receiving and producing messages.
   *
   * @param pId     the id of the worker
   * @param pLogger a logger to log messages
   */
  protected Worker(String pId, LogManager pLogger, AnalysisOptions pOptions) {
    logger = pLogger;
    id = pId;
    analysisOptions = pOptions;
  }

  /**
   * Get the next message from the connection.
   * Note that the connection requires a blocking read()
   *
   * @return the current message to be processed
   * @throws InterruptedException thrown if thread is interrupted
   */
  public Message nextMessage() throws InterruptedException, SolverException {
    return connection.read();
  }

  public abstract Collection<Message> processMessage(Message pMessage)
      throws InterruptedException, IOException,
             SolverException, CPAException;

  public void broadcast(Collection<Message> pMessage) throws IOException, InterruptedException {
    Objects.requireNonNull(connection, "Connection cannot be null.");
    pMessage.forEach(m -> {
      logger.log(Level.ALL, m);
      stats.sentMessages.inc();
    });
    for (Message message : pMessage) {
      connection.write(message);
    }
  }

  @Override
  public void run() {
    try {
      while (!finished) {
        broadcast(processMessage(nextMessage()));
        finished |= Thread.currentThread().isInterrupted();
      }
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      try {
        // result unknown if error occurs, terminate other workers.
        broadcast(ImmutableList.of(Message.newErrorMessage(getId(), pE)));
      } catch (IOException | InterruptedException pEx) {
        // in case broadcast fails, throw unchecked exception.
      }
      // error messages dont print stack trace... this worker terminates
      throw new AssertionError(pE);
    }
  }

  public synchronized void shutdown() throws IOException {
    finished = true;
    connection.close();
  }

  public boolean hasPendingMessages() {
    return !connection.isEmpty();
  }

  public final String getId() {
    return id;
  }

  final void setConnection(Connection pConnection) {
    connection = pConnection;
    connection.setOrdering(MessageType.FOUND_RESULT, MessageType.ERROR, MessageType.ERROR_CONDITION,
        MessageType.ERROR_CONDITION_UNREACHABLE, MessageType.BLOCK_POSTCONDITION);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }

}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class Worker implements Runnable {

  protected final LogManager logger;
  protected Connection connection;

  protected boolean finished;

  protected static final Optional<Message> noResponse = Optional.empty();

  protected Worker(LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * Get the next message from the connection.
   * Note that the connection must have a blocking read()
   * @return the current message to be processed
   * @throws InterruptedException thrown if thread is interrupted
   */
  public Message nextMessage() throws InterruptedException {
    return connection.read();
  }

  public abstract Collection<Message> processMessage(Message pMessage) throws InterruptedException, IOException,
                                                                            SolverException, CPAException;

  public void broadcast(Collection<Message> pMessage) throws IOException, InterruptedException {
    Objects.requireNonNull(connection, "Connection cannot be null.");
    for (Message message : pMessage) {
      connection.write(message);
    }
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

  public void shutdown() throws IOException {
    finished = true;
    connection.close();
    Thread.currentThread().interrupt();
  }

  final void setConnection(Connection pConnection) {
    connection = pConnection;
  }

}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.util.Objects;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class Worker implements Runnable {

  protected final LogManager logger;
  protected Connection connection;

  protected boolean finished;

  protected Worker(LogManager pLogger) {
    logger = pLogger;
  }

  public abstract Message nextMessage() throws InterruptedException;

  public abstract Message processMessage(Message pMessage) throws InterruptedException, IOException,
                                                                  SolverException, CPAException;

  public void broadcast(Message pMessage) throws IOException, InterruptedException {
    Objects.requireNonNull(connection, "Connection cannot be null.");
    connection.write(pMessage);
  }

  @Override
  public abstract void run();

  public void shutdown() throws IOException {
    finished = true;
    connection.close();
    Thread.currentThread().interrupt();
  }

  void setConnection(Connection pConnection) {
    connection = pConnection;
  }
}

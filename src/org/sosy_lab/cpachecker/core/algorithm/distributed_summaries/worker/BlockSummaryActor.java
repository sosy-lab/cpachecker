// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.io.IOException;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public interface BlockSummaryActor extends Runnable {

  /**
   * Answer a received message by a potentially empty set of responses.
   *
   * @param pMessage The message to answer
   * @return Potentially empty set of responses
   * @throws InterruptedException thrown if program is interrupted from the outside
   * @throws IOException thrown if loggers cannot write to the logfile
   * @throws SolverException thrown if SMT based calculations face problems
   * @throws CPAException thrown it the analysis should end with crashing
   */
  Collection<ActorMessage> processMessage(ActorMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException;

  void broadcast(Collection<ActorMessage> pMessages) throws InterruptedException;

  Connection getConnection();

  boolean shutdownRequested();

  String getId();

  default ActorMessage nextMessage() throws InterruptedException {
    return getConnection().read();
  }
}

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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A {@link BlockSummaryActor} is connected to a network of {@link BlockSummaryActor}s. All
 * participating {@link BlockSummaryActor}s exchange messages. Every message is broadcast to every
 * other actor. Every actor decides on its own how to handle the message.
 */
public interface BlockSummaryActor extends Runnable {

  /**
   * Answer a received message by a potentially empty set of responses.
   *
   * @param pMessage The message to answer.
   * @return Potentially empty set of responses.
   * @throws InterruptedException thrown if program is interrupted from the outside.
   * @throws IOException thrown if loggers cannot write to the logfile.
   * @throws SolverException thrown if SMT based calculations face problems.
   * @throws CPAException thrown it the analysis should end with crashing.
   */
  Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException;

  /**
   * Broadcast a collection of {@link BlockSummaryMessage}s to all other actors that implement this
   * interface.
   *
   * @param pMessages A collection of messages that all actors should receive
   * @throws InterruptedException thrown if system is interrupted unexpectedly
   */
  void broadcast(Collection<BlockSummaryMessage> pMessages) throws InterruptedException;

  /**
   * Returns a connection to all other available actors.
   *
   * @return Connection to all other available actors.
   * @see BlockSummaryConnection
   */
  BlockSummaryConnection getConnection();

  /**
   * Returns false as long as this worker should run. Once it returns true it should never switch
   * back to returning false.
   *
   * @return true iff actor should stop working and is never needed again, false otherwise.
   * @see BlockSummaryWorker
   */
  boolean shutdownRequested();

  /**
   * Get an unique ID of this actor.
   *
   * @return Unique ID of this worker.
   */
  String getId();

  /**
   * Get the next received message.
   *
   * @return Next {@link BlockSummaryMessage} to process with {@link
   *     BlockSummaryActor#processMessage(BlockSummaryMessage)}
   * @throws InterruptedException thrown if system is interrupted unexpectedly
   */
  default BlockSummaryMessage nextMessage() throws InterruptedException {
    return getConnection().read();
  }
}

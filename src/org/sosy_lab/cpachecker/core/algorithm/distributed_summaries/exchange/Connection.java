// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import java.io.Closeable;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public interface Connection extends Closeable, StatisticsProvider {

  /**
   * Wait for an incoming {@link ActorMessage} and return it.
   *
   * @return current {@link ActorMessage} to process
   * @throws InterruptedException thrown if thread is interrupted.
   */
  ActorMessage read() throws InterruptedException;

  /**
   * Indicates if pending messages exist. This method should only relay on the number of messages
   * that have already been parsed to {@link ActorMessage}s. Since the method is probably used
   * frequently, the calculation should be cheap.
   *
   * @return true, if no pending messages exist, false otherwise
   */
  boolean hasPendingMessages();

  /**
   * Write and broadcast a message to all connections including itself.
   *
   * @param message Message to broadcast
   */
  void write(ActorMessage message) throws InterruptedException;

  @Override
  default void collectStatistics(Collection<Statistics> statsCollection) {}
}

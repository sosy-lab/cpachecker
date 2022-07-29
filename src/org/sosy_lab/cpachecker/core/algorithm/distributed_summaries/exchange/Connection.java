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
   * Returns the size of pending messages. Calculating the number of pending messages should never
   * be expensive. This method only guarantees to count the messages that have already been parsed
   * to {@link ActorMessage}s.
   *
   * @return size of pending messages
   */
  int size();

  /**
   * Indicates if pending messages exist. This method only guarantees to count the messages that
   * have already been parsed to {@link ActorMessage}s.
   *
   * @return true, if no pending messages exist, false otherwise
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Write and broadcast a message to all connections including itself.
   *
   * @param message Message to broadcast
   */
  void write(ActorMessage message) throws InterruptedException;

  @Override
  default void collectStatistics(Collection<Statistics> statsCollection) {}
}

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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public interface Connection extends Closeable, StatisticsProvider {

  /**
   * Wait for an incoming Message and return it.
   *
   * @return current Message to process
   * @throws InterruptedException if thread is interrupted.
   */
  ActorMessage read() throws InterruptedException;

  /**
   * Returns the size of pending messages
   *
   * @return size of pending messages
   */
  int size();

  /**
   * Indicates if pending messages exist
   *
   * @return true, if no pending messages exist, false otherwise
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Write and broadcast a message to all connections including itself
   *
   * @param message Message to broadcast
   */
  void write(ActorMessage message) throws InterruptedException;

  @Override
  default void collectStatistics(Collection<Statistics> statsCollection) {}
}

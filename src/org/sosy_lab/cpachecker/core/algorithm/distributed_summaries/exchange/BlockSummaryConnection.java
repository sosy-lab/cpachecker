// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import java.io.Closeable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

public interface BlockSummaryConnection extends Closeable {

  /**
   * Wait for an incoming {@link BlockSummaryMessage} and return it.
   *
   * @return current {@link BlockSummaryMessage} to process
   * @throws InterruptedException thrown if thread is interrupted.
   */
  BlockSummaryMessage read() throws InterruptedException;

  /**
   * Indicates if pending messages exist. A pending message is a message that has already been fully
   * read by the Connection, and waits on the {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor} for
   * processing. Since the method is probably used frequently, the calculation should be cheap
   * (preferably in O(1)).
   *
   * @return true, if no pending messages exist, false otherwise
   */
  boolean hasPendingMessages();

  /**
   * Write and broadcast a message to all connections including itself.
   *
   * @param message Message to broadcast
   */
  void write(BlockSummaryMessage message) throws InterruptedException;
}

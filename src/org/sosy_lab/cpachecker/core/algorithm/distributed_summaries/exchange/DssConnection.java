// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import java.io.Closeable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;

public interface DssConnection extends Closeable {

  /**
   * Wait for an incoming {@link DssMessage} and return it.
   *
   * @return current {@link DssMessage} to process
   * @throws InterruptedException thrown if thread is interrupted.
   */
  DssMessage read() throws InterruptedException;

  /**
   * Indicates if pending messages exist. A pending message is a message that has already been fully
   * read by the Connection, and waits on the {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor} for processing.
   * Since the method is probably used frequently, the calculation should be cheap (preferably in
   * O(1)).
   *
   * @return whether pending messages exist
   */
  boolean hasPendingMessages();

  /**
   * Write and broadcast a message to all connections including itself.
   *
   * @param message Message to broadcast
   */
  void write(DssMessage message) throws InterruptedException;
}

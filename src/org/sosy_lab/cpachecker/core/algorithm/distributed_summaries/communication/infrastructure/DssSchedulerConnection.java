// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

public class DssSchedulerConnection implements DssConnection {

  private final BlockingQueue<DssMessage> incomingQueue;
  private final DssMessageBroadcaster broadcaster;
  private boolean closed;

  /**
   * Creates a new connection for the scheduler.
   *
   * @param pIncomingQueue the incoming queue for messages to the scheduler
   * @param pBroadcaster the broadcaster to use for sending messages
   */
  public DssSchedulerConnection(
      BlockingQueue<DssMessage> pIncomingQueue, DssMessageBroadcaster pBroadcaster) {
    incomingQueue = pIncomingQueue;
    broadcaster = pBroadcaster;
  }

  @Override
  public DssMessage read() throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot read from an already closed " + DssSchedulerConnection.class);
    }
    return incomingQueue.take();
  }

  @Override
  public boolean hasPendingMessages() {
    return !incomingQueue.isEmpty();
  }

  @Override
  public DssMessageBroadcaster getBroadcaster() {
    return broadcaster;
  }

  @Override
  public void close() throws IOException {
    incomingQueue.clear();
    closed = true;
  }
}

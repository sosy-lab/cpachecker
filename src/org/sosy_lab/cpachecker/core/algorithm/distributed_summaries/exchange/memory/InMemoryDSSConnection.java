// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;

/**
 * The {@link InMemoryDSSConnection} provides a queue for incoming messages and knows about all
 * outgoing connections (that are the queues for incoming messages of other workers).
 */
public class InMemoryDSSConnection implements DSSConnection {

  private final BlockingQueue<DSSMessage> in;
  private final ConcurrentLinkedQueue<BlockingQueue<DSSMessage>> out;
  private boolean closed;

  InMemoryDSSConnection(BlockingQueue<DSSMessage> pIn, List<BlockingQueue<DSSMessage>> pOut) {
    in = pIn;
    out = new ConcurrentLinkedQueue<>(pOut);
    closed = false;
  }

  @Override
  public DSSMessage read() throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot read from an already closed " + InMemoryDSSConnection.class);
    }
    return in.take();
  }

  @Override
  public boolean hasPendingMessages() {
    return !in.isEmpty();
  }

  @Override
  public void write(DSSMessage message) throws InterruptedException {
    if (closed) {
      return;
    }
    for (BlockingQueue<DSSMessage> messages : out) {
      messages.add(message);
    }
  }

  @Override
  public void close() throws IOException {
    in.clear();
    out.clear();
    closed = true;
  }
}

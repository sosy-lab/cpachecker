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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

/**
 * The {@link InMemoryBlockSummaryConnection} provides a queue for incoming messages and knows about
 * all outgoing connections (that are the queues for incoming messages of other workers).
 */
public class InMemoryBlockSummaryConnection implements BlockSummaryConnection {

  private final BlockingQueue<BlockSummaryMessage> in;
  private final ConcurrentLinkedQueue<BlockingQueue<BlockSummaryMessage>> out;
  private boolean closed;

  InMemoryBlockSummaryConnection(
      BlockingQueue<BlockSummaryMessage> pIn, List<BlockingQueue<BlockSummaryMessage>> pOut) {
    in = pIn;
    out = new ConcurrentLinkedQueue<>(pOut);
    closed = false;
  }

  @Override
  public BlockSummaryMessage read() throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot read from an already closed " + InMemoryBlockSummaryConnection.class);
    }
    return in.take();
  }

  @Override
  public boolean hasPendingMessages() {
    return !in.isEmpty();
  }

  @Override
  public void write(BlockSummaryMessage message) throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot write to an already closed " + InMemoryBlockSummaryConnection.class);
    }
    for (BlockingQueue<BlockSummaryMessage> messages : out) {
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

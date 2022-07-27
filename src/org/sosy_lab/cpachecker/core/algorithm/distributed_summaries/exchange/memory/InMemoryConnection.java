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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

/**
 * The {@link InMemoryConnection} provides a queue for incoming messages and knows about all
 * outgoing connections (that are the queues for incoming messages of other workers).
 */
public class InMemoryConnection implements Connection, StatisticsProvider {

  private final BlockingQueue<ActorMessage> in;
  private final ConcurrentLinkedQueue<BlockingQueue<ActorMessage>> out;
  private boolean closed;

  InMemoryConnection(BlockingQueue<ActorMessage> pIn, List<BlockingQueue<ActorMessage>> pOut) {
    in = pIn;
    out = new ConcurrentLinkedQueue<>(pOut);
    closed = false;
  }

  @Override
  public ActorMessage read() throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot read from an already closed " + InMemoryConnection.class);
    }
    return in.take();
  }

  @Override
  public int size() {
    return in.size();
  }

  @Override
  public boolean isEmpty() {
    return in.isEmpty();
  }

  @Override
  public void write(ActorMessage message) throws InterruptedException {
    if (closed) {
      throw new IllegalStateException(
          "Cannot write to an already closed " + InMemoryConnection.class);
    }
    for (BlockingQueue<ActorMessage> messages : out) {
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

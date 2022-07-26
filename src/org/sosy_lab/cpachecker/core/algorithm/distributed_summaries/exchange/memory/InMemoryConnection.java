// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ConnectionStats;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class InMemoryConnection implements Connection, StatisticsProvider {

  private final BlockingQueue<ActorMessage> in;
  private final List<BlockingQueue<ActorMessage>> out;
  private boolean closed;

  private static final ConnectionStats stats = new ConnectionStats();

  InMemoryConnection(BlockingQueue<ActorMessage> pIn, List<BlockingQueue<ActorMessage>> pOut) {
    in = pIn;
    out = pOut;
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
    stats.averageMessageSize.setNextValue(
        (message.getUniqueBlockId()
                + message.getType()
                + message.getPayload()
                + message.getTargetNodeNumber())
            .length());
    for (BlockingQueue<ActorMessage> messages : out) {
      messages.put(message);
    }
  }

  @Override
  public void close() throws IOException {
    in.clear();
    out.clear();
    closed = true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

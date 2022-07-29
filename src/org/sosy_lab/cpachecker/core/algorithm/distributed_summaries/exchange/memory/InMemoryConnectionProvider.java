// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;

public class InMemoryConnectionProvider implements ConnectionProvider<InMemoryConnection> {

  private final Supplier<BlockingQueue<ActorMessage>> queueFactory;

  /**
   * Create a new {@link InMemoryConnectionProvider}. The given supplier is used as factory for
   * {@link BlockingQueue}s. It has to supply a <b>new</b> {@link BlockingQueue} object on each
   * invocation, and must allow an arbitrary number of invocations.
   */
  public InMemoryConnectionProvider(Supplier<BlockingQueue<ActorMessage>> pQueueFactory) {
    queueFactory = pQueueFactory;
  }

  @Override
  public ImmutableList<InMemoryConnection> createConnections(int connections) throws IOException {
    List<BlockingQueue<ActorMessage>> outs = new ArrayList<>();
    for (int i = 0; i < connections; i++) {
      outs.add(queueFactory.get());
    }
    return transformedImmutableListCopy(outs, out -> new InMemoryConnection(out, outs));
  }
}

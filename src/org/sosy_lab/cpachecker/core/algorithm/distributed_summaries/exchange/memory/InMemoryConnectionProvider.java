// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.memory;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.CleverMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ConnectionProvider;

public class InMemoryConnectionProvider implements ConnectionProvider<InMemoryConnection> {

  private final Supplier<BlockingQueue<ActorMessage>> queueFactory;

  /**
   * Create a new InMemoryConnectionProvider. The given supplier is used as factory for
   * BlockingQueue. It has to supply a <b>new</b> BlockingQueue object on each invocation, and must
   * allow an arbitray number of invocations. (probably better to create a separate factory
   * interface for this)
   */
  public InMemoryConnectionProvider(Supplier<BlockingQueue<ActorMessage>> pQueueFactory) {
    queueFactory = pQueueFactory;
  }

  @Override
  public List<InMemoryConnection> createConnections(int connections) throws IOException {
    List<BlockingQueue<ActorMessage>> outs = new ArrayList<>();
    for (int i = 0; i < connections; i++) {
      outs.add(new CleverMessageQueue());
    }
    return FluentIterable.from(outs).transform(out -> new InMemoryConnection(out, outs)).toList();
  }
}

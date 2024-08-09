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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;

public class InMemoryDSSConnectionProvider implements DSSConnectionProvider<InMemoryDSSConnection> {

  private final Supplier<BlockingQueue<DSSMessage>> queueFactory;

  /**
   * Create a new {@link InMemoryDSSConnectionProvider}. The given supplier is used as factory for
   * {@link BlockingQueue}s. It has to supply a <b>new</b> {@link BlockingQueue} object on each
   * invocation, and must allow an arbitrary number of invocations.
   */
  public InMemoryDSSConnectionProvider(Supplier<BlockingQueue<DSSMessage>> pQueueFactory) {
    queueFactory = pQueueFactory;
  }

  @Override
  public ImmutableList<InMemoryDSSConnection> createConnections(int connections)
      throws IOException {
    List<BlockingQueue<DSSMessage>> outs = new ArrayList<>();
    for (int i = 0; i < connections; i++) {
      outs.add(queueFactory.get());
    }
    return transformedImmutableListCopy(outs, out -> new InMemoryDSSConnection(out, outs));
  }
}

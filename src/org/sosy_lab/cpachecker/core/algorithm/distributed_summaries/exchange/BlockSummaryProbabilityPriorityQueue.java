// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.ArrayList;import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;

public class BlockSummaryProbabilityPriorityQueue extends ForwardingBlockingQueue<BlockSummaryMessage> {

  private final BlockingQueue<BlockSummaryMessage> queue;
  private final List<BlockSummaryMessage> reordered;
  private final ImmutableMap<MessageType, Double> probability;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private BlockSummaryProbabilityPriorityQueue(BlockingQueue<BlockSummaryMessage> pQueue) {
    queue = pQueue;
    reordered = new ArrayList<>();
    probability = ImmutableMap.<MessageType, Double>builder()
        .put(MessageType.STATISTICS, 1d)
        .put(MessageType.ERROR, 1d)
        .put(MessageType.FOUND_RESULT, 1d)
        .put(MessageType.ERROR_CONDITION_UNREACHABLE, 1d)
        .put(MessageType.ERROR_CONDITION, .7)
        .put(MessageType.BLOCK_POSTCONDITION, .5)
        .put(MessageType.ABSTRACTION_STATE, .1)
        .build();
  }

  public BlockSummaryProbabilityPriorityQueue() {
    this(new LinkedBlockingQueue<>());
  }

  @Override
  protected BlockingQueue<BlockSummaryMessage> delegate() {
    return queue;
  }

  /**
   * Messages are returned according to the defined ordering.
   *
   * @return Next message to process
   * @throws InterruptedException thrown if the process is interrupted
   */
  @Override
  public BlockSummaryMessage take() throws InterruptedException {
    // empty pending messages (non blocking)
    // return queue.take();
    while (!queue.isEmpty()) {
      double random = Math.random();
      BlockSummaryMessage message = queue.take();
      if (random < probability.get(message.getType())) {
        reordered.add(0, message);
      } else {
        reordered.add(message);
      }
    }
    if(!reordered.isEmpty()) {
      return reordered.remove(0);
    }
    return queue.take();
  }
}

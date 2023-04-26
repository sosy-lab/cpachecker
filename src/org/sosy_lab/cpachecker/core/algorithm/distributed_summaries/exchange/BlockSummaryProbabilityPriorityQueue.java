// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;

public class BlockSummaryProbabilityPriorityQueue
    extends ForwardingBlockingQueue<BlockSummaryMessage> {

  private final BlockingQueue<BlockSummaryMessage> queue;
  private final List<BlockSummaryMessage> reordered;
  private final ImmutableMap<MessageType, ReorderStrategy> reordering;

  @FunctionalInterface
  interface ReorderStrategy {
    boolean reorder();
  }

  private static class CountReorderStrategy implements ReorderStrategy {

    private int count;
    private final int max;
    private final int yes;

    public CountReorderStrategy(int pNo, int pYes) {
      Preconditions.checkArgument(pNo >= 0);
      Preconditions.checkArgument(pYes >= 0);
      Preconditions.checkArgument(pYes > 0 || pNo > 0);
      max = pYes + pNo;
      yes = pYes;
    }

    @Override
    public boolean reorder() {
      boolean reorder = count < yes;
      count = (count + 1) % max;
      return reorder;
    }
  }

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private BlockSummaryProbabilityPriorityQueue(BlockingQueue<BlockSummaryMessage> pQueue) {
    queue = pQueue;
    reordered = new ArrayList<>();
    // necessary to avoid endless loops of ErrorCondition messages for loop structures.
    // the number is the probability within [0;1) that the messages will be processed next
    reordering =
        ImmutableMap.<MessageType, ReorderStrategy>builder()
            .put(MessageType.STATISTICS, () -> true)
            .put(MessageType.ERROR, () -> true)
            .put(MessageType.FOUND_RESULT, () -> true)
            .put(MessageType.ERROR_CONDITION_UNREACHABLE, () -> true)
            .put(MessageType.ERROR_CONDITION, new CountReorderStrategy(1, 3))
            .put(MessageType.BLOCK_POSTCONDITION, new CountReorderStrategy(1, 1))
            .buildKeepingLast();
    if (reordering.size() != MessageType.values().length) {
      throw new AssertionError(
          "Forgot to add a missing message type to reorder map"
              + Sets.difference(
                  ImmutableSet.copyOf(MessageType.values()),
                  reordering.keySet()));
    }
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
    while (!queue.isEmpty()) {
      BlockSummaryMessage message = queue.take();
      if (Objects.requireNonNull(reordering.getOrDefault(message.getType(), () -> true))
          .reorder()) {
        reordered.add(0, message);
      } else {
        reordered.add(message);
      }
    }
    if (!reordered.isEmpty()) {
      return reordered.remove(0);
    }
    return queue.take();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;

public class BlockSummarySortedMessageQueue extends ForwardingBlockingQueue<BlockSummaryMessage> {

  private final BlockingQueue<BlockSummaryMessage> queue;
  private final Multimap<MessageType, BlockSummaryMessage> messages;

  private final MessageType[] ordering;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private BlockSummarySortedMessageQueue(BlockingQueue<BlockSummaryMessage> pQueue) {
    queue = pQueue;
    messages = ArrayListMultimap.create();
    ordering =
        new MessageType[] {
          MessageType.FOUND_RESULT,
          MessageType.ERROR,
          MessageType.ERROR_CONDITION,
          MessageType.ERROR_CONDITION_UNREACHABLE,
          MessageType.BLOCK_POSTCONDITION
        };
  }

  public BlockSummarySortedMessageQueue() {
    this(new LinkedBlockingQueue<>());
  }

  @Override
  protected BlockingQueue<BlockSummaryMessage> delegate() {
    return queue;
  }

  public void setOrdering(MessageType... pOrdering) {
    assert pOrdering.length == ordering.length : "please provide all types for the ordering";
    System.arraycopy(pOrdering, 0, ordering, 0, pOrdering.length);
  }

  private void moveToMap(BlockSummaryMessage pMessage) {
    messages.put(pMessage.getType(), pMessage);
  }

  private Optional<BlockSummaryMessage> firstOfType(MessageType pType) {
    if (!messages.get(pType).isEmpty()) {
      Optional<BlockSummaryMessage> optionalMessage = messages.get(pType).stream().findFirst();
      messages.remove(pType, optionalMessage.orElseThrow());
      return optionalMessage;
    }
    return Optional.empty();
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
      moveToMap(queue.take());
    }
    for (MessageType messageType : ordering) {
      Optional<BlockSummaryMessage> m = firstOfType(messageType);
      if (m.isPresent()) {
        return m.orElseThrow();
      }
    }
    return queue.take();
  }
}

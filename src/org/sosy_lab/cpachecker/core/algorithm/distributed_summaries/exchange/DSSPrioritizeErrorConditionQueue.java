// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage.MessageType;

public class DSSPrioritizeErrorConditionQueue extends ForwardingBlockingQueue<DSSMessage> {

  private final BlockingQueue<DSSMessage> queue;
  private static final int TAKE_POSTCONDITION = 4;
  private int current = 0;

  private final Deque<DSSMessage> highestPriority;
  private final Map<MessageType, ArrayDeque<DSSMessage>> next;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private DSSPrioritizeErrorConditionQueue(BlockingQueue<DSSMessage> pQueue) {
    queue = pQueue;
    highestPriority = new ArrayDeque<>();
    next = new LinkedHashMap<>();
    next.put(MessageType.ERROR_CONDITION, new ArrayDeque<>());
    next.put(MessageType.BLOCK_POSTCONDITION, new ArrayDeque<>());
  }

  public DSSPrioritizeErrorConditionQueue() {
    this(new LinkedBlockingQueue<>());
  }

  @Override
  protected BlockingQueue<DSSMessage> delegate() {
    return queue;
  }

  /**
   * Messages are returned according to the defined ordering.
   *
   * @return Next message to process
   * @throws InterruptedException thrown if the process is interrupted
   */
  @Override
  public DSSMessage take() throws InterruptedException {
    // empty pending messages (non blocking)
    while (!queue.isEmpty()) {
      DSSMessage message = queue.take();
      switch (message.getType()) {
        case STATISTICS, FOUND_RESULT, ERROR, ERROR_CONDITION_UNREACHABLE ->
            highestPriority.add(message);
        case ERROR_CONDITION, BLOCK_POSTCONDITION -> next.get(message.getType()).add(message);
      }
    }
    if (!highestPriority.isEmpty()) {
      return highestPriority.removeFirst();
    }
    Deque<DSSMessage> errorConditions = next.get(MessageType.ERROR_CONDITION);
    Deque<DSSMessage> postConditions = next.get(MessageType.BLOCK_POSTCONDITION);
    if (!errorConditions.isEmpty()) {
      if (current >= TAKE_POSTCONDITION && !postConditions.isEmpty()) {
        current = 0;
        return postConditions.removeFirst();
      } else {
        current++;
        return errorConditions.removeFirst();
      }
    } else if (!postConditions.isEmpty()) {
      return postConditions.removeFirst();
    }
    return queue.take();
  }
}

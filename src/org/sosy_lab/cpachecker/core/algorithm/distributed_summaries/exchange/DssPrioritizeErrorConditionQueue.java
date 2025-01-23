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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageType;

public class DssPrioritizeErrorConditionQueue
    extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private static final int TAKE_POSTCONDITION = 4;
  private int current = 0;

  private final Deque<DssMessage> highestPriority;
  private final Map<MessageType, ArrayDeque<DssMessage>> next;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private DssPrioritizeErrorConditionQueue(BlockingQueue<DssMessage> pQueue) {
    queue = pQueue;
    highestPriority = new ArrayDeque<>();
    next = new LinkedHashMap<>();
    next.put(MessageType.ERROR_CONDITION, new ArrayDeque<>());
    next.put(MessageType.BLOCK_POSTCONDITION, new ArrayDeque<>());
  }

  public DssPrioritizeErrorConditionQueue() {
    this(new LinkedBlockingQueue<>());
  }

  @Override
  protected BlockingQueue<DssMessage> delegate() {
    return queue;
  }

  /**
   * Messages are returned according to the defined ordering.
   *
   * @return Next message to process
   * @throws InterruptedException thrown if the process is interrupted
   */
  @Override
  public DssMessage take() throws InterruptedException {
    // empty pending messages (non blocking)
    while (!queue.isEmpty()) {
      DssMessage message = queue.take();
      switch (message.getType()) {
        case STATISTICS, FOUND_RESULT, ERROR -> highestPriority.add(message);
        case ERROR_CONDITION, BLOCK_POSTCONDITION -> next.get(message.getType()).add(message);
      }
    }
    if (!highestPriority.isEmpty()) {
      return highestPriority.removeFirst();
    }
    Deque<DssMessage> errorConditions = next.get(MessageType.ERROR_CONDITION);
    Deque<DssMessage> postConditions = next.get(MessageType.BLOCK_POSTCONDITION);
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

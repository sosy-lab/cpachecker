// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;

public class DssPrioritizeViolationConditionQueue extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private static final int TAKE_POSTCONDITION = 4;
  private int current = 0;

  private final Deque<DssMessage> highestPriority;
  private final Map<DssMessageType, ArrayDeque<DssMessage>> next;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private DssPrioritizeViolationConditionQueue(BlockingQueue<DssMessage> pQueue) {
    queue = pQueue;
    highestPriority = new ArrayDeque<>();
    next = new LinkedHashMap<>();
    next.put(DssMessageType.VIOLATION_CONDITION, new ArrayDeque<>());
    next.put(DssMessageType.PRECONDITION, new ArrayDeque<>());
  }

  public DssPrioritizeViolationConditionQueue() {
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
      Deque<DssMessage> queueForMessage =
          switch (message.getType()) {
            case STATISTIC, RESULT, EXCEPTION -> highestPriority;
            case VIOLATION_CONDITION, PRECONDITION -> next.get(message.getType());
          };
      queueForMessage.add(message);
    }
    if (!highestPriority.isEmpty()) {
      return highestPriority.removeFirst();
    }
    Deque<DssMessage> ViolationConditions = next.get(DssMessageType.VIOLATION_CONDITION);
    Deque<DssMessage> postConditions = next.get(DssMessageType.PRECONDITION);
    if (!ViolationConditions.isEmpty()) {
      if (current >= TAKE_POSTCONDITION && !postConditions.isEmpty()) {
        current = 0;
        return postConditions.removeFirst();
      } else {
        current++;
        return ViolationConditions.removeFirst();
      }
    } else if (!postConditions.isEmpty()) {
      return postConditions.removeFirst();
    }
    return queue.take();
  }
}

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssFixpointNotifier;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageType;

public class DssDefaultQueue extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private final Deque<DssMessage> highestPriority;
  private final Deque<DssMessage> next;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link MessageType}.
   *
   * @param pQueue the queue to forward
   */
  private DssDefaultQueue(BlockingQueue<DssMessage> pQueue) {
    queue = pQueue;
    highestPriority = new ArrayDeque<>();
    next = new ArrayDeque<>();
  }

  public DssDefaultQueue() {
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
        case VIOLATION_CONDITION, BLOCK_POSTCONDITION -> next.add(message);
      }
    }
    if (!highestPriority.isEmpty()) {
      return highestPriority.removeFirst();
    }
    if (!next.isEmpty()) {
      return next.removeFirst();
    }
    DssFixpointNotifier.getInstance().waiting(Thread.currentThread().getName());
    try {
      return queue.take();
    } finally {
      DssFixpointNotifier.getInstance().active(Thread.currentThread().getName());
    }
  }
}

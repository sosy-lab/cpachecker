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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssThreadMonitor;

public class DssDefaultQueue extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private final Deque<DssMessage> highestPriority;
  private final Deque<DssMessage> next;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType}
   */
  public DssDefaultQueue() {
    queue = new LinkedBlockingQueue<>();
    highestPriority = new ArrayDeque<>();
    next = new ArrayDeque<>();
  }

  @Override
  protected BlockingQueue<DssMessage> delegate() {
    return queue;
  }

  @Override
  public boolean isEmpty() {
    return next.isEmpty() && highestPriority.isEmpty() && queue.isEmpty();
  }

  @Override
  public boolean add(DssMessage pMessage) {
    return queue.add(pMessage);
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
            case VIOLATION_CONDITION, POST_CONDITION -> next;
          };
      queueForMessage.add(message);
    }
    if (!highestPriority.isEmpty()) {
      return highestPriority.removeFirst();
    }
    if (!next.isEmpty()) {
      return next.removeFirst();
    }
    DssThreadMonitor.remove(Thread.currentThread().getName());
    try {
      return queue.take();
    } finally {
      DssThreadMonitor.add(Thread.currentThread().getName());
    }
  }
}

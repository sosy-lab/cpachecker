// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

public class DssDefaultQueue extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private final Deque<DssMessage> highestPriority;
  private final Deque<DssMessage> next;
  private final DssWorkCounter workCounter;
  private final AtomicInteger pendingMessages;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType}
   */
  public DssDefaultQueue() {
    this(new DssWorkCounter());
  }

  /**
   * Creates a queue that reports its messages and the activity of its worker to {@code
   * pWorkCounter}.
   *
   * <p>The worker counts as busy from the creation of the queue on. A worker can do useful work
   * before it first calls {@link #take()}, and the counter needs to see that too. Every queue that
   * shares the counter therefore needs a worker that eventually takes from it.
   */
  public DssDefaultQueue(DssWorkCounter pWorkCounter) {
    queue = new LinkedBlockingQueue<>();
    highestPriority = new ArrayDeque<>();
    next = new ArrayDeque<>();
    workCounter = Objects.requireNonNull(pWorkCounter);
    pendingMessages = new AtomicInteger();
    workCounter.workAdded();
  }

  @Override
  protected BlockingQueue<DssMessage> delegate() {
    return queue;
  }

  @Override
  public boolean isEmpty() {
    return pendingMessages.get() == 0;
  }

  @Override
  public boolean add(DssMessage pMessage) {
    // Count the message before the receiver can take it, so that it never counts as done before
    // it was counted as added.
    pendingMessages.incrementAndGet();
    workCounter.workAdded();
    boolean added = false;
    try {
      added = queue.add(pMessage);
      return added;
    } finally {
      if (!added) {
        pendingMessages.decrementAndGet();
        workCounter.workDone();
      }
    }
  }

  private DssMessage startProcessing(DssMessage pMessage) {
    int remainingMessages = pendingMessages.decrementAndGet();
    checkState(remainingMessages >= 0, "Consumed a message that was not registered as pending");
    // The worker is busy while it processes the message, so the message itself is done.
    workCounter.workDone();
    return pMessage;
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
            case WITNESS, RESULT, EXCEPTION -> highestPriority;
            case VIOLATION_CONDITION, POST_CONDITION -> next;
          };
      queueForMessage.add(message);
    }
    if (!highestPriority.isEmpty()) {
      return startProcessing(highestPriority.removeFirst());
    }
    if (!next.isEmpty()) {
      return startProcessing(next.removeFirst());
    }
    // The worker has nothing left to do and becomes idle until the next message arrives.
    workCounter.workDone();
    try {
      DssMessage message = queue.take();
      // Mark the worker busy before the message stops counting. This keeps the work visible to the
      // counter while it moves from the queue to the worker.
      workCounter.workAdded();
      return startProcessing(message);
    } catch (InterruptedException e) {
      // The worker still has to react to the interrupt.
      workCounter.workAdded();
      throw e;
    }
  }
}

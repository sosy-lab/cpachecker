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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

public class DssDefaultQueue extends ForwardingBlockingQueue<DssMessage> {

  private final BlockingQueue<DssMessage> queue;
  private final Deque<DssMessage> highestPriority;
  private final Deque<DssMessage> next;
  private final Set<String> activeWorkers;
  private final AtomicInteger pendingMessages;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize
   * messages according to the enum {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType}
   */
  public DssDefaultQueue() {
    this(ConcurrentHashMap.newKeySet());
  }

  /**
   * Creates a queue that shares its worker's activity with the termination monitor.
   *
   * <p>Add the worker to {@code pActiveWorkers} before starting its thread. A worker can do useful
   * work before it first calls {@link #take()}, and the monitor needs to see that too.
   */
  public DssDefaultQueue(Set<String> pActiveWorkers) {
    queue = new LinkedBlockingQueue<>();
    highestPriority = new ArrayDeque<>();
    next = new ArrayDeque<>();
    activeWorkers = Objects.requireNonNull(pActiveWorkers);
    pendingMessages = new AtomicInteger();
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
    // Count the message first. Otherwise, the monitor could look between these two steps and see
    // neither an active sender nor a queued message.
    pendingMessages.incrementAndGet();
    boolean added = false;
    try {
      added = queue.add(pMessage);
      return added;
    } finally {
      if (!added) {
        pendingMessages.decrementAndGet();
      }
    }
  }

  private DssMessage startProcessing(DssMessage pMessage) {
    int remainingMessages = pendingMessages.decrementAndGet();
    checkState(remainingMessages >= 0, "Consumed a message that was not registered as pending");
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
    activeWorkers.remove(Thread.currentThread().getName());
    try {
      DssMessage message = queue.take();
      // Mark the worker active before the message stops counting as pending. This keeps the work
      // visible to the monitor while it moves from the queue to the worker.
      activeWorkers.add(Thread.currentThread().getName());
      return startProcessing(message);
    } catch (InterruptedException e) {
      activeWorkers.add(Thread.currentThread().getName());
      throw e;
    }
  }
}

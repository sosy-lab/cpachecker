// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterators;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;

/**
 * The incoming messages of one DSS worker, ordered by priority.
 *
 * <p>Messages that end the analysis ({@link DssMessageType#WITNESS}, {@link DssMessageType#RESULT},
 * and {@link DssMessageType#EXCEPTION}) are taken before all others. Messages of the same priority
 * are taken in the order in which they were added.
 *
 * <p>The queue is thread-safe: the ordering is left to a {@link PriorityBlockingQueue}. Any thread
 * may add messages, but only the worker the queue belongs to should take them, because the queue
 * reports the activity of that worker to a {@link DssWorkCounter}.
 */
public class DssDefaultQueue extends AbstractQueue<DssMessage>
    implements BlockingQueue<DssMessage> {

  /** A message together with its position in the order in which messages were added. */
  private record Entry(DssMessage message, long sequenceNumber) {}

  private static final Comparator<Entry> ORDER =
      Comparator.comparingInt((Entry entry) -> priority(entry.message().getType()))
          .thenComparingLong(Entry::sequenceNumber);

  private final PriorityBlockingQueue<Entry> queue = new PriorityBlockingQueue<>(11, ORDER);
  private final AtomicLong nextSequenceNumber = new AtomicLong();
  private final DssWorkCounter workCounter;

  /** Creates a queue whose worker does not take part in a shared {@link DssWorkCounter}. */
  public DssDefaultQueue() {
    this(new DssWorkCounter());
  }

  /**
   * Creates a queue that reports its messages and the activity of its worker to {@code
   * pWorkCounter}. Every queue that shares the counter therefore needs a worker that eventually
   * takes from it.
   */
  public DssDefaultQueue(DssWorkCounter pWorkCounter) {
    workCounter = Objects.requireNonNull(pWorkCounter);
    workCounter.workerStarted();
  }

  private static int priority(DssMessageType pType) {
    return switch (pType) {
      case WITNESS, RESULT, EXCEPTION -> 0;
      case VIOLATION_CONDITION, POST_CONDITION -> 1;
    };
  }

  @Override
  public boolean offer(DssMessage pMessage) {
    Objects.requireNonNull(pMessage);
    // Count the message before its worker can take it, so that it never counts as taken before it
    // was counted as queued.
    workCounter.messageQueued();
    queue.offer(new Entry(pMessage, nextSequenceNumber.getAndIncrement()));
    return true;
  }

  /** The queue is unbounded, so this never waits. */
  @Override
  public boolean offer(DssMessage pMessage, long pTimeout, TimeUnit pUnit) {
    return offer(pMessage);
  }

  /** The queue is unbounded, so this never waits. */
  @Override
  public void put(DssMessage pMessage) {
    offer(pMessage);
  }

  /**
   * Returns the next message, and blocks while there is none.
   *
   * <p>While it blocks, the worker of this queue counts as idle.
   */
  @Override
  public DssMessage take() throws InterruptedException {
    Entry entry = queue.poll();
    if (entry == null) {
      workCounter.workerIdle();
      try {
        entry = queue.take();
      } finally {
        // Mark the worker busy before the message stops counting. This keeps the work visible to
        // the counter while it moves from the queue to the worker. After an interrupt, the worker
        // is busy as well, because it still has to react to it.
        workCounter.workerBusy();
      }
    }
    workCounter.messageTaken();
    return entry.message();
  }

  @Override
  public DssMessage poll() {
    Entry entry = queue.poll();
    if (entry == null) {
      return null;
    }
    workCounter.messageTaken();
    return entry.message();
  }

  /**
   * Not supported: a worker waits for messages with {@link #take()}, which marks it as idle while
   * it waits.
   */
  @Override
  public DssMessage poll(long pTimeout, TimeUnit pUnit) {
    throw new UnsupportedOperationException("Wait for messages with take()");
  }

  @Override
  public DssMessage peek() {
    Entry entry = queue.peek();
    return entry == null ? null : entry.message();
  }

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  @Override
  public int size() {
    return queue.size();
  }

  /** Iterates over the messages in no particular order. The iterator cannot remove messages. */
  @Override
  public Iterator<DssMessage> iterator() {
    return Iterators.unmodifiableIterator(Iterators.transform(queue.iterator(), Entry::message));
  }

  @Override
  public int remainingCapacity() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int drainTo(Collection<? super DssMessage> pTarget) {
    return drainTo(pTarget, Integer.MAX_VALUE);
  }

  @Override
  public int drainTo(Collection<? super DssMessage> pTarget, int pMaxElements) {
    checkArgument(pTarget != this, "Cannot drain a queue into itself");
    int drained = 0;
    DssMessage message;
    while (drained < pMaxElements && (message = poll()) != null) {
      pTarget.add(message);
      drained++;
    }
    return drained;
  }
}

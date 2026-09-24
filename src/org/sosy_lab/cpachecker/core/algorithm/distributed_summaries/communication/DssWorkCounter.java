// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts the work that is left in a network of DSS workers and signals once there is none.
 *
 * <p>The counter always equals the number of busy workers plus the number of messages that wait in
 * a queue. Each {@link DssDefaultQueue} belongs to exactly one worker, the one that takes messages
 * from it, and reports both parts for that worker and its messages:
 *
 * <ul>
 *   <li>{@link #workerStarted()}: the queue is created. Its worker counts as busy from its start,
 *       because it can do useful work, e.g., its initial analysis, before it ever takes a message.
 *       Once all queues of a network are created, the counter therefore equals the number of
 *       workers.
 *   <li>{@link #messageQueued()}: a message is added to the queue.
 *   <li>{@link #messageTaken()}: the worker takes a message from the queue. It is busy while it
 *       processes the message, so the message itself no longer counts.
 *   <li>{@link #workerIdle()}: the worker finds its queue empty and blocks.
 *   <li>{@link #workerBusy()}: the blocked worker wakes up, because a message arrived or it was
 *       interrupted.
 * </ul>
 *
 * <p>A worker sends its messages while it is busy, so each of them is counted before the worker can
 * become idle. The counter thus only drops to zero once no worker is busy and no message is
 * waiting. At that point the analysis reached its fixpoint, which is a proof.
 *
 * <p>The count lives in a single atomic, so there is no moment in which a message is on its way
 * from one worker to another without being counted. A {@link java.util.concurrent.Phaser} would
 * offer the same, but it is limited to 65535 parties, while the number of queued messages has no
 * bound.
 */
public final class DssWorkCounter {

  private final AtomicLong outstandingWork = new AtomicLong();
  private final CountDownLatch noWorkLeft = new CountDownLatch(1);

  void workerStarted() {
    outstandingWork.incrementAndGet();
  }

  void workerIdle() {
    decrement();
  }

  void workerBusy() {
    outstandingWork.incrementAndGet();
  }

  void messageQueued() {
    outstandingWork.incrementAndGet();
  }

  void messageTaken() {
    decrement();
  }

  private void decrement() {
    long remaining = outstandingWork.decrementAndGet();
    checkState(remaining >= 0, "Completed more work than was registered");
    if (remaining == 0) {
      noWorkLeft.countDown();
    }
  }

  /** Blocks until, for the first time, all workers are idle and no message waits in any queue. */
  public void awaitNoWorkLeft() throws InterruptedException {
    noWorkLeft.await();
  }
}

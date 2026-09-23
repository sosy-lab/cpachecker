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
 * <p>A unit of work is either a message that waits in a queue or a worker that is busy. A worker is
 * busy from its start until it blocks on its empty queue, and again from the moment it receives a
 * message. Every message a worker sends is counted before the worker itself can become idle, so the
 * counter only drops to zero once no worker is busy and no message is waiting. At that point the
 * analysis reached its fixpoint, which is a proof.
 *
 * <p>The count lives in a single atomic, so there is no moment in which a message is on its way
 * from one worker to another without being counted. A {@link java.util.concurrent.Phaser} would
 * offer the same, but it is limited to 65535 parties, while the number of queued messages has no
 * bound.
 */
public final class DssWorkCounter {

  private final AtomicLong outstandingWork = new AtomicLong();
  private final CountDownLatch noWorkLeft = new CountDownLatch(1);

  void workAdded() {
    outstandingWork.incrementAndGet();
  }

  void workDone() {
    long remaining = outstandingWork.decrementAndGet();
    checkState(remaining >= 0, "Completed more work than was registered");
    if (remaining == 0) {
      noWorkLeft.countDown();
    }
  }

  /** Blocks until all workers are idle and no message waits in any queue for the first time. */
  public void awaitNoWorkLeft() throws InterruptedException {
    noWorkLeft.await();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssDefaultQueueTest {

  /** Messages waiting in the local priority buffer are still pending work. */
  @Test
  public void bufferedMessagesAreStillPending() throws Exception {
    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    DssDefaultQueue queue = new DssDefaultQueue();
    DssMessage postCondition =
        messageFactory.createDssPostConditionMessage(
            "worker",
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableList.of(ImmutableMap.of("dummy", "state")));
    DssMessage result = messageFactory.createDssResultMessage("monitor", Result.TRUE);

    queue.add(postCondition);
    queue.add(result);

    assertThat(queue.take()).isSameInstanceAs(result);
    assertThat(queue.isEmpty()).isFalse();
    assertThat(queue.take()).isSameInstanceAs(postCondition);
    assertThat(queue.isEmpty()).isTrue();
  }

  /** A worker becomes active before its dequeued message stops counting as pending. */
  @Test(timeout = 5000)
  public void workerIsActiveAfterTakingMessage() throws Exception {
    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    String workerId = "queue-worker";
    Set<String> activeWorkers = ConcurrentHashMap.newKeySet();
    activeWorkers.add(workerId);
    DssDefaultQueue queue = new DssDefaultQueue(activeWorkers);
    DssMessage message = messageFactory.createDssResultMessage("monitor", Result.TRUE);
    CountDownLatch finished = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread worker =
        new Thread(
            () -> {
              try {
                assertThat(queue.take()).isSameInstanceAs(message);
                assertThat(activeWorkers).contains(workerId);
                assertThat(queue.isEmpty()).isTrue();
              } catch (Throwable t) {
                failure.set(t);
              } finally {
                finished.countDown();
              }
            },
            workerId);
    worker.start();

    while (activeWorkers.contains(workerId) && worker.isAlive()) {
      Thread.onSpinWait();
    }
    assertThat(activeWorkers).doesNotContain(workerId);
    queue.add(message);

    assertThat(finished.await(1, SECONDS)).isTrue();
    worker.join();
    assertThat(failure.get()).isNull();
  }
}

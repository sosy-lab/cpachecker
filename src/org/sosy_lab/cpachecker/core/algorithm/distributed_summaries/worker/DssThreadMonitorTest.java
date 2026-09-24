// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.CommunicationId;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssCommunicationEntity;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssSchedulerConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;

public class DssThreadMonitorTest {

  /** The monitor must wait while a worker is still starting up. */
  @Test(timeout = 5000)
  public void monitorWaitsForStartingWorker() throws Exception {
    String workerId = "initializing-worker";
    Set<String> activeWorkers = ConcurrentHashMap.newKeySet();
    activeWorkers.add(workerId);

    BlockingQueue<DssMessage> workerQueue = new LinkedBlockingQueue<>();
    BlockingQueue<DssMessage> observerQueue = new LinkedBlockingQueue<>();
    DssMessageBroadcaster broadcaster =
        new DssMessageBroadcaster(
            ImmutableMap.of(
                new CommunicationId(workerId, DssCommunicationEntity.BLOCK),
                workerQueue,
                new CommunicationId("observer", DssCommunicationEntity.OBSERVER),
                observerQueue));
    DssConnection workerConnection = new DssSchedulerConnection(workerQueue, broadcaster);
    DssConnection observerConnection = new DssSchedulerConnection(observerQueue, broadcaster);

    CountDownLatch workerStarted = new CountDownLatch(1);
    CountDownLatch releaseWorker = new CountDownLatch(1);
    Thread worker =
        new Thread(
            () -> {
              workerStarted.countDown();
              try {
                releaseWorker.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            },
            workerId);
    worker.start();
    assertThat(workerStarted.await(1, SECONDS)).isTrue();

    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    DssThreadMonitor monitor =
        new DssThreadMonitor(
            ImmutableList.of(worker),
            messageFactory,
            observerConnection,
            ImmutableList.of(workerConnection),
            activeWorkers);
    monitor.start();

    try {
      assertThat(observerQueue.poll(1, SECONDS)).isNull();

      activeWorkers.remove(workerId);
      DssMessage resultMessage = observerQueue.poll(1, SECONDS);
      assertThat(resultMessage).isNotNull();
      assertThat(resultMessage.getResult()).isEqualTo(Result.TRUE);
    } finally {
      releaseWorker.countDown();
      worker.join();
      monitor.interrupt();
      monitor.join();
    }
  }
}

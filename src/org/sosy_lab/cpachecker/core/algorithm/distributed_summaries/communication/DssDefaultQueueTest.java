// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
            "worker", AlgorithmStatus.SOUND_AND_PRECISE, ImmutableMap.of());
    DssMessage result = messageFactory.createDssResultMessage("monitor", Result.TRUE);

    queue.add(postCondition);
    queue.add(result);

    assertThat(queue.take()).isSameInstanceAs(result);
    assertThat(queue.isEmpty()).isFalse();
    assertThat(queue.take()).isSameInstanceAs(postCondition);
    assertThat(queue.isEmpty()).isTrue();
  }

  /**
   * No work is left only after a message that one worker sends to another has been taken by the
   * receiver and the receiver has become idle again.
   */
  @Test(timeout = 5000)
  public void noWorkLeftOnlyAfterForwardedMessageWasProcessed() throws Exception {
    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    DssMessage message = messageFactory.createDssResultMessage("monitor", Result.TRUE);
    DssWorkCounter workCounter = new DssWorkCounter();
    DssDefaultQueue sender = new DssDefaultQueue(workCounter);
    DssDefaultQueue receiver = new DssDefaultQueue(workCounter);
    AtomicBoolean received = new AtomicBoolean();
    sender.add(message);

    try (ExecutorService executor =
        Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory())) {
      executor.execute(
          () -> {
            try {
              receiver.add(sender.take());
              sender.take();
            } catch (InterruptedException e) {
              // expected once the test is done
            }
          });
      executor.execute(
          () -> {
            try {
              receiver.take();
              received.set(true);
              receiver.take();
            } catch (InterruptedException e) {
              // expected once the test is done
            }
          });

      workCounter.awaitNoWorkLeft();
      assertThat(received.get()).isTrue();
      assertThat(sender.isEmpty()).isTrue();
      assertThat(receiver.isEmpty()).isTrue();
      executor.shutdownNow();
    }
  }
}

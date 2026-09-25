// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssDefaultQueueTest {

  private static final DssMessageFactory MESSAGE_FACTORY = createMessageFactory();

  private static DssMessageFactory createMessageFactory() {
    try {
      return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    } catch (InvalidConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  private static DssMessage postCondition(String pSenderId) {
    return MESSAGE_FACTORY.createDssPostConditionMessage(
        pSenderId, AlgorithmStatus.SOUND_AND_PRECISE, ImmutableMap.of());
  }

  /**
   * Messages that end the analysis come first, and messages of the same priority keep the order in
   * which they were added.
   */
  @Test
  public void messagesAreTakenByPriorityThenInOrder() throws Exception {
    DssDefaultQueue queue = new DssDefaultQueue();
    DssMessage first = postCondition("first");
    DssMessage second = postCondition("second");
    DssMessage result = MESSAGE_FACTORY.createDssResultMessage("monitor", Result.TRUE);

    queue.add(first);
    queue.add(second);
    queue.add(result);

    assertThat(queue).hasSize(3);
    assertThat(queue.take()).isSameInstanceAs(result);
    assertThat(queue.take()).isSameInstanceAs(first);
    assertThat(queue.isEmpty()).isFalse();
    assertThat(queue.take()).isSameInstanceAs(second);
    assertThat(queue.isEmpty()).isTrue();
  }

  /**
   * Messages that several threads add while the worker takes them are neither lost nor reordered
   * within the messages of one sender.
   */
  @Test(timeout = 10000)
  public void concurrentSendersLoseNoMessages() throws Exception {
    int senders = 4;
    int messagesPerSender = 2000;
    DssWorkCounter workCounter = new DssWorkCounter();
    DssDefaultQueue queue = new DssDefaultQueue(workCounter);
    Map<String, Integer> lastReceived = new HashMap<>();

    try (ExecutorService executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofPlatform()
                .name(DssDefaultQueueTest.class.getSimpleName() + "-", 0)
                .factory())) {
      for (int sender = 0; sender < senders; sender++) {
        String senderId = "sender-" + sender;
        executor.execute(
            () -> {
              for (int i = 0; i < messagesPerSender; i++) {
                queue.add(postCondition(senderId + ":" + i));
              }
            });
      }
      // this thread is the worker of the queue
      for (int received = 0; received < senders * messagesPerSender; received++) {
        List<String> senderAndIndex = Splitter.on(':').splitToList(queue.take().getSenderId());
        int index = Integer.parseInt(senderAndIndex.get(1));
        Integer previous = lastReceived.put(senderAndIndex.getFirst(), index);
        assertThat(index).isEqualTo(previous == null ? 0 : previous + 1);
      }
    }
    assertThat(queue.isEmpty()).isTrue();
    assertThat(lastReceived).hasSize(senders);
  }

  /**
   * No work is left only after a message that one worker sends to another has been taken by the
   * receiver and the receiver has become idle again.
   */
  @Test(timeout = 5000)
  public void noWorkLeftOnlyAfterForwardedMessageWasProcessed() throws Exception {
    DssMessage message = MESSAGE_FACTORY.createDssResultMessage("monitor", Result.TRUE);
    DssWorkCounter workCounter = new DssWorkCounter();
    DssDefaultQueue sender = new DssDefaultQueue(workCounter);
    DssDefaultQueue receiver = new DssDefaultQueue(workCounter);
    AtomicBoolean received = new AtomicBoolean();
    sender.add(message);

    try (ExecutorService executor =
        Executors.newThreadPerTaskExecutor(
            Thread.ofPlatform()
                .name(DssDefaultQueueTest.class.getSimpleName() + "-", 0)
                .factory())) {
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

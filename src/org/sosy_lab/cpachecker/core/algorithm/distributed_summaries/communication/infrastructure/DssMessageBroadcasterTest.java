// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.BlockingQueue;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DssMessageBroadcasterTest {

  @Test
  public void testBroadcastToAll() throws InvalidConfigurationException {

    BlockingQueue<DssMessage> observer = new DssDefaultQueue();
    BlockingQueue<DssMessage> block1 = new DssDefaultQueue();
    BlockingQueue<DssMessage> block2 = new DssDefaultQueue();

    ImmutableMap.Builder<CommunicationId, BlockingQueue<DssMessage>> queues =
        ImmutableMap.builderWithExpectedSize(3);

    queues.put(new CommunicationId("obs", DssCommunicationEntity.OBSERVER), observer);
    queues.put(new CommunicationId("bl1", DssCommunicationEntity.BLOCK), block1);
    queues.put(new CommunicationId("bl2", DssCommunicationEntity.BLOCK), block2);

    DssMessageBroadcaster broadcaster = new DssMessageBroadcaster(queues.buildOrThrow());

    DssAnalysisOptions options =
        new DssAnalysisOptions(TestDataTools.configurationForTest().build());
    DssMessageFactory messageFactory = new DssMessageFactory(options);
    DssMessage message = messageFactory.createDssResultMessage("bl1", Result.UNKNOWN);
    broadcaster.broadcastToAll(message);

    assertWithMessage("Message was not broadcast to all").that(observer.isEmpty()).isFalse();
    assertWithMessage("Message was not broadcast to all").that(observer.isEmpty()).isFalse();
    assertWithMessage("Message was not broadcast to all").that(observer.isEmpty()).isFalse();
  }

  @Test
  public void testBroadcastToObserver() throws InvalidConfigurationException {

    BlockingQueue<DssMessage> observer = new DssDefaultQueue();
    BlockingQueue<DssMessage> block1 = new DssDefaultQueue();
    BlockingQueue<DssMessage> block2 = new DssDefaultQueue();

    ImmutableMap.Builder<CommunicationId, BlockingQueue<DssMessage>> queues =
        ImmutableMap.builderWithExpectedSize(3);

    queues.put(new CommunicationId("obs", DssCommunicationEntity.OBSERVER), observer);
    queues.put(new CommunicationId("bl1", DssCommunicationEntity.BLOCK), block1);
    queues.put(new CommunicationId("bl2", DssCommunicationEntity.BLOCK), block2);

    DssMessageBroadcaster broadcaster = new DssMessageBroadcaster(queues.buildOrThrow());

    DssAnalysisOptions options =
        new DssAnalysisOptions(TestDataTools.configurationForTest().build());
    DssMessageFactory messageFactory = new DssMessageFactory(options);
    DssMessage message = messageFactory.createDssResultMessage("bl1", Result.UNKNOWN);
    broadcaster.broadcastToObserver(message);

    assertWithMessage("Message was not broadcast to observer").that(observer.isEmpty()).isFalse();
    assertWithMessage("Message should only be sent to observer").that(block1.isEmpty()).isTrue();
    assertWithMessage("Message should only be sent to observer").that(block2.isEmpty()).isTrue();
  }

  @Test
  public void testBroadcastToIDs() throws InvalidConfigurationException {

    BlockingQueue<DssMessage> observer = new DssDefaultQueue();
    BlockingQueue<DssMessage> block1 = new DssDefaultQueue();
    BlockingQueue<DssMessage> block2 = new DssDefaultQueue();

    ImmutableMap.Builder<CommunicationId, BlockingQueue<DssMessage>> queues =
        ImmutableMap.builderWithExpectedSize(3);

    queues.put(new CommunicationId("obs", DssCommunicationEntity.OBSERVER), observer);
    queues.put(new CommunicationId("bl1", DssCommunicationEntity.BLOCK), block1);
    queues.put(new CommunicationId("bl2", DssCommunicationEntity.BLOCK), block2);

    DssMessageBroadcaster broadcaster = new DssMessageBroadcaster(queues.buildOrThrow());

    DssAnalysisOptions options =
        new DssAnalysisOptions(TestDataTools.configurationForTest().build());
    DssMessageFactory messageFactory = new DssMessageFactory(options);
    DssMessage message = messageFactory.createDssResultMessage("bl1", Result.UNKNOWN);
    broadcaster.broadcastToIds(message, ImmutableSet.of("obs", "bl1"));

    assertWithMessage("Message was not broadcast to id").that(observer.isEmpty()).isFalse();
    assertWithMessage("Message was not broadcast to id").that(block1.isEmpty()).isFalse();
    assertWithMessage("Message should only be sent to ids").that(block2.isEmpty()).isTrue();
  }
}

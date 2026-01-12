// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

/**
 * Broadcasts DSS messages to multiple connections based on their communication entity or sender ID.
 */
public class DssMessageBroadcaster {

  private final Map<String, @NonNull BlockingQueue<DssMessage>> connectionsBySenderId;
  private final Multimap<DssCommunicationEntity, BlockingQueue<DssMessage>> connectionsByEntity;

  /**
   * Creates a new broadcaster for the given connections.
   *
   * @param pConnections the connections to use for broadcasting. The key is a pair of the sender ID
   *     and the communication entity.
   */
  public DssMessageBroadcaster(Map<CommunicationId, BlockingQueue<DssMessage>> pConnections) {
    connectionsBySenderId = new ConcurrentHashMap<>();
    connectionsByEntity = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    pConnections.forEach(
        (id, queue) -> {
          connectionsBySenderId.put(id.senderId(), queue);
          connectionsByEntity.put(id.dssCommunicationEntity(), queue);
          connectionsByEntity.put(DssCommunicationEntity.ALL, queue);
        });
  }

  public boolean isEmpty() {
    return connectionsBySenderId.values().stream().allMatch(BlockingQueue::isEmpty);
  }

  private void broadcast(DssMessage message, DssCommunicationEntity entity) {
    Collection<BlockingQueue<DssMessage>> queues = connectionsByEntity.get(entity);
    for (BlockingQueue<DssMessage> queue : queues) {
      queue.add(message);
    }
  }

  /**
   * Broadcasts a message to the connections with the given receiver ids.
   *
   * @param message the message to broadcast
   * @param ids the receiver ids to broadcast to
   */
  public void broadcastToIds(DssMessage message, ImmutableSet<String> ids) {
    for (String id : ids) {
      BlockingQueue<DssMessage> queue = connectionsBySenderId.get(id);
      Objects.requireNonNull(queue, "No connection found for id: " + id).add(message);
    }
  }

  /**
   * Broadcasts a message to all connected entities.
   *
   * @param message the message to broadcast
   */
  public void broadcastToAll(DssMessage message) {
    broadcast(message, DssCommunicationEntity.ALL);
  }

  /**
   * Broadcasts a message to all observer workers. Observer workers are workers that do not perform
   * any analysis but only observe the analysis results and statistics.
   *
   * @param message the message to broadcast
   */
  public void broadcastToObserver(DssMessage message) {
    broadcast(message, DssCommunicationEntity.OBSERVER);
  }
}

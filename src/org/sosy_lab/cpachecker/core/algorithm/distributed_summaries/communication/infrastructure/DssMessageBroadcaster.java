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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;

public class DssMessageBroadcaster {

  public record CommunicationId(String senderId, DssCommunicationEntity dssCommunicationEntity) {}

  private final Map<String, BlockingQueue<DssMessage>> connectionsBySenderId;
  private final Multimap<DssCommunicationEntity, BlockingQueue<DssMessage>> connectionsByEntity;

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

  public void broadcast(DssMessage message, String receiver) {
    BlockingQueue<DssMessage> queue = connectionsBySenderId.get(receiver);
    queue.add(message);
  }

  public void broadcast(DssMessage message, List<String> receivers) {
    for (String receiver : receivers) {
      broadcast(message, receiver);
    }
  }

  public void broadcast(DssMessage message, DssCommunicationEntity entity) {
    Collection<BlockingQueue<DssMessage>> queues = connectionsByEntity.get(entity);
    synchronized (connectionsByEntity) {
      for (BlockingQueue<DssMessage> queue : queues) {
        queue.add(message);
      }
    }
  }

  public void broadcastToIds(DssMessage message, ImmutableSet<String> ids) {
    for (String id : ids) {
      BlockingQueue<DssMessage> queue = connectionsBySenderId.get(id);
      Objects.requireNonNull(queue, "No connection found for id: " + id).add(message);
    }
  }

  public void broadcastToAll(DssMessage message) {
    broadcast(message, DssCommunicationEntity.ALL);
  }

  public void broadcastToBlocks(DssMessage message) {
    broadcast(message, DssCommunicationEntity.BLOCK);
  }

  public void broadcastToObserver(DssMessage message) {
    broadcast(message, DssCommunicationEntity.OBSERVER);
  }
}

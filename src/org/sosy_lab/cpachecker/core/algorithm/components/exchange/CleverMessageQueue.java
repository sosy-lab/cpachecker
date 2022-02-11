// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;

public class CleverMessageQueue extends ForwardingBlockingQueue<Message> {

  private final BlockingQueue<Message> queue;
  private final Multimap<MessageType, Message> messages;

  private final MessageType[] ordering;

  private static final int SKIP = 5;
  private int current;

  /**
   * Mimics a blocking queue but changes the blocking method <code>take</code> to prioritize messages
   *
   * @param pQueue the queue to forward
   */
  public CleverMessageQueue(BlockingQueue<Message> pQueue) {
    queue = pQueue;
    messages = ArrayListMultimap.create();
    ordering = MessageType.values();
    current = 0;
  }

  public CleverMessageQueue() {
    this(new LinkedBlockingQueue<>());
  }

  @Override
  protected BlockingQueue<Message> delegate() {
    return queue;
  }

  public void setOrdering(MessageType... pOrdering) {
    assert pOrdering.length == ordering.length : "please provide all types for the ordering";
    System.arraycopy(pOrdering, 0, ordering, 0, pOrdering.length);
  }

  private void moveToMap(Message pMessage) {
    messages.put(pMessage.getType(), pMessage);
  }

  private Optional<Message> firstOfType(MessageType pType) {
    if (!messages.get(pType).isEmpty()) {
      Message m = messages.get(pType).stream().findFirst().orElseThrow();
      messages.remove(pType, m);
      return Optional.of(m);
    }
    return Optional.empty();
  }

  /**
   * Returns the next message to be processed.
   * The method sorts all new messages of the underlying blocking queue by their type in a multimap.
   * As long as entries are present in the multimap, simply return them by the defined ordering
   * If no message is present, wait for one by calling the take method of the underlying blocking queue.
   */
  @Override
  public Message take() throws InterruptedException {
    current++;
    // empty pending messages (non blocking)
    while (!queue.isEmpty()) {
      moveToMap(queue.take());
    }
    MessageType[] currentOrdering = ordering;
    if (current == SKIP) {
      current = 0;
      currentOrdering =
          new MessageType[]{MessageType.ERROR, MessageType.FOUND_RESULT, MessageType.BLOCK_POSTCONDITION, MessageType.ERROR_CONDITION, MessageType.ERROR_CONDITION_UNREACHABLE};
    }
    for (MessageType messageType : currentOrdering) {
      Optional<Message> m = firstOfType(messageType);
      if (m.isPresent()) {
        return m.orElseThrow();
      }
    }
    return queue.take();
  }
}

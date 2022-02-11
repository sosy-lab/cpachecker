// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import java.io.Closeable;
import java.io.IOException;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;

public interface Connection extends Closeable {

  /**
   * Wait for an incoming Message and return it.
   *
   * @return current Message to process
   * @throws InterruptedException if thread is interrupted.
   */
  Message read() throws InterruptedException;

  /**
   * Returns the size of pending messages
   * @return size of pending messages
   */
  int size();

  /**
   * Indicates if pending messages exist
   * @return true, if no pending messages exist, false otherwise
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * If a queue supports on the fly ordering of message types, the desired ordering can be set here.
   * If available, <code>read</code> returns messages by their type in the given order.
   *
   * Example:
   * pOrdering = [A, B, C]
   * read will first return all pending messages of type A, then B, then C
   *
   * @param pOrdering an array of MessageTypes.
   */
  default void setOrdering(MessageType... pOrdering) {
    throw new AssertionError("This implementation does not support custom orderings");
  }

  /*
   * Get all messages with a certain type
   * @param pType filter messages with this type
   * @return collection of messages with type pType
   *//*
  Collection<Message> readType(MessageType pType);*/

  /**
   * Write and broadcast a message to all connections including itself
   *
   * @param message Message to broadcast
   * @throws IOException          if write operation fails
   * @throws InterruptedException if thread is interrupted
   */
  void write(Message message) throws IOException, InterruptedException;

}

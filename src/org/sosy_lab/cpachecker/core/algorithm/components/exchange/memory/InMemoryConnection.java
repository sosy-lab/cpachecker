// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.memory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.CleverMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;

public class InMemoryConnection implements Connection {

  private final BlockingQueue<Message> in;
  private final List<BlockingQueue<Message>> out;

  public InMemoryConnection(BlockingQueue<Message> pIn, List<BlockingQueue<Message>> pOut) {
    in = pIn;
    out = pOut;
  }

  @Override
  public Message read() throws InterruptedException {
    return in.take();
  }

  @Override
  public int size() {
    return in.size();
  }

  @Override
  public boolean isEmpty() {
    return in.isEmpty();
  }

  @Override
  public void write(Message message) throws IOException, InterruptedException {
    for (BlockingQueue<Message> messages : out) {
      messages.put(message);
    }
  }

  @Override
  public void setOrdering(MessageType... pOrdering) {
    if (in instanceof CleverMessageQueue) {
      ((CleverMessageQueue) in).setOrdering(pOrdering);
    }
  }

  @Override
  public void close() throws IOException {

  }
}

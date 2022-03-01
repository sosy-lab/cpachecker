// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.nio_network;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.CleverMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class NetworkConnection implements Connection {

  private final Collection<NetworkSender> sender;
  private final NetworkReceiver receiver;
  private final BlockingQueue<Message> sharedQueue;

  NetworkConnection(NetworkReceiver pReceiver, Collection<NetworkSender> pSender) {
    receiver = pReceiver;
    sender = pSender;
    sharedQueue = pReceiver.getSharedQueue();
  }

  @Override
  public Message read() throws InterruptedException {
    return sharedQueue.take();
  }

  @Override
  public int size() {
    return sharedQueue.size();
  }

  @Override
  public boolean isEmpty() {
    return sharedQueue.isEmpty();
  }

  @Override
  public void write(Message message) throws IOException {
    for (NetworkSender messageSender : sender) {
      messageSender.send(message);
    }
  }

  @Override
  public void setOrdering(MessageType... pOrdering) {
    if (sharedQueue instanceof CleverMessageQueue) {
      ((CleverMessageQueue) sharedQueue).setOrdering(pOrdering);
    }
  }

  @Override
  public void close() throws IOException {
    receiver.close();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    sender.stream().findFirst().orElseThrow().collectStatistics(statsCollection);
  }
}

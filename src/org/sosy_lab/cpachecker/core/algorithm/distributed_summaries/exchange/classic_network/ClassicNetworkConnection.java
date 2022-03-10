// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.classic_network;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class ClassicNetworkConnection implements Connection {

  private final BlockingQueue<Message> read;
  private final Collection<ClassicNetworkSender> clients;
  private final ClassicNetworkReceiver server;

  public ClassicNetworkConnection(Collection<ClassicNetworkSender> pClients, ClassicNetworkReceiver pServer) {
    read = pServer.getBlockingQueue();
    clients = pClients;
    server = pServer;
  }

  @Override
  public Message read() throws InterruptedException {
    return read.take();
  }

  @Override
  public int size() {
    return read.size();
  }

  @Override
  public void write(Message message) throws IOException, InterruptedException {
    for (ClassicNetworkSender client : clients) {
      client.sendMessage(message, 20);
    }
  }

  @Override
  public void close() throws IOException {
    for (ClassicNetworkSender client : clients) {
      client.close();
    }
    server.close();
  }

  @Override
  public void setOrdering(MessageType... pOrdering) {
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    clients.stream().findFirst().orElseThrow().collectStatistics(statsCollection);
  }
}

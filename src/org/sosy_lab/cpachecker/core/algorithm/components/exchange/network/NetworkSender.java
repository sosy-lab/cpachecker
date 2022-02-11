// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionStats;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.CompressedMessageConverter;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageConverter;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class NetworkSender implements StatisticsProvider {

  private final MessageConverter converter;
  private final InetSocketAddress address;

  private final static ConnectionStats stats = new ConnectionStats();

  public NetworkSender(String pAddress, int pPort) throws IOException {
    this(new InetSocketAddress(pAddress, pPort));
  }

  public NetworkSender(InetSocketAddress pAddress) throws IOException {
    converter = new CompressedMessageConverter();
    address = pAddress;
  }

  public void send(Message pMessage) throws IOException {
    SocketChannel client = SocketChannel.open(address);
    byte[] message = converter.messageToJson(pMessage);
    stats.averageMessageSize.setNextValue(message.length);
    ByteBuffer buffer = ByteBuffer.wrap(message);
    client.write(buffer);
    client.close();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}


// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.classic_network;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ConnectionStats;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message.MessageConverter;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class ClassicNetworkSender implements Closeable, StatisticsProvider {
  private final MessageConverter converter;
  private final InetSocketAddress address;

  private final static ConnectionStats stats = new ConnectionStats();

  public ClassicNetworkSender(InetSocketAddress pAddress) {
    address = pAddress;
    converter = new MessageConverter();
  }

  public void sendMessage(Message pMessage, int retries) throws IOException {
    try (Socket client = new Socket();
         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
      client.connect(address);
      byte[] message = converter.messageToJson(pMessage);
      stats.averageMessageSize.setNextValue(message.length);
      out.write(message);
      out.flush();
    } catch (ConnectException pE) {
      // in case of many blocks, connecting may time out.
      // repeat the connection attempt.
      if (retries == 0) {
        throw new AssertionError("Out of retries", pE);
      }
      sendMessage(pMessage, retries < 0 ? retries : retries - 1);
    }
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}

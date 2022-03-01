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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;

public class NetworkConnectionProvider implements ConnectionProvider<NetworkConnection> {

  @Override
  public List<NetworkConnection> createConnections(int numConnections) throws IOException {
    List<NetworkReceiver> receivers = new ArrayList<>();
    // TODO options...
    int startPort = 8080;
    String address = "localhost";
    for (int i = 0; i < numConnections; i++) {
      receivers.add(new NetworkReceiver(new PriorityBlockingQueue<>(),
          new InetSocketAddress(address, startPort++)));
    }
    List<NetworkConnection> connections = new ArrayList<>();
    for (NetworkReceiver receiver : receivers) {
      List<NetworkSender> workerSender = new ArrayList<>();
      for (NetworkReceiver networkReceiver : receivers) {
        workerSender.add(new NetworkSender(networkReceiver.getListenAddress()));
      }
      connections.add(new NetworkConnection(receiver, workerSender));
    }
    return connections;
  }
}

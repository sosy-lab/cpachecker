// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.classic_network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.CleverMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;

public class ClassicNetworkConnectionProvider implements ConnectionProvider<ClassicNetworkConnection> {

  @Override
  public List<ClassicNetworkConnection> createConnections(int numConnections) throws IOException {
    List<ClassicNetworkReceiver> receivers = new ArrayList<>();
    int startPort = 8080;
    for (int i = 0; i < numConnections; i++) {
      receivers.add(new ClassicNetworkReceiver(new CleverMessageQueue(), startPort++));
    }
    List<ClassicNetworkConnection> connections = new ArrayList<>();
    for (ClassicNetworkReceiver receiver : receivers) {
      List<ClassicNetworkSender> workerSender = new ArrayList<>();
      for (ClassicNetworkReceiver networkReceiver : receivers) {
        workerSender.add(new ClassicNetworkSender(networkReceiver.getAddress()));
      }
      connections.add(new ClassicNetworkConnection(workerSender, receiver));
    }
    return connections;
  }

}

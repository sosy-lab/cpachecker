// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.memory;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.CleverMessageQueue;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;

public class InMemoryConnectionProvider implements ConnectionProvider<InMemoryConnection> {

  @Override
  public List<InMemoryConnection> createConnections(int connections) throws IOException {
    List<BlockingQueue<Message>> outs = new ArrayList<>();
    for (int i = 0; i < connections; i++) {
      outs.add(new CleverMessageQueue());
    }
    return outs.stream().map(out -> new InMemoryConnection(out, outs)).collect(
        ImmutableList.toImmutableList());
  }

}

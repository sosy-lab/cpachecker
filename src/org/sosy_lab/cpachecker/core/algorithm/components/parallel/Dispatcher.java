// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class Dispatcher {

  private final BlockingQueue<Message> inputStream;
  private final ConcurrentLinkedQueue<BlockingQueue<Message>> outputStreams;


  public Dispatcher() {
    inputStream = new LinkedBlockingQueue<>();
    outputStreams = new ConcurrentLinkedQueue<>();
  }

  public synchronized Worker registerNodeAndGetWorker(BlockNode pNode, LogManager pLogger, CFA pCFA, Specification pSpecification, Configuration pConfiguration, ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    outputStreams.add(queue);
    return new Worker(pNode, queue, inputStream, pLogger, pCFA, pSpecification, pConfiguration, pShutdownManager);
  }

  public void start() throws InterruptedException {
    while (true) {
      final Message m = inputStream.take();
      for (BlockingQueue<Message> outputStream : outputStreams) {
        outputStream.add(m);
      }
      if (m.getType() == MessageType.FINISHED) {
        return;
      }
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;

public class Dispatcher {

  private final BlockingQueue<Message> inputStream;
  private final ConcurrentLinkedQueue<BlockingQueue<Message>> outputStreams;


  public Dispatcher() {
    inputStream = new LinkedBlockingQueue<>();
    outputStreams = new ConcurrentLinkedQueue<>();
  }

  public synchronized Worker registerNodeAndGetWorker(BlockNode pNode, LogManager pLogger) {
    BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    outputStreams.add(queue);
    return new Worker(pNode, queue, inputStream, pLogger);
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

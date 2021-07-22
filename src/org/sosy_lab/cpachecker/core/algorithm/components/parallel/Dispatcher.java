// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;

public class Dispatcher {

  // TODO: priority queue?
  private final ConcurrentLinkedQueue<Message> inputStream;

  private final ConcurrentLinkedQueue<ConcurrentLinkedQueue<Message>> outputStreams;

  public Dispatcher() {
    inputStream = new ConcurrentLinkedQueue<>();
    outputStreams = new ConcurrentLinkedQueue<>();
  }

  public synchronized Worker registerNodeAndGetWorker(BlockNode pNode, LogManager pLogger) {
    ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
    outputStreams.add(queue);
    return new Worker(pNode, queue, inputStream, pLogger);
  }

  public synchronized void start() throws InterruptedException {
    Iterator<Message> iterator = inputStream.iterator();
    while (iterator.hasNext()) {
      final Message message = iterator.next();
      inputStream.remove(message);
      outputStreams.forEach(queue -> queue.add(message));
      if (message.getType() == MessageType.FINISHED) {
        inputStream.notifyAll();
        return;
      }
    }
    start();
  }
}

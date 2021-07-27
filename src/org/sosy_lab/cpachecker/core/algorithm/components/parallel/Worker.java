// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Worker implements Runnable {

  private final BlockNode block;
  private final BlockingQueue<Message> read;
  private final BlockingQueue<Message> write;

  private final ConcurrentHashMap<BlockNode, BooleanFormula> postConditionUpdates;
  private final ConcurrentHashMap<BlockNode, BooleanFormula> preConditionUpdates;

  private final LogManager logger;

  private boolean finished;

  public Worker(
      BlockNode pBlock,
      BlockingQueue<Message> pOutputStream,
      BlockingQueue<Message> pInputStream,
      LogManager pLogger) {
    block = pBlock;
    read = pOutputStream;
    write = pInputStream;
    postConditionUpdates = new ConcurrentHashMap<>();
    preConditionUpdates = new ConcurrentHashMap<>();
    logger = pLogger;
    finished = false;
  }

  public BooleanFormula getPostCondition() {
    if (postConditionUpdates.containsKey(block)) {
      throw new AssertionError("postConditionUpdates must contain own post-condition");
    }
    return postConditionUpdates.get(block);
  }

  public BooleanFormula getPreCondition() {
    if (preConditionUpdates.containsKey(block)) {
      throw new AssertionError("preConditionUpdates must contain own pre-condition");
    }
    return preConditionUpdates.get(block);
  }

  public void analyze() throws InterruptedException {
    while(true) {
      Message m = read.take();
      processMessage(m);
      if (finished) {
        return;
      }
    }
  }

  private void processMessage(Message message) throws InterruptedException {
    switch (message.getType()) {
      case FINISHED:
        finished = true;
        return;
      case PRECONDITION:
        if (message.getFrom().getSuccessors().contains(block)) {
          //preConditionUpdates.put(message.getFrom(), message.getCondition());
          write.add(forwardAnalysis());
        }
        break;
      case POSTCONDITION:
        if (message.getFrom().getPredecessors().contains(block)) {
          //postConditionUpdates.put(message.getFrom(), message.getCondition());
          write.add(backwardAnalysis());
        }
        break;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  // return post condition
  private Message forwardAnalysis() {
    return new Message(MessageType.PRECONDITION, block, null);
  }

  // return pre condition
  private Message backwardAnalysis() {
    return new Message(MessageType.POSTCONDITION, block, null);
  }

  private void runContinuousAnalysis() {
    try {
      analyze();
    } catch (InterruptedException pE) {
      if (!finished) {
        logger.log(Level.SEVERE, this + " run into an error while waiting because of " + pE);
        logger.log(Level.SEVERE, "Restarting Worker " + this + "...");
        runContinuousAnalysis();
      } else {
        logger.log(
            Level.SEVERE,
            this
                + " run into an error while waiting because of "
                + pE
                + " but there is nothing to do because analysis finished before.");
      }
    }
  }

  @Override
  public void run() {
    write.add(forwardAnalysis());
    runContinuousAnalysis();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }
}

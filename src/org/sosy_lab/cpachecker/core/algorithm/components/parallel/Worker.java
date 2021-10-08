// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket.WorkerSocketFactory;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Worker implements Runnable {

  private final LogManager logger;

  private final BlockNode block;
  private final BlockingQueue<Message> read;
  private final WorkerSocket socket;
  private final List<WorkerClient> clients;

  private final ForwardAnalysis forwardAnalysis;
  private final BackwardAnalysis backwardAnalysis;

  // String -> uniqueBlockId
  private final ConcurrentHashMap<String, Message> postConditionUpdates;
  private final ConcurrentHashMap<String, Message> preConditionUpdates;

  private boolean finished;
  private AlgorithmStatus status;

  private Optional<Message> lastPreConditionMessage;
  private Optional<Message> lastPostConditionMessage;

  public static Worker registerNodeAndGetWorker(
      BlockNode pNode,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      WorkerSocketFactory pFactory,
      String pAddress,
      int pPort)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    BlockingQueue<Message> sharedQueue = new LinkedBlockingQueue<>();
    WorkerSocket socket = pFactory.makeSocket(pLogger, sharedQueue, pAddress,
        pPort);
    return new Worker(pNode, sharedQueue, socket, pLogger, pCFA, pSpecification, pConfiguration,
        pShutdownManager);
  }

  public void addClient(WorkerClient client) {
    clients.add(client);
  }

  public Worker(
      BlockNode pBlock,
      BlockingQueue<Message> pOutputStream,
      WorkerSocket pWorkerSocket,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    block = pBlock;
    read = pOutputStream;
    logger = pLogger;
    finished = false;

    clients = new ArrayList<>();
    socket = pWorkerSocket;

    postConditionUpdates = new ConcurrentHashMap<>();
    preConditionUpdates = new ConcurrentHashMap<>();

    // TODO figure out correct and relative path
    Configuration backward = Configuration.builder()
        .loadFromFile(
            "/home/matket/SosyLab/cpachecker/config/includes/predicateAnalysisBackward.properties")
        .clearOption("analysis.initialStatesFor")
        .setOption("analysis.initialStatesFor", "TARGET")
        .setOption("CompositeCPA.cpas",
            "cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "../specification/MainEntry.spc")
        .setOption("specification", "../specification/MainEntry.spc")
        .build();

    backwardAnalysis = new BackwardAnalysis(pLogger, pBlock, pCFA, pSpecification,
        backward,
        pShutdownManager);
    forwardAnalysis = new ForwardAnalysis(pLogger, pBlock, pCFA, pSpecification, pConfiguration,
        pShutdownManager, backwardAnalysis);

    status = forwardAnalysis.getStatus();

    lastPreConditionMessage = Optional.empty();
    lastPostConditionMessage = Optional.empty();

    new Thread(() -> {
      try {
        socket.startServer();
      } catch (IOException pE) {
        logger.log(Level.SEVERE, pE);
      }
    }).start();
  }

  public BooleanFormula getPostCondition(FormulaManagerView fmgr) {
    return postConditionUpdates.values().stream().map(message ->
            fmgr.parse(message.getCondition()))
        .collect(fmgr.getBooleanFormulaManager().toDisjunction());
  }

  public BooleanFormula getPreCondition(FormulaManagerView fmgr) {
    return preConditionUpdates.values().stream().map(message -> fmgr.parse(message.getCondition()))
        .collect(fmgr.getBooleanFormulaManager().toDisjunction());
  }

  public void analyze() throws InterruptedException, CPAException, IOException {
    while (true) {
      Message m = read.take();
      processMessage(m);
      if (finished) {
        return;
      }
    }
  }

  private void processMessage(Message message)
      throws InterruptedException, CPAException, IOException {
    switch (message.getType()) {
      case FINISHED:
        finished = true;
        break;
      case PRECONDITION:
        if (message.getTargetNodeNumber() == block.getStartNode().getNodeNumber()) {
          preConditionUpdates.put(message.getUniqueBlockId(), message);
          Message toSend = forwardAnalysis();
          if (lastPreConditionMessage.isEmpty() || !toSend.equals(
              lastPreConditionMessage.orElseThrow())) {
            broadcast(toSend);
          }
        }
        break;
      case POSTCONDITION:
        if (message.getTargetNodeNumber() == block.getLastNode().getNodeNumber()) {
          postConditionUpdates.put(message.getUniqueBlockId(), message);
          Message toSend = backwardAnalysis();
          if (lastPostConditionMessage.isEmpty() || !toSend.equals(
              lastPostConditionMessage.orElseThrow())) {
            broadcast(toSend);
          }
        }
        break;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  private void broadcast(Message toSend) throws IOException {
    for (WorkerClient client : clients) {
      client.broadcast(toSend);
    }
  }

  // return post condition
  private Message forwardAnalysis() throws CPAException, InterruptedException {
    Message message = forwardAnalysis.analyze(forwardAnalysis.getStartState(getPreCondition(forwardAnalysis.getFmgr())));
    status = forwardAnalysis.getStatus();
    if (message.getType() == MessageType.PRECONDITION) {
      lastPreConditionMessage = Optional.of(message);
    } else if (message.getType() == MessageType.POSTCONDITION) {
      lastPostConditionMessage = Optional.of(message);
    }
    return message;
  }

  // return pre condition
  private Message backwardAnalysis() throws CPAException, InterruptedException {
    Message message = backwardAnalysis.analyze(null);
    status = backwardAnalysis.getStatus();
    if (message.getType() == MessageType.PRECONDITION) {
      lastPreConditionMessage = Optional.of(message);
    } else if (message.getType() == MessageType.POSTCONDITION) {
      lastPostConditionMessage = Optional.of(message);
    }
    return message;
  }

  private void runContinuousAnalysis() {
    try {
      analyze();
    } catch (InterruptedException | CPAException | IOException pE) {
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
    try {
      broadcast(forwardAnalysis());
      runContinuousAnalysis();
    } catch (CPAException | InterruptedException | IOException pE) {
      logger.log(Level.SEVERE, "ComponentAnalysis run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
    }
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

  public AlgorithmStatus getStatus() {
    return status;
  }
}

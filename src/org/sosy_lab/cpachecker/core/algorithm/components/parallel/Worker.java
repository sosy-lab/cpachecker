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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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

    // TODO make this config a new properties file
    Configuration backward = Configuration.builder()
        .loadFromFile(
            "config/includes/predicateAnalysisBackward.properties")
        .clearOption("analysis.initialStatesFor")
        .setOption("analysis.initialStatesFor", "TARGET")
        .setOption("CompositeCPA.cpas",
            "cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "../specification/MainEntry.spc")
        .setOption("specification", "../specification/MainEntry.spc")
        .build();

    forwardAnalysis = new ForwardAnalysis(pLogger, pBlock, pCFA, pSpecification, pConfiguration,
        pShutdownManager);
    backwardAnalysis = new BackwardAnalysis(pLogger, pBlock, pCFA, pSpecification, backward, pShutdownManager);

    status = forwardAnalysis.getStatus();

    lastPreConditionMessage = Optional.empty();
    lastPostConditionMessage = Optional.empty();

    // start thread
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
    Optional<CFANode> optionalMessageNode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalMessageNode.isEmpty()) {
      return;
    }
    CFANode node = optionalMessageNode.orElseThrow();
    switch (message.getType()) {
      case FINISHED:
        finished = true;
        break;
      case PRECONDITION:
        if (node.equals(block.getStartNode())) {
          preConditionUpdates.put(message.getUniqueBlockId(), message);
          Message toSend = forwardAnalysis(node);
          if (lastPreConditionMessage.isEmpty() || !toSend.equals(
              lastPreConditionMessage.orElseThrow())) {
            broadcast(toSend);
          }
          lastPreConditionMessage = Optional.of(toSend);
        }
        break;
      case POSTCONDITION:
        if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
            block.getStartNode()) && block.getNodesInBlock().contains(node)) {
          postConditionUpdates.put(message.getUniqueBlockId(), message);
          Message toSend = backwardAnalysis(node);
          if (lastPostConditionMessage.isEmpty() || !toSend.equals(
              lastPostConditionMessage.orElseThrow())) {
            broadcast(toSend);
          }
          lastPostConditionMessage = Optional.of(toSend);
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
  private Message forwardAnalysis(CFANode pStartNode) throws CPAException, InterruptedException {
    Message message =
        forwardAnalysis.analyze(getPreCondition(forwardAnalysis.getFmgr()), pStartNode);
    status = forwardAnalysis.getStatus();
    return message;
  }

  // return pre condition
  private Message backwardAnalysis(CFANode pStartNode) throws CPAException, InterruptedException {
    Message message =
        backwardAnalysis.analyze(getPostCondition(backwardAnalysis.getFmgr()), pStartNode);
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
      broadcast(forwardAnalysis(block.getStartNode()));
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

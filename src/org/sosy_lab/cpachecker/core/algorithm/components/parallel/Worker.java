// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket.WorkerSocketFactory;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.util.MessageLogger;
import org.sosy_lab.cpachecker.core.algorithm.components.util.MessageLogger.Action;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

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

  private final MessageLogger messageLogger;

  private final Thread socketThread;

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
    MessageLogger messageLogger = new MessageLogger(pNode.getId());
    WorkerSocket socket = pFactory.makeSocket(pLogger, messageLogger, sharedQueue, pNode.getId(), pAddress,
        pPort);
    return new Worker(pNode, sharedQueue, socket, messageLogger, pLogger, pCFA, pSpecification, pConfiguration,
        pShutdownManager);
  }

  public void addClient(WorkerClient client) {
    clients.add(client);
  }

  private Worker(
      BlockNode pBlock,
      BlockingQueue<Message> pOutputStream,
      WorkerSocket pWorkerSocket,
      MessageLogger pMessageLogger,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    block = pBlock;
    read = pOutputStream;
    logger = pLogger;
    messageLogger = pMessageLogger;
    finished = false;

    clients = new ArrayList<>();
    socket = pWorkerSocket;

    postConditionUpdates = new ConcurrentHashMap<>();
    preConditionUpdates = new ConcurrentHashMap<>();

    Configuration backward = Configuration.builder()
        .copyFrom(pConfiguration)
        .loadFromFile(
            "config/includes/predicateAnalysisBackward.properties")
        .clearOption("analysis.initialStatesFor")
        .setOption("analysis.initialStatesFor", "TARGET")
        .setOption("CompositeCPA.cpas",
            "cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "../specification/MainEntry.spc")
        .setOption("specification", "../specification/MainEntry.spc")
        .build();

    Configuration forward = Configuration.builder().copyFrom(pConfiguration).setOption("CompositeCPA.cpas",
        "cpa.block.BlockCPA,cpa.predicate.PredicateCPA").build();

    forwardAnalysis = new ForwardAnalysis(pLogger, pBlock, pCFA, pSpecification, forward,
        pShutdownManager);
    backwardAnalysis = new BackwardAnalysis(pLogger, pBlock, pCFA, pSpecification, backward, pShutdownManager);

    status = forwardAnalysis.getStatus();

    lastPreConditionMessage = Optional.empty();
    lastPostConditionMessage = Optional.empty();

    // start thread
    socketThread = new Thread(() -> {
      try {
        socket.startServer();
      } catch (IOException pE) {
        logger.log(Level.SEVERE, pE);
      }
    });
    socketThread.start();
  }

  public BooleanFormula getPostCondition(FormulaManagerView fmgr) {
    if (postConditionUpdates.isEmpty()) {
      return fmgr.getBooleanFormulaManager().makeTrue();
    }
    return postConditionUpdates.values().stream().map(message ->
            fmgr.parse(message.getPayload()))
        .collect(fmgr.getBooleanFormulaManager().toDisjunction());
  }

  public BooleanFormula getPreCondition(FormulaManagerView fmgr) {
    if (preConditionUpdates.isEmpty()) {
      return fmgr.getBooleanFormulaManager().makeTrue();
    }
    return preConditionUpdates.values().stream().map(message -> fmgr.parse(message.getPayload()))
        .collect(fmgr.getBooleanFormulaManager().toDisjunction());
  }

  public void analyze() throws InterruptedException, CPAException, IOException, SolverException {
    while (!finished) {
      Message m = read.take();
      messageLogger.log(Action.TAKE, m);
      processMessage(m);
    }
  }

  private void processMessage(Message message)
      throws InterruptedException, CPAException, IOException, SolverException {

    switch (message.getType()) {
      case POSTCONDITION:
        processPostConditionMessage(message);
        break;
      case PRECONDITION:
        processPreconditionMessage(message);
        break;
      case FINISHED:
        processFinishMessage(message);
        break;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  private void processFinishMessage(Message message) throws IOException {
    Preconditions.checkArgument(message.getType() == MessageType.FINISHED,
        "can only process messages with type %s", MessageType.FINISHED);
    messageLogger.log(Action.FINISH, message);
    finished = true;
    broadcast(Message.newFinishMessage(block.getId(), message.getTargetNodeNumber(),
        Result.valueOf(message.getPayload())));
    shutdownCommunication();
    logger.log(Level.INFO, "Shutting down worker for", block.getId());
    Thread.currentThread().interrupt();
  }

  private void processPreconditionMessage(Message message)
      throws IOException, CPAException, InterruptedException {
    Preconditions.checkArgument(message.getType() == MessageType.PRECONDITION,
        "can only process messages with type %s", MessageType.PRECONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      messageLogger.log(Action.DUMP, message);
      return;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getStartNode())) {
      messageLogger.log(Action.FORWARD, message);
      preConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = forwardAnalysis(node);
      if (lastPreConditionMessage.isEmpty() || !toSend.equals(
          lastPreConditionMessage.orElseThrow())) {
        messageLogger.log(Action.BROADCAST, toSend);
        broadcast(toSend);
      } else {
        messageLogger.log(Action.ALREADY_ENQUEUED, toSend);
      }
      lastPreConditionMessage = Optional.of(toSend);
    } else {
      messageLogger.log(Action.DUMP, message);
    }
  }

  private void processPostConditionMessage(Message message)
      throws SolverException, InterruptedException, IOException, CPAException {
    Preconditions.checkArgument(message.getType() == MessageType.POSTCONDITION,
        "can only process messages with type %s", MessageType.POSTCONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      messageLogger.log(Action.DUMP, message);
      return;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
        block.getStartNode()) && block.getNodesInBlock().contains(node)) {
      if (lastPreConditionMessage.isPresent() && backwardAnalysis.cantContinue(lastPreConditionMessage.orElseThrow().getPayload(), message.getPayload())) {
        return;
      }
      messageLogger.log(Action.BACKWARD, message);
      postConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = backwardAnalysis(node);
      if (lastPostConditionMessage.isEmpty() || !toSend.equals(
          lastPostConditionMessage.orElseThrow())) {
        messageLogger.log(Action.BROADCAST, toSend);
        broadcast(toSend);
      } else {
        messageLogger.log(Action.ALREADY_ENQUEUED, toSend);
      }
      lastPostConditionMessage = Optional.of(toSend);
    } else {
      messageLogger.log(Action.DUMP, message);
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
    return message;
  }

  private void runContinuousAnalysis() {
    try {
      analyze();
    } catch (InterruptedException | CPAException | IOException | SolverException pE) {
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

  public void shutdownCommunication() throws IOException {
    for (WorkerClient client : clients) {
      client.close();
    }
    socketThread.interrupt();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

  public AlgorithmStatus getStatus() {
    return status;
  }
}

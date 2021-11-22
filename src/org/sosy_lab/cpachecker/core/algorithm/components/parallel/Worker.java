// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.WorkerSocket.WorkerSocketFactory;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
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
  private Optional<Message> lastMessage;

  private final Thread socketThread;

  public void addClient(WorkerClient client) {
    clients.add(client);
  }

  private Worker(
      String pId,
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

    Configuration backwardConfiguration = Configuration.builder()
        .copyFrom(pConfiguration)
        .loadFromFile(
            "config/includes/predicateAnalysisBackward.properties")
        .clearOption("analysis.initialStatesFor")
        .setOption("analysis.initialStatesFor", "TARGET")
        .setOption("CompositeCPA.cpas",
            "cpa.location.LocationCPABackwards, cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "config/specification/MainEntry.spc")
        .setOption("specification", "config/specification/MainEntry.spc")
        .setOption("cpa.predicate.abstractAtTargetState", "false")
        .build();

    /*Configuration backwardConfiguration = Configuration.builder()
        .copyFrom(pConfiguration)
        .loadFromFile(
            "config/includes/predicateAnalysisBackward.properties")
        .setOption("CompositeCPA.cpas", "cpa.location.LocationCPABackwards, cpa.block.BlockCPABackward, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
        .setOption("backwardSpecification", "config/specification/MainEntry.spc")
        .setOption("specification", "config/specification/MainEntry.spc")
        .build();*/

    Specification backwardSpecification = Specification.fromFiles(ImmutableSet.of(Path.of("config/specification/MainEntry.spc")), pCFA, backwardConfiguration, logger, pShutdownManager.getNotifier());

    Configuration forwardConfiguration = Configuration.builder().copyFrom(pConfiguration).setOption("CompositeCPA.cpas",
        "cpa.location.LocationCPA, cpa.block.BlockCPA, cpa.predicate.PredicateCPA").build();

    forwardAnalysis = new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pSpecification, forwardConfiguration, pShutdownManager);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA, backwardSpecification, backwardConfiguration, pShutdownManager);


    status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    lastPreConditionMessage = Optional.empty();
    lastPostConditionMessage = Optional.empty();
    lastMessage = Optional.empty();

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

  public PathFormula getPostCondition(FormulaManagerView fmgr, PathFormulaManagerImpl manager) {
    return getBooleanFormula(fmgr, manager, postConditionUpdates);
  }

  public PathFormula getPreCondition(FormulaManagerView fmgr, PathFormulaManagerImpl manager) {
    return getBooleanFormula(fmgr, manager, preConditionUpdates);
  }

  @Nonnull
  private PathFormula getBooleanFormula(
      FormulaManagerView fmgr,
      PathFormulaManagerImpl manager,
      ConcurrentHashMap<String, Message> pUpdates) {
    if (pUpdates.isEmpty()) {
      return manager.makeEmptyPathFormula();
    }
    BooleanFormula disjunction = fmgr.getBooleanFormulaManager().makeFalse();
    SSAMapBuilder mapBuilder = SSAMap.emptySSAMap().builder();
    for (Message message : pUpdates.values()) {
      BooleanFormula parsed = fmgr.parse(message.getPayload());
      for (String variable : fmgr.extractVariables(parsed).keySet()) {
        Pair<String, OptionalInt> variableIndexPair = FormulaManagerView.parseName(variable);
        if (!variable.contains(".") && variableIndexPair.getSecond().isPresent()) {
          //TODO find correct type
          mapBuilder = mapBuilder.setIndex(variableIndexPair.getFirst(), CNumericTypes.SIGNED_INT, variableIndexPair.getSecond()
              .orElse(1));
        }
      }
      disjunction = fmgr.getBooleanFormulaManager().or(disjunction, parsed);
    }
    disjunction = fmgr.uninstantiate(disjunction);
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(mapBuilder.build(),
        PointerTargetSet.emptyPointerTargetSet()), disjunction);
  }

  public void analyze() throws InterruptedException, CPAException, IOException, SolverException {
    while (!finished) {
      if (read.isEmpty()) {
        broadcast(Message.newStaleMessage(block.getId(), true));
      }
      Message m = read.take();
      broadcast(Message.newStaleMessage(block.getId(), false));
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
      case SHUTDOWN:
        shutdown(message);
        break;
      case FOUND_VIOLATION:
      case STALE:
        break;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  private void shutdown(Message message) throws IOException {
    Preconditions.checkArgument(message.getType() == MessageType.SHUTDOWN,
        "can only process messages with type %s", MessageType.SHUTDOWN);
    logger.log(Level.INFO, "Shutting down worker for", block.getId());
    finished = true;
    for (WorkerClient client : clients) {
      client.close();
    }
    socketThread.interrupt();
    Thread.currentThread().interrupt();
  }

  private void processPreconditionMessage(Message message)
      throws IOException, CPAException, InterruptedException {
    Preconditions.checkArgument(message.getType() == MessageType.PRECONDITION,
        "can only process messages with type %s", MessageType.PRECONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getStartNode())) {
      preConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = forwardAnalysis(node);
      if (lastPreConditionMessage.isEmpty() || !toSend.equals(
          lastPreConditionMessage.orElseThrow())) {
        broadcast(toSend);
      }
      lastPreConditionMessage = Optional.of(toSend);
    }
  }

  private void processPostConditionMessage(Message message)
      throws SolverException, InterruptedException, IOException, CPAException {
    Preconditions.checkArgument(message.getType() == MessageType.POSTCONDITION,
        "can only process messages with type %s", MessageType.POSTCONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
        block.getStartNode()) && block.getNodesInBlock().contains(node)) {
      if (lastPreConditionMessage.isPresent() && backwardAnalysis.cantContinue(lastPreConditionMessage.orElseThrow().getPayload(), message.getPayload())) {
        return;
      }
      postConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = backwardAnalysis(node);
      if (lastPostConditionMessage.isEmpty() || !toSend.equals(
          lastPostConditionMessage.orElseThrow())) {
        broadcast(toSend);
      }
      lastPostConditionMessage = Optional.of(toSend);
    }
  }

  private void broadcast(Message toSend) throws IOException {
    lastMessage = Optional.of(toSend);
    for (WorkerClient client : clients) {
      client.broadcast(toSend);
    }
  }

  // return post condition
  private Message forwardAnalysis(CFANode pStartNode) throws CPAException, InterruptedException {
    Message message =
        forwardAnalysis.analyze(getPreCondition(forwardAnalysis.getFmgr(),
            forwardAnalysis.getPathFormulaManager()), pStartNode);
    status = forwardAnalysis.getStatus();
    return message;
  }

  // return pre condition
  private Message backwardAnalysis(CFANode pStartNode)
      throws CPAException, InterruptedException, SolverException {
    Message message =
        backwardAnalysis.analyze(getPostCondition(backwardAnalysis.getFmgr(),
            backwardAnalysis.getPathFormulaManager()), pStartNode);
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

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  public static class WorkerFactory {

    private int workerCount;
    private final WorkerSocketFactory socketFactory;

    public WorkerFactory() {
      socketFactory = new WorkerSocketFactory();
    }

    public Worker createWorker(
        BlockNode pNode,
        LogManager pLogger,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager,
        String pAddress,
        int pPort)
        throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
      workerCount++;
      BlockingQueue<Message> sharedQueue = new PriorityBlockingQueue<>();
      WorkerSocket socket = socketFactory.makeSocket(pLogger, sharedQueue, pAddress, pPort);
      return new Worker(pNode.getId() + "W" + workerCount, pNode, sharedQueue, socket, pLogger, pCFA, pSpecification, pConfiguration,
          pShutdownManager);
    }

    public WorkerSocketFactory getSocketFactory() {
      return socketFactory;
    }

    public int getWorkerCount() {
      return workerCount;
    }
  }
}

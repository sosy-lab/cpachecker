// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class AnalysisWorker extends Worker {

  private final BlockNode block;

  private final ForwardAnalysis forwardAnalysis;
  private final BackwardAnalysis backwardAnalysis;

  // String -> uniqueBlockId
  private final ConcurrentHashMap<String, Message> postConditionUpdates;
  private final ConcurrentHashMap<String, Message> preConditionUpdates;

  private AlgorithmStatus status;

  private Optional<Message> lastPreConditionMessage;
  private Optional<Message> lastPostConditionMessage;

  private final SSAMap typeMap;

  AnalysisWorker(
      String pId,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pLogger);

    block = pBlock;
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

    Specification backwardSpecification =
        Specification.fromFiles(ImmutableSet.of(Path.of("config/specification/MainEntry.spc")),
            pCFA, backwardConfiguration, logger, pShutdownManager.getNotifier());

    Configuration forwardConfiguration =
        Configuration.builder().copyFrom(pConfiguration).setOption("CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.block.BlockCPA, cpa.predicate.PredicateCPA").build();

    forwardAnalysis =
        new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pSpecification, forwardConfiguration,
            pShutdownManager);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA, backwardSpecification,
        backwardConfiguration, pShutdownManager);


    status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    lastPreConditionMessage = Optional.empty();
    lastPostConditionMessage = Optional.empty();

    Set<CFANode> allNodes = block.getNodesInBlock();
    List<CFAEdge> blockEdges = new ArrayList<>();
    for (CFANode cfaNode : allNodes) {
      CFAUtils.leavingEdges(cfaNode).filter(edge -> allNodes.contains(edge.getSuccessor())).copyInto(blockEdges);
    }
    typeMap = forwardAnalysis.getPathFormulaManager().makeFormulaForPath(blockEdges).getSsa();
  }

  private PathFormula getPostCondition(FormulaManagerView fmgr, PathFormulaManagerImpl manager) {
    return getBooleanFormula(fmgr, manager, postConditionUpdates);
  }

  private PathFormula getPreCondition(FormulaManagerView fmgr, PathFormulaManagerImpl manager) {
    return getBooleanFormula(fmgr, manager, preConditionUpdates);
  }

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
          String variableName = variableIndexPair.getFirst();
          if (variableName != null) {
            mapBuilder = mapBuilder.setIndex(variableName, typeMap.getType(variableName),
                variableIndexPair.getSecond()
                    .orElse(1));
          }
        }
      }
      disjunction = fmgr.getBooleanFormulaManager().or(disjunction, parsed);
    }
    disjunction = fmgr.uninstantiate(disjunction);
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(mapBuilder.build(),
        PointerTargetSet.emptyPointerTargetSet()), disjunction);
  }

  @Override
  public Optional<Message> processMessage(Message message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case POSTCONDITION:
        return processPostConditionMessage(message);
      case PRECONDITION:
        return processPreconditionMessage(message);
      case ERROR:
      case FOUND_RESULT:
        shutdown();
      case POSTCONDITION_UNREACHABLE:
        return noResponse;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  private Optional<Message> processPreconditionMessage(Message message)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(message.getType() == MessageType.PRECONDITION,
        "can only process messages with type %s", MessageType.PRECONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return noResponse;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getStartNode())) {
      preConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = forwardAnalysis(node);
      if (lastPreConditionMessage.isEmpty() || !toSend.equals(
          lastPreConditionMessage.orElseThrow())) {
        lastPreConditionMessage = Optional.of(toSend);
        return answer(toSend);
      }
      lastPreConditionMessage = Optional.of(toSend);
    }
    return noResponse;
  }

  private Optional<Message> processPostConditionMessage(Message message)
      throws SolverException, InterruptedException, CPAException {
    Preconditions.checkArgument(message.getType() == MessageType.POSTCONDITION,
        "can only process messages with type %s", MessageType.POSTCONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return noResponse;
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
        block.getStartNode()) && block.getNodesInBlock().contains(node)) {
      if (lastPreConditionMessage.isPresent() && backwardAnalysis.cantContinue(
          lastPreConditionMessage.orElseThrow().getPayload(), message.getPayload())) {
        return answer(Message.newPostConditionUnreachableMessage(block.getId()));
      }
      postConditionUpdates.put(message.getUniqueBlockId(), message);
      Message toSend = backwardAnalysis(node);
      if (lastPostConditionMessage.isEmpty() || !toSend.equals(
          lastPostConditionMessage.orElseThrow())) {
        lastPostConditionMessage = Optional.of(toSend);
        return answer(toSend);
      }
      lastPostConditionMessage = Optional.of(toSend);
    }
    return noResponse;
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

  @Override
  public void run() {
    try {
      broadcast(Optional.of(forwardAnalysis(block.getStartNode())));
      super.run();
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

}

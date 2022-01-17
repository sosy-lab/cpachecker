// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
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

public class AnalysisWorker extends Worker {

  protected final BlockNode block;

  private final BlockAnalysis forwardAnalysis;
  protected final BlockAnalysis backwardAnalysis;

  // String >-> uniqueBlockId
  protected final ConcurrentHashMap<String, Message> receivedPostConditions;
  protected final ConcurrentHashMap<String, Message> receivedErrorConditions;

  private AlgorithmStatus status;

  private Optional<Message> lastPreConditionMessage;
  private Optional<Message> firstPreConditionMessage;
  private boolean lastPreConditionBasedOnAllPredecessors;
  private boolean fullPath;

  private final SSAMap typeMap;

  AnalysisWorker(
      String pId,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      SSAMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super(pLogger);
    block = pBlock;
    receivedPostConditions = new ConcurrentHashMap<>();
    receivedErrorConditions = new ConcurrentHashMap<>();
    typeMap = pTypeMap;

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

    forwardAnalysis = new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pSpecification,
            forwardConfiguration,
            pShutdownManager);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA,
        backwardSpecification,
        backwardConfiguration, pShutdownManager);


    status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    lastPreConditionMessage = Optional.empty();
    firstPreConditionMessage = Optional.empty();
  }

  private PathFormula getPreCondition(FormulaManagerView fmgr, PathFormulaManagerImpl manager) {
    return getBooleanFormula(fmgr, manager, receivedPostConditions);
  }

  protected PathFormula getBooleanFormula(
      FormulaManagerView fmgr,
      PathFormulaManagerImpl manager,
      ConcurrentHashMap<String, Message> pUpdates) {
    if (pUpdates.isEmpty()) {
      return manager.makeEmptyPathFormula();
    }
    BooleanFormula disjunction = fmgr.getBooleanFormulaManager().makeFalse();
    SSAMapBuilder mapBuilder = SSAMap.emptySSAMap().builder();
    for (Message message : pUpdates.values()) {
      BooleanFormula parsed = parse(message.getPayload(), mapBuilder, fmgr);
      disjunction = fmgr.getBooleanFormulaManager().or(disjunction, parsed);
    }
    disjunction = fmgr.uninstantiate(disjunction);
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(mapBuilder.build(),
        PointerTargetSet.emptyPointerTargetSet()), disjunction);
  }

  /**
   * pBuilder will be modified
   * @param formula formula to be parsed
   * @param pBuilder SSAMapBuilder storing information about the returned formula
   * @param fmgr the FormulaManager that is responsible for converting the formula string
   * @return a boolean formula representing the string formula
   */
  protected final BooleanFormula parse(String formula, SSAMapBuilder pBuilder, FormulaManagerView fmgr) {
    BooleanFormula parsed = fmgr.parse(formula);
    for (String variable : fmgr.extractVariables(parsed).keySet()) {
      Pair<String, OptionalInt> variableIndexPair = FormulaManagerView.parseName(variable);
      if (!variable.contains(".") && variableIndexPair.getSecond().isPresent()) {
        String variableName = variableIndexPair.getFirst();
        if (variableName != null) {
          pBuilder.setIndex(variableName, typeMap.getType(variableName),
              variableIndexPair.getSecond()
                  .orElse(1));
        }
      }
    }
    return parsed;
  }

  @Override
  public Collection<Message> processMessage(Message message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case ERROR_CONDITION:
        return processErrorCondition(message);
      case BLOCK_POSTCONDITION:
        return processBlockPostCondition(message);
      case ERROR:
      case FOUND_RESULT:
        shutdown();
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  private Collection<Message> processBlockPostCondition(Message message)
      throws CPAException, InterruptedException, SolverException {
    Preconditions.checkArgument(message.getType() == MessageType.BLOCK_POSTCONDITION,
        "can only process messages with type %s", MessageType.BLOCK_POSTCONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return ImmutableSet.of();
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getStartNode())) {
      if (!message.getUniqueBlockId().equals(block.getId()) || fullPath) {
        receivedPostConditions.put(message.getUniqueBlockId(), message);
      }
      lastPreConditionBasedOnAllPredecessors = receivedPostConditions.size() == block.getPredecessors()
          .size();
      Collection<Message> messages = forwardAnalysis(node);
      messages.stream().filter(m -> m.getType() == MessageType.BLOCK_POSTCONDITION).forEach(toSend -> lastPreConditionMessage = Optional.of(toSend));
      return messages;
    }
    return ImmutableSet.of();
  }

  protected PathFormula payloadToPathFormula(String pPostCondition) {
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    PathFormulaManagerImpl manager = backwardAnalysis.getPathFormulaManager();
    BooleanFormula formula = parse(pPostCondition, builder, backwardAnalysis.getFmgr());
    formula = backwardAnalysis.getFmgr().uninstantiate(formula);
    PathFormula pathFormula = manager.makeAnd(manager.makeEmptyPathFormulaWithContext(builder.build(),
        PointerTargetSet.emptyPointerTargetSet()), formula);
    return pathFormula;
  }

  private Collection<Message> processErrorCondition(Message message)
      throws SolverException, InterruptedException, CPAException {
    Preconditions.checkArgument(message.getType() == MessageType.ERROR_CONDITION,
        "can only process messages with type %s", MessageType.ERROR_CONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return ImmutableSet.of();
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
        block.getStartNode()) && block.getNodesInBlock().contains(node)) {
      // under-approximating ?
      receivedErrorConditions.put(message.getUniqueBlockId(), message);
      if (lastPreConditionBasedOnAllPredecessors) {
        if (lastPreConditionMessage.isPresent()) {
          if ((firstPreConditionMessage.isPresent() && backwardAnalysis.cantContinue(
              firstPreConditionMessage.orElseThrow().getPayload(), message.getPayload())) || backwardAnalysis.cantContinue(
              lastPreConditionMessage.orElseThrow().getPayload(), message.getPayload())) {
            return ImmutableSet.of(Message.newErrorConditionUnreachableMessage(block.getId()));
          }
        }
      }
      return backwardAnalysis(node, payloadToPathFormula(message.getPayload()));
    }
    return ImmutableSet.of();
  }

  // return post condition
  private Collection<Message> forwardAnalysis(CFANode pStartNode)
      throws CPAException, InterruptedException, SolverException {
    Collection<Message> messages =
        forwardAnalysis.analyze(getPreCondition(forwardAnalysis.getFmgr(),
            forwardAnalysis.getPathFormulaManager()), pStartNode);
    for (Message message : messages) {
      if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
        int self = block.getPredecessors().contains(block) ? 1 : 0;
        fullPath =
            fullPath || receivedPostConditions.size() == block.getPredecessors().size() - self
                && receivedPostConditions.values()
                .stream().allMatch(m -> Boolean.parseBoolean(m.getAdditionalInformation()));
        message.setAdditionalInformation(Boolean.toString(fullPath));
      }
    }
    status = forwardAnalysis.getStatus();
    return messages;
  }

  // return pre-condition
  protected Collection<Message> backwardAnalysis(CFANode pStartNode, PathFormula pFormula)
      throws CPAException, InterruptedException, SolverException {
    Collection<Message> messages =
        backwardAnalysis.analyze(pFormula, pStartNode);
    status = backwardAnalysis.getStatus();
    return messages;
  }

  @Override
  public void run() {
    try {
      List<Message> initialMessages = ImmutableList.copyOf(forwardAnalysis(block.getStartNode()));
      if (initialMessages.size() == 1) {
        Message message = initialMessages.get(0);
        if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
          firstPreConditionMessage = Optional.of(message);
        }
      }
      broadcast(initialMessages);
      super.run();
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
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

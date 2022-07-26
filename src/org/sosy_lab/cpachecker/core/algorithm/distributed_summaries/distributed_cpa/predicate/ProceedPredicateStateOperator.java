// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedPredicateStateOperator implements ProceedOperator {

  private final AnalysisDirection direction;
  private final AnalysisOptions analysisOptions;
  private final BlockNode block;
  private final Solver solver;
  private final DeserializeOperator deserialize;

  private final Set<String> unsatPredecessors;
  private final Map<String, ActorMessage> receivedPostConditions;

  private ActorMessage latestOwnPostConditionMessage;

  public ProceedPredicateStateOperator(
      AnalysisOptions pOptions,
      AnalysisDirection pDirection,
      BlockNode pBlockNode,
      Solver pSolver,
      DeserializeOperator pDeserializeOperator) {
    direction = pDirection;
    analysisOptions = pOptions;
    block = pBlockNode;
    solver = pSolver;
    deserialize = pDeserializeOperator;

    unsatPredecessors = new HashSet<>();
    receivedPostConditions = new HashMap<>();
  }

  @Override
  public MessageProcessing proceed(ActorMessage pMessage)
      throws InterruptedException, SolverException {
    return direction == AnalysisDirection.FORWARD
        ? proceedForward(pMessage)
        : proceedBackward(pMessage);
  }

  @Override
  public MessageProcessing proceedBackward(ActorMessage message)
      throws SolverException, InterruptedException {
    Preconditions.checkArgument(
        message.getType() == MessageType.ERROR_CONDITION,
        "can only process messages with type %s",
        MessageType.ERROR_CONDITION);
    CFANode node = block.getNodeWithNumber(message.getTargetNodeNumber());
    if (!(node.equals(block.getLastNode())
        || (!node.equals(block.getLastNode())
            && !node.equals(block.getStartNode())
            && block.getNodesInBlock().contains(node)))) {
      return MessageProcessing.stop();
    }
    if (analysisOptions.checkEveryErrorConditionForUnsatisfiability()) {
      // can the error condition be denied?
      FormulaManagerView fmgr = solver.getFormulaManager();
      BooleanFormula messageFormula =
          fmgr.parse(
              PredicateOperatorUtil.extractFormulaString(
                  message.getPayload(), PredicateCPA.class, solver.getFormulaManager()));
      if (solver.isUnsat(messageFormula)) {
        return MessageProcessing.stopWith(
            ActorMessage.newErrorConditionUnreachableMessage(
                block.getId(), "unsat-formula: " + messageFormula));
      }
    }
    if (latestOwnPostConditionMessage != null
        && (receivedPostConditions.size() <= 3
            || analysisOptions.checkEveryErrorConditionForUnsatisfiability())
        && receivedPostConditions.size() + unsatPredecessors.size()
            == block.getPredecessors().size()) {
      FormulaManagerView fmgr = solver.getFormulaManager();
      BooleanFormula messageFormula =
          fmgr.parse(
              PredicateOperatorUtil.extractFormulaString(
                  message.getPayload(), PredicateCPA.class, solver.getFormulaManager()));
      BooleanFormula check =
          fmgr.getBooleanFormulaManager()
              .and(
                  messageFormula,
                  fmgr.parse(
                      PredicateOperatorUtil.extractFormulaString(
                          latestOwnPostConditionMessage.getPayload(),
                          PredicateCPA.class,
                          solver.getFormulaManager())));
      if (solver.isUnsat(check)) {
        return MessageProcessing.stopWith(
            ActorMessage.newErrorConditionUnreachableMessage(
                block.getId(), "unsat-with-last-post: " + check));
      }
    }
    return MessageProcessing.proceedWith(message);
  }

  @Override
  public MessageProcessing proceedForward(ActorMessage message) throws InterruptedException {
    Preconditions.checkArgument(
        message.getType() == MessageType.BLOCK_POSTCONDITION,
        "can only process messages with type %s not %s",
        MessageType.BLOCK_POSTCONDITION,
        message.getType());
    CFANode node = block.getNodeWithNumber(message.getTargetNodeNumber());
    if (!block.getStartNode().equals(node)) {
      return MessageProcessing.stop();
    }
    if (!Boolean.parseBoolean(
        message.getPayload().getOrDefault(Payload.REACHABLE, Boolean.toString(true)))) {
      unsatPredecessors.add(message.getUniqueBlockId());
      return MessageProcessing.stop();
    }
    PredicateAbstractState state = (PredicateAbstractState) deserialize.deserialize(message);
    try {
      if (solver.isUnsat(state.getPathFormula().getFormula())) {
        receivedPostConditions.remove(message.getUniqueBlockId());
        unsatPredecessors.add(message.getUniqueBlockId());
        return MessageProcessing.stop();
      }
    } catch (SolverException pE) {
      return MessageProcessing.stopWith(ActorMessage.newErrorMessage(block.getId(), pE));
    }
    unsatPredecessors.remove(message.getUniqueBlockId());
    storePostCondition(message);
    // check if every predecessor contains the full path (root node)
    if (receivedPostConditions.size() + unsatPredecessors.size()
        == block.getPredecessors().size()) {
      return MessageProcessing.proceedWith(receivedPostConditions.values());
    } else {
      // would equal initial message that has already been or will be processed by other workers
      return MessageProcessing.stop();
    }
  }

  private void storePostCondition(ActorMessage pMessage) {
    Payload payload = pMessage.getPayload();
    Map<String, String> copy = new HashMap<>(payload);
    copy.remove(Payload.SMART);
    ActorMessage toStore =
        ActorMessage.replacePayload(
            pMessage, new Payload.Builder().addAllEntries(copy).buildPayload());
    if (analysisOptions.storeCircularPostConditions()
        && Splitter.on(",").splitToList(payload.getOrDefault(Payload.VISITED, "")).stream()
            .anyMatch(s -> s.equals(block.getId()))) {
      if (Boolean.parseBoolean(payload.get(Payload.FULL_PATH))) {
        receivedPostConditions.put(pMessage.getUniqueBlockId(), toStore);
      } else {
        receivedPostConditions.remove(pMessage.getUniqueBlockId());
      }
    } else {
      receivedPostConditions.put(pMessage.getUniqueBlockId(), toStore);
    }
  }

  @Override
  public void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pDCPA) {
    ProceedPredicateStateOperator proceed =
        ((ProceedPredicateStateOperator) pDCPA.getProceedOperator());
    if (direction == AnalysisDirection.BACKWARD) {
      latestOwnPostConditionMessage = proceed.latestOwnPostConditionMessage;
      unsatPredecessors.clear();
      unsatPredecessors.addAll(proceed.unsatPredecessors);
      receivedPostConditions.clear();
      receivedPostConditions.putAll(proceed.receivedPostConditions);
    }
  }

  @Override
  public void update(ActorMessage pLatestOwnPreconditionMessage) {
    latestOwnPostConditionMessage = pLatestOwnPreconditionMessage;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedPredicateCPA extends AbstractDistributedCPA<PredicateCPA, PredicateAbstractState> {

  private final PredicateAbstractStateTransformer transformer;
  private final ConcurrentHashMap<String, Message> receivedErrorConditions;
  private final ConcurrentHashMap<String, Message> receivedPostConditions;

  private Message lastOwnPostConditionMessage;

  public DistributedPredicateCPA(String pWorkerId, BlockNode pNode, SSAMap pTypeMap, Precision pPrecision, AnalysisDirection pDirection) throws
                                                                                                                   CPAException {
    super(pWorkerId, pNode, pTypeMap, pPrecision, pDirection);
    transformer = new PredicateAbstractStateTransformer(pWorkerId, direction, pTypeMap);
    receivedErrorConditions = new ConcurrentHashMap<>();
    receivedPostConditions = new ConcurrentHashMap<>();
  }

  @Override
  public Payload encode(Collection<PredicateAbstractState> statesAtBlockEntry) {
    return transformer.encode(statesAtBlockEntry, parentCPA.getSolver().getFormulaManager());
  }

  @Override
  public PredicateAbstractState decode(Collection<Payload> messages, PredicateAbstractState previousState) {
    return transformer.decode(messages.stream().map(this::extractFormulaString).collect(Collectors.toList()), previousState, parentCPA.getPathFormulaManager(), parentCPA.getSolver()
        .getFormulaManager());
  }

  private String extractFormulaString(Payload pPayload) {
    String formula = pPayload.get(getParentCPAClass().getName());
    if (formula == null) {
      FormulaManagerView fmgr = parentCPA.getSolver().getFormulaManager();
      return fmgr.dumpFormula(fmgr.getBooleanFormulaManager().makeTrue()).toString();
    }
    return formula;
  }

  @Override
  public MessageProcessing stopBackward(Message message) throws SolverException, InterruptedException {
    Preconditions.checkArgument(message.getType() == MessageType.ERROR_CONDITION,
        "can only process messages with type %s", MessageType.ERROR_CONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return MessageProcessing.stop();
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (node.equals(block.getLastNode()) || !node.equals(block.getLastNode()) && !node.equals(
        block.getStartNode()) && block.getNodesInBlock().contains(node)) {
      // under-approximating ?
      receivedErrorConditions.put(message.getUniqueBlockId(), message);
      // can the error condition be denied?
      Solver solver = parentCPA.getSolver();
      FormulaManagerView fmgr = solver.getFormulaManager();
      BooleanFormula messageFormula = fmgr.parse(extractFormulaString(message.getPayload()));
      if (parentCPA.getSolver().isUnsat(messageFormula)) {
        return MessageProcessing.stopWith(Message.newErrorConditionUnreachableMessage(block.getId()));
      }
      if (receivedPostConditions.size() == block.getPredecessors().size()) {
        if (lastOwnPostConditionMessage != null) {
          if (solver.isUnsat(fmgr.getBooleanFormulaManager().and(messageFormula, fmgr.parse(extractFormulaString(lastOwnPostConditionMessage.getPayload()))))) {
            return MessageProcessing.stopWith(Message.newErrorConditionUnreachableMessage(block.getId()));
          }
        }
        if (firstMessage != null) {
          if (solver.isUnsat(fmgr.getBooleanFormulaManager().and(messageFormula, fmgr.parse(extractFormulaString(firstMessage.getPayload()))))) {
            return MessageProcessing.stopWith(Message.newErrorConditionUnreachableMessage(block.getId()));
          }
        }
      }
      return MessageProcessing.proceed();
    }
    return MessageProcessing.stop();
  }

  @Override
  public void setFirstMessage(Message pFirstMessage) {
    lastOwnPostConditionMessage = pFirstMessage;
    super.setFirstMessage(pFirstMessage);
  }

  @Override
  public MessageProcessing stopForward(Message message) {
    Preconditions.checkArgument(message.getType() == MessageType.BLOCK_POSTCONDITION,
        "can only process messages with type %s", MessageType.BLOCK_POSTCONDITION);
    Optional<CFANode> optionalCFANode = block.getNodesInBlock().stream()
        .filter(node -> node.getNodeNumber() == message.getTargetNodeNumber()).findAny();
    if (optionalCFANode.isEmpty()) {
      return MessageProcessing.stop();
    }
    CFANode node = optionalCFANode.orElseThrow();
    if (!node.equals(block.getStartNode())) {
      return MessageProcessing.stop();
    }
    receivedPostConditions.put(message.getUniqueBlockId(), message);
    return MessageProcessing.proceedWith(receivedPostConditions.values());
  }

  @Override
  public Class<PredicateCPA> getParentCPAClass() {
    return PredicateCPA.class;
  }

  @Override
  public Class<PredicateAbstractState> getAbstractStateClass() {
    return PredicateAbstractState.class;
  }
}

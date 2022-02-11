// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedPredicateCPA extends AbstractDistributedCPA {

  private final ConcurrentHashMap<String, Message> receivedErrorConditions;
  private final ConcurrentHashMap<String, Message> receivedPostConditions;
  private final ConcurrentHashMap<String, Boolean> circularPredecessors;
  private final Map<Formula, Formula> substitutions;
  private int executionCounter;

  private Message lastOwnPostConditionMessage;

  public DistributedPredicateCPA(
      String pWorkerId,
      BlockNode pNode,
      SSAMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection) throws
                                    CPAException {
    super(pWorkerId, pNode, pTypeMap, pPrecision, pDirection);
    receivedErrorConditions = new ConcurrentHashMap<>();
    receivedPostConditions = new ConcurrentHashMap<>();
    circularPredecessors = new ConcurrentHashMap<>();
    substitutions = new HashMap<>();
  }

  public Map<Formula, Formula> getSubstitutions() {
    return new HashMap<>(substitutions);
  }

  @Override
  public AbstractState deserialize(Message pMessage) throws InterruptedException {
    String formula = extractFormulaString(pMessage.getPayload());
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        getPathFormula(formula),
        (PredicateAbstractState) getInitialState(
            block.getNodeWithNumber(pMessage.getTargetNodeNumber()),
            StateSpacePartition.getDefaultPartition()));
  }

  @Override
  public Payload serialize(AbstractState pState) {
    FormulaManagerView fmgr = getSolver().getFormulaManager();
    PathFormulaManager pmgr = ((PredicateCPA) parentCPA).getPathFormulaManager();
    PredicateAbstractState state = ((PredicateAbstractState) pState);
    PathFormula pathFormula;
    if (state.isAbstractionState()) {
      // formula before abstraction
      if (fmgr.getBooleanFormulaManager().isTrue(state.getAbstractionFormula().asFormula())) {
        pathFormula = state.getAbstractionFormula().getBlockFormula();
      } else {
        pathFormula = pmgr.makeEmptyPathFormulaWithContextFrom(state.getAbstractionFormula().getBlockFormula());
        pathFormula = pmgr.makeAnd(pathFormula, state.getAbstractionFormula().asFormula());
      }
    } else {
      pathFormula = state.getPathFormula();
    }
    String formula =
        fmgr.dumpFormula(uninstantiate(pathFormula).getFormula())
            .toString();
    return Payload.builder().addEntry(parentCPA.getClass().getName(), formula).build();
  }

  public Solver getSolver() {
    return ((PredicateCPA) parentCPA).getSolver();
  }


  private String extractFormulaString(Payload pPayload) {
    String formula = pPayload.get(parentCPA.getClass().getName());
    if (formula == null) {
      FormulaManagerView fmgr = getSolver().getFormulaManager();
      return fmgr.dumpFormula(fmgr.getBooleanFormulaManager().makeTrue()).toString();
    }
    return formula;
  }

  @Override
  public MessageProcessing proceedBackward(Message message)
      throws SolverException, InterruptedException {
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
      Solver solver = getSolver();
      FormulaManagerView fmgr = solver.getFormulaManager();
      BooleanFormula messageFormula = fmgr.parse(extractFormulaString(message.getPayload()));
      if (solver.isUnsat(messageFormula)) {
        return MessageProcessing.stopWith(
            Message.newErrorConditionUnreachableMessage(block.getId()));
      }
      if (receivedPostConditions.size() == block.getPredecessors().size()) {
        if (lastOwnPostConditionMessage != null) {
          if (solver.isUnsat(fmgr.getBooleanFormulaManager().and(messageFormula,
              fmgr.parse(extractFormulaString(lastOwnPostConditionMessage.getPayload()))))) {
            return MessageProcessing.stopWith(
                Message.newErrorConditionUnreachableMessage(block.getId()));
          }
        }
        if (firstMessage != null) {
          if (solver.isUnsat(fmgr.getBooleanFormulaManager()
              .and(messageFormula, fmgr.parse(extractFormulaString(firstMessage.getPayload()))))) {
            return MessageProcessing.stopWith(
                Message.newErrorConditionUnreachableMessage(block.getId()));
          }
        }
      }
      return MessageProcessing.proceedWith(message);
    }
    return MessageProcessing.stop();
  }

  @Override
  public boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return PredicateAbstractState.class.isAssignableFrom(pClass);
  }

  @Override
  public void setFirstMessage(Message pFirstMessage) {
    lastOwnPostConditionMessage = pFirstMessage;
    super.setFirstMessage(pFirstMessage);
  }

  @Override
  public AbstractState combine(
      AbstractState pState1, AbstractState pState2) throws InterruptedException {
    PredicateAbstractState state1 = (PredicateAbstractState) pState1;
    PredicateAbstractState state2 = (PredicateAbstractState) pState2;
    PathFormulaManager manager = ((PredicateCPA) parentCPA).getPathFormulaManager();
    PathFormula newFormula = manager.makeOr(uninstantiate(state1.getPathFormula()),
        uninstantiate(state2.getPathFormula()));
    return PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        newFormula,
        state1,
        state2.getPreviousAbstractionState());
  }

  @Override
  public MessageProcessing proceedForward(Message message) {
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
    Set<String> visited = new HashSet<>(
        Splitter.on(",").splitToList(message.getPayload().getOrDefault(Payload.VISITED, "")));
    if (visited.contains(block.getId())) {
      circularPredecessors.put(message.getUniqueBlockId(), visited.contains("B0"));
    }
    if (receivedPostConditions.values().size() == block.getPredecessors().size()) {
      List<Message> messages = receivedPostConditions.values().stream()
          .filter(m -> circularPredecessors.getOrDefault(m.getUniqueBlockId(), true)).collect(
              ImmutableList.toImmutableList());
      return MessageProcessing.proceedWith(messages);
    } else {
      return MessageProcessing.proceed();
    }
  }

  public PathFormula getPathFormula(String formula) {
    PathFormulaManager manager = ((PredicateCPA) parentCPA).getPathFormulaManager();
    FormulaManagerView fmgr = getSolver().getFormulaManager();
    if (formula.isEmpty()) {
      return manager.makeEmptyPathFormula();
    }
    SSAMapBuilder mapBuilder = SSAMap.emptySSAMap().builder();
    BooleanFormula parsed = parse(formula, mapBuilder, fmgr);
    parsed = fmgr.uninstantiate(parsed);
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(mapBuilder.build(),
        PointerTargetSet.emptyPointerTargetSet()), parsed);
  }

  /**
   * pBuilder will be modified
   *
   * @param formula  formula to be parsed
   * @param pBuilder SSAMapBuilder storing information about the returned formula
   * @param fmgr     the FormulaManager that is responsible for converting the formula string
   * @return a boolean formula representing the string formula
   */
  private BooleanFormula parse(String formula, SSAMapBuilder pBuilder, FormulaManagerView fmgr) {
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

  private PathFormula uninstantiate(PathFormula pPathFormula) {
    executionCounter++;
    FormulaManagerView fmgr = getSolver().getFormulaManager();
    BooleanFormula booleanFormula = pPathFormula.getFormula();
    SSAMap ssaMap = pPathFormula.getSsa();
    Map<String, Formula> variableToFormula = fmgr.extractVariables(booleanFormula);
    substitutions.clear();
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();

      List<String> nameAndIndex = Splitter.on("@").limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty() || name.contains(".")) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula),
            name + "." + id + "E" + executionCounter + direction.name().charAt(0) + index));
      } else {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name, 1));
        builder = builder.setIndex(name, ssaMap.getType(name), 1);
      }
    }
    PathFormulaManager manager = ((PredicateCPA) parentCPA).getPathFormulaManager();
    SSAMap ssaMapFinal = builder.build();
    return manager.makeAnd(manager.makeEmptyPathFormulaWithContext(ssaMapFinal,
            PointerTargetSet.emptyPointerTargetSet()),
        fmgr.uninstantiate(fmgr.substitute(booleanFormula, substitutions)));
  }

}

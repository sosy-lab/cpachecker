/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.termination;

import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.createDummyLiteral;
import static org.sosy_lab.cpachecker.util.CFAUtils.edgeHasType;
import static org.sosy_lab.cpachecker.util.CFAUtils.enteringEdges;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TerminationTransferRelation implements TransferRelation {

  /*
   *  Inserted nodes and edges:                  .
   *                                             . int x',y',z', ..., pc';
   *                                             .
   *                                             |
   *                                             0 loop head
   *                                            / \
   *                       [ranking_relation]  /   \ [! (ranking_relation)]
   *                                          /     \
   *                                  node1  0       0 potential non-termination
   *                                         |       |
   *     int __CPAchecker_termination_temp;  |       | Label: __CPACHECKER_NON_TERMINATION
   *                                         |       |
   *                                  node2  0       0
   *                                         |
   *    __CPAchecker_termination_temp =      |
   *           __VERIFIER_nondet_int();      |
   *                                         |
   *                                  node3  0
   *                                        / \
   * [__CPAchecker_termination_temp == 0]  /   \ [! (__CPAchecker_termination_temp == 0)]
   *                                      /     \
   *                                     |       0 node4
   *                                     |      /
   *                                     |     / x' = x;
   *                                     |    /  y' = y;
   *                                     |   /   ...
   *                                     |  /
   *                                     | /
   *                                     0  node5 = loopHead
   *                                     |
   *                   original edge     |
   *                   after loop head   |
   *                                     |
   *                                     0
   *                                     |
   *                                     .
   *                                     .
   *                                     .
   */

  private final static String TMP_VARIABLE_NAME = "__CPAchecker_termination_temp";

  private final static String PRIMED_VARIABLE_POSTFIX = "__TERMINATION_PRIMED";

  private final static CFunctionDeclaration NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          CFunctionType.functionTypeWithReturnType(CNumericTypes.INT),
          "__VERIFIER_nondet_int",
          Collections.emptyList());

  private static final String PRIMED_PC_NAME = "__PC__" + PRIMED_VARIABLE_POSTFIX;
  private static final CVariableDeclaration PRIMED_PC =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          CNumericTypes.INT,
          PRIMED_PC_NAME,
          PRIMED_PC_NAME,
          PRIMED_PC_NAME,
          new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));

  /**
   * The loop that is currently analyzed by the {@link TerminationAlgorithm}
   */
  private Optional<CFANode> loopHead = Optional.empty();

  /**
   * The current ranking relation as disjunction.
   */
  private CExpression rankingRelations;

  /**
   * Mapping of relevant variables to the corresponding primed variable.
   */
  private Map<CVariableDeclaration, CVariableDeclaration> relevantVariables =
      Collections.emptyMap();

  private final TransferRelation transferRelation;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public TerminationTransferRelation(
      TransferRelation pTransferRelation, MachineModel pMachineModel, LogManager pLogger) {
    transferRelation = Preconditions.checkNotNull(pTransferRelation);
    binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    rankingRelations = resetRankingRelation();
  }

  private CBinaryExpression resetRankingRelation() {
    return binaryExpressionBuilder.buildBinaryExpressionUnchecked(ONE, ZERO, EQUALS);
  }

  /**
   * Adds a new ranking relation that is valid for the loop currently processed.
   *
   * @param pRankingRelation
   *            the new ranking relation to add as condition
   * @throws UnrecognizedCCodeException if <code>pRankingRelation</code> is not a valid condition
   */
  void addRankingRelation(CExpression pRankingRelation) throws UnrecognizedCCodeException {
    rankingRelations =
        binaryExpressionBuilder.buildBinaryExpression(
            rankingRelations, pRankingRelation, BinaryOperator.BINARY_OR);
  }

  /**
   * Sets the loop to check for non-termination.
   *
   * @param pLoopHead
   *        the loop's head node
   * @param pRelevantVariables
   *        all variables that might be relevant to prove (non-)termination of the given loop.
   */
  void setProcessedLoop(CFANode pLoopHead, Set<CVariableDeclaration> pRelevantVariables) {
    loopHead = Optional.of(pLoopHead);
    resetRankingRelation();

    Builder<CVariableDeclaration, CVariableDeclaration> builder = ImmutableMap.builder();
    for (CVariableDeclaration relevantVariable : pRelevantVariables) {
      CVariableDeclaration primedVariable =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              relevantVariable.getType(),
              relevantVariable.getName() + PRIMED_VARIABLE_POSTFIX,
              relevantVariable.getOrigName() + PRIMED_VARIABLE_POSTFIX,
              relevantVariable.getQualifiedName() + PRIMED_VARIABLE_POSTFIX,
              null);
      relevantVariables.put(relevantVariable, primedVariable);
    }
    relevantVariables = builder.build();
  }

  @Override
  public Collection<? extends TerminationState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    CFANode location = AbstractStates.extractLocation(pState);
    AbstractState wrappedState = ((TerminationState) pState).getWrappedState();
    Collection<? extends AbstractState> successors;

    if (location == null) {
      throw new UnsupportedOperationException(
          "TerminationTransferRelation requieres location infromation.");

    } else if (!loopHead.isPresent()
        && enteringEdges(location).anyMatch(edgeHasType(CFAEdgeType.FunctionCallEdge))
        && loopHead.get().getFunctionName().equals(location.getFunctionName())) {
      successors = declarePrimedVariables(location, wrappedState, pPrecision);

    } else if (loopHead.isPresent() && location.equals(loopHead.get())) {
      successors = insertRankingRelation(wrappedState, pPrecision, location);

    } else {
      successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision);
    }

    return wrapStates(successors);
  }

  private Collection<? extends TerminationState> wrapStates(
      Collection<? extends AbstractState> states) {
    return states
        .stream()
        .map(TerminationState::new)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private Collection<? extends AbstractState> declarePrimedVariables(
      CFANode pCfaNode, AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    String function = pCfaNode.getFunctionName();
    CFANode currentNode = pCfaNode;
    Collection<AbstractState> states = Collections.singletonList(pState);

    for (CVariableDeclaration primedVariable : relevantVariables.values()) {
      CFANode nextNode = creatCfaNode(function);
      CFAEdge edge = createDeclarationEdge(primedVariable, currentNode, nextNode);
      states = getWrappedSucessors(states, pPrecision, edge);
      currentNode = nextNode;
    }

    // primed program counter
    CFAEdge edge = createDeclarationEdge(PRIMED_PC, currentNode, pCfaNode);
    states = getWrappedSucessors(states, pPrecision, edge);

    // original edges after pCfaNode
    Collection<AbstractState> resultingSuccessors = Lists.newArrayListWithCapacity(2);
    for (AbstractState state : states) {
      resultingSuccessors.addAll(transferRelation.getAbstractSuccessors(state, pPrecision));
    }

    return resultingSuccessors;
  }

  private Collection<? extends AbstractState> insertRankingRelation(
      AbstractState pWrappedState, Precision pPrecision, CFANode loopHead)
      throws CPATransferException, InterruptedException {

    Collection<AbstractState> resultingSuccessors;
    String functionName = loopHead.getFunctionName();

    // loopHead - [!(rankingFunction)] -> potentialNonTerminationNode
    CFANode potentialNonTerminationNode = creatCfaNode(functionName);
    CFAEdge negativeRankingRelation =
        createAssumeEdge(rankingRelations, loopHead, potentialNonTerminationNode, false);

    Collection<? extends AbstractState> potentialNonTerminationStates =
        transferRelation.getAbstractSuccessorsForEdge(
            pWrappedState, pPrecision, negativeRankingRelation);

    // non termination label is reachable
    if (!potentialNonTerminationStates.isEmpty()) {

      // loopHead - Label: __CPACHECKER_NON_TERMINATION; -> state
      CFANode nodeAfterLabel = new CFATerminationNode(functionName);
      CFAEdge nonTerminationLabel =
          new BlankEdge(
              "Label: __CPACHECKER_NON_TERMINATION;",
              FileLocation.DUMMY,
              potentialNonTerminationNode,
              nodeAfterLabel,
              "Label: __CPACHECKER_NON_TERMINATION");

      resultingSuccessors =
          getWrappedSucessors(potentialNonTerminationStates, pPrecision, nonTerminationLabel);

    } else {
      resultingSuccessors = Lists.newArrayListWithCapacity(2);

      // loopHead - [rankingFunction] -> node1);
      CFANode node1 = creatCfaNode(functionName);
      CFAEdge positiveRankingRelation =
          createAssumeEdge(rankingRelations, loopHead, potentialNonTerminationNode, true);

      Collection<? extends AbstractState> states =
          transferRelation.getAbstractSuccessorsForEdge(
              pWrappedState, pPrecision, positiveRankingRelation);

      // node3 - int __CPAchecker_termination_temp;  -> node 5
      CFANode node2 = creatCfaNode(functionName);
      CVariableDeclaration nondetVariable = createLoaclVariable(functionName, TMP_VARIABLE_NAME);
      CFAEdge nondetEdge = createDeclarationEdge(nondetVariable, node1, node2);
      states = getWrappedSucessors(states, pPrecision, nondetEdge);

      // node3 - __CPAchecker_termination_temp = __VERIFIER_nondet_int  -> node 5
      CFANode node3 = creatCfaNode(functionName);
      CFunctionCallAssignmentStatement nondetAssignment =
          new CFunctionCallAssignmentStatement(
              FileLocation.DUMMY,
              new CIdExpression(FileLocation.DUMMY, nondetVariable),
              new CFunctionCallExpression(
                  FileLocation.DUMMY,
                  CNumericTypes.INT,
                  new CIdExpression(FileLocation.DUMMY, NONDET_INT),
                  Collections.emptyList(),
                  NONDET_INT));

      CFAEdge nondetAssignmentEdge = crateCStatementEdge(nondetAssignment, node2, node3);
      states = getWrappedSucessors(states, pPrecision, nondetAssignmentEdge);

      // node3 - [! (__CPAchecker_termination_temp == 0)] -> node 4
      CFANode node4 = creatCfaNode(functionName);
      CExpression nondetTmpVariable = new CIdExpression(FileLocation.DUMMY, nondetVariable);
      CExpression nondetTmpVariableAssumption =
          new CBinaryExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              CNumericTypes.INT,
              nondetTmpVariable,
              CIntegerLiteralExpression.ZERO,
              BinaryOperator.EQUALS);
      CFAEdge negativeNodetAssumeEdge =
          createAssumeEdge(nondetTmpVariableAssumption, node3, node4, false);

      Collection<AbstractState> statesAtNode4 =
          getWrappedSucessors(states, pPrecision, negativeNodetAssumeEdge);

      Collection<AbstractState> statesAtNode5 = Lists.newArrayListWithCapacity(2);

      // node4 - x' = x; y' = y; ... pc' = pc; -> node 5
      CFANode node5 = loopHead;
      statesAtNode5.addAll(initializePrimedVariables(node4, node5, statesAtNode4, pPrecision));

      // node3 - [__CPAchecker_termination_temp == 0] -> node 5
      CFAEdge positiveNodetAssumeEdge =
          createAssumeEdge(nondetTmpVariableAssumption, node3, node5, true);

      statesAtNode5.addAll(getWrappedSucessors(statesAtNode4, pPrecision, positiveNodetAssumeEdge));

      // original edges after loop head
      for (AbstractState state : statesAtNode5) {
        resultingSuccessors.addAll(transferRelation.getAbstractSuccessors(state, pPrecision));
      }
    }

    return resultingSuccessors;
  }

  private Collection<? extends AbstractState> initializePrimedVariables(
      CFANode startNode,
      CFANode endNode,
      Collection<? extends AbstractState> pStates,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    CIntegerLiteralExpression loopHeadProgramLocation =
        loopHead
            .map(CFANode::getNodeNumber)
            .map(pc -> createDummyLiteral(pc, CNumericTypes.INT))
            .orElseThrow(IllegalStateException::new);

    String function = startNode.getFunctionName();
    CFANode currentNode = startNode;
    Collection<? extends AbstractState> states = pStates;

    // x' = x; y' = y; ....
    for (Entry<CVariableDeclaration, CVariableDeclaration> relevantVariable :
        relevantVariables.entrySet()) {
      CFANode nextNode = creatCfaNode(function);

      CVariableDeclaration unprimedVariable = relevantVariable.getKey();
      CVariableDeclaration primedVariable = relevantVariable.getValue();
      CStatement assignment = createAssignmentStatement(primedVariable, unprimedVariable);
      CFAEdge edge = crateCStatementEdge(assignment, currentNode, nextNode);

      states = getWrappedSucessors(states, pPrecision, edge);
      currentNode = nextNode;
    }

    // pc' = loopHead.get().getNodeNumber()
    CStatement assignment = createAssignmentStatement(PRIMED_PC, loopHeadProgramLocation);
    CFAEdge edge = crateCStatementEdge(assignment, currentNode, endNode);
    states = getWrappedSucessors(states, pPrecision, edge);

    return states;
  }

  private CStatementEdge crateCStatementEdge(
      CStatement pStatement, CFANode pPredecessor, CFANode pSuccessor) {
    return new CStatementEdge(
        pStatement.toASTString(), pStatement, FileLocation.DUMMY, pPredecessor, pSuccessor);
  }

  private CExpressionAssignmentStatement createAssignmentStatement(
      CSimpleDeclaration pLeftHandSide, CSimpleDeclaration pRightHandSide) {
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        new CIdExpression(FileLocation.DUMMY, pLeftHandSide),
        new CIdExpression(FileLocation.DUMMY, pRightHandSide));
  }

  private CExpressionAssignmentStatement createAssignmentStatement(
      CSimpleDeclaration pLeftHandSide, CExpression pRightHandSide) {
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY, new CIdExpression(FileLocation.DUMMY, pLeftHandSide), pRightHandSide);
  }

  private CVariableDeclaration createLoaclVariable(String functionName, String variableName) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        CNumericTypes.INT,
        variableName,
        variableName,
        functionName + "::" + variableName,
        null);
  }

  private CAssumeEdge createAssumeEdge(
      CExpression condition, CFANode predecessor, CFANode successor, boolean postive) {
    return new CAssumeEdge(
        condition.toASTString(), FileLocation.DUMMY, predecessor, successor, condition, postive);
  }

  private CDeclarationEdge createDeclarationEdge(
      CDeclaration pDeclaration, CFANode pPredecessor, CFANode pSuccessor) {
    return new CDeclarationEdge(
        pDeclaration.toASTString(), FileLocation.DUMMY, pPredecessor, pSuccessor, pDeclaration);
  }

  private Collection<AbstractState> getWrappedSucessors(
      Collection<? extends AbstractState> pStates, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {

    Collection<AbstractState> sucessors = Lists.newArrayListWithCapacity(pStates.size());
    for (AbstractState state : pStates) {
      sucessors.addAll(transferRelation.getAbstractSuccessorsForEdge(state, pPrecision, pEdge));
    }

    return sucessors;
  }

  private CFANode creatCfaNode(String functionName) {
    return new CFANode(functionName);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "TerminationCPA does not support returning successors for a single edge.");
  }
}

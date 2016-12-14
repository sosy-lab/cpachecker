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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.singletonList;
import static java.util.logging.Level.FINEST;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationLoopInformation;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TerminationTransferRelation implements TransferRelation {

  /*
   *  Inserted nodes and edges:                  .  int x',y',z', ..., pc';
   *                                             .
   *                                             .
   *                                             .
   *                                             |
   *          original edge(s) after loop head   v   loop head
   *   ... <------------------------------------ 0 <-------------------------------------------
   *                                            / \                                            |
   *                      [ranking_relation]   /   \ [! (ranking_relation)]                    |
   *                                          /     \                                          |
   *                                         v       v                                         |
   *                                  node1  0<------0 potential non-termination               |
   *                                         |       |                                         |
   *     int __CPAchecker_termination_temp;  |       | Label: __CPACHECKER_NON_TERMINATION     |
   *                                         |       |                                         |
   *                                         v       v                                         |
   *                                  node2  0       0                                         |
   *                                         |                                                 |
   *       __CPAchecker_termination_temp =   |                                                 |
   *               __VERIFIER_nondet_int();  |                                                 |
   *                                         v                                                 |
   *                                  node3  0                                                 |
   *                                        / \                                                |
   * [__CPAchecker_termination_temp == 0]  /   \ [! (__CPAchecker_termination_temp == 0)]      |
   *                                      /     \                                              |
   *                                     |       v                                             |
   *                                     |       0 node4                                       |
   *                                     |      /                                              |
   *                                     |     / x' = x;                                       |
   *                                     |    /  y' = y;                                       |
   *                                     v   /   ...                                           |
   *                              node5  0 <-                                                  |
   *                                     |                                                     |
   *                                     |_____________________________________________________|
   *
   *
   */

  private final static String TMP_VARIABLE_NAME = "__CPAchecker_termination_temp";

  private final static CFunctionDeclaration NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          CFunctionType.functionTypeWithReturnType(CNumericTypes.INT),
          "__VERIFIER_nondet_int",
          Collections.emptyList());

  private Set<CFAEdge> createdCfaEdges = Sets.newLinkedHashSet();

  private final TransferRelation transferRelation;
  private final TerminationLoopInformation terminationInformation;
  private final LogManager logger;

  public TerminationTransferRelation(
      TransferRelation pTransferRelation,
      TerminationLoopInformation terminationInformation,
      LogManager pLogger) {
    transferRelation = checkNotNull(pTransferRelation);
    this.terminationInformation = checkNotNull(terminationInformation);
    logger = checkNotNull(pLogger);
  }

  @Override
  public Collection<? extends TerminationState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    CFANode location = AbstractStates.extractLocation(pState);
    TerminationState terminationState = (TerminationState) pState;
    Collection<TerminationState> statesAtCurrentLocation;
    Collection<TerminationState> targetStatesAtCurrentLocation;

    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");

    } else if (terminationState.isPartOfStem()
        && terminationInformation.isPredecessorOfIncommingEdge(location)) {
      statesAtCurrentLocation = declarePrimedVariables(terminationState, pPrecision, location);
      targetStatesAtCurrentLocation = Collections.emptyList();

    } else if (terminationInformation.isLoopHead(location)) {
      statesAtCurrentLocation = insertRankingRelation(terminationState, pPrecision, location);
      targetStatesAtCurrentLocation =
          statesAtCurrentLocation
              .stream()
              .filter(AbstractStates::isTargetState)
              .collect(Collectors.toList());
      statesAtCurrentLocation.removeAll(targetStatesAtCurrentLocation);

    } else {
      statesAtCurrentLocation = Collections.singleton(terminationState);
      targetStatesAtCurrentLocation = Collections.emptyList();
    }

    resetCfa();
    assert !(statesAtCurrentLocation.isEmpty() && targetStatesAtCurrentLocation.isEmpty())
        : pState + " has no successors.";

    Collection<TerminationState> resultingSuccessors =
        Lists.newArrayListWithCapacity(statesAtCurrentLocation.size());

    // Add the non target states first because they should be added to the wait list
    // before the CPA algorithm stops due to a target state.
    resultingSuccessors.addAll(getAbstractSuccessors0(statesAtCurrentLocation, pPrecision));

    // pass negative ranking relation to other AbstarctStates
    for (TerminationState targetState : targetStatesAtCurrentLocation) {
      Collection<? extends AbstractState> strengthenedStates =
          transferRelation.strengthen(
              targetState.getWrappedState(), singletonList(targetState), null, pPrecision);
      strengthenedStates
          .stream()
          .map(targetState::withWrappedState)
          .forEach(resultingSuccessors::add);
    }

    return resultingSuccessors;
  }

  private Collection<TerminationState> declarePrimedVariables(
      TerminationState pState, Precision pPrecision, CFANode pCfaNode)
      throws CPATransferException, InterruptedException {

    Collection<TerminationState> states = Collections.singletonList(pState);

    for (CFAEdge edge : terminationInformation.createPrimedVariableDeclarations(pCfaNode)) {
      states = getAbstractSuccessorsForEdge0(states, pPrecision, edge);
    }

    return states;
  }

  private Collection<TerminationState> insertRankingRelation(
      TerminationState loopHeadState, Precision pPrecision, CFANode loopHead)
      throws CPATransferException, InterruptedException {
    Collection<TerminationState> resultingSuccessors = Lists.newArrayListWithCapacity(4);
    String functionName = loopHead.getFunctionName();

    logger.logf(
        FINEST,
        "Adding ranking relations %s after location %s in function %s.",
        terminationInformation.getRankingRelationAsCExpression(),
        loopHead,
        functionName);

    // loopHead - [!(rankingFunction)] -> potentialNonTerminationNode
    CFANode potentialNonTerminationNode = creatCfaNode(functionName);
    CFAEdge negativeRankingRelation =
        terminationInformation.createRankingRelationAssumeEdge(
            loopHead, potentialNonTerminationNode, false);

    Collection<? extends TerminationState> potentialNonTerminationStates =
        getAbstractSuccessorsForEdge0(
            Collections.singleton(loopHeadState), pPrecision, negativeRankingRelation);

    // non-termination requires a loop that started at the loopHeads location
    if (loopHeadState.isPartOfLoop() && loopHeadState.getHondaLocation().equals(loopHead)) {
      // loopHead - Label: __CPACHECKER_NON_TERMINATION; -> nodeAfterLabel
      CFAEdge nonTerminationLabel =
          terminationInformation.createEdgeToNonTerminationLabel(potentialNonTerminationNode);

      Collection<TerminationState> targetStates =
          getAbstractSuccessorsForEdge0(
              potentialNonTerminationStates, pPrecision, nonTerminationLabel);

      // Use a direct edge from the loopHead to the target state
      // because the intermediate state is never visible to any other component.
      CFAEdge edgeToTargetState =
          terminationInformation.createNegatedRankingRelationAssumeEdgeToTargetNode(loopHead);
      Optional<RankingRelation> rankingRelation = terminationInformation.getRankingRelation();
      from(targetStates)
          .transform(ts -> ts.withDummyLocation(Collections.singleton(edgeToTargetState)))
          .transform(ts -> rankingRelation.map(ts::withUnsatisfiedRankingRelation).orElse(ts))
          .copyInto(resultingSuccessors);
    }

    CFANode node1 = creatCfaNode(functionName);
    Collection<TerminationState> statesAtNode1 = Lists.newArrayListWithCapacity(2);

    // potentialNonTerminationNode --> node1
    CFAEdge blankEdge = createBlankEdge(potentialNonTerminationNode, node1, "");
    statesAtNode1.addAll(
        getAbstractSuccessorsForEdge0(potentialNonTerminationStates, pPrecision, blankEdge));

    // loopHead --> node1
    CFAEdge positiveRankingRelation =
        terminationInformation.createRankingRelationAssumeEdge(loopHead, node1, true);
    statesAtNode1.addAll(
        getAbstractSuccessorsForEdge0(
            Collections.singleton(loopHeadState), pPrecision, positiveRankingRelation));

    // node1 - int __CPAchecker_termination_temp;  -> node 2
    CFANode node2 = creatCfaNode(functionName);
    CDeclarationEdge nondetEdge = createTmpVarDeclaration(node1, node2);
    Collection<TerminationState> statesAtNode2 =
        getAbstractSuccessorsForEdge0(statesAtNode1, pPrecision, nondetEdge);

    // node2 - __CPAchecker_termination_temp = __VERIFIER_nondet_int()  -> node 3
    CFANode node3 = creatCfaNode(functionName);
    CFunctionCallAssignmentStatement nondetAssignment =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            new CIdExpression(FileLocation.DUMMY, nondetEdge.getDeclaration()),
            new CFunctionCallExpression(
                FileLocation.DUMMY,
                CNumericTypes.INT,
                new CIdExpression(FileLocation.DUMMY, NONDET_INT),
                Collections.emptyList(),
                NONDET_INT));

    CFAEdge nondetAssignmentEdge = crateCStatementEdge(nondetAssignment, node2, node3);
    Collection<TerminationState> statesAtNode3 =
        getAbstractSuccessorsForEdge0(statesAtNode2, pPrecision, nondetAssignmentEdge);

    // node3 - [! (__CPAchecker_termination_temp == 0)] -> node 4
    CFANode node4 = creatCfaNode(functionName);
    CExpression nondetTmpVariable = new CIdExpression(DUMMY, nondetEdge.getDeclaration());
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

    // Enter loop only once.
    Collection<TerminationState> nonLoopStatesAtNode3 =
        statesAtNode3
            .stream()
            .filter(TerminationState::isPartOfStem)
            .collect(Collectors.toCollection(ArrayList::new));
    Collection<TerminationState> statesAtNode4 =
        getAbstractSuccessorsForEdge0(nonLoopStatesAtNode3, pPrecision, negativeNodetAssumeEdge);

    Collection<TerminationState> statesAtNode5 = Lists.newArrayList();

    // node4 - x' = x; y' = y; ... -> node 5
    CFANode node5 = creatCfaNode(functionName);
    initializePrimedVariables(node4, node5, statesAtNode4, pPrecision)
        .stream()
        .map((s) -> s.enterLoop(loopHead)) // pc' = loopHead
        .forEach(statesAtNode5::add);

    // node3 - [__CPAchecker_termination_temp == 0] -> node 5
    CFAEdge positiveNodetAssumeEdge =
        createAssumeEdge(nondetTmpVariableAssumption, node3, node5, true);
    statesAtNode5.addAll(
        getAbstractSuccessorsForEdge0(statesAtNode3, pPrecision, positiveNodetAssumeEdge));

    // node5 - BlankEdge -> loopHead
    CFAEdge edgeBackToLoopHead = createBlankEdge(node5, loopHead, "");
    resultingSuccessors.addAll(
        getAbstractSuccessorsForEdge0(statesAtNode5, pPrecision, edgeBackToLoopHead));

    return resultingSuccessors;
  }

  private Collection<? extends TerminationState> initializePrimedVariables(
      CFANode startNode,
      CFANode endNode,
      Collection<? extends TerminationState> pStates,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    Collection<? extends TerminationState> states = pStates;

    // x' = x; y' = y; ....
    List<CFAEdge> stemToLoopTransition =
        terminationInformation.createStemToLoopTransition(startNode, endNode);
    for (CFAEdge assignment : stemToLoopTransition) {
      states = getAbstractSuccessorsForEdge0(states, pPrecision, assignment);
    }

    return states;
  }

  private Collection<TerminationState> getAbstractSuccessorsForEdge0(
      Collection<? extends TerminationState> pStates, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    Collection<TerminationState> successors = Lists.newArrayListWithCapacity(pStates.size());

    for (TerminationState state : pStates) {

      // Loop states should never leave the loop currently processed.
      if (state.isPartOfStem() || !terminationInformation.isloopLeavingEdge(pEdge)) {

        AbstractState wrappedState = state.getWrappedState();
        transferRelation
            .getAbstractSuccessorsForEdge(wrappedState, pPrecision, pEdge)
            .stream()
            .map(state::withWrappedState)
            .forEach(successors::add);
      }
    }

    return successors;
  }

  private Collection<TerminationState> getAbstractSuccessors0(
      Collection<? extends TerminationState> pStates, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    Collection<TerminationState> resultingSuccessors =
        Lists.newArrayListWithCapacity(pStates.size());

    for (TerminationState state : pStates) {
      transferRelation
          .getAbstractSuccessors(state.getWrappedState(), pPrecision)
          .stream()
          .map(state::withWrappedState)
          .filter(
              s ->
                  s.isPartOfStem()
                      || !terminationInformation.isloopLeavingLocation(extractLocation(s)))
          .forEach(resultingSuccessors::add);
    }

    return resultingSuccessors;
  }

  private CDeclarationEdge createTmpVarDeclaration(CFANode predecessor, CFANode successor) {
    String function = predecessor.getFunctionName();
    CVariableDeclaration declaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            TMP_VARIABLE_NAME,
            TMP_VARIABLE_NAME,
            function + "::" + TMP_VARIABLE_NAME,
            null);
    CDeclarationEdge edge =
        new CDeclarationEdge(declaration.toASTString(), DUMMY, predecessor, successor, declaration);
    addToCfa(edge);
    return edge;
  }

  private CFANode creatCfaNode(String functionName) {
    return new CFANode(functionName);
  }

  private BlankEdge createBlankEdge(CFANode pPredecessor, CFANode pSuccessor, String pDescription) {
    BlankEdge edge =
        new BlankEdge(pDescription + ";", DUMMY, pPredecessor, pSuccessor, pDescription);
    addToCfa(edge);
    return edge;
  }

  private CStatementEdge crateCStatementEdge(
      CStatement pStatement, CFANode pPredecessor, CFANode pSuccessor) {
    CStatementEdge edge =
        new CStatementEdge(pStatement.toASTString(), pStatement, DUMMY, pPredecessor, pSuccessor);
    addToCfa(edge);
    return edge;
  }

  private CAssumeEdge createAssumeEdge(
      CExpression condition, CFANode predecessor, CFANode successor, boolean postive) {
    CAssumeEdge edge =
        new CAssumeEdge(condition.toASTString(), DUMMY, predecessor, successor, condition, postive);
    addToCfa(edge);
    return edge;
  }

  /**
   * Adds <code>edge</code> temporarily to the CFA.
   *
   * @param edge {@link CFAEdge} to add
   */
  private void addToCfa(CFAEdge edge) {
    CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    createdCfaEdges.add(edge);
  }

  /**
   * Removes all temporarily added {@link CFAEdge}s from the CFA.
   */
  private void resetCfa() {
    createdCfaEdges.forEach(CFACreationUtils::removeEdgeFromNodes);
    createdCfaEdges.clear();
    terminationInformation.resetCfa();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "TerminationCPA does not support returning successors for a single edge.");
  }
}

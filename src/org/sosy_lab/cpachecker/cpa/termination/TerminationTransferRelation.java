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

import static java.util.logging.Level.FINEST;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
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
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TerminationTransferRelation implements TransferRelation {

  /*
   *  Inserted nodes and edges:                  .  int x',y',z', ..., pc';
   *                                             .
   *                                             .
   *                                             .  [stay_in_loop]
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

  private final static String NON_TERMINATION_LABEL = "__CPACHECKER_NON_TERMINATION";

  private final static String TMP_VARIABLE_NAME = "__CPAchecker_termination_temp";

  private final static String PRIMED_VARIABLE_POSTFIX = "__TERMINATION_PRIMED";

  private final static CFunctionDeclaration NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          CFunctionType.functionTypeWithReturnType(CNumericTypes.INT),
          "__VERIFIER_nondet_int",
          Collections.emptyList());

  /**
   * The loop that is currently analyzed by the {@link TerminationAlgorithm}
   */
  private Optional<Loop> loop = Optional.empty();

  /**
   * All locations after an outgoing edge of the loop currently processed or an empty set.
   * Needs to be set before modifying the {@link CFA}!
   *
   * @see Loop#getOutgoingEdges()
   */
  private Set<CFANode> loopLeavingLocations = Collections.emptySet();

  /**
   * All outgoing edges of the loop currently processed or an empty set.
   * Needs to be set before modifying the {@link CFA}!
   *
   * @see Loop#getOutgoingEdges()
   */
  private Set<CFAEdge> loopLeavingEdges = Collections.emptySet();

  /**
   * The current ranking relation as disjunction.
   */
  private CExpression rankingRelations;

  /**
   * Mapping of relevant variables to the corresponding primed variable.
   */
  private Map<CVariableDeclaration, CVariableDeclaration> relevantVariables =
      Collections.emptyMap();

  // reusing of intermediate location is required to build counter examples
  private List<CFANode> relevantVariablesInitializationIntermediateLocations =
      Collections.emptyList();

  private Set<CFAEdge> createdCfaEdges = Sets.newLinkedHashSet();

  private final TransferRelation transferRelation;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;
  private final LogManager logger;

  public TerminationTransferRelation(
      TransferRelation pTransferRelation, MachineModel pMachineModel, LogManager pLogger) {
    transferRelation = Preconditions.checkNotNull(pTransferRelation);
    binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    logger = Preconditions.checkNotNull(pLogger);
    resetRankingRelation();
  }

  private void resetRankingRelation() {
    rankingRelations = binaryExpressionBuilder.buildBinaryExpressionUnchecked(ONE, ZERO, EQUALS);
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
   * @param pLoop
   *        the loop to process
   * @param pRelevantVariables
   *        all variables that might be relevant to prove (non-)termination of the given loop.
   */
  void setProcessedLoop(Loop pLoop, Set<CVariableDeclaration> pRelevantVariables) {
    resetCfa();
    loop = Optional.of(pLoop);
    loopLeavingLocations =
        pLoop.getOutgoingEdges().stream().map(CFAEdge::getSuccessor).collect(Collectors.toSet());
    loopLeavingEdges =
        pLoop.getOutgoingEdges().stream().collect(Collectors.toSet());
    resetRankingRelation();

    String functionName = pLoop.getLoopHeads().iterator().next().getFunctionName();
    ImmutableList.Builder<CFANode> intermediateStates = ImmutableList.builder();
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
      builder.put(relevantVariable, primedVariable);
      intermediateStates.add(new CFANode(functionName));
    }

    relevantVariablesInitializationIntermediateLocations = intermediateStates.build();
    relevantVariables = builder.build();
  }

  @Override
  public Collection<? extends TerminationState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    CFANode location = AbstractStates.extractLocation(pState);
    TerminationState terminationState = (TerminationState) pState;
    Collection<? extends TerminationState> statesAtCurrentLocation;

    if (location == null) {
      throw new UnsupportedOperationException("TransferRelation requires location information.");

    } else if (loop.isPresent()
        && CFAUtils.leavingEdges(location).anyMatch(edge -> loop.get().getIncomingEdges().contains(edge))) {
      statesAtCurrentLocation = declarePrimedVariables(terminationState, pPrecision, location);

    } else if (loop.map(Loop::getLoopHeads).map(lh -> lh.contains(location)).orElse(false)) {
      statesAtCurrentLocation = insertRankingRelation(terminationState, pPrecision, location);

    } else {
      statesAtCurrentLocation = Collections.singleton(terminationState);
    }

    resetCfa();

    assert !statesAtCurrentLocation.isEmpty() : pState + " has no successors.";
    Collection<TerminationState> abstractSuccessors =
        getAbstractSuccessors0(statesAtCurrentLocation, pPrecision);

    // add all intermediate target states
    statesAtCurrentLocation
        .stream()
        .filter(AbstractStates::isTargetState)
        .forEach(abstractSuccessors::add);
    return abstractSuccessors;
  }

  private Collection<? extends TerminationState> declarePrimedVariables(
      TerminationState pState, Precision pPrecision, CFANode pCfaNode)
      throws CPATransferException, InterruptedException {
    String function = pCfaNode.getFunctionName();

    logger.logf(
        FINEST,
        "Adding declarations of primed variables %s after %s in function %s.",
        MoreStrings.longStringOf(
            () ->
                relevantVariables
                    .values()
                    .stream()
                    .map(AbstractSimpleDeclaration::getName)
                    .collect(Collectors.joining(" ,"))),
        pCfaNode,
        function);

    CFANode currentNode = pCfaNode;
    Collection<TerminationState> states = Collections.singletonList(pState);

    for (CVariableDeclaration primedVariable : relevantVariables.values()) {
      CFANode nextNode = creatCfaNode(function);
      CFAEdge edge = createDeclarationEdge(primedVariable, currentNode, nextNode);
      states = getAbstractSuccessorsForEdge0(states, pPrecision, edge);
      currentNode = nextNode;
    }

    // blank edge back to original CFA node
    CFAEdge edge = createBlankEdge(currentNode, pCfaNode, "");
    states = getAbstractSuccessorsForEdge0(states, pPrecision, edge);

    return getAbstractSuccessors0(states, pPrecision);
  }

  private Collection<? extends TerminationState> insertRankingRelation(
      TerminationState loopHeadState, Precision pPrecision, CFANode loopHead)
      throws CPATransferException, InterruptedException {
    Collection<TerminationState> resultingSuccessors = Lists.newArrayList();
    String functionName = loopHead.getFunctionName();

    logger.logf(
        FINEST,
        "Adding ranking relations %s after location %s in function %s.",
        rankingRelations,
        loopHead,
        functionName);

    // loopHead - [!(rankingFunction)] -> potentialNonTerminationNode
    CFANode potentialNonTerminationNode = creatCfaNode(functionName);
    CFAEdge negativeRankingRelation =
        createNegatedRankingRelationAssumeEdge(loopHead, potentialNonTerminationNode);

    Collection<? extends TerminationState> potentialNonTerminationStates =
        getAbstractSuccessorsForEdge0(
            Collections.singleton(loopHeadState), pPrecision, negativeRankingRelation);

    // non-termination requires a loop
    if (loopHeadState.isPartOfLoop()) {
      // loopHead - Label: __CPACHECKER_NON_TERMINATION; -> nodeAfterLabel
      CFANode nodeAfterLabel = new CLabelNode(functionName, NON_TERMINATION_LABEL);
      CFAEdge nonTerminationLabel =
          createBlankEdge(
              potentialNonTerminationNode, nodeAfterLabel, "Label: " + NON_TERMINATION_LABEL);
      resultingSuccessors.addAll(
          getAbstractSuccessorsForEdge0(
              potentialNonTerminationStates, pPrecision, nonTerminationLabel));
    }

    CFANode node1 = creatCfaNode(functionName);
    Collection<TerminationState> statesAtNode1 = Lists.newArrayListWithCapacity(2);

    // potentialNonTerminationNode --> node1
    CFAEdge blankEdge = createBlankEdge(potentialNonTerminationNode, node1, "");
    statesAtNode1.addAll(
        getAbstractSuccessorsForEdge0(potentialNonTerminationStates, pPrecision, blankEdge));

    // loopHead --> node1
    CFAEdge positiveRankingRelation = createAssumeEdge(rankingRelations, loopHead, node1, true);
    statesAtNode1.addAll(
        getAbstractSuccessorsForEdge0(
            Collections.singleton(loopHeadState), pPrecision, positiveRankingRelation));

    // node1 - int __CPAchecker_termination_temp;  -> node 2
    CFANode node2 = creatCfaNode(functionName);
    CVariableDeclaration nondetVariable = createLoaclVariable(functionName, TMP_VARIABLE_NAME);
    CFAEdge nondetEdge = createDeclarationEdge(nondetVariable, node1, node2);
    Collection<TerminationState> statesAtNode2 =
        getAbstractSuccessorsForEdge0(statesAtNode1, pPrecision, nondetEdge);

    // node2 - __CPAchecker_termination_temp = __VERIFIER_nondet_int()  -> node 3
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
    Collection<TerminationState> statesAtNode3 =
        getAbstractSuccessorsForEdge0(statesAtNode2, pPrecision, nondetAssignmentEdge);

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
        .map(TerminationState::enterLoop) // pc' = loopHead
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

  protected CFAEdge createNegatedRankingRelationAssumeEdge(CFANode startNode, CFANode endNode) {
    return createAssumeEdge(rankingRelations, startNode, endNode, false);
  }

  private Collection<? extends TerminationState> initializePrimedVariables(
      CFANode startNode,
      CFANode endNode,
      Collection<? extends TerminationState> pStates,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    CFANode currentNode = startNode;
    Collection<? extends TerminationState> states = pStates;
    Iterator<CFANode> intermediateLocations =
        relevantVariablesInitializationIntermediateLocations.iterator();

    // x' = x; y' = y; ....
    for (CStatement assignment : createPrimedVariableAssignments()) {
      CFANode nextNode = intermediateLocations.next();
      CFAEdge edge = crateCStatementEdge(assignment, currentNode, nextNode);
      states = getAbstractSuccessorsForEdge0(states, pPrecision, edge);
      currentNode = nextNode;
    }

    CFAEdge edge = createBlankEdge(currentNode, endNode, "");
    states = getAbstractSuccessorsForEdge0(states, pPrecision, edge);

    return states;
  }

  private List<CStatement> createPrimedVariableAssignments() {
    ImmutableList.Builder<CStatement> builder = ImmutableList.builder();

    for (Entry<CVariableDeclaration, CVariableDeclaration> relevantVariable :
        relevantVariables.entrySet()) {

      CVariableDeclaration unprimedVariable = relevantVariable.getKey();
      CVariableDeclaration primedVariable = relevantVariable.getValue();
      CStatement assignment = createAssignmentStatement(primedVariable, unprimedVariable);

      builder.add(assignment);
    }

    return builder.build();
  }

  protected List<CFAEdge> createStemToLoopTransition(CFANode startNode, CFANode endNode) {
    CFANode currentNode = startNode;
    ImmutableList.Builder<CFAEdge> builder = ImmutableList.builder();

    Iterator<CFANode> intermediateLocations =
        relevantVariablesInitializationIntermediateLocations.iterator();
    for (CStatement assignment : createPrimedVariableAssignments()) {
      CFANode nextNode = intermediateLocations.next();
      CFAEdge edge = crateCStatementEdge(assignment, currentNode, nextNode);

      builder.add(edge);
      currentNode = nextNode;
    }

    // blank edge to endNode
    CFAEdge edge = createBlankEdge(currentNode, endNode, "");
    builder.add(edge);

    return builder.build();
  }

  private CStatementEdge crateCStatementEdge(
      CStatement pStatement, CFANode pPredecessor, CFANode pSuccessor) {
    CStatementEdge edge =
        new CStatementEdge(pStatement.toASTString(), pStatement, DUMMY, pPredecessor, pSuccessor);
    addToCfa(edge);
    return edge;
  }

  private CExpressionAssignmentStatement createAssignmentStatement(
      CSimpleDeclaration pLeftHandSide, CSimpleDeclaration pRightHandSide) {
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY,
        new CIdExpression(FileLocation.DUMMY, pLeftHandSide),
        new CIdExpression(FileLocation.DUMMY, pRightHandSide));
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
    CAssumeEdge edge =
        new CAssumeEdge(condition.toASTString(), DUMMY, predecessor, successor, condition, postive);
    addToCfa(edge);
    return edge;
  }

  private CDeclarationEdge createDeclarationEdge(
      CDeclaration pDeclaration, CFANode pPredecessor, CFANode pSuccessor) {
    CDeclarationEdge edge =
        new CDeclarationEdge(
            pDeclaration.toASTString(), FileLocation.DUMMY, pPredecessor, pSuccessor, pDeclaration);
    addToCfa(edge);
    return edge;
  }

  private BlankEdge createBlankEdge(CFANode pPredecessor, CFANode pSuccessor, String pDescription) {
    BlankEdge edge =
        new BlankEdge(pDescription + ";", DUMMY, pPredecessor, pSuccessor, pDescription);
    addToCfa(edge);
    return edge;
  }

  private Collection<TerminationState> getAbstractSuccessorsForEdge0(
      Collection<? extends TerminationState> pStates, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    Collection<TerminationState> successors = Lists.newArrayListWithCapacity(pStates.size());

    for (TerminationState state : pStates) {

      // Loop states should never leave the loop currently processed.
      if (state.isPartOfStem() || ! loopLeavingEdges.contains(pEdge)) {

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
          .filter(s -> s.isPartOfStem() || !loopLeavingLocations.contains(extractLocation(s)))
          .forEach(resultingSuccessors::add);
    }

    return resultingSuccessors;
  }

  private CFANode creatCfaNode(String functionName) {
    return new CFANode(functionName);
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
  void resetCfa() {
    createdCfaEdges.forEach(CFACreationUtils::removeEdgeFromNodes);
    createdCfaEdges.clear();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException(
        "TerminationCPA does not support returning successors for a single edge.");
  }
 }

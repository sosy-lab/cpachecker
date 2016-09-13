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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static java.util.logging.Level.FINEST;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;

import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.cpa.termination.TerminationARGPath;
import org.sosy_lab.cpachecker.cpa.termination.TerminationTransferRelation;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * {@link TerminationLoopInformation} is used by the {@link TerminationAlgorithm} to store
 * information about the termination analysis of a {@link Loop}.
 * {@link TerminationTransferRelation} and {@link TerminationARGPath} use this information
 * to instrument the analyzed program.
 */
@NotThreadSafe
public class TerminationLoopInformation {

  private final static String NON_TERMINATION_LABEL = "__CPACHECKER_NON_TERMINATION";

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
   * The current ranking relation.
   */
  private Optional<RankingRelation> rankingRelation = Optional.empty();

  /**
   * Mapping of relevant variables to the corresponding primed variable.
   */
  private Map<CExpression, CVariableDeclaration> relevantVariables = Collections.emptyMap();

  // reusing of intermediate location is required to build counter examples
  private List<CFANode> relevantVariablesInitializationIntermediateLocations =
      Collections.emptyList();

  private Set<CFAEdge> createdCfaEdges = Sets.newLinkedHashSet();

  private Optional<CFANode> targetNode = Optional.empty();

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public TerminationLoopInformation(MachineModel pMachineModel, LogManager pLogger) {
    logger = Preconditions.checkNotNull(pLogger);
    binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    resetRankingRelation();
  }

  private void resetRankingRelation() {
    rankingRelation = Optional.empty();
  }

  public boolean isLoopHead(CFANode pLocation) {
    return loop.map(Loop::getLoopHeads).map(lh -> lh.contains(pLocation)).orElse(false);
  }

  public boolean isPredecessorOfIncommingEdge(CFANode pLocation) {
    return loop.isPresent()
        && leavingEdges(pLocation).anyMatch(edge -> loop.get().getIncomingEdges().contains(edge));
  }

  public CExpression getRankingRelationAsCExpression() {
    return rankingRelation
        .map(RankingRelation::asCExpression)
        .orElseGet(() -> binaryExpressionBuilder.buildBinaryExpressionUnchecked(ZERO, ONE, EQUALS));
  }

  public Optional<RankingRelation> getRankingRelation() {
    return rankingRelation;
  }

  public boolean isloopLeavingEdge(CFAEdge pEdge) {
    return loopLeavingEdges.contains(pEdge);
  }

  public boolean isloopLeavingLocation(CFANode pLocation) {
    return loopLeavingLocations.contains(pLocation);
  }

  /**
   * Adds a new ranking relation that is valid for the loop currently processed.
   *
   * @param pRankingRelation
   *            the new {@link RankingRelation}
   */
  void addRankingRelation(RankingRelation pRankingRelation) {
    rankingRelation =
        Optional.of(rankingRelation.map(r -> r.merge(pRankingRelation)).orElse(pRankingRelation));
  }

  /**
   * Sets the loop to check for non-termination. Resets all other stored data.
   *
   * @param pLoop
   *        the loop to process
   * @param pRelevantVariables
   *        all variables that might be relevant to prove (non-)termination of the given loop.
   */
  void setProcessedLoop(Loop pLoop, Set<CVariableDeclaration> pRelevantVariables) {
    loop = Optional.of(pLoop);
    loopLeavingLocations =
        pLoop.getOutgoingEdges().stream().map(CFAEdge::getSuccessor).collect(Collectors.toSet());
    loopLeavingEdges = pLoop.getOutgoingEdges().stream().collect(Collectors.toSet());
    resetRankingRelation();

    String functionName = pLoop.getLoopHeads().iterator().next().getFunctionName();
    ImmutableList.Builder<CFANode> intermediateStates = ImmutableList.builder();
    Builder<CExpression, CVariableDeclaration> builder = ImmutableMap.builder();

    for (CVariableDeclaration relevantVariable : pRelevantVariables) {
      CExpression unprimedVariable = new CIdExpression(DUMMY, relevantVariable);
      CVariableDeclaration primedVariable = TerminationUtils.createPrimedVariable(relevantVariable);
      builder.put(unprimedVariable, primedVariable);
      intermediateStates.add(new CFANode(functionName));

      // x__TERMINATION_PRIMED__TERMINATION_DEREFERENCED = *x;
      CType type = relevantVariable.getType();
      while (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
        if (type instanceof CVoidType || type instanceof CFunctionType) {
          break; // Cannot declare variable of type void or of function type.
        }

        unprimedVariable = new CPointerExpression(DUMMY, type, unprimedVariable);
        primedVariable = TerminationUtils.createDereferencedVariable(primedVariable);

        builder.put(unprimedVariable, primedVariable);
        intermediateStates.add(new CFANode(functionName));
      }
    }

    // Create a unique target node for each loop
    targetNode = Optional.of(new CLabelNode(functionName, NON_TERMINATION_LABEL));

    relevantVariablesInitializationIntermediateLocations = intermediateStates.build();
    relevantVariables = builder.build();
  }

  /**
   * The {@link TerminationLoopInformation} is reseted.
   * No loop will be checked for non-termination.
   */
  void reset() {
    loop = Optional.empty();
    loopLeavingLocations = Collections.emptySet();
    loopLeavingEdges = Collections.emptySet();
    relevantVariables = Collections.emptyMap();
    relevantVariablesInitializationIntermediateLocations = Collections.emptyList();
    targetNode = Optional.empty();
    resetCfa();
  }

  public CFAEdge createRankingRelationAssumeEdge(
      CFANode startNode, CFANode endNode, boolean postive) {
    return createAssumeEdge(getRankingRelationAsCExpression(), startNode, endNode, postive);
  }

  public List<CFAEdge> createStemToLoopTransition(CFANode startNode, CFANode endNode) {
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

  private List<CStatement> createPrimedVariableAssignments() {
    ImmutableList.Builder<CStatement> builder = ImmutableList.builder();

    for (Entry<CExpression, CVariableDeclaration> relevantVariable : relevantVariables.entrySet()) {

      CExpression unprimedVariable = relevantVariable.getKey();
      CVariableDeclaration primedVariable = relevantVariable.getValue();
      CStatement assignment = createAssignmentStatement(primedVariable, unprimedVariable);

      builder.add(assignment);
    }

    return builder.build();
  }

  public List<CFAEdge> createPrimedVariableDeclarations(CFANode startLocation) {
    String function = startLocation.getFunctionName();
    logger.logf(
        FINEST,
        "Adding declarations of primed variables %s after %s in function %s.",
        MoreStrings.lazyString(
            () ->
                relevantVariables
                    .values()
                    .stream()
                    .map(AbstractSimpleDeclaration::getName)
                    .collect(Collectors.joining(" ,"))),
        startLocation,
        function);

    ImmutableList.Builder<CFAEdge> builder = ImmutableList.builder();
    CFANode currentNode = startLocation;

    for (CVariableDeclaration primedVariable : relevantVariables.values()) {
      CFANode nextNode = creatCfaNode(function);
      CFAEdge edge = createDeclarationEdge(primedVariable, currentNode, nextNode);
      builder.add(edge);
      currentNode = nextNode;
    }

    // blank edge back to original CFA node
    CFAEdge edge = createBlankEdge(currentNode, startLocation, "");
    builder.add(edge);

    return builder.build();
  }

  public CFAEdge createEdgeToNonTerminationLabel(CFANode pLocation) {
    Preconditions.checkState(targetNode.isPresent());
    return createBlankEdge(pLocation, targetNode.get(), "Label: " + NON_TERMINATION_LABEL);
  }

  public CFAEdge createNegatedRankingRelationAssumeEdgeToTargetNode(CFANode pLoopHead) {
    Preconditions.checkState(targetNode.isPresent());
    return createRankingRelationAssumeEdge(pLoopHead, targetNode.get(), false);
  }

  private CFANode creatCfaNode(String functionName) {
    return new CFANode(functionName);
  }

  private CExpressionAssignmentStatement createAssignmentStatement(
      CSimpleDeclaration pLeftHandSide, CExpression pRightHandSide) {
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY, new CIdExpression(FileLocation.DUMMY, pLeftHandSide), pRightHandSide);
  }

  public BlankEdge createBlankEdge(CFANode pPredecessor, CFANode pSuccessor, String pDescription) {
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

  private CDeclarationEdge createDeclarationEdge(
      CDeclaration declaration, CFANode predecessor, CFANode successor) {
    CDeclarationEdge edge =
        new CDeclarationEdge(declaration.toASTString(), DUMMY, predecessor, successor, declaration);
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
  public void resetCfa() {
    createdCfaEdges.forEach(CFACreationUtils::removeEdgeFromNodes);
    createdCfaEdges.clear();
  }
}

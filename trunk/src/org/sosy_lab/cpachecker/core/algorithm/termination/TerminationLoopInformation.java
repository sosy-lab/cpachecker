// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static java.util.logging.Level.FINEST;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
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

/**
 * {@link TerminationLoopInformation} is used by the {@link TerminationAlgorithm} to store
 * information about the termination analysis of a {@link Loop}. {@link TerminationTransferRelation}
 * and {@link TerminationARGPath} use this information to instrument the analyzed program.
 */
@NotThreadSafe
public class TerminationLoopInformation {

  private static final String NON_TERMINATION_LABEL = "__CPACHECKER_NON_TERMINATION";

  /** The loop that is currently analyzed by the {@link TerminationAlgorithm} */
  private Optional<Loop> loop = Optional.empty();

  /**
   * All locations after an outgoing edge of the loop currently processed or an empty set. Needs to
   * be set before modifying the {@link CFA}!
   *
   * @see Loop#getOutgoingEdges()
   */
  private Set<CFANode> loopLeavingLocations = ImmutableSet.of();

  /**
   * All outgoing edges of the loop currently processed or an empty set. Needs to be set before
   * modifying the {@link CFA}!
   *
   * @see Loop#getOutgoingEdges()
   */
  private Set<CFAEdge> loopLeavingEdges = ImmutableSet.of();

  /** The current ranking relation. */
  private Optional<RankingRelation> rankingRelation = Optional.empty();

  /** Mapping of relevant variables to the corresponding primed variable. */
  private Map<CExpression, CVariableDeclaration> relevantVariables = ImmutableMap.of();

  // reusing of intermediate location is required to build counter examples
  private List<CFANode> relevantVariablesInitializationIntermediateLocations = ImmutableList.of();

  private Set<CFAEdge> createdCfaEdges = new LinkedHashSet<>();

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

  public boolean isPredecessorOfIncomingEdge(CFANode pLocation) {
    return loop.isPresent()
        && leavingEdges(pLocation)
            .anyMatch(edge -> loop.orElseThrow().getIncomingEdges().contains(edge));
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
   * @param pRankingRelation the new {@link RankingRelation}
   */
  void addRankingRelation(RankingRelation pRankingRelation) {
    rankingRelation =
        Optional.of(rankingRelation.map(r -> r.merge(pRankingRelation)).orElse(pRankingRelation));
  }

  /**
   * Sets the loop to check for non-termination. Resets all other stored data.
   *
   * @param pLoop the loop to process
   * @param pRelevantVariables all variables that might be relevant to prove (non-)termination of
   *     the given loop.
   */
  void setProcessedLoop(Loop pLoop, Set<CVariableDeclaration> pRelevantVariables) {
    loop = Optional.of(pLoop);
    loopLeavingLocations =
        transformedImmutableSetCopy(pLoop.getOutgoingEdges(), CFAEdge::getSuccessor);
    loopLeavingEdges = ImmutableSet.copyOf(pLoop.getOutgoingEdges());
    resetRankingRelation();

    AFunctionDeclaration functionName = pLoop.getLoopHeads().iterator().next().getFunction();
    ImmutableList.Builder<CFANode> intermediateStates = ImmutableList.builder();
    ImmutableMap.Builder<CExpression, CVariableDeclaration> builder = ImmutableMap.builder();

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
    targetNode = Optional.of(new CFALabelNode(functionName, NON_TERMINATION_LABEL));

    relevantVariablesInitializationIntermediateLocations = intermediateStates.build();
    relevantVariables = builder.buildOrThrow();
  }

  /** Reset the {@link TerminationLoopInformation}. No loop will be checked for non-termination. */
  void reset() {
    loop = Optional.empty();
    loopLeavingLocations = ImmutableSet.of();
    loopLeavingEdges = ImmutableSet.of();
    relevantVariables = ImmutableMap.of();
    relevantVariablesInitializationIntermediateLocations = ImmutableList.of();
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
    AFunctionDeclaration function = startLocation.getFunction();
    logger.logf(
        FINEST,
        "Adding declarations of primed variables %s after %s in function %s.",
        MoreStrings.lazyString(
            () ->
                relevantVariables.values().stream()
                    .map(AbstractSimpleDeclaration::getName)
                    .collect(Collectors.joining(" ,"))),
        startLocation,
        function);

    ImmutableList.Builder<CFAEdge> builder = ImmutableList.builder();
    CFANode currentNode = startLocation;

    for (CVariableDeclaration primedVariable : relevantVariables.values()) {
      CFANode nextNode = createCfaNode(function);
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
    return createBlankEdge(pLocation, targetNode.orElseThrow(), "Label: " + NON_TERMINATION_LABEL);
  }

  public CFAEdge createNegatedRankingRelationAssumeEdgeToTargetNode(CFANode pLoopHead) {
    Preconditions.checkState(targetNode.isPresent());
    return createRankingRelationAssumeEdge(pLoopHead, targetNode.orElseThrow(), false);
  }

  private CFANode createCfaNode(AFunctionDeclaration pFunction) {
    return new CFANode(pFunction);
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

  /** Removes all temporarily added {@link CFAEdge}s from the CFA. */
  public void resetCfa() {
    createdCfaEdges.forEach(CFACreationUtils::removeEdgeFromNodes);
    createdCfaEdges.clear();
  }
}

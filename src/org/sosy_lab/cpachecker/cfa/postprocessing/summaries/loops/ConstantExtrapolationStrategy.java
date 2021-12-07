// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.visitors.ReplaceVariablesVisitor;
import org.sosy_lab.cpachecker.cfa.ast.visitors.VariableCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class ConstantExtrapolationStrategy extends AbstractLoopExtrapolationStrategy {

  private StrategiesEnum strategyEnum;

  public ConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);

    this.strategyEnum = StrategiesEnum.LoopConstantExtrapolation;
  }

  protected Optional<GhostCFA> summarizeLoop(
      AExpression pIterations,
      AExpression pLoopBoundExpression,
      Loop pLoopStructure,
      CFANode pBeforeWhile) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    startNodeGhostCFA.connectTo(startUnrolledLoopNode);

    CFANode currentSummaryNodeCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            endUnrolledLoopNode,
            currentSummaryNodeCFA,
            (CExpression) pLoopBoundExpression,
            true); // TODO: this may not be the correct way to do this; Review
    loopBoundCFAEdge.connect();

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(endUnrolledLoopNode, endNodeGhostCFA);
    negatedBoundCFAEdge.connect();

    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());


    // To evade race conditions when the loop bound variables are updated before the
    // rest. The solution is to init new Variables and set them to the original value and replace
    // the new ones into the iterations Expression
    VariableCollectorVisitor<Exception> variableCollectorVisitor = new VariableCollectorVisitor<>();

    Set<AVariableDeclaration> modifiedVariablesLocal;

    try {
      modifiedVariablesLocal = pIterations.accept_(variableCollectorVisitor);
    } catch (Exception e) {
      return Optional.empty();
    }

    Map<AVariableDeclaration, AVariableDeclaration> mappingFromOriginalToTmpVariables =
        new HashMap<>();

    for (AVariableDeclaration var : modifiedVariablesLocal) {

      if (!(var instanceof CVariableDeclaration)) {
        return Optional.empty();
      }

      // First create the new variable
      CVariableDeclaration newVariable =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              ((CVariableDeclaration) var).getCStorageClass(),
              (CType) var.getType(),
              var.getName() + "TmpVariableForLoopBoundary",
              var.getOrigName() + "TmpVariableForLoopBoundary",
              var.getQualifiedName() + "TmpVariableForLoopBoundary",
              null);

      mappingFromOriginalToTmpVariables.put(var, newVariable);

      CFAEdge varInitEdge =
          new CDeclarationEdge(
              newVariable.toString(),
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode,
              newVariable);
      varInitEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      // Then create a new Variable based on the value of the old variable
      CIdExpression oldVariableAsExpression =
          new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) var);

      CExpressionAssignmentStatement assignmentExpression =
          (CExpressionAssignmentStatement)
              new AExpressionFactory(oldVariableAsExpression).assignTo(newVariable);

      CFAEdge dummyEdge =
          new CStatementEdge(
              assignmentExpression.toString(),
              assignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Transform the iterations by replacing the Variables
    ReplaceVariablesVisitor<Exception> replaceVariablesVisitor =
        new ReplaceVariablesVisitor<>(mappingFromOriginalToTmpVariables);
    AExpression transformedIterations;
    try {
      transformedIterations = pIterations.accept_(replaceVariablesVisitor);
    } catch (Exception e) {
      return Optional.empty();
    }

    // Make Summary of Loop
    for (AVariableDeclaration var : pLoopStructure.getModifiedVariables()) {
      Optional<Integer> deltaMaybe = pLoopStructure.getDelta(var.getQualifiedName());
      if (deltaMaybe.isEmpty()) {
        return Optional.empty();
      }

      Integer delta = deltaMaybe.orElseThrow();

      CExpressionAssignmentStatement assignmentExpression =
          (CExpressionAssignmentStatement)
              new AExpressionFactory(transformedIterations)
                  .binaryOperation(
                      Integer.valueOf(1),
                      new CSimpleType(
                          false,
                          false,
                          CBasicType.INT,
                          true,
                          false,
                          true,
                          false,
                          false,
                          false,
                          false),
                      CBinaryExpression.BinaryOperator.MINUS)
                  .binaryOperation(
                      Integer.valueOf(delta),
                      new CSimpleType(
                          false,
                          false,
                          CBasicType.INT,
                          true,
                          false,
                          true,
                          false,
                          false,
                          false,
                          false),
                      CBinaryExpression.BinaryOperator.MULTIPLY)
                  .binaryOperation(
                      new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) var),
                      CBinaryExpression.BinaryOperator.PLUS)
                  .assignTo(var);

      CFAEdge dummyEdge =
          new CStatementEdge(
              assignmentExpression.toString(),
              assignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Unroll Loop Once again

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();
    currentSummaryNodeCFA.connectTo(startUnrolledLoopNode);

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode secondStartUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode secondEndUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    endUnrolledLoopNode.connectTo(secondStartUnrolledNode);
    secondEndUnrolledNode.connectTo(endNodeGhostCFA);

    CFAEdge leavingEdge;
    Iterator<CFAEdge> iter =
        pLoopStructure.getOutgoingEdges().iterator();
    if (iter.hasNext()) {
      leavingEdge = iter.next();
      if (iter.hasNext()) {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            pBeforeWhile,
            leavingEdge.getSuccessor(),
            this.strategyEnum));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode beforeWhile) {

    List<CFAEdge> filteredOutgoingEdges =
        this.summaryFilter.getEdgesForStrategies(beforeWhile.getLeavingEdges(), new HashSet<>(Arrays.asList(StrategiesEnum.Base, this.strategyEnum)));

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!filteredOutgoingEdges.get(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNode = filteredOutgoingEdges.get(0).getSuccessor();

    Optional<Loop> loopStructureMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopStructureMaybe.isEmpty()) {
      return Optional.empty();
    }
    Loop loopStructure = loopStructureMaybe.orElseThrow();

    if (!loopStructure.onlyConstantVarModification()) {
      return Optional.empty();
    }

    Optional<AExpression> loopBoundExpressionMaybe = loopStructure.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Optional<AExpression> iterationsMaybe = this.loopIterations(loopBoundExpression, loopStructure);

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }

    AExpression iterations = iterationsMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(iterations, loopBoundExpression, loopStructure, beforeWhile);

    return summarizedLoopMaybe;

  }
}

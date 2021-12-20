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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.factories.TypeFactory;
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
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
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

  protected Optional<Pair<CFANode, AVariableDeclaration>> createIterationsVariable(
      CFANode pStartNode, AExpression pIterations, CFANode pBeforeWhile) {
    // Overflows occur since the iterations calculation variables do not have the correct type.
    // Because of this new Variables with more general types are introduced in order to not have
    // this deficiency
    CFANode currentSummaryNode = pStartNode;
    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

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
              (CType) TypeFactory.getBiggestType(var.getType()),
              var.getName() + "TmpVariableReallyTmp",
              var.getOrigName() + "TmpVariableReallyTmp",
              var.getQualifiedName() + "TmpVariableReallyTmp",
              null);

      mappingFromOriginalToTmpVariables.put(var, newVariable);

      CFAEdge varInitEdge =
          new CDeclarationEdge(
              newVariable.toString(),
              FileLocation.DUMMY,
              currentSummaryNode,
              nextSummaryNode,
              newVariable);
      varInitEdge.connect();

      currentSummaryNode = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      // Then create a new Variable based on the value of the old variable
      CIdExpression oldVariableAsExpression =
          new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) var);

      AExpressionFactory expressionFactory = new AExpressionFactory();
      CExpressionAssignmentStatement assignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory.from(oldVariableAsExpression).assignTo(newVariable);

      CFAEdge dummyEdge =
          new CStatementEdge(
              assignmentExpression.toString(),
              assignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNode,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNode = nextSummaryNode;
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

    CVariableDeclaration iterationsVariable =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            new CSimpleType(
                false, false, CBasicType.INT, false, false, false, false, false, false, true),
            "iterationsTmpVariableForLoopBoundary",
            "iterationsTmpVariableForLoopBoundary",
            pBeforeWhile.getFunctionName() + "::iterationsTmpVariableForLoopBoundary",
            null);

    CFAEdge varInitEdge =
        new CDeclarationEdge(
            iterationsVariable.toString(),
            FileLocation.DUMMY,
            currentSummaryNode,
            nextSummaryNode,
            iterationsVariable);
    varInitEdge.connect();

    currentSummaryNode = nextSummaryNode;
    nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Set the iterations to the new Variable

    CExpressionAssignmentStatement iterationVariableAssignmentExpression =
        (CExpressionAssignmentStatement)
            new AExpressionFactory(transformedIterations).assignTo(iterationsVariable);

    CFAEdge assignmentIterationsVariableEdge =
        new CStatementEdge(
            iterationVariableAssignmentExpression.toString(),
            iterationVariableAssignmentExpression,
            FileLocation.DUMMY,
            currentSummaryNode,
            nextSummaryNode);
    assignmentIterationsVariableEdge.connect();

    currentSummaryNode = nextSummaryNode;

    return Optional.of(Pair.of(currentSummaryNode, iterationsVariable));
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


    Optional<Pair<CFANode, AVariableDeclaration>> nextNodeAndIterationsVariable =
        createIterationsVariable(currentSummaryNodeCFA, pIterations, pBeforeWhile);

    if (nextNodeAndIterationsVariable.isEmpty()) {
      return Optional.empty();
    }

    currentSummaryNodeCFA = nextNodeAndIterationsVariable.get().getFirst();
    AVariableDeclaration iterationsVariable = nextNodeAndIterationsVariable.get().getSecond();

    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Make Summary of Loop
    for (AVariableDeclaration var : pLoopStructure.getModifiedVariables()) {
      Optional<Integer> deltaMaybe = pLoopStructure.getDelta(var.getQualifiedName());
      if (deltaMaybe.isEmpty()) {
        return Optional.empty();
      }

      // Create a new tmp variable in order for the overflow check to work
      CVariableDeclaration newVariableForOverflows =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              (CType) var.getType(),
              var.getName() + "TmpVariableReallyReallyTmp",
              var.getOrigName() + "TmpVariableReallyReallyTmp",
              var.getQualifiedName() + "::TmpVariableReallyReallyTmp",
              null);

      CFAEdge newVarInitEdge =
          new CDeclarationEdge(
              newVariableForOverflows.toString(),
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode,
              newVariableForOverflows);
      newVarInitEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      AExpressionFactory expressionFactory = new AExpressionFactory();
      CExpressionAssignmentStatement newVariableAssignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory.from(var).assignTo(newVariableForOverflows);

      CFAEdge dummyEdge =
          new CStatementEdge(
              newVariableAssignmentExpression.toString(),
              newVariableAssignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      // Make the extrapolation
      Integer delta = deltaMaybe.orElseThrow();

      CExpression leftHandSide =
          (CExpression)
              new AExpressionFactory()
                  .from(iterationsVariable)
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
                      new CIdExpression(FileLocation.DUMMY, newVariableForOverflows),
                      CBinaryExpression.BinaryOperator.PLUS)
                  .build();

      CExpressionAssignmentStatement assignmentExpressionExtrapolation =
          (CExpressionAssignmentStatement)
              new AExpressionFactory().from(leftHandSide).assignTo(var);

      CFAEdge extrapolationDummyEdge =
          new CStatementEdge(
              assignmentExpressionExtrapolation.toString(),
              assignmentExpressionExtrapolation,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      extrapolationDummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      // Since the formula for checking for an overflow explicitly is very expensive
      // we add an if statement to the CFA, which checkss if the possibility of an overflow exists.
      // If it does, the statement is executed in order to explicitly find the overflow
      // TODO

      // Make a statement in order to check for an overflow
      // INT_MAX + (  ((int)(x + incr)) == x + incr  )  to raise the overflow if it happens, since
      // the c standard implicitly calculates modulo when a long is assigned to an int

      CExpression overflowExpression =
          (CExpression)
              new AExpressionFactory()
                  .from(var)
                  .binaryOperation(leftHandSide, BinaryOperator.NOT_EQUALS)
                  .binaryOperation(
                      TypeFactory.getUpperLimit(var.getType()),
                      var.getType(),
                      var.getType(),
                      BinaryOperator.PLUS)
                  .build();

      CFAEdge overflowCheckEdge =
          new CStatementEdge(
              overflowExpression.toString(),
              new CExpressionStatement(FileLocation.DUMMY, overflowExpression),
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      overflowCheckEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Unroll Loop two times

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

    if (!loopStructure.hasOnlyConstantVariableModifications()
        || loopStructure.amountOfInnerStatementEdges() != 1) {
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

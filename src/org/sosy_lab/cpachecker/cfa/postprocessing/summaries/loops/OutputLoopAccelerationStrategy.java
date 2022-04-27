// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.factories.AFunctionFactory;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class OutputLoopAccelerationStrategy extends LoopStrategy {

  private StrategiesEnum strategyEnum;

  public OutputLoopAccelerationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);

    this.strategyEnum = StrategiesEnum.OUTPUTLOOPACCELERATION;
  }

  @SuppressWarnings("unused")
  private Optional<GhostCFA> summarizeLoop(
      Loop pLoopStructure,
      Set<AVariableDeclaration> pModifiedVariables,
      Set<AVariableDeclaration> pReadWriteVariables,
      CFANode pBeforeWhile,
      AExpression pLoopBoundExpression) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode neverReturnNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode currentNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdgeTrue =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            startNodeGhostCFA,
            currentNode,
            (CExpression) pLoopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeTrue);

    CAssumeEdge loopBoundCFAEdgeFalse =
        ((CAssumeEdge) loopBoundCFAEdgeTrue).negate().copyWith(startNodeGhostCFA, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeFalse);

    String counterVariableName =
        "ThisIsACounterSpecificOnlyForSummaryStrategiesDoNotUseInNormalCode";

    Integer amountOfIterations = pModifiedVariables.size();

    // Init counter Variable

    CVariableDeclaration counterVariableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CNumericTypes.LONG_LONG_INT,
            counterVariableName,
            counterVariableName,
            pBeforeWhile.getFunctionName() + "::" + counterVariableName,
            null);

    CExpressionAssignmentStatement counterVariableInit =
        (CExpressionAssignmentStatement)
            new AExpressionFactory().from(0, INTTYPE).assignTo(counterVariableDeclaration);

    CFAEdge counterInitEdge =
        new CStatementEdge(
            counterVariableInit.toString(),
            counterVariableInit,
            FileLocation.DUMMY,
            currentNode,
            newNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(counterInitEdge);

    // Update nodes
    currentNode = newNode;
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode startOfForLoop = currentNode;

    // If statements for entering the for loop
    CFAEdge counterLessThanOutputVariablesTrue =
        new CAssumeEdge(
            "i < |Output Variables|",
            FileLocation.DUMMY,
            startOfForLoop,
            newNode,
            (CExpression)
                new AExpressionFactory()
                    .from(pModifiedVariables.size(), INTTYPE)
                    .binaryOperation(
                        counterVariableDeclaration, CBinaryExpression.BinaryOperator.GREATER_THAN)
                    .build(),
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(counterLessThanOutputVariablesTrue);

    CFANode afterForLoopNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge counterLessThanOutputVariablesFalse =
        ((CAssumeEdge) counterLessThanOutputVariablesTrue)
            .negate()
            .copyWith(startOfForLoop, afterForLoopNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(counterLessThanOutputVariablesFalse);

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeTrue.copyWith(afterForLoopNode, neverReturnNode));

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeFalse.copyWith(afterForLoopNode, endNodeGhostCFA));

    currentNode = newNode;
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeTrue.copyWith(currentNode, newNode));

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeFalse.copyWith(currentNode, endNodeGhostCFA));

    currentNode = newNode;
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Havoc all IO Variables
    for (AVariableDeclaration var : pReadWriteVariables) {
      CFunctionCallExpression havocFunction =
          (CFunctionCallExpression) new AFunctionFactory().callNondetFunction(var.getType());
      if (havocFunction == null) {
        return Optional.empty();
      }

      // TODO improve for Java
      CFunctionCallAssignmentStatement havocAssignment =
          new CFunctionCallAssignmentStatement(
              FileLocation.DUMMY,
              new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) var),
              havocFunction);

      CFAEdge dummyEdge =
          new CStatementEdge(
              var.getName() + " = NONDET",
              havocAssignment,
              FileLocation.DUMMY,
              currentNode,
              newNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);
      currentNode = newNode;
      newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Assume the loop Bound Condition
    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeTrue.copyWith(currentNode, newNode));

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        loopBoundCFAEdgeFalse.copyWith(currentNode, neverReturnNode));

    currentNode = newNode;
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Go through the Loop Body Once

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(currentNode, startUnrolledLoopNode);
    currentNode = newNode;
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFACreationUtils.connectNodes(endUnrolledLoopNode, currentNode);

    // Increment Counter

    CExpressionAssignmentStatement incrementCounterExpression =
        (CExpressionAssignmentStatement)
            new AExpressionFactory()
                .from(1, INTTYPE)
                .binaryOperation(counterVariableDeclaration, BinaryOperator.PLUS)
                .assignTo(counterVariableDeclaration);

    CFAEdge counterIncrementEdge =
        new CStatementEdge(
            incrementCounterExpression.toString(),
            incrementCounterExpression,
            FileLocation.DUMMY,
            currentNode,
            startOfForLoop);
    CFACreationUtils.addEdgeUnconditionallyToCFA(counterIncrementEdge);

    CFAEdge leavingEdge;
    Iterator<CFAEdge> iter = pLoopStructure.getOutgoingEdges().iterator();
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
        this.summaryFilter.getEdgesForStrategies(
            beforeWhile.getLeavingEdges(),
            new HashSet<>(Arrays.asList(StrategiesEnum.BASE, this.strategyEnum)));

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

    // Function calls may change global variables, or have assert statements, which cannot be
    // summarized correctly
    if (loopStructure.containsUserDefinedFunctionCalls()) {
      return Optional.empty();
    }

    Set<AVariableDeclaration> modifiedVariables = loopStructure.getModifiedVariables();
    Set<AVariableDeclaration> readVariables = loopStructure.getReadVariables();
    Set<AVariableDeclaration> readWriteVariables = new HashSet<>(modifiedVariables);
    readWriteVariables.retainAll(readVariables);

    Optional<AExpression> loopBoundExpressionMaybe = loopStructure.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(
            loopStructure, modifiedVariables, readWriteVariables, beforeWhile, loopBoundExpression);

    return summarizedLoopMaybe;
  }
}

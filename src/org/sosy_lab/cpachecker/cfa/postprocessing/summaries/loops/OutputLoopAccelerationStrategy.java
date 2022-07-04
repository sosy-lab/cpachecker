// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This strategy reassembles output abstraction as described in section II.B of "Over-approximating
 * loops to prove properties using bounded model checking" (https://doi.org/10.7873/DATE.2015.0245)
 */
public class OutputLoopAccelerationStrategy extends LoopStrategy {

  // counter name for output acceleration
  private static final String SUMMARY_COUNTER_VARIABLE = "__VERIFIER_LA_OA_counter";

  public OutputLoopAccelerationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.OUTPUTLOOPACCELERATION,
        pCFA);
  }

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

    String counterVariableName = SUMMARY_COUNTER_VARIABLE;

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
            new AExpressionFactory().from(0, SIGNED_LONG_INT).assignTo(counterVariableDeclaration);

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
    startOfForLoop.setLoopStart();

    // If statements for entering the for loop
    CFAEdge counterLessThanOutputVariablesTrue =
        new CAssumeEdge(
            "i < |Output Variables|",
            FileLocation.DUMMY,
            startOfForLoop,
            newNode,
            (CExpression)
                new AExpressionFactory()
                    .from(pModifiedVariables.size(), SIGNED_LONG_INT)
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

    // Go through the Loop Body Once

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(currentNode, startUnrolledLoopNode);
    newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFACreationUtils.connectNodes(endUnrolledLoopNode, newNode);
    currentNode = newNode;

    // Increment Counter

    CExpressionAssignmentStatement incrementCounterExpression =
        (CExpressionAssignmentStatement)
            new AExpressionFactory()
                .from(1, SIGNED_LONG_INT)
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
    Optional<CFANode> maybeLoopHead = this.determineLoopHead(beforeWhile);
    if (maybeLoopHead.isEmpty()) {
      return Optional.empty();
    }
    CFANode loopStartNode = maybeLoopHead.orElseThrow();

    Optional<Loop> loopMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopMaybe.isEmpty()) {
      return Optional.empty();
    }

    Loop loop = loopMaybe.orElseThrow();

    // Function calls may change global variables, or have assert statements, which cannot be
    // summarized correctly
    if (loop.containsUserDefinedFunctionCalls()) {
      return Optional.empty();
    }

    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    Set<AVariableDeclaration> readVariables = loop.getReadVariables();
    Set<AVariableDeclaration> readWriteVariables = new HashSet<>(modifiedVariables);
    readWriteVariables.retainAll(readVariables);
    readWriteVariables.retainAll(getModifiedNonLocalVariables(loop));

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(
            loop, modifiedVariables, readWriteVariables, beforeWhile, loopBoundExpression);

    return summarizedLoopMaybe;
  }

  public static Optional<String> summarizeAsCode(Loop loop) {
    StringBuilder builder = new StringBuilder();

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    Set<AVariableDeclaration> readVariables = loop.getReadVariables();
    Set<AVariableDeclaration> readWriteVariables = new HashSet<>(modifiedVariables);
    readWriteVariables.retainAll(readVariables);
    readWriteVariables.retainAll(getModifiedNonLocalVariables(loop));

    final String loopBoundExpressionString =
        ((CExpression) loopBoundExpression)
            .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER);
    builder.append(
        String.format(
            "for (long long %s = 0; %s < %d && %s ; %s++) {\n",
            SUMMARY_COUNTER_VARIABLE,
            SUMMARY_COUNTER_VARIABLE,
            modifiedVariables.size(),
            loopBoundExpressionString,
            SUMMARY_COUNTER_VARIABLE));
    if (!havocVarsAsCode(readWriteVariables, builder)) {
      return Optional.empty();
    }
    builder.append(String.format("if (!(%s)) abort();\n", loopBoundExpressionString));
    try {
      executeLoopBodyAsCode(loop, builder);
    } catch (IOException e) {
      return Optional.empty();
    }
    builder.append("}\n");
    builder.append(String.format("if (%s) abort();\n", loopBoundExpressionString));
    return Optional.of(builder.toString());
  }
}

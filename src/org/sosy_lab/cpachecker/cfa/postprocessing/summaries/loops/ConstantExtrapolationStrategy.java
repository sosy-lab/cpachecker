// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.LoopAbstractionExpressibleAsCode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class ConstantExtrapolationStrategy extends LoopExtrapolationStrategy
    implements LoopAbstractionExpressibleAsCode {

  public ConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.LOOPCONSTANTEXTRAPOLATION,
        pCFA);
  }

  public ConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      StrategiesEnum pStrategyEnum,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pStrategyEnum, pCFA);
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

    CFACreationUtils.connectNodes(startNodeGhostCFA, startUnrolledLoopNode);

    CFANode currentSummaryNodeCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            endUnrolledLoopNode,
            currentSummaryNodeCFA,
            (CExpression) pLoopBoundExpression,
            true); // TODO: this may not be the correct way to do this; Review
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdge);

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(endUnrolledLoopNode, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdge);

    Optional<CFANode> nextNode =
        initializeIterationsVariable(currentSummaryNodeCFA, pIterations, pBeforeWhile);

    if (nextNode.isEmpty()) {
      return Optional.empty();
    }

    currentSummaryNodeCFA = nextNode.orElseThrow();
    AVariableDeclaration iterationsVariable =
        createIterationsVariable(pBeforeWhile.getFunctionName());

    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Make Summary of Loop
    for (ASimpleDeclaration var : pLoopStructure.getModifiedVariables()) {
      if (!(var instanceof AVariableDeclaration)) {
        // TODO: Handle array lookups i.e. the other types of declarations
        return Optional.empty();
      }

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
              var.getName() + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX + nameCounter,
              var.getOrigName() + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX + nameCounter,
              var.getQualifiedName()
                  + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX
                  + nameCounter,
              null);

      CFAEdge newVarInitEdge =
          new CDeclarationEdge(
              newVariableForOverflows.toString(),
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode,
              newVariableForOverflows);
      CFACreationUtils.addEdgeUnconditionallyToCFA(newVarInitEdge);

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      AExpressionFactory expressionFactory = new AExpressionFactory();
      CExpressionAssignmentStatement newVariableAssignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory.from((AVariableDeclaration) var).assignTo(newVariableForOverflows);

      CFAEdge dummyEdge =
          new CStatementEdge(
              newVariableAssignmentExpression.toString(),
              newVariableAssignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

      // Make the extrapolation
      Integer delta = deltaMaybe.orElseThrow();

      CExpression leftHandSide =
          (CExpression)
              new AExpressionFactory()
                  .from(iterationsVariable)
                  .binaryOperation(
                      Integer.valueOf(1), SIGNED_LONG_INT, CBinaryExpression.BinaryOperator.MINUS)
                  .binaryOperation(
                      delta, SIGNED_LONG_INT, CBinaryExpression.BinaryOperator.MULTIPLY)
                  .binaryOperation(
                      new CIdExpression(FileLocation.DUMMY, newVariableForOverflows),
                      CBinaryExpression.BinaryOperator.PLUS)
                  .build();

      CExpressionAssignmentStatement assignmentExpressionExtrapolation =
          (CExpressionAssignmentStatement)
              new AExpressionFactory().from(leftHandSide).assignTo((AVariableDeclaration) var);

      CFAEdge extrapolationDummyEdge =
          new CStatementEdge(
              assignmentExpressionExtrapolation.toString(),
              assignmentExpressionExtrapolation,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(extrapolationDummyEdge);

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
                  .from((AVariableDeclaration) var)
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
      CFACreationUtils.addEdgeUnconditionallyToCFA(overflowCheckEdge);

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
    CFACreationUtils.connectNodes(currentSummaryNodeCFA, startUnrolledLoopNode);

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode secondStartUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode secondEndUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(endUnrolledLoopNode, secondStartUnrolledNode);
    CFACreationUtils.connectNodes(secondEndUnrolledNode, endNodeGhostCFA);

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
            this.strategyEnum,
            new ArrayList<>(),
            StrategyQualifier.Precise));
  }

  @Override
  public synchronized Optional<GhostCFA> summarize(final CFANode beforeWhile) {
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

    // TODO: support more general loop conditions!
    if (!hasOnlyConstantVariableModifications(loop) || loop.amountOfInnerAssumeEdges() != 1) {
      return Optional.empty();
    }

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    if (loop.containsFunctionCalls()) {
      return Optional.empty();
    }

    Optional<AExpression> iterationsMaybe = this.loopIterations(loopBoundExpression, loop);

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }

    AExpression iterations = iterationsMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(iterations, loopBoundExpression, loop, beforeWhile);

    this.nameCounter += 1;

    return summarizedLoopMaybe;
  }

  @Override
  protected boolean isSupportedLoop(Loop loop) {
    return super.isSupportedLoop(loop)
        && hasOnlyConstantVariableModifications(loop)
        && loop.amountOfInnerAssumeEdges() == 1;
  }

  @Override
  public synchronized Optional<String> summarizeAsCode(Loop loop) {

    if (!isSupportedLoop(loop) || !getClass().equals(ConstantExtrapolationStrategy.class)) {
      return Optional.empty();
    }

    StringBuilder builder = new StringBuilder();
    final String functionName =
        FluentIterable.from(loop.getLoopHeads()).first().get().getFunctionName();

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }

    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    builder.append(
        String.format(
            "if (%s) {\n",
            ((CExpression) loopBoundExpression)
                .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)));
    final Consumer<String> addLine =
        (str) -> {
          builder.append(String.format("  %s\n", str));
        };

    Optional<AExpression> iterationsMaybe = this.loopIterations(loopBoundExpression, loop);
    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression iterations = iterationsMaybe.orElseThrow();
    VariableCollectorVisitor variableCollectorVisitor = new VariableCollectorVisitor();

    // Start by unrolling the loop once, since sometimes
    // there are just plain assigments in the loop
    // for example x = 0
    try {
      executeLoopBodyAsCode(loop, builder);
    } catch (IOException e) {
      return Optional.empty();
    }

    Set<AVariableDeclaration> modifiedVariablesLocal = new HashSet<>();

    for (ASimpleDeclaration v : iterations.accept_(variableCollectorVisitor)) {
      if (v instanceof AVariableDeclaration) {
        modifiedVariablesLocal.add((AVariableDeclaration) v);
      } else {
        // TODO: Handle the other cases, for example when we are considering an array lookup
        return Optional.empty();
      }
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
              var.getOrigName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              var.getOrigName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              var.getQualifiedName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              null);
      addLine.accept(newVariable.toASTString());
      mappingFromOriginalToTmpVariables.put(var, newVariable);

      // Then create a new Variable based on the value of the old variable
      CIdExpression oldVariableAsExpression =
          new CIdExpression(
              FileLocation.DUMMY,
              ((CSimpleDeclaration) var).getType(),
              ((CSimpleDeclaration) var).getOrigName(),
              (CSimpleDeclaration) var);

      AExpressionFactory expressionFactory = new AExpressionFactory();
      CExpressionAssignmentStatement assignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory.from(oldVariableAsExpression).assignTo(newVariable);
      addLine.accept(assignmentExpression.toASTString());
    }

    // Transform the iterations by replacing the Variables
    ReplaceVariablesVisitor replaceVariablesVisitor =
        new ReplaceVariablesVisitor(mappingFromOriginalToTmpVariables);
    AExpression transformedIterations;
    transformedIterations = iterations.accept_(replaceVariablesVisitor);

    CVariableDeclaration iterationsVariable = createIterationsVariable(functionName);

    addLine.accept(iterationsVariable.toASTString());

    // Set the iterations to the new Variable

    CExpressionAssignmentStatement iterationVariableAssignmentExpression =
        (CExpressionAssignmentStatement)
            new AExpressionFactory(transformedIterations).assignTo(iterationsVariable);

    addLine.accept(iterationVariableAssignmentExpression.toASTString());

    // Make Summary of Loop
    for (ASimpleDeclaration var : loop.getModifiedVariables()) {
      Optional<Integer> deltaMaybe = loop.getDelta(var.getQualifiedName());
      if (deltaMaybe.isEmpty() || !(var instanceof CVariableDeclaration)) {
        return Optional.empty();
      }

      if (!(var instanceof AVariableDeclaration)) {
        // TODO: Handle array lookups i.e. the other types of declarations
        return Optional.empty();
      }

      // Create a new tmp variable in order for the overflow check to work
      CVariableDeclaration newVariableForOverflows =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              (CType) var.getType(),
              var.getOrigName() + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX + nameCounter,
              var.getOrigName() + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX + nameCounter,
              var.getQualifiedName()
                  + LoopExtrapolationStrategy.LA_TMP_OLD_VAR_PREFIX
                  + nameCounter,
              null);

      addLine.accept(newVariableForOverflows.toASTString());

      CVariableDeclaration origNameVar =
          new CVariableDeclaration(
              var.getFileLocation(),
              ((AVariableDeclaration) var).isGlobal(),
              ((CVariableDeclaration) var).getCStorageClass(),
              ((CVariableDeclaration) var).getType(),
              var.getOrigName(), // important
              var.getOrigName(),
              var.getQualifiedName(),
              null);
      AExpressionFactory expressionFactory = new AExpressionFactory();
      CExpressionAssignmentStatement newVariableAssignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory.from(origNameVar).assignTo(newVariableForOverflows);

      addLine.accept(newVariableAssignmentExpression.toASTString());

      // Make the extrapolation
      Integer delta = deltaMaybe.orElseThrow();

      CExpression leftHandSide =
          (CExpression)
              new AExpressionFactory()
                  .from(iterationsVariable)
                  .binaryOperation(
                      delta, SIGNED_LONG_INT, CBinaryExpression.BinaryOperator.MULTIPLY)
                  .binaryOperation(
                      new CIdExpression(FileLocation.DUMMY, newVariableForOverflows),
                      CBinaryExpression.BinaryOperator.PLUS)
                  .build();

      CExpressionAssignmentStatement assignmentExpressionExtrapolation =
          (CExpressionAssignmentStatement)
              new AExpressionFactory().from(leftHandSide).assignTo(origNameVar);
      addLine.accept(assignmentExpressionExtrapolation.toASTString());
    }

    // Unroll the loop once for safety's sake if the update per iteration is not one
    // this is to not have problems occur when the division on the extrapolation
    // is not exact and has a rest
    Optional<Integer> loopBoundAbsoluteChange =
        loopBoundDeltaAbsoluteValue(loopBoundExpression, loop);
    if (loopBoundAbsoluteChange.isPresent()) {
      if (loopBoundAbsoluteChange.orElseThrow() != 1) {
        try {
          executeLoopBodyAsCode(loop, builder);
        } catch (IOException e) {
          return Optional.empty();
        }
      }
    }

    builder.append("}\n");

    this.nameCounter += 1;

    return Optional.of(builder.toString());
  }
}

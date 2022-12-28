// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.factories.TypeFactory;
import org.sosy_lab.cpachecker.cfa.ast.visitors.LinearVariableDependencyVisitor;
import org.sosy_lab.cpachecker.cfa.ast.visitors.OnlyConstantVariableIncrementsVisitor;
import org.sosy_lab.cpachecker.cfa.ast.visitors.ReplaceVariablesVisitor;
import org.sosy_lab.cpachecker.cfa.ast.visitors.VariableCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LinearVariableDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopExtrapolationStrategy extends LoopStrategy {

  private static final String LA_TMP_ITERATION_VAR_PREFIX = "__VERIFIER_LA_iterations";
  protected static final String LA_TMP_VAR_PREFIX = "__VERIFIER_LA_tmp";
  protected static final String LA_TMP_OLD_VAR_PREFIX = "__VERIFIER_LA_old_tmp";

  // maps for caching the flags for each loop; not thread-safe
  // TODO: we compute this more than once if we use multiple (child) class instances
  // at once, so at some point we might want to optimize this (though this should not be relevant)
  private final Map<Loop, Boolean> onlyConstantVariableModifications = new HashMap<>();
  private final Map<Loop, Boolean> onlyLinearVariableModifications = new HashMap<>();

  protected Integer nameCounter = 0;

  protected LoopExtrapolationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      StrategiesEnum pStrategyEnum,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pStrategyEnum, pCFA);
  }

  /**
   * This method returns the Amount of iterations the loop will go through, if it is possible to
   * calculate this
   *
   * @param loopBoundExpression the expression of the loop while (EXPR) { something; }
   * @param loopStructure The loop structure which is being summarized
   */
  public Optional<AExpression> loopIterations(AExpression loopBoundExpression, Loop loopStructure) {
    // This expression is the amount of iterations given in symbols
    Optional<AExpression> iterationsMaybe = Optional.empty();
    // TODO For now it only works for c programs
    if (loopBoundExpression instanceof CBinaryExpression) {
      LoopVariableDeltaVisitor variableVisitor = new LoopVariableDeltaVisitor(loopStructure, true);

      CExpression operand1 = ((CBinaryExpression) loopBoundExpression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) loopBoundExpression).getOperand2();
      BinaryOperator operator = ((CBinaryExpression) loopBoundExpression).getOperator();

      Optional<Integer> operand1variableDelta;
      Optional<Integer> operand2variableDelta;
      operand1variableDelta = operand1.accept(variableVisitor);
      operand2variableDelta = operand2.accept(variableVisitor);

      if (operand1variableDelta.isPresent() && operand2variableDelta.isPresent()) {

        switch (operator) {
          case EQUALS:
            // Should iterate at most once if the Deltas are non zero
            // If the deltas are zero and the integer is zero this loop would not terminate
            // TODO: What do we do if the loop does not terminate?
            // TODO: this can be improved if the value of the variables is known.
            if (operand1variableDelta.orElseThrow() - operand2variableDelta.orElseThrow() != 0) {
              // Returning this works because for any number of iterations less than or equal to 2
              // The loop is simply unrolled. Since because of overflows no extrapolation can be
              // made
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory()
                          .from(Integer.valueOf(1), LoopStrategy.SIGNED_LONG_INT)
                          .build());
            }
            break;
          case GREATER_EQUAL:
            if (operand1variableDelta.orElseThrow() - operand2variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand1)
                          .binaryOperation(operand2, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(
                                  operand2variableDelta.orElseThrow()
                                      - operand1variableDelta.orElseThrow()),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.DIVIDE)
                          .build());
            }
            break;
          case GREATER_THAN:
            if (operand1variableDelta.orElseThrow() - operand2variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand1)
                          .binaryOperation(operand2, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(
                                  operand2variableDelta.orElseThrow()
                                      - operand1variableDelta.orElseThrow()),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.DIVIDE)
                          .binaryOperation(
                              Integer.valueOf(1),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.PLUS)
                          .build());
            }
            break;
          case LESS_EQUAL:
            if (operand2variableDelta.orElseThrow() - operand1variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand2)
                          .binaryOperation(operand1, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(
                                  operand1variableDelta.orElseThrow()
                                      - operand2variableDelta.orElseThrow()),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.DIVIDE)
                          .binaryOperation(
                              Integer.valueOf(1),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.PLUS)
                          .build());
            }
            break;
          case LESS_THAN:
            if (operand2variableDelta.orElseThrow() - operand1variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand2)
                          .binaryOperation(operand1, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(
                                  operand1variableDelta.orElseThrow()
                                      - operand2variableDelta.orElseThrow()),
                              LoopStrategy.SIGNED_LONG_INT,
                              CBinaryExpression.BinaryOperator.DIVIDE)
                          .build());
            }
            break;
          case NOT_EQUALS:
            // Should iterate at most once if the Deltas are zero
            // If the deltas are non zero and the integer is zero this loop could terminate, but
            // it is not known when this could happen
            // TODO: What do we do if the loop does not terminate?
            // TODO: this can be improved if the value of the variables is known.
            if (operand1variableDelta.orElseThrow() - operand2variableDelta.orElseThrow() == 0) {
              // Returning this works because for any number of iterations less than or equal to 2
              // The loop is simply unrolled. Since because of overflows no extrapolation can be
              // made
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory()
                          .from(Integer.valueOf(1), LoopStrategy.SIGNED_LONG_INT)
                          .build());
            }
            break;
          default:
            break;
        }
      }
    }
    return iterationsMaybe;
  }

  /**
   * This function creates new temporary variables, which are then replaced in the pIterations
   * expression. The goal behind this is to create a new variable where the amount of iterations is
   * contained, which is large enough to contain the amount of iterations, without generating an
   * overflow. If the amount of iterations is larger than a long long, this calculation will not be
   * correct, but in that case an overflow will probably happen anyway.
   *
   * @param pStartNode The start Node, from which the CFAEdges with the statements declaring the new
   *     variables should start
   * @param pIterations The expression in which new variables will replace the old variables in
   *     order to generate a new variable, which defines this new expression
   * @param pBeforeWhile The CFANode before the while statement. This is only needed in order for
   *     the CFANode's to be exported correctly, since the function name in which they are ocuring
   *     is present there. This may be refactored into a String containing only the function name.
   * @return If the new expression can be generated this function returns a pair of the end CFANode
   *     of the block where the declaration of the new variables are generated. The second element
   *     of the Pair is the Variable encoding the new expression.
   */
  protected Optional<CFANode> initializeIterationsVariable(
      CFANode pStartNode, AExpression pIterations, CFANode pBeforeWhile) {
    // Overflows occur since the iterations calculation variables do not have the correct type.
    // Because of this new Variables with more general types are introduced in order to not have
    // this deficiency
    CFANode currentSummaryNode = pStartNode;
    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    VariableCollectorVisitor variableCollectorVisitor = new VariableCollectorVisitor();

    Set<AVariableDeclaration> modifiedVariablesLocal = new HashSet<>();

    for (ASimpleDeclaration v : pIterations.accept_(variableCollectorVisitor)) {
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
              var.getName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              var.getOrigName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              var.getQualifiedName() + LA_TMP_VAR_PREFIX + this.nameCounter,
              null);

      mappingFromOriginalToTmpVariables.put(var, newVariable);

      CFAEdge varInitEdge =
          new CDeclarationEdge(
              newVariable.toString(),
              FileLocation.DUMMY,
              currentSummaryNode,
              nextSummaryNode,
              newVariable);
      CFACreationUtils.addEdgeUnconditionallyToCFA(varInitEdge);

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
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);

      currentSummaryNode = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Transform the iterations by replacing the Variables
    ReplaceVariablesVisitor replaceVariablesVisitor =
        new ReplaceVariablesVisitor(mappingFromOriginalToTmpVariables);
    AExpression transformedIterations;
    transformedIterations = pIterations.accept_(replaceVariablesVisitor);

    CVariableDeclaration iterationsVariable =
        createIterationsVariable(pBeforeWhile.getFunctionName());

    CFAEdge varInitEdge =
        new CDeclarationEdge(
            iterationsVariable.toString(),
            FileLocation.DUMMY,
            currentSummaryNode,
            nextSummaryNode,
            iterationsVariable);
    CFACreationUtils.addEdgeUnconditionallyToCFA(varInitEdge);

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
    CFACreationUtils.addEdgeUnconditionallyToCFA(assignmentIterationsVariableEdge);

    currentSummaryNode = nextSummaryNode;

    return Optional.of(currentSummaryNode);
  }

  protected CVariableDeclaration createIterationsVariable(String functionName) {

    CVariableDeclaration iterationsVariable =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            new CSimpleType(
                false, false, CBasicType.INT, false, false, false, false, false, false, true),
            LA_TMP_ITERATION_VAR_PREFIX + this.nameCounter,
            LA_TMP_ITERATION_VAR_PREFIX + this.nameCounter,
            functionName + "::" + LA_TMP_ITERATION_VAR_PREFIX + this.nameCounter,
            null);

    return iterationsVariable;
  }

  protected boolean hasOnlyConstantVariableModifications(Loop loop) {
    if (!onlyConstantVariableModifications.containsKey(loop)) {
      onlyConstantVariableModifications.put(loop, true);
      // Calculate the value if it is not present
      for (CFAEdge e : loop.getInnerLoopEdges()) {
        if (e instanceof AStatementEdge) {
          AStatementEdge stmtEdge = (AStatementEdge) e;
          if (stmtEdge.getStatement() instanceof AAssignment) {
            AAssignment assignment = (AAssignment) stmtEdge.getStatement();
            ALeftHandSide leftHandSide = assignment.getLeftHandSide();
            ARightHandSide rightHandSide = assignment.getRightHandSide();
            if (leftHandSide instanceof AIdExpression && rightHandSide instanceof AExpression) {
              OnlyConstantVariableIncrementsVisitor visitor =
                  new OnlyConstantVariableIncrementsVisitor(
                      Optional.of(
                          ImmutableSet.of(
                              (AVariableDeclaration)
                                  ((AIdExpression) leftHandSide).getDeclaration())));
              if (!((AExpression) rightHandSide).accept_(visitor)) {
                onlyConstantVariableModifications.put(loop, false);
                break;
              }
            }
          }
        }
      }
    }
    return onlyConstantVariableModifications.get(loop);
  }

  protected boolean hasOnlyLinearVariableModifications(Loop loop) {
    if (!onlyLinearVariableModifications.containsKey(loop)) {
      if (onlyConstantVariableModifications.containsKey(loop)
          && onlyConstantVariableModifications.get(loop)) {
        onlyLinearVariableModifications.put(loop, true);
        return true;
      }

      // Calculate the value if it is not present
      onlyLinearVariableModifications.put(loop, true);
      for (CFAEdge e : loop.getInnerLoopEdges()) {
        if (e instanceof AStatementEdge) {
          AStatementEdge stmtEdge = (AStatementEdge) e;
          if (stmtEdge.getStatement() instanceof AAssignment) {
            AAssignment assignment = (AAssignment) stmtEdge.getStatement();
            ALeftHandSide leftHandSide = assignment.getLeftHandSide();
            ARightHandSide rightHandSide = assignment.getRightHandSide();
            if (leftHandSide instanceof AIdExpression && rightHandSide instanceof AExpression) {
              LinearVariableDependencyVisitor visitor = new LinearVariableDependencyVisitor();
              Optional<LinearVariableDependency> valueOptional =
                  ((AExpression) rightHandSide).accept_(visitor);
              if (!valueOptional.isPresent()) {
                onlyLinearVariableModifications.put(loop, false);
                break;
              }
            }
          }
        }
      }
    }
    return onlyLinearVariableModifications.get(loop);
  }
}

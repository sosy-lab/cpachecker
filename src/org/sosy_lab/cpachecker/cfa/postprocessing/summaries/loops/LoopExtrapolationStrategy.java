// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.HashMap;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.factories.TypeFactory;
import org.sosy_lab.cpachecker.cfa.ast.visitors.ReplaceVariablesVisitor;
import org.sosy_lab.cpachecker.cfa.ast.visitors.VariableCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class LoopExtrapolationStrategy extends LoopStrategy {

  protected Integer nameCounter = 0;

  protected LoopExtrapolationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
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
      LoopVariableDeltaVisitor<Exception> variableVisitor =
          new LoopVariableDeltaVisitor<>(loopStructure, true);

      CExpression operand1 = ((CBinaryExpression) loopBoundExpression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) loopBoundExpression).getOperand2();
      BinaryOperator operator = ((CBinaryExpression) loopBoundExpression).getOperator();

      Optional<Integer> operand1variableDelta;
      Optional<Integer> operand2variableDelta;
      try {
        operand1variableDelta = operand1.accept(variableVisitor);
        operand2variableDelta = operand2.accept(variableVisitor);
      } catch (Exception e) {
        return Optional.empty();
      }

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
                          .from(
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
                                  false))
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
                              CBinaryExpression.BinaryOperator.DIVIDE)
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
                              CBinaryExpression.BinaryOperator.PLUS)
                          .build());
            }
            break;
          case LESS_EQUAL:
            if (operand2variableDelta.orElseThrow() - operand1variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand2)
                          .binaryOperation(
                              operand1, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(operand1variableDelta.orElseThrow()
                                  - operand2variableDelta.orElseThrow()), new CSimpleType(false, false, CBasicType.INT, true, false, true, false, false, false, false) , CBinaryExpression.BinaryOperator.DIVIDE)
                          .binaryOperation(
                              Integer.valueOf(1), new CSimpleType(false, false, CBasicType.INT, true, false, true, false, false, false, false) , CBinaryExpression.BinaryOperator.PLUS)
                          .build());
            }
            break;
          case LESS_THAN:
            if (operand2variableDelta.orElseThrow() - operand1variableDelta.orElseThrow() < 0) {
              iterationsMaybe =
                  Optional.of(
                      new AExpressionFactory(operand2)
                          .binaryOperation(
                              operand1, CBinaryExpression.BinaryOperator.MINUS)
                          .binaryOperation(
                              Integer.valueOf(operand1variableDelta.orElseThrow()
                                  - operand2variableDelta.orElseThrow()), new CSimpleType(false, false, CBasicType.INT, true, false, true, false, false, false, false) , CBinaryExpression.BinaryOperator.DIVIDE)
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
                  Optional.of(new AExpressionFactory().from(Integer.valueOf(1), new CSimpleType(false, false, CBasicType.INT, true, false, true, false, false, false, false)).build());
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
              var.getName() + "TmpVariableReallyTmp" + this.nameCounter,
              var.getOrigName() + "TmpVariableReallyTmp" + this.nameCounter,
              var.getQualifiedName() + "TmpVariableReallyTmp" + this.nameCounter,
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
            "iterationsTmpVariableForLoopBoundary" + this.nameCounter,
            "iterationsTmpVariableForLoopBoundary" + this.nameCounter,
            pBeforeWhile.getFunctionName()
                + "::iterationsTmpVariableForLoopBoundary"
                + this.nameCounter,
            null);

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

    return Optional.of(Pair.of(currentSummaryNode, iterationsVariable));
  }

  @Override
  public boolean isPrecise() {
    return true;
  }
}

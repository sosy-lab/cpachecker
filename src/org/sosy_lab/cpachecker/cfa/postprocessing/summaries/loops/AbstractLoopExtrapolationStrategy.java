// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class AbstractLoopExtrapolationStrategy extends AbstractLoopStrategy {
  protected AbstractLoopExtrapolationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
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

  @Override
  public boolean isPrecise() {
    return true;
  }
}

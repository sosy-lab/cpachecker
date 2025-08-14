// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class TaintAnalysisUtils {

  /**
   * Collects all variables present in the given pExpression and returns them as {@link
   * CIdExpression}
   *
   * @param pExpression to collect in
   * @return the {@link CIdExpression} present
   */
  public static Set<CIdExpression> getAllVarsAsCExpr(CExpression pExpression) {
    // Initialize and invoke the visitor
    CollectCIdExpressionsVisitor visitor = new CollectCIdExpressionsVisitor();
    return pExpression.accept(visitor);
  }

  public static Collection<TaintAnalysisState> getStatesWithSingeValueMapping(
      TaintAnalysisState joinState) {

    Map<CIdExpression, List<CExpression>> evaluatedValuesWithMultipleMapping =
        joinState.getEvaluatedValues();

    int numberOfMergedStates = 0;

    if (!evaluatedValuesWithMultipleMapping.isEmpty()) {
      numberOfMergedStates = evaluatedValuesWithMultipleMapping.values().iterator().next().size();
    }

    if (numberOfMergedStates <= 1) {
      return ImmutableList.of(joinState);
    }

    Collection<TaintAnalysisState> originalStates = new HashSet<>();

    if (numberOfMergedStates == 2) {

      originalStates.addAll(joinState.getPredecessors());
    } else {

      for (TaintAnalysisState predecessor : joinState.getPredecessors()) {
        originalStates.addAll(getStatesWithSingeValueMapping(predecessor));
      }
    }

    return updateTaintForStates(originalStates, joinState);
  }

  private static Collection<TaintAnalysisState> updateTaintForStates(
      Collection<TaintAnalysisState> statesWithOutdatedTaint,
      TaintAnalysisState stateWithUpdatedTaint) {

    Collection<TaintAnalysisState> updatedStates = new HashSet<>();

    for (TaintAnalysisState outdatedState : statesWithOutdatedTaint) {

      Set<CIdExpression> taintedVariables =
          new HashSet<>(stateWithUpdatedTaint.getTaintedVariables());
      taintedVariables.removeIf(var -> !outdatedState.getEvaluatedValues().containsKey(var));

      Set<CIdExpression> untaintedVariables =
          new HashSet<>(stateWithUpdatedTaint.getUntaintedVariables());
      untaintedVariables.removeIf(var -> !outdatedState.getEvaluatedValues().containsKey(var));

      TaintAnalysisState updatedState =
          new TaintAnalysisState(
              taintedVariables,
              untaintedVariables,
              outdatedState.getEvaluatedValues(),
              ImmutableSet.of(stateWithUpdatedTaint));

      updatedState
          .getNonTrivialPathStartStates()
          .removeIf(
              inheritedPathStartState ->
                  !inheritedPathStartState.isContainedIn(
                      outdatedState.getNonTrivialPathStartStates()));

      if (outdatedState.isPathStart()
          && !outdatedState.isContainedIn(updatedState.getNonTrivialPathStartStates())) {

        updatedState.getNonTrivialPathStartStates().add(outdatedState);
      }

      updatedStates.add(updatedState);
    }

    return updatedStates;
  }

  public static CIdExpression getCidExpressionForCVarDec(CVariableDeclaration pDec) {
    return new CIdExpression(pDec.getFileLocation(), pDec);
  }

  public static CIdExpression getCidExpressionForCParDec(CParameterDeclaration pDec) {
    return new CIdExpression(pDec.getFileLocation(), pDec);
  }

  public static int evaluateExpressionToInteger(CExpression expression)
      throws CPATransferException {
    if (expression instanceof CIntegerLiteralExpression integerLiteral) {
      BigInteger value = integerLiteral.getValue();
      if (value.equals(BigInteger.ZERO) || value.equals(BigInteger.ONE)) {
        return value.intValue();
      } else {
        throw new CPATransferException(
            "Invalid taint assertion: Expected either 0 (not tainted) or 1 (tainted), but got "
                + value);
      }
    }
    throw new CPATransferException(
        "Invalid taint assertion: Second parameter must be an integer literal (0 or 1).");
  }

  @Nullable
  public static CExpression evaluateExpression(
      CExpression expression, Map<CIdExpression, CExpression> evaluatedValues) {

    // TODO: Use visitor (?)
    if (expression instanceof CLiteralExpression literalExpression) {

      // Base case: the expression is already a literal
      return literalExpression;

    } else if (expression instanceof CIdExpression idExpression) {

      // Look up the value of the variable in the tainted and untainted maps.
      return evaluateExpression(evaluatedValues.getOrDefault(idExpression, null), evaluatedValues);

    } else if (expression instanceof CBinaryExpression binaryExpression) {

      // Evaluate recursively
      CLiteralExpression operand1 =
          (CLiteralExpression) evaluateExpression(binaryExpression.getOperand1(), evaluatedValues);

      CLiteralExpression operand2 =
          (CLiteralExpression) evaluateExpression(binaryExpression.getOperand2(), evaluatedValues);

      return computeBinaryOperation(operand1, operand2, binaryExpression.getOperator());

    } else if (expression instanceof CUnaryExpression unaryExpression) {

      return computeUnaryOperation(unaryExpression);

    } else if (expression instanceof CCastExpression castExpression) {
      // For cast expressions, evaluate the inner expression
      return evaluateExpression(castExpression.getOperand(), evaluatedValues);
    }

    return null;
  }

  private static CLiteralExpression computeBinaryOperation(
      CLiteralExpression operand1, CLiteralExpression operand2, BinaryOperator operator) {

    if (operator.isLogicalOperator() && operand1 != null && operand2 != null) {
      BigInteger evaluatedBinExpr = evaluateBinaryCondition(operand1, operand2, operator);
      return new CIntegerLiteralExpression(
          operand1.getFileLocation(), operand1.getExpressionType(), evaluatedBinExpr);
    }

    if (operand1 instanceof CIntegerLiteralExpression intExpr1
        && operand2 instanceof CIntegerLiteralExpression intExpr2) {
      BigInteger value1 = intExpr1.getValue();
      BigInteger value2 = intExpr2.getValue();

      BigInteger result =
          switch (operator) {
            case PLUS -> value1.add(value2);
            case MINUS -> value1.subtract(value2);
            case MULTIPLY -> value1.multiply(value2);
            case DIVIDE -> {
              if (value2.equals(BigInteger.ZERO)) {
                throw new ArithmeticException("Division by zero.");
              }
              yield value1.divide(value2);
            }
            case MODULO -> value1.mod(value2);
            default -> throw new UnsupportedOperationException("Unsupported operator: " + operator);
          };

      return new CIntegerLiteralExpression(
          operand1.getFileLocation(), operand1.getExpressionType(), result);
    }

    if (operand1 instanceof CFloatLiteralExpression floatExpr1
        && operand2 instanceof CFloatLiteralExpression floatExpr2) {

      FloatValue value1 = floatExpr1.getValue();
      FloatValue value2 = floatExpr2.getValue();

      FloatValue result =
          switch (operator) {
            case PLUS -> value1.add(value2);
            case MINUS -> value1.subtract(value2);
            case MULTIPLY -> value1.multiply(value2);
            case DIVIDE -> {
              if (value2.equalTo(FloatValue.zero(value2.getFormat()))) {
                throw new ArithmeticException("Division by zero.");
              }
              yield value1.divide(value2);
            }
            default -> throw new UnsupportedOperationException("Unsupported operator: " + operator);
          };

      return new CFloatLiteralExpression(
          operand1.getFileLocation(), MachineModel.LINUX64, operand1.getExpressionType(), result);
    }

    if (operand1 instanceof CStringLiteralExpression strExpr1
        && operand2 instanceof CStringLiteralExpression strExpr2) {

      // TODO: other operations for strings (?)
      if (operator == BinaryOperator.PLUS) {
        String result = strExpr1.toASTString() + strExpr2.toASTString();
        return new CStringLiteralExpression(operand1.getFileLocation(), result);
      }
      throw new UnsupportedOperationException("Unsupported operation for strings: " + operator);
    }

    return null;
  }

  public static BigInteger evaluateBinaryCondition(
      CLiteralExpression leftOperand, CLiteralExpression rightOperand, BinaryOperator operator) {

    // TODO: parsing of values, e.g., for ((int) x < (float) y) (?)

    if (leftOperand instanceof CIntegerLiteralExpression leftInt
        && rightOperand instanceof CIntegerLiteralExpression rightInt) {
      BigInteger leftValue = leftInt.getValue();
      BigInteger rightValue = rightInt.getValue();

      boolean result =
          switch (operator) {
            case EQUALS -> leftValue.equals(rightValue);
            case NOT_EQUALS -> !leftValue.equals(rightValue);
            case LESS_THAN -> leftValue.compareTo(rightValue) < 0;
            case LESS_EQUAL -> leftValue.compareTo(rightValue) <= 0;
            case GREATER_THAN -> leftValue.compareTo(rightValue) > 0;
            case GREATER_EQUAL -> leftValue.compareTo(rightValue) >= 0;
            default -> throw new UnsupportedOperationException("Unsupported operator: " + operator);
          };
      return BigInteger.valueOf(result ? 1 : 0);
    }

    // Handle float-based conditions
    if (leftOperand instanceof CFloatLiteralExpression leftFloat
        && rightOperand instanceof CFloatLiteralExpression rightFloat) {
      BigDecimal leftValue = BigDecimal.valueOf(leftFloat.getValue().doubleValue());
      BigDecimal rightValue = BigDecimal.valueOf(rightFloat.getValue().doubleValue());

      boolean result =
          switch (operator) {
            case EQUALS -> leftValue.compareTo(rightValue) == 0;
            case NOT_EQUALS -> leftValue.compareTo(rightValue) != 0;
            case LESS_THAN -> leftValue.compareTo(rightValue) < 0;
            case LESS_EQUAL -> leftValue.compareTo(rightValue) <= 0;
            case GREATER_THAN -> leftValue.compareTo(rightValue) > 0;
            case GREATER_EQUAL -> leftValue.compareTo(rightValue) >= 0;
            default -> throw new UnsupportedOperationException("Unsupported operator: " + operator);
          };
      return BigInteger.valueOf(result ? 1 : 0);
    }
    return BigInteger.valueOf(0);
  }

  private static CExpression computeUnaryOperation(CUnaryExpression unaryExpression) {

    CExpression operand = unaryExpression.getOperand();
    UnaryOperator operator = unaryExpression.getOperator();

    if (Objects.requireNonNull(operator) == UnaryOperator.MINUS) {

      if (operand instanceof CIntegerLiteralExpression intExpr) {
        BigInteger result = intExpr.getValue().negate();
        return new CIntegerLiteralExpression(
            operand.getFileLocation(), operand.getExpressionType(), result);

      } else if (operand instanceof CFloatLiteralExpression floatExpr) {
        FloatValue result = floatExpr.getValue().negate();
        return new CFloatLiteralExpression(
            operand.getFileLocation(), MachineModel.LINUX64, operand.getExpressionType(), result);
      }

    } else if (Objects.requireNonNull(operator) == UnaryOperator.AMPER) {

      if (operand instanceof CIdExpression) {
        return operand;
      }

    } else if (Objects.requireNonNull(operator) == UnaryOperator.TILDE) {
      // TODO
    } else if (Objects.requireNonNull(operator) == UnaryOperator.SIZEOF) {
      // TODO
    } else if (Objects.requireNonNull(operator) == UnaryOperator.ALIGNOF) {
      // TODO
    }

    return null;
  }
}

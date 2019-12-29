/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util;

import java.math.BigInteger;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class OverflowAssumptionManager {

  private final CBinaryExpressionBuilder cBinaryExpressionBuilder;
  private final MachineModel machineModel;

  public OverflowAssumptionManager(MachineModel pMachineModel, LogManager pLogger) {
    cBinaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    machineModel = pMachineModel;
  }

  /**
   * see {@link OverflowAssumptionManager#getAdditiveAssumption(CExpression, CExpression,
   * BinaryOperator, CLiteralExpression, boolean)}
   */
  public CExpression getUpperAssumption(
      CExpression operand1, CExpression operand2, BinaryOperator operator, CLiteralExpression max)
      throws UnrecognizedCodeException {
    return getAdditiveAssumption(operand1, operand2, operator, max, true);
  }

  /**
   * see {@link OverflowAssumptionManager#getAdditiveAssumption(CExpression, CExpression,
   * BinaryOperator, CLiteralExpression, boolean)}
   */
  public CExpression getLowerAssumption(
      CExpression operand1, CExpression operand2, BinaryOperator operator, CLiteralExpression min)
      throws UnrecognizedCodeException {
    return getAdditiveAssumption(operand1, operand2, operator, min, false);
  }

  public CExpression getConjunctionOfAdditiveAssumptions(
      CExpression operand1,
      CExpression operand2,
      BinaryOperator operator,
      CSimpleType type,
      boolean negate)
      throws UnrecognizedCodeException {
    CExpression assumption =
        cBinaryExpressionBuilder.buildBinaryExpression(
            getLowerAssumption(operand1, operand2, operator, getLowerBound(type)),
            getUpperAssumption(operand1, operand2, operator, getUpperBound(type)),
            BinaryOperator.BINARY_AND);
    if (negate) {
      assumption =
          cBinaryExpressionBuilder.buildBinaryExpression(
              assumption, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
    }
    return assumption;
  }

  public CExpression getResultOfAdditiveOperation(
      CExpression operand1, CExpression operand2, BinaryOperator operator)
      throws UnrecognizedCodeException {
    return cBinaryExpressionBuilder.buildBinaryExpression(operand1, operand2, operator);
  }

  /**
   * This helper method generates assumptions for checking overflows in signed integer
   * additions/subtractions. Since the assumptions are {@link CExpression}s as well, they are
   * structured in such a way that they do not suffer* from overflows themselves (this is of
   * particular importance e.g. if bit vector theory is used for representation!) *The assumptions
   * contain overflows because the second part is always evaluated, but their resulting value will
   * then not depend on the outcome of that part of the formula!
   *
   * <p>For addition (operator = BinaryOperator.PLUS) these assumptions are lower and upper limits:
   * (operand2 <= 0) | (operand1 <= limit - operand2) // upper limit (operand2 >= 0) | (operand1 >=
   * limit - operand2) // lower limit
   *
   * <p>For subtraction (operator = BinaryOperator.MINUS) the assumptions are lower and upper
   * limits: (operand2 >= 0) | (operand1 <= limit + operand2) // upper limit (operand2 <= 0) |
   * (operand1 >= limit + operand2) // lower limit
   *
   * @param operand1 first operand in the C Expression for which the assumption should be generated
   * @param operand2 second operand in the C Expression for which the assumption should be generated
   * @param operator either BinaryOperator.MINUS or BinaryOperator.PLUS
   * @param limit the {@link CLiteralExpression} representing the overflow bound for the type of the
   *     expression
   * @param isUpperLimit whether the limit supplied is the upper bound (otherwise it will be used as
   *     lower bound)
   * @return an assumption that has to hold in order for the input addition/subtraction NOT to have
   *     an overflow
   */
  public CExpression getAdditiveAssumption(
      CExpression operand1,
      CExpression operand2,
      BinaryOperator operator,
      CLiteralExpression limit,
      boolean isUpperLimit)
      throws UnrecognizedCodeException {

    boolean isMinusMode = (operator == BinaryOperator.MINUS);
    assert (isMinusMode || (operator == BinaryOperator.PLUS))
        : "operator has to be either BinaryOperator.PLUS or BinaryOperator.MINUS!";

    // We construct assumption by writing each of the 4 possible assumptions as:
    // term1 | term3

    // where term1 is structured this way:
    // operand2 term1Operator 0
    BinaryOperator term1Operator =
        (isUpperLimit ^ isMinusMode) ? BinaryOperator.LESS_EQUAL : BinaryOperator.GREATER_EQUAL;
    CExpression term1 =
        cBinaryExpressionBuilder.buildBinaryExpression(
            operand2, CIntegerLiteralExpression.ZERO, term1Operator);

    // and term2 is structured this way:
    // limit term2Operator operand2
    BinaryOperator term2Operator = isMinusMode ? BinaryOperator.PLUS : BinaryOperator.MINUS;
    CExpression term2 =
        cBinaryExpressionBuilder.buildBinaryExpression(limit, operand2, term2Operator);

    // and term3 is structured this way:
    // operand1 term3Operator term2
    BinaryOperator term3Operator =
        isUpperLimit ? BinaryOperator.LESS_EQUAL : BinaryOperator.GREATER_EQUAL;
    CExpression term3 =
        cBinaryExpressionBuilder.buildBinaryExpression(operand1, term2, term3Operator);

    // the final assumption will look like this:
    // (operand1 term1Operator 0) | ( operand1 term3Operator (limit term2Operator operand2) )
    CExpression assumption =
        cBinaryExpressionBuilder.buildBinaryExpression(term1, term3, BinaryOperator.BINARY_OR);

    return assumption;
  }

  /**
   * This helper method generates assumptions for checking overflows in signed integer
   * multiplications. Since the assumptions are {@link CExpression}s as well, they are structured in
   * such a way that they do not suffer* from overflows themselves (this is of particular importance
   * e.g. if bit vector theory is used for representation!) *The assumptions contain overflows
   * because the second part is always evaluated, but their resulting value will then not depend on
   * the outcome of that part of the formula!
   *
   * <p>The necessary assumptions for multiplication to be free from overflows look as follows:
   * (operand2 <= 0) | (operand1 <= pUpperLimit / operand2) (operand2 <= 0) | (operand1 >=
   * pLowerLimit / operand2) (operand1 <= 0) | (operand2 >= pLowerLimit / operand1) (operand1 >= 0)
   * | (operand2 >= pUpperLimit / operand1)
   *
   * @param operand1 first operand in the C Expression for which the assumption should be generated
   * @param operand2 second operand in the C Expression for which the assumption should be generated
   * @param pLowerLimit the {@link CLiteralExpression} representing the overflow bound for the type
   *     of the expression
   * @param pUpperLimit the {@link CLiteralExpression} representing the overflow bound for the type
   *     of the expression
   * @param result the set to which the generated assumptions are added
   */
  public void addMultiplicationAssumptions(
      CExpression operand1,
      CExpression operand2,
      CLiteralExpression pLowerLimit,
      CLiteralExpression pUpperLimit,
      Set<CExpression> result)
      throws UnrecognizedCodeException {

    for (boolean operand1isFirstOperand : new boolean[] {false, true}) {
      CExpression firstOperand = operand1isFirstOperand ? operand1 : operand2;
      CExpression secondOperand = operand1isFirstOperand ? operand2 : operand1;
      for (boolean usesUpperLimit : new boolean[] {false, true}) {
        CLiteralExpression limit = usesUpperLimit ? pUpperLimit : pLowerLimit;

        // We construct assumption by writing each of the 4 possible assumptions as:
        // term1 | term3

        // where term1 is structured this way:
        // firstOperand term1Operator 0
        BinaryOperator term1Operator =
            usesUpperLimit && operand1isFirstOperand
                ? BinaryOperator.GREATER_EQUAL
                : BinaryOperator.LESS_EQUAL;
        CExpression term1 =
            cBinaryExpressionBuilder.buildBinaryExpression(
                firstOperand, CIntegerLiteralExpression.ZERO, term1Operator);

        // and term2 is structured this way:
        // limit BinaryOperator.DIVIDE firstOperand
        CExpression term2 =
            cBinaryExpressionBuilder.buildBinaryExpression(
                limit, firstOperand, BinaryOperator.DIVIDE);

        // and term3 is structured this way:
        // secondOperand term3Operator term2
        BinaryOperator term3Operator =
            usesUpperLimit && !operand1isFirstOperand
                ? BinaryOperator.LESS_EQUAL
                : BinaryOperator.GREATER_EQUAL;
        CExpression term3 =
            cBinaryExpressionBuilder.buildBinaryExpression(secondOperand, term2, term3Operator);

        // the final assumption will look like this:
        // (firstOperand term1Operator 0) |
        // ( secondOperand term3Operator (limit BinaryOperator.DIVIDE firstOperand) )
        CExpression assumption =
            cBinaryExpressionBuilder.buildBinaryExpression(term1, term3, BinaryOperator.BINARY_OR);
        result.add(assumption);
      }
    }
  }

  /**
   * This helper method generates assumptions for checking overflows in signed integer divisions and
   * modulo operations.
   *
   * <p>The necessary assumption for division or modulo to be free from overflows looks as follows:
   *
   * <p>(operand1 != limit) | (operand2 != -1)
   *
   * @param operand1 first operand in the C Expression for which the assumption should be generated
   * @param operand2 second operand in the C Expression for which the assumption should be generated
   * @param limit the smallest value in the expression's type
   * @param result the set to which the generated assumptions are added
   */
  public void addDivisionAssumption(
      CExpression operand1, CExpression operand2, CLiteralExpression limit, Set<CExpression> result)
      throws UnrecognizedCodeException {

    // operand1 != limit
    CExpression term1 =
        cBinaryExpressionBuilder.buildBinaryExpression(operand1, limit, BinaryOperator.NOT_EQUALS);
    // -1
    CExpression term2 =
        new CUnaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            CIntegerLiteralExpression.ZERO,
            UnaryOperator.MINUS);
    // operand2 != -1
    CExpression term3 =
        cBinaryExpressionBuilder.buildBinaryExpression(operand2, term2, BinaryOperator.NOT_EQUALS);
    // (operand1 != INT_MIN) | (operand2 != -1)
    CExpression assumption =
        cBinaryExpressionBuilder.buildBinaryExpression(term1, term3, BinaryOperator.BINARY_OR);
    result.add(assumption);
  }

  /**
   * @param operand1 first operand in the C Expression for which the assumption should be generated
   * @param operand2 second operand in the C Expression for which the assumption should be generated
   * @param limit the largest value in the expression's type
   * @param result the set to which the generated assumptions are added
   */
  public void addLeftShiftAssumptions(
      CExpression operand1, CExpression operand2, CLiteralExpression limit, Set<CExpression> result)
      throws UnrecognizedCodeException {

    // For no undefined behavior, both operands need to be positive:
    // But this is (currently) not considered as overflow!
    /*result.add(cBinaryExpressionBuilder.buildBinaryExpression(operand1,
        CIntegerLiteralExpression.ZERO, BinaryOperator.GREATER_EQUAL));
    result.add(cBinaryExpressionBuilder.buildBinaryExpression(operand2,
        CIntegerLiteralExpression.ZERO, BinaryOperator.GREATER_EQUAL));*/

    // Shifting the precision of the type or a bigger number of bits  is undefined behavior:
    // operand2 < width
    // But this is (currently) not considered as overflow!
    /*result.add(
    cBinaryExpressionBuilder.buildBinaryExpression(operand2, width, BinaryOperator.LESS_THAN));*/

    // Shifting out set bits is undefined behavior that is considered to be an overflow.
    // This is equivalent to the assumption:
    // operand1 <= (limit >> operand2)
    CExpression term1 =
        cBinaryExpressionBuilder.buildBinaryExpression(limit, operand2, BinaryOperator.SHIFT_RIGHT);
    result.add(
        cBinaryExpressionBuilder.buildBinaryExpression(operand1, term1, BinaryOperator.LESS_EQUAL));
  }

  public static BigInteger getWidthForMaxOf(BigInteger pMax) {
    return BigInteger.valueOf(pMax.bitLength() + 1);
  }

  public CExpression getNegationAssumption(CExpression operand, CLiteralExpression limit)
      throws UnrecognizedCodeException {
    return cBinaryExpressionBuilder.buildBinaryExpression(
        operand, limit, BinaryOperator.NOT_EQUALS);
  }

  public CLiteralExpression getUpperBound(CSimpleType type) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, type, machineModel.getMaximalIntegerValue(type));
  }

  public CLiteralExpression getLowerBound(CSimpleType type) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, type, machineModel.getMinimalIntegerValue(type));
  }
}

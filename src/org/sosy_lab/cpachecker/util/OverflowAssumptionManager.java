// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class OverflowAssumptionManager extends AssumptionManager {


  public OverflowAssumptionManager(MachineModel pMachineModel, LogManager pLogger) {
    super(pMachineModel, pLogger);
  }

  @Override
  public CLiteralExpression getBound(CSimpleType type) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        type,
        machineModel.getMaximalIntegerValue(type));
  }

  /**
   * see
   * {@link AssumptionManager#getAdditiveAssumption(CExpression, CExpression, BinaryOperator, CLiteralExpression, boolean)}
   */

  @Override
  public CExpression getBoundAssumption(
      CExpression operand1,
      CExpression operand2,
      BinaryOperator operator,
      CLiteralExpression max)
      throws UnrecognizedCodeException {
    return getAdditiveAssumption(operand1, operand2, operator, max, true);
  }



  /**
   * This helper method generates assumptions for checking overflows in signed integer
   * multiplications. Since the assumptions are {@link CExpression}s as well, they are structured in
   * such a way that they do not suffer* from overflows themselves (this is of particular importance
   * e.g. if bit vector theory is used for representation!) *The assumptions contain overflows
   * because the second part is always evaluated, but their resulting value will then not depend on
   * the outcome of that part of the formula!
   *
   * <p>
   * The necessary assumptions for multiplication to be free from overflows look as follows:
   * <ul>
   * <li>(operand2 <= 0) | (operand1 <= pUpperLimit / operand2)
   * <li>(operand1 >= 0) | (operand2 >= pUpperLimit / operand1)
   * </ul>
   *
   * @param operand1 first operand in the C Expression for which the assumption should be generated
   * @param operand2 second operand in the C Expression for which the assumption should be generated
   * @param pLimit the {@link CLiteralExpression} representing the overflow bound for the type of
   *        the expression
   */

  @Override
  public Set<CExpression> addMultiplicationAssumptions(
      CExpression operand1,
      CExpression operand2,
      CLiteralExpression pLimit)
      throws UnrecognizedCodeException {

    ImmutableSet.Builder<CExpression> result = ImmutableSet.builder();
    for (boolean operand1isFirstOperand : new boolean[] {false, true}) {
      CExpression firstOperand = operand1isFirstOperand ? operand1 : operand2;
      CExpression secondOperand = operand1isFirstOperand ? operand2 : operand1;
      CLiteralExpression limit = pLimit;

        // We construct assumption by writing each of the 4 possible assumptions as:
        // term1 | term3

        // where term1 is structured this way:
        // firstOperand term1Operator 0
        BinaryOperator term1Operator =
             operand1isFirstOperand
                ? BinaryOperator.LESS_EQUAL
                : BinaryOperator.GREATER_EQUAL;
        CExpression term1 =
            cBinaryExpressionBuilder
                .buildBinaryExpression(firstOperand, CIntegerLiteralExpression.ZERO, term1Operator);

        // and term2 is structured this way:
        // limit BinaryOperator.DIVIDE firstOperand
        CExpression term2 =
            cBinaryExpressionBuilder
                .buildBinaryExpression(limit, firstOperand, BinaryOperator.DIVIDE);

        // and term3 is structured this way:
        // secondOperand term3Operator term2
        BinaryOperator term3Operator =
             !operand1isFirstOperand
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


    return result.build();
  }



}
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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class EnhancedCExpressionSimplificationVisitor extends ExpressionSimplificationVisitor {

  public EnhancedCExpressionSimplificationVisitor(
      MachineModel pMm,
      LogManagerWithoutDuplicates pLogger) {
    super(pMm, pLogger);

  }

  public CExpression visit(CExpression pExpr) {
    if (pExpr instanceof CAddressOfLabelExpression) {
      return this.visit((CAddressOfLabelExpression) pExpr);
    } else if (pExpr instanceof CBinaryExpression) {
      return this.visit((CBinaryExpression) pExpr);
    } else if (pExpr instanceof CCastExpression) {
      return this.visit((CCastExpression) pExpr);
    } else if (pExpr instanceof CTypeIdExpression) {
      return this.visit((CTypeIdExpression) pExpr);
    } else if (pExpr instanceof CUnaryExpression) {
      return this.visit((CUnaryExpression) pExpr);
    } else {
      return pExpr;
    }
  }

  @Override
  public CExpression visit(CBinaryExpression expr) {
    final BinaryOperator binaryOperator = expr.getOperator();

    final CExpression op1 = recursive(expr.getOperand1());
    final NumericValue value1 = getValue(op1);

    final CExpression op2 = recursive(expr.getOperand2());
    final NumericValue value2 = getValue(op2);

    // if one side can not be evaluated, build new expression
    if (value1 == null || value2 == null) {

      final CBinaryExpression newExpr;
      if (op1 == expr.getOperand1() && op2 == expr.getOperand2()) {
        // If at least the right hand side has a value, check, if this can be used on the other side
        if (value2 != null) {
          CExpression e = simplify(expr, value2, op1);
          if (e != null) {
            return e;
          }
        }

        // shortcut: if nothing has changed, use the original expression
        newExpr = expr;
      } else {
        final CBinaryExpressionBuilder binExprBuilder =
            new CBinaryExpressionBuilder(machineModel, logger);
        switch (binaryOperator) {
          case BINARY_AND:
            return super.visit(expr);
          case BINARY_OR:
            return super.visit(expr);
          default:
            break;
        }
        newExpr = binExprBuilder.buildBinaryExpressionUnchecked(op1, op2, binaryOperator);
      }
      return newExpr;
    }

    // TODO: handle the case that it's not a CSimpleType or that it's not a number
    Value result =
        AbstractExpressionValueVisitor
            .calculateBinaryOperation(value1, value2, expr, machineModel, logger);

    return convertExplicitValueToExpression(expr, result);
  }

  /**
   * <b> Important: Only use this method, if it is guaranteed, that there are no overflows! </b>
   * Checks, if the value (integer literal or something like that) can be used on the left hand side
   * to simplify the expression
   *
   * @param topLevelExpr containing left and right hand side
   * @param valueOfrhsExpr as number
   * @param exprLHS containing a non nested binary expression, where the right hand side needs to be
   *        a integer literal
   * @return either null, if the expression cannot be simplified or a simplified term
   */
  private CExpression
      simplify(CBinaryExpression topLevelExpr, NumericValue valueOfrhsExpr, CExpression exprLHS) {
    if (isNotNestedtwice(exprLHS)) {
      // Check, if the non evaluated expression contains in a binary subexpression value1 and if
      // they cancel out
      CBinaryExpression bin = (CBinaryExpression) exprLHS;
      NumericValue valueRHSBin = getValue(bin.getOperand2());

      if (valueRHSBin != null
          && valueRHSBin.equals(valueOfrhsExpr)
          && operatorsCancelOut(topLevelExpr.getOperator(), bin.getOperator())) {
        return bin.getOperand1();
      }
    }
    return null;
  }

  private boolean operatorsCancelOut(BinaryOperator pOperator, BinaryOperator pOperator2) {
    return (pOperator.equals(BinaryOperator.PLUS) && pOperator2.equals(BinaryOperator.MINUS))
        || (pOperator.equals(BinaryOperator.MINUS) && pOperator2.equals(BinaryOperator.PLUS));
  }

  private boolean isNotNestedtwice(CExpression pOp2) {
    return pOp2 instanceof CBinaryExpression
        && !(((CBinaryExpression) pOp2).getOperand1() instanceof CBinaryExpression)
        && !(((CBinaryExpression) pOp2).getOperand2() instanceof CBinaryExpression);
  }


}

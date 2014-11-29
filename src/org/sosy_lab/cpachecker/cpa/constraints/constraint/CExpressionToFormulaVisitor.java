/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Class for transforming {@link CExpression} objects into their {@link InvariantsFormula} representation.
 *
 * <p>Always use {@link #transform} to create correct representations. Otherwise, correctness can't be assured.</p>
 */
public class CExpressionToFormulaVisitor extends ExpressionToFormulaVisitor
    implements CRightHandSideVisitor<InvariantsFormula<Value>, UnrecognizedCodeException> {

  private static final String AMPERSAND = "&";

  public CExpressionToFormulaVisitor(String pFunctionName) {
    super(pFunctionName);
  }

  public CExpressionToFormulaVisitor(String pFunctionName, ValueAnalysisState pValueState) {
    super(pFunctionName, pValueState);
  }

  public InvariantsFormula<Value> transform(CExpression pExpression) throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public InvariantsFormula<Value> visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
    InvariantsFormula<Value> operand1Formula = pIastBinaryExpression.getOperand1().accept(this);
    InvariantsFormula<Value> operand2Formula = pIastBinaryExpression.getOperand2().accept(this);
    final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;

    if (operand1Formula == null) {
      return operand2Formula;
    } else if (operand2Formula == null) {
      return operand1Formula;
    }

    switch (pIastBinaryExpression.getOperator()) {
      case MINUS:
        operand2Formula = negate(operand2Formula);
        // $FALL-THROUGH$
      case PLUS:
        return factory.add(operand1Formula, operand2Formula);
      case MULTIPLY:
        return factory.multiply(operand1Formula, operand2Formula);
      case DIVIDE:
        return factory.divide(operand1Formula, operand2Formula);
      case MODULO:
        return factory.modulo(operand1Formula, operand2Formula);
      case SHIFT_LEFT:
        return factory.shiftLeft(operand1Formula, operand2Formula);
      case SHIFT_RIGHT:
        return factory.shiftRight(operand1Formula, operand2Formula);
      case BINARY_AND:
        return factory.binaryAnd(operand1Formula, operand2Formula);
      case BINARY_OR:
        return factory.binaryOr(operand1Formula, operand2Formula);
      case BINARY_XOR:
        return factory.binaryXor(operand1Formula, operand2Formula);
      case EQUALS:
        return factory.equal(operand1Formula, operand2Formula);
      case NOT_EQUALS:
        return factory.logicalNot(factory.equal(operand1Formula, operand2Formula));
      case LESS_THAN:
        return factory.lessThan(operand1Formula, operand2Formula);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(operand1Formula, operand2Formula);
      case GREATER_THAN:
        return factory.greaterThan(operand1Formula, operand2Formula);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(operand1Formula, operand2Formula);
      default:
        throw new AssertionError("Unhandled binary operation " + pIastBinaryExpression.getOperator());
    }
  }

  @Override
  public InvariantsFormula<Value> visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
    final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;
    final CUnaryExpression.UnaryOperator operator = pIastUnaryExpression.getOperator();

    switch (operator) {
      case MINUS:
      case TILDE: {
        InvariantsFormula<Value> operand = pIastUnaryExpression.getOperand().accept(this);

        if (operand == null) {
          return null;
        } else {
          return transformUnaryArithmetic(operator, operand);
        }
      }

      default:
        return null; // TODO: amper, alignof, sizeof with own formulas
    }
  }

  private InvariantsFormula<Value> transformUnaryArithmetic(CUnaryExpression.UnaryOperator pOperator,
      InvariantsFormula<Value> pOperand) {
    switch (pOperator) {
      case MINUS:
        return negate(pOperand);
      case TILDE:
        return InvariantsFormulaManager.INSTANCE.binaryNot(pOperand);
      default:
        throw new AssertionError("No arithmetic operator: " + pOperator);
    }
  }

  @Override
  public InvariantsFormula<Value> visit(CIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    return super.visit(pIastIdExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastCharLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastFloatLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return super.visit(pIastIntegerLiteralExpression);
  }

  @Override
  public InvariantsFormula<Value> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CImaginaryLiteralExpression pIastLiteralExpression)
      throws UnrecognizedCodeException {
    assert false : "Imaginary literal";
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CFieldReference pIastFieldReference) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CPointerExpression pointerExpression) throws UnrecognizedCodeException {
    return null;
  }


  @Override
  public InvariantsFormula<Value> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CCastExpression pIastCastExpression) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public InvariantsFormula<Value> visit(CComplexCastExpression complexCastExpression) throws UnrecognizedCodeException {
    return null;
  }
}

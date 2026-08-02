// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;

public class IntegerArithmaticExpressionVisitor implements CExpressionVisitor {
  private final ImmutableList.Builder<CIdExpression> encounteredVariables;
  private final ExprEvaluator util;
  boolean success = true;

  public IntegerArithmaticExpressionVisitor() {
    encounteredVariables = ImmutableList.builder();
    util = new ExprEvaluator();
  }

  public ImmutableList<CIdExpression> getEncounteredVariables() {
    return encounteredVariables.build();
  }

  public boolean failed() {
    return !success;
  }

  public IExpr visit(CExpression pExpression) throws Exception {
    if (!success) {return null;}
    IExpr symbolicExpression = null;
    switch (pExpression) {
      case CBinaryExpression binaryExpression:
        symbolicExpression = visit(binaryExpression);
        break;
      case CUnaryExpression unaryExpression:
        symbolicExpression = visit(unaryExpression);
        break;
      case CIntegerLiteralExpression integerLiteralExpression:
        symbolicExpression = F.ZZ(integerLiteralExpression.getValue());
        break;
      case CIdExpression idExpression:
        encounteredVariables.add(idExpression);
        symbolicExpression = F.$s(idExpression.getName()+"-");
        break;
      default:
        break;
    }
    if (symbolicExpression == null) {
      success = false;
    }
    return symbolicExpression;
  }

  @Override
  public IExpr visit(CBinaryExpression pIastBinaryExpression) throws Exception {
    IExpr symbolicExpression;
    CExpression lhs = pIastBinaryExpression.getOperand1();
    CExpression rhs = pIastBinaryExpression.getOperand2();
    switch (pIastBinaryExpression.getOperator()) {
      case PLUS:
        symbolicExpression = util.eval(visit(lhs).plus(visit(rhs)));
        break;
      case MINUS:
        symbolicExpression = util.eval(visit(lhs).minus(visit(rhs)));
        break;
      case MULTIPLY:
        symbolicExpression = util.eval(visit(lhs).times(visit(rhs)));
        break;
      case DIVIDE:
        symbolicExpression = util.eval(visit(lhs).divide(visit(rhs)));
        break;
      case MODULO:
        symbolicExpression =  util.eval(visit(lhs).mod(visit(rhs)));
        break;
      default:
        success = false;
        return null;
    }
    return symbolicExpression;
  }

  @Override
  public IExpr visit(CCastExpression pIastCastExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CStringLiteralExpression pIastStringLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CUnaryExpression pIastUnaryExpression) throws Exception {
    IExpr symbolicExpression;
    CExpression expression = pIastUnaryExpression.getOperand();
    switch (pIastUnaryExpression.getOperator()) {
      case MINUS:
        symbolicExpression = util.eval(F.Times(-1, visit(expression)));
        break;
      default:
        return null;
    }
    return symbolicExpression;
  }

  @Override
  public IExpr visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CFieldReference pIastFieldReference) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CIdExpression pIastIdExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CPointerExpression pointerExpression) throws Exception {
    return null;
  }

  @Override
  public IExpr visit(CComplexCastExpression complexCastExpression) throws Exception {
    return null;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

public class CExpressionToExpressionTreeVisitor
    implements CExpressionVisitor<ExpressionTree<CExpression>, NoException> {

  @Override
  public ExpressionTree<CExpression> visit(CBinaryExpression pIastBinaryExpression)
      throws NoException {
    if (pIastBinaryExpression.getOperator() == BinaryOperator.BINARY_AND) {
      ExpressionTree<CExpression> left = visitDefault(pIastBinaryExpression.getOperand1());
      ExpressionTree<CExpression> right = visitDefault(pIastBinaryExpression.getOperand2());
      return And.of(left, right);
    } else if (pIastBinaryExpression.getOperator() == BinaryOperator.BINARY_OR) {
      ExpressionTree<CExpression> left = visitDefault(pIastBinaryExpression.getOperand1());
      ExpressionTree<CExpression> right = visitDefault(pIastBinaryExpression.getOperand2());
      return Or.of(left, right);
    } else {return LeafExpression.of(pIastBinaryExpression);}
  }

  public ExpressionTree<CExpression> visitDefault(AExpression pIastExpression) {
    if (pIastExpression instanceof CExpression) {
      if (pIastExpression instanceof CBinaryExpression) {
        return this.visit((CBinaryExpression) pIastExpression);
      } else {
        return LeafExpression.of((CExpression) pIastExpression);
      }
    } else {
      // TODO Find nicer way
      throw new IllegalArgumentException("Only applicable for CExpressions");
    }
  }

  @Override
  public ExpressionTree<CExpression> visit(CCastExpression pIastCastExpression) throws NoException {
    return LeafExpression.of(pIastCastExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws NoException {
    return LeafExpression.of(pIastCharLiteralExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws NoException {
    return LeafExpression.of(pIastFloatLiteralExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws NoException {
    return LeafExpression.of(pIastIntegerLiteralExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws NoException {
    return LeafExpression.of(pIastStringLiteralExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CTypeIdExpression pIastTypeIdExpression)
      throws NoException {
    return LeafExpression.of(pIastTypeIdExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CUnaryExpression pIastUnaryExpression)
      throws NoException {
    return LeafExpression.of(pIastUnaryExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws NoException {
    return LeafExpression.of(PIastLiteralExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws NoException {
    return LeafExpression.of(pAddressOfLabelExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws NoException {
    return LeafExpression.of(pIastArraySubscriptExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CFieldReference pIastFieldReference) throws NoException {
    return LeafExpression.of(pIastFieldReference);
  }

  @Override
  public ExpressionTree<CExpression> visit(CIdExpression pIastIdExpression) throws NoException {
    return LeafExpression.of(pIastIdExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CPointerExpression pointerExpression)
      throws NoException {
    return LeafExpression.of(pointerExpression);
  }

  @Override
  public ExpressionTree<CExpression> visit(CComplexCastExpression complexCastExpression)
      throws NoException {
    return LeafExpression.of(complexCastExpression);
  }
}

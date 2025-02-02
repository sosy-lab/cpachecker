// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import org.sosy_lab.cpachecker.cfa.ast.c.*;

import java.util.HashSet;
import java.util.Set;

public class CollectCIdExpressionsVisitor
    implements CExpressionVisitor<Set<CIdExpression>, RuntimeException> {

  @Override
  public Set<CIdExpression> visit(CBinaryExpression pIastBinaryExpression) {
    Set<CIdExpression> result = new HashSet<>();
    result.addAll(pIastBinaryExpression.getOperand1().accept(this));
    result.addAll(pIastBinaryExpression.getOperand2().accept(this));
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CUnaryExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CIdExpression pIastIdExpression) {
    Set<CIdExpression> result = new HashSet<>();
    result.add(pIastIdExpression);
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    Set<CIdExpression> result = new HashSet<>();
    result.addAll(pIastArraySubscriptExpression.getArrayExpression().accept(this));
    result.addAll(pIastArraySubscriptExpression.getSubscriptExpression().accept(this));
    return result;
  }

  @Override
  public Set<CIdExpression> visit(CFieldReference pIastFieldReference) {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CPointerExpression pPointerExpression) {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CComplexCastExpression pCastExpression) {
    return pCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<CIdExpression> visit(CCharLiteralExpression pIastCharLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CStringLiteralExpression pIastStringLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CTypeIdExpression pIastTypeIdExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CImaginaryLiteralExpression pIastImaginaryLiteralExpression) {
    return new HashSet<>();
  }

  @Override
  public Set<CIdExpression> visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return new HashSet<>();
  }
}

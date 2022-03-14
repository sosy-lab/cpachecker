// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
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
import org.sosy_lab.cpachecker.exceptions.NoException;

enum IsConstantExpressionVisitor implements CExpressionVisitor<Boolean, NoException> {
  INSTANCE;

  @Override
  public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    return false;
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) {
    return false;
  }

  @Override
  public Boolean visit(CIdExpression pIastIdExpression) {
    return false;
  }

  @Override
  public Boolean visit(CPointerExpression pPointerExpression) {
    return false;
  }

  @Override
  public Boolean visit(CComplexCastExpression pComplexCastExpression) {
    return pComplexCastExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CBinaryExpression pIastBinaryExpression) {
    return pIastBinaryExpression.getOperand1().accept(this)
        && pIastBinaryExpression.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
    return true;
  }

  @Override
  public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
    return true;
  }

  @Override
  public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
    return true;
  }

  @Override
  public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
    return true;
  }

  @Override
  public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
    return false;
  }

  @Override
  public Boolean visit(CUnaryExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) {
    return true;
  }

  @Override
  public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    return false;
  }
}

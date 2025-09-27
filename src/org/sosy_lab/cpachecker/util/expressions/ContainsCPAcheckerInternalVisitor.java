// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

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

public class ContainsCPAcheckerInternalVisitor implements CExpressionVisitor<Boolean, Exception> {

  @Override
  public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CIdExpression pIastIdExpression) throws Exception {
    return pIastIdExpression.getName().startsWith("__CPAchecker_TMP");
  }

  @Override
  public Boolean visit(CPointerExpression pPointerExpression) throws Exception {
    return pPointerExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CComplexCastExpression pComplexCastExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CBinaryExpression pIastBinaryExpression) throws Exception {
    return pIastBinaryExpression.getOperand1().accept(this)
        || pIastBinaryExpression.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CCastExpression pIastCastExpression) throws Exception {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CUnaryExpression pIastUnaryExpression) throws Exception {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) throws Exception {
    return false;
  }

  @Override
  public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws Exception {
    return false;
  }
}

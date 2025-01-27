// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CLemmaFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class ComparesPointerWithNonPointer implements CExpressionVisitor<Boolean, NoException> {
  @Override
  public Boolean visit(CBinaryExpression pIastBinaryExpression) throws NoException {
    CExpression operand1 = pIastBinaryExpression.getOperand1();
    CExpression operand2 = pIastBinaryExpression.getOperand2();
    // If at least one of the operands is a pointer and the other is not, return true
    if (operand1.getExpressionType() instanceof CPointerType
        != operand2.getExpressionType() instanceof CPointerType) {
      return true;
    }

    return operand1.accept(this) || operand2.accept(this);
  }

  @Override
  public Boolean visit(CCastExpression pIastCastExpression) throws NoException {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CUnaryExpression pIastUnaryExpression) throws NoException {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CLemmaFunctionCall pCLemmaFunctionCall) {
    return false;
  }

  @Override
  public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws NoException {
    return pIastArraySubscriptExpression.getArrayExpression().accept(this)
        || pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
  }

  @Override
  public Boolean visit(CFieldReference pIastFieldReference) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CIdExpression pIastIdExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CPointerExpression pointerExpression) throws NoException {
    return false;
  }

  @Override
  public Boolean visit(CComplexCastExpression complexCastExpression) throws NoException {
    return false;
  }
}

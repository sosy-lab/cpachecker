// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
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

public class CollectCIdExpressionsVisitor
    implements CExpressionVisitor<Set<CIdExpression>, RuntimeException> {

  @Override
  public Set<CIdExpression> visit(CBinaryExpression pIastBinaryExpression) {
    return Sets.union(
        pIastBinaryExpression.getOperand1().accept(this),
        pIastBinaryExpression.getOperand2().accept(this));
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
    return Sets.union(
        pIastArraySubscriptExpression.getArrayExpression().accept(this),
        pIastArraySubscriptExpression.getSubscriptExpression().accept(this));
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

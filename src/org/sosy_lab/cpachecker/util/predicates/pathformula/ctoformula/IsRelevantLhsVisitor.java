// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;

class IsRelevantLhsVisitor extends DefaultCExpressionVisitor<Boolean, NoException> {

  private final CtoFormulaConverter conv;

  IsRelevantLhsVisitor(CtoFormulaConverter pConv) {
    conv = pConv;
  }

  @Override
  public Boolean visit(final CArraySubscriptExpression e) {
    return e.getArrayExpression().accept(this);
  }

  @Override
  public Boolean visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Boolean visit(final CFieldReference e) {
    CType fieldOwnerType = e.getFieldOwner().getExpressionType().getCanonicalType();
    if (fieldOwnerType instanceof CPointerType) {
      fieldOwnerType = ((CPointerType) fieldOwnerType).getType();
    }
    assert fieldOwnerType instanceof CCompositeType : "Field owner should have composite type";
    return conv.isRelevantField((CCompositeType) fieldOwnerType, e.getFieldName());
  }

  @Override
  public Boolean visit(final CIdExpression e) {
    return conv.isRelevantVariable(e.getDeclaration())
        && !conv.isAbstractedVariable(e.getDeclaration());
  }

  @Override
  public Boolean visit(CPointerExpression e) {
    return true;
  }

  @Override
  public Boolean visit(CUnaryExpression e) {
    // Inside casts an arbitrary expression may appear on the LHS
    return e.getOperand().accept(this);
  }

  @Override
  protected Boolean visitDefault(CExpression e) {
    throw new IllegalArgumentException("Undexpected left hand side: " + e.toString());
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This Visitor evaluates the visited expression and returns iff the given variable is used in it.
 */
class VarCExpressionVisitor extends DefaultCExpressionVisitor<Boolean, NoException> {

  private String varName;

  VarCExpressionVisitor(String var) {
    varName = var;
  }

  private Boolean handle(CExpression exp) {
    String name = BDDTransferRelation.getCanonicalName(exp);
    return varName.equals(name == null ? exp.toASTString() : name);
  }

  @Override
  public Boolean visit(CArraySubscriptExpression exp) {
    return handle(exp);
  }

  @Override
  public Boolean visit(CBinaryExpression exp) {
    return exp.getOperand1().accept(this) || exp.getOperand2().accept(this);
  }

  @Override
  public Boolean visit(CCastExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CComplexCastExpression exp) {
    // TODO check if only the part of the operand should be evaluated which the
    // expression casts to
    return exp.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CFieldReference exp) {
    return handle(exp);
  }

  @Override
  public Boolean visit(CIdExpression exp) {
    return varName.equals(exp.getDeclaration().getQualifiedName());
  }

  @Override
  public Boolean visit(CUnaryExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  public Boolean visit(CPointerExpression exp) {
    return exp.getOperand().accept(this);
  }

  @Override
  protected Boolean visitDefault(CExpression pExp) {
    return false;
  }
}

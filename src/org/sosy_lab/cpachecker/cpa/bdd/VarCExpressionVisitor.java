/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
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
    this.varName = var;
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

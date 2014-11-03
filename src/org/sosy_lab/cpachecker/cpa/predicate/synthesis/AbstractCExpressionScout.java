/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;

import com.google.common.collect.Sets;

public abstract class AbstractCExpressionScout extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

  private final Set<CExpression> visited;

  public AbstractCExpressionScout() {
    super();
    visited = Sets.newHashSet();
  }

  public abstract boolean matches (CExpression pExpr);

  public Boolean x(CExpression pParent, CExpression... pExprOperands)  {
    if (!visited.add(pParent)) {
      return false;
    }

    if (matches(pParent)) {
      return true;
    }

    for (CExpression op: pExprOperands) {

      if (matches(op)) {
        return true;
      }

      if (op.accept(this)) {
        return true;
      }

    }

    return false;
  }

  @Override
  public Boolean visit(CTypeIdExpression pE) throws RuntimeException {
    return x(pE);
  }

  @Override
  public Boolean visit(CPointerExpression pE) throws RuntimeException {
    return x(pE, pE.getOperand());
  }

  @Override
  public Boolean visit(CComplexCastExpression pE) throws RuntimeException {
    return x(pE, pE.getOperand());
  }

  @Override
  public Boolean visit(CArraySubscriptExpression pE) throws RuntimeException {
    return x(pE, pE.getArrayExpression(), pE.getSubscriptExpression());
  }

  @Override
  protected Boolean visitDefault(CExpression pE) {
    return x(pE);
  }

  @Override
  public Boolean visit(CCastExpression pE) throws RuntimeException {
    return x(pE, pE.getOperand());
  }

  @Override
  public Boolean visit(CUnaryExpression pE) throws RuntimeException {
    return x(pE, pE.getOperand());
  }

  @Override
  public Boolean visit(CBinaryExpression pE) throws RuntimeException {
    return x(pE, pE.getOperand1(), pE.getOperand2());
  }

}

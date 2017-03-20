/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import java.util.LinkedList;
import java.util.List;
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
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.util.Pair;

public class ExpressionHandler extends DefaultCExpressionVisitor<Void, HandleCodeException> {

  private List<Pair<CExpression, Access>> result;
  protected Access accessMode;

  public void setMode(Access mode) {
    result = new LinkedList<>();
    accessMode = mode;
  }

  @Override
  public Void visit(CArraySubscriptExpression expression) throws HandleCodeException {
    addExpression(expression);
    accessMode = Access.READ;
    expression.getArrayExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CBinaryExpression expression) throws HandleCodeException {
    if (accessMode == Access.READ) {
      expression.getOperand1().accept(this);
      expression.getOperand2().accept(this);
    } else {
      //We can't be here. This is error: a + b = ...
      throw new HandleCodeException("Writing to BinaryExpression: " + expression.toASTString());
    }
    return null;
  }

  @Override
  public Void visit(CCastExpression expression) throws HandleCodeException {
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldReference expression) throws HandleCodeException {
    addExpression(expression);
    if (expression.isPointerDereference()) {
      accessMode = Access.READ;
      expression.getFieldOwner().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CIdExpression expression) throws HandleCodeException {
    addExpression(expression);
    return null;
  }

  @Override
  public Void visit(CUnaryExpression expression) throws HandleCodeException {
    if (expression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      addExpression(expression);
      return null;
    }
    //In all other unary operation we only read the operand
    accessMode = Access.READ;
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CPointerExpression pPointerExpression) throws HandleCodeException {
    //write: *s =
    addExpression(pPointerExpression);
    accessMode = Access.READ;
    pPointerExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression pComplexCastExpression) throws HandleCodeException {
    pComplexCastExpression.getOperand().accept(this);
    return null;
  }

  private void addExpression(CExpression e) {
    /*creator.clearDereference();
    AbstractIdentifier id = e.accept(creator);
    id = currentState.getLinksIfNecessary(id);*/
    result.add(Pair.of(e, accessMode));
  }

  public List<Pair<CExpression, Access>> getProcessedExpressions() {
    return result;
  }

  @Override
  protected Void visitDefault(CExpression pExp) throws HandleCodeException {
    return null;
  }
}


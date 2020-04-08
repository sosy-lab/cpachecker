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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.Identifiers;

public class ExpressionHandler extends DefaultCExpressionVisitor<Void, NoException> {

  private final List<Pair<AbstractIdentifier, Access>> result;
  private final String fName;
  private Access accessMode;

  public ExpressionHandler(Access mode, String functionName) {
    result = new ArrayList<>();
    accessMode = mode;
    fName = functionName;
  }

  @Override
  public Void visit(CArraySubscriptExpression expression) {
    addExpression(expression);
    accessMode = Access.READ;
    expression.getArrayExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CBinaryExpression expression) {
    checkArgument(
        accessMode == Access.READ, "Writing to BinaryExpression: %s", expression.toASTString());
    expression.getOperand1().accept(this);
    expression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression expression) {
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldReference expression) {
    addExpression(expression);
    if (expression.isPointerDereference()) {
      accessMode = Access.READ;
      expression.getFieldOwner().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CIdExpression expression) {
    addExpression(expression);
    return null;
  }

  @Override
  public Void visit(CUnaryExpression expression) {
    if (expression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      addExpression(expression);
      return null;
    }
    // In all other unary operation we only read the operand
    accessMode = Access.READ;
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CPointerExpression pPointerExpression) {
    // write: *s =
    addExpression(pPointerExpression);
    accessMode = Access.READ;
    pPointerExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression pComplexCastExpression) {
    pComplexCastExpression.getOperand().accept(this);
    return null;
  }

  private void addExpression(CExpression e) {
    AbstractIdentifier id = Identifiers.createIdentifier(e, fName);
    result.add(Pair.of(id, accessMode));
  }

  public List<Pair<AbstractIdentifier, Access>> getProcessedExpressions() {
    return result;
  }

  @Override
  protected Void visitDefault(CExpression pExp) {
    return null;
  }
}

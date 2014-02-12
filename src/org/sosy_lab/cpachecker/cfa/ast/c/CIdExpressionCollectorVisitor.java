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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.HashSet;
import java.util.Set;


public class CIdExpressionCollectorVisitor extends DefaultCExpressionVisitor<Void, RuntimeException> {

  private final Set<CIdExpression> referencedVariables = new HashSet<>();

  public Set<CIdExpression> getReferencedIdExpressions() {
    return referencedVariables;
  }

  @Override
  protected Void visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Void visit(CIdExpression pIastIdExpression) {
    referencedVariables.add(pIastIdExpression);
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
    pIastArraySubscriptExpression.getArrayExpression().accept(this);
    pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression pIastBinaryExpression) {
    pIastBinaryExpression.getOperand1().accept(this);
    pIastBinaryExpression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CComplexCastExpression pIastCastExpression) {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference) {
    return pIastFieldReference.getFieldOwner().accept(this);
  }

  @Override
  public Void visit(CUnaryExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Void visit(CPointerExpression pIastUnaryExpression) {
    return pIastUnaryExpression.getOperand().accept(this);
  }
}

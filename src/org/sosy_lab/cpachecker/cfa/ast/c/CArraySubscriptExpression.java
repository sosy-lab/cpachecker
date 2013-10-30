/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CArraySubscriptExpression extends CExpression {

  private final CExpression arrayExpression;
  private final CExpression subscriptExpression;

  public CArraySubscriptExpression(final CFileLocation pFileLocation,
                                      final CType pType,
                                      final CExpression pArrayExpression,
                                      final CExpression pSubscriptExpression) {
    super(pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;
  }

  public CExpression getArrayExpression() {
    return arrayExpression;
  }

  public CExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    String left = (arrayExpression instanceof CArraySubscriptExpression) ? arrayExpression.toASTString() : arrayExpression.toParenthesizedASTString();
    return left + "[" + subscriptExpression.toASTString() + "]";
  }
}

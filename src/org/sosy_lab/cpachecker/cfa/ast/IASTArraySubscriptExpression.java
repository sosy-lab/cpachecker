/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

public final class IASTArraySubscriptExpression extends IASTExpression {

  private final IASTExpression arrayExpression;
  private final IASTExpression subscriptExpression;

  public IASTArraySubscriptExpression(final IASTFileLocation pFileLocation,
                                      final IType pType,
                                      final IASTExpression pArrayExpression,
                                      final IASTExpression pSubscriptExpression) {
    super(pFileLocation, pType);
    arrayExpression = pArrayExpression;
    subscriptExpression = pSubscriptExpression;
  }

  public IASTExpression getArrayExpression() {
    return arrayExpression;
  }

  public IASTExpression getSubscriptExpression() {
    return subscriptExpression;
  }

  @Override
  public <R, X extends Exception> R accept(ExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(RightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return arrayExpression.toASTString()
        + "[" + subscriptExpression.toASTString() + "]";
  }
}

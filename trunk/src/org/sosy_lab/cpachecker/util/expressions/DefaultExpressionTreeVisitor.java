/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;


public abstract class DefaultExpressionTreeVisitor<LeafType, T, E extends Throwable>
    implements ExpressionTreeVisitor<LeafType, T, E> {

  protected abstract T visitDefault(ExpressionTree<LeafType> pExpressionTree) throws E;

  @Override
  public T visit(And<LeafType> pAnd) throws E {
    return visitDefault(pAnd);
  }

  @Override
  public T visit(Or<LeafType> pOr) throws E {
    return visitDefault(pOr);
  }

  @Override
  public T visit(LeafExpression<LeafType> pLeafExpression) throws E {
    return visitDefault(pLeafExpression);
  }

  @Override
  public T visitTrue() throws E {
    return visitDefault(ExpressionTrees.<LeafType>getTrue());
  }

  @Override
  public T visitFalse() throws E {
    return visitDefault(ExpressionTrees.<LeafType>getFalse());
  }

}

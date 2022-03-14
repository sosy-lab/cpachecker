// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
    return visitDefault(ExpressionTrees.getTrue());
  }

  @Override
  public T visitFalse() throws E {
    return visitDefault(ExpressionTrees.getFalse());
  }
}

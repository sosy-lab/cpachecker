// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public abstract class ForwardingCExpressionVisitor<R, X extends Exception>
    implements CExpressionVisitor<R, X> {

  protected final CExpressionVisitor<R, X> delegate;

  protected ForwardingCExpressionVisitor(CExpressionVisitor<R, X> pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public R visit(CArraySubscriptExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CBinaryExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CCastExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CComplexCastExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CFieldReference e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CIdExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CCharLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CFloatLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CIntegerLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CImaginaryLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CStringLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CTypeIdExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CUnaryExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(CPointerExpression e) throws X {
    return delegate.visit(e);
  }
}

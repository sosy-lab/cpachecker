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



public abstract class ForwardingCExpressionVisitor<R, X extends Exception>
    implements CExpressionVisitor<R, X> {

  protected final CExpressionVisitor<R, X> delegate;

  public ForwardingCExpressionVisitor(CExpressionVisitor<R, X> pDelegate) {
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
  public R visit(CTypeIdInitializerExpression e) throws X {
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
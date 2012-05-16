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
package org.sosy_lab.cpachecker.cfa.ast;

public abstract class ForwardingExpressionVisitor<R, X extends Exception>
    implements ExpressionVisitor<R, X> {

  protected final ExpressionVisitor<R, X> delegate;

  public ForwardingExpressionVisitor(ExpressionVisitor<R, X> pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public R visit(IASTArraySubscriptExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTBinaryExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTCastExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTFieldReference e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTIdExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTCharLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTFloatLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTIntegerLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTStringLiteralExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTTypeIdExpression e) throws X {
    return delegate.visit(e);
  }

  @Override
  public R visit(IASTUnaryExpression e) throws X {
    return delegate.visit(e);
  }
}
/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import com.google.errorprone.annotations.ForOverride;

/**
 * Class similar to {@link ForwardingJSExpressionVisitor} that allows to have a different return
 * type than the delegate visitor.
 *
 * @param <R> The return type of the methods of this class.
 * @param <D> The return type of the methods of the delegate visitor.
 * @param <X> The (common) exception type.
 */
public abstract class AdaptingJSExpressionVisitor<R, D, X extends Exception>
    implements JSExpressionVisitor<R, X> {

  protected final JSExpressionVisitor<D, X> delegate;

  protected AdaptingJSExpressionVisitor(JSExpressionVisitor<D, X> pDelegate) {
    delegate = pDelegate;
  }

  @ForOverride
  protected abstract R convert(D value, JSExpression exp) throws X;

  @Override
  public R visit(JSBinaryExpression e) throws X {
    return convert(delegate.visit(e), e);
  }

  @Override
  public R visit(JSStringLiteralExpression e) throws X {
    return convert(delegate.visit(e), e);
  }

  @Override
  public R visit(JSUnaryExpression e) throws X {
    return convert(delegate.visit(e), e);
  }
}

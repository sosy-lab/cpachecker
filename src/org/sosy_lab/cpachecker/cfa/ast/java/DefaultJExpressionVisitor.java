// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

public abstract class DefaultJExpressionVisitor<R, X extends Exception> implements JExpressionVisitor<R, X> {

  protected abstract R visitDefault(JExpression e);

  @Override
  public R visit(JIntegerLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JThisExpression e) throws X {
    return visitDefault(e);
  }


  @Override
  public R visit(JCastExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JBooleanLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JFloatLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JCharLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JStringLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JBinaryExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JUnaryExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JArrayCreationExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JArrayInitializer e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JArraySubscriptExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JArrayLengthExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JVariableRunTimeType e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JIdExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JRunTimeTypeEqualsType e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JNullLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JEnumConstantExpression e) throws X {
    return visitDefault(e);
  }

}

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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;





public abstract class DefaultJExpressionVisitor<R ,X extends Exception> implements JExpressionVisitor<R, X> {

  protected abstract R visitDefault(IAExpression e);

  @Override
  public R visit(JIntegerLiteralExpression e) throws X {
    return visitDefault(e);
  }


  @Override
  public R visit(JBooleanLiteralExpression e) throws X{
    return visitDefault(e);
  }

  @Override
  public R visit(JFloatLiteralExpression e) throws X{
    return visitDefault(e);
  }

  @Override
  public R visit(ACharLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(AStringLiteralExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JBinaryExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(AUnaryExpression e) throws X {
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
  public R visit(AArraySubscriptExpression e) throws X {
    return visitDefault(e);
  }

  @Override
  public R visit(JThisRunTimeType e) throws X {
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

}
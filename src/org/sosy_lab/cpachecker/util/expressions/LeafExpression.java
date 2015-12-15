/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.util.Collections;
import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;

import com.google.common.base.Function;


public class LeafExpression implements ExpressionTree {

  private final AExpression expression;

  private LeafExpression(AExpression pExpression) {
    this.expression = pExpression;
  }

  public AExpression getExpression() {
    return expression;
  }

  @Override
  public Iterator<ExpressionTree> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public <T> T accept(ExpressionTreeVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return expression.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof LeafExpression) {
      return expression.equals(((LeafExpression) pObj).expression);
    }
    return false;
  }

  @Override
  public String toString() {
    return ToCodeVisitor.INSTANCE.visit(this);
  }

  public static ExpressionTree of(AExpression pExpression) {
    return new LeafExpression(pExpression);
  }

  public static final Function<AExpressionStatement, LeafExpression> FROM_STATEMENT =
      new Function<AExpressionStatement, LeafExpression>() {

        @Override
        public LeafExpression apply(AExpressionStatement s) {
          return new LeafExpression(s.getExpression());
        }
  };

}

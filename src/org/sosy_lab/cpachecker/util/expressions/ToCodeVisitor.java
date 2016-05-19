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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;


public class ToCodeVisitor<LeafType> extends CachingVisitor<LeafType, String, RuntimeException> {

  private static String wrapInParentheses(String pCode) {
    return "(" + pCode + ")";
  }

  private final Function<? super LeafType, String> leafExpressionToCodeFunction;

  public ToCodeVisitor(Function<? super LeafType, String> pLeafExpressionToCodeFunction) {
    this.leafExpressionToCodeFunction = pLeafExpressionToCodeFunction;
  }

  private final Function<ExpressionTree<LeafType>, String> toParenthesizedCodeFunction() {
    return new Function<ExpressionTree<LeafType>, String>() {

      @Override
      public String apply(ExpressionTree<LeafType> pExpressionTree) {
        return pExpressionTree.accept(
            new ExpressionTreeVisitor<LeafType, String, RuntimeException>() {

              @Override
              public String visit(And<LeafType> pAnd) {
                return wrapInParentheses(pAnd.accept(ToCodeVisitor.this));
              }

              @Override
              public String visit(Or<LeafType> pOr) {
                return wrapInParentheses(pOr.accept(ToCodeVisitor.this));
              }

              @Override
              public String visit(LeafExpression<LeafType> pLeafExpression) {
                return pLeafExpression.accept(ToCodeVisitor.this);
              }

              @Override
              public String visitTrue() {
                return ToCodeVisitor.this.visitTrue();
              }

              @Override
              public String visitFalse() {
                return ToCodeVisitor.this.visitFalse();
              }
            });
      }
    };
  }

  @Override
  protected String cacheMissAnd(And<LeafType> pAnd) {
    assert pAnd.iterator().hasNext();
    return Joiner.on(" && ")
        .join(FluentIterable.from(pAnd).transform(toParenthesizedCodeFunction()));
  }

  @Override
  protected String cacheMissOr(Or<LeafType> pOr) {
    assert pOr.iterator().hasNext();
    return Joiner.on(" || ")
        .join(FluentIterable.from(pOr).transform(toParenthesizedCodeFunction()));
  }

  @Override
  protected String cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) {
    LeafType expression = pLeafExpression.getExpression();
    String expressionCode = leafExpressionToCodeFunction.apply(expression);
    if (pLeafExpression.assumeTruth()) {
      return expressionCode;
    }
    if (!expressionCode.startsWith("(") || !expressionCode.endsWith(")")) {
      expressionCode = wrapInParentheses(expressionCode);
    }
    return "!" + expressionCode;
  }

  @Override
  protected String cacheMissTrue() {
    return "1";
  }

  @Override
  protected String cacheMissFalse() {
    return "0";
  }

}

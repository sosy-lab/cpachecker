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

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;


public class ToCodeVisitor<LeafType> extends CachingVisitor<LeafType, String, RuntimeException> {

  private static final Function<String, String> WRAP_IN_PARENTHESES =
      new Function<String, String>() {

        @Override
        public String apply(String pCode) {
          return "(" + pCode + ")";
        }
      };

  public static ToCodeVisitor<AExpression> A_EXPRESSION_TREE_TO_CODE_VISITOR =
      new ToCodeVisitor<>(
          new Function<AExpression, String>() {

            @Override
            public String apply(AExpression pAExpression) {
              if (!(pAExpression instanceof CExpression)) {
                throw new AssertionError("Unsupported expression.");
              }
              return ((CExpression) pAExpression).accept(CExpressionToOrinalCodeVisitor.INSTANCE);
            }
          });

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
                return WRAP_IN_PARENTHESES.apply(pAnd.accept(ToCodeVisitor.this));
              }

              @Override
              public String visit(Or<LeafType> pOr) {
                return WRAP_IN_PARENTHESES.apply(pOr.accept(ToCodeVisitor.this));
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
      expressionCode = WRAP_IN_PARENTHESES.apply(expressionCode);
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

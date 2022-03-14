// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class ToCodeVisitor<LeafType> extends CachingVisitor<LeafType, String, NoException> {

  private static String wrapInParentheses(String pCode) {
    return "(" + pCode + ")";
  }

  private final Function<? super LeafType, String> leafExpressionToCodeFunction;

  public ToCodeVisitor(Function<? super LeafType, String> pLeafExpressionToCodeFunction) {
    this.leafExpressionToCodeFunction = pLeafExpressionToCodeFunction;
  }

  private String toParenthesizedCode(ExpressionTree<LeafType> pExpressionTree) {
    return pExpressionTree.accept(
        new ExpressionTreeVisitor<LeafType, String, NoException>() {

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

  @Override
  protected String cacheMissAnd(And<LeafType> pAnd) {
    assert pAnd.iterator().hasNext();
    return Joiner.on(" && ").join(FluentIterable.from(pAnd).transform(this::toParenthesizedCode));
  }

  @Override
  protected String cacheMissOr(Or<LeafType> pOr) {
    assert pOr.iterator().hasNext();
    return Joiner.on(" || ").join(FluentIterable.from(pOr).transform(this::toParenthesizedCode));
  }

  @Override
  protected String cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) {
    LeafType expression = pLeafExpression.getExpression();
    String expressionCode = leafExpressionToCodeFunction.apply(expression);
    if (pLeafExpression.assumeTruth()) {
      return expressionCode;
    }
    expressionCode = wrapInParentheses(expressionCode);
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

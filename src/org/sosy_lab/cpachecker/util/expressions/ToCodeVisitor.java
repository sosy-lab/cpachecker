// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import java.util.List;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class ToCodeVisitor<LeafType> extends CachingVisitor<LeafType, String, NoException> {

  private static String wrapInParentheses(String pCode) {
    return "(" + pCode + ")";
  }

  private final Function<? super LeafType, String> leafExpressionToCodeFunction;

  public ToCodeVisitor(Function<? super LeafType, String> pLeafExpressionToCodeFunction) {
    leafExpressionToCodeFunction = pLeafExpressionToCodeFunction;
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

  public static String binaryMerge(List<String> items, String separator) {
    if (items.size() == 1) {
      return items.getFirst();
    }
    if (items.size() == 2) {
      return "(" + items.getFirst() + separator + items.get(1) + ")";
    }

    int mid = items.size() / 2;
    String left = binaryMerge(items.subList(0, mid), separator);
    String right = binaryMerge(items.subList(mid, items.size()), separator);
    return "(" + left + separator + right + ")";
  }

  @Override
  protected String cacheMissAnd(And<LeafType> pAnd) {
    assert pAnd.iterator().hasNext();
    return binaryMerge(from(pAnd).transform(this::toParenthesizedCode).toList(), " && ");
  }

  @Override
  protected String cacheMissOr(Or<LeafType> pOr) {
    assert pOr.iterator().hasNext();
    return binaryMerge(from(pOr).transform(this::toParenthesizedCode).toList(), " || ");
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

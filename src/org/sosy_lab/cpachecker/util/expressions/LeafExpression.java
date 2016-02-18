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

import java.math.BigInteger;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

import com.google.common.base.Function;

public class LeafExpression<LeafType> extends AbstractExpressionTree<LeafType> {

  public static final Function<AExpressionStatement, ExpressionTree<AExpression>>
      FROM_EXPRESSION_STATEMENT =
          new Function<AExpressionStatement, ExpressionTree<AExpression>>() {

            @Override
            public ExpressionTree<AExpression> apply(AExpressionStatement pExpressionStatement) {
              return of(pExpressionStatement.getExpression());
            }
          };

  public static ExpressionTree<AExpression> fromStatement(
      AStatement pStatement, CBinaryExpressionBuilder pBinaryExpressionBuilder) {
    if (pStatement instanceof AExpressionStatement) {
      return FROM_EXPRESSION_STATEMENT.apply((AExpressionStatement) pStatement);
    }
    if (pStatement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) pStatement;
      if (assignment.getRightHandSide() instanceof CExpression) {
        CExpression expression = (CExpression) assignment.getRightHandSide();
        CBinaryExpression assumeExp =
            pBinaryExpressionBuilder.buildBinaryExpressionUnchecked(
                assignment.getLeftHandSide(), expression, CBinaryExpression.BinaryOperator.EQUALS);
        return of((AExpression) assumeExp);
      }
    }
    return ExpressionTrees.getTrue();
  }

  private final LeafType expression;

  private final boolean assumeTruth;

  private LeafExpression(LeafType pExpression, boolean pAssumeTruth) {
    this.expression = Objects.requireNonNull(pExpression);
    this.assumeTruth = pAssumeTruth;
  }

  public LeafType getExpression() {
    return expression;
  }

  public boolean assumeTruth() {
    return assumeTruth;
  }

  @Override
  public <T, E extends Throwable> T accept(ExpressionTreeVisitor<LeafType, T, E> pVisitor)
      throws E {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assumeTruth, expression);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof LeafExpression) {
      LeafExpression<?> other = (LeafExpression<?>) pObj;
      return assumeTruth == other.assumeTruth && expression.equals(other.expression);
    }
    return false;
  }

  public LeafExpression<LeafType> negate() {
    return new LeafExpression<>(expression, !assumeTruth);
  }

  public static <LeafType> ExpressionTree<LeafType> of(LeafType pLeafExpression) {
    return of(pLeafExpression, true);
  }

  public static <LeafType> ExpressionTree<LeafType> of(
      LeafType pLeafExpression, boolean pAssumeTruth) {
    if (pLeafExpression instanceof AIntegerLiteralExpression) {
      AIntegerLiteralExpression expression = (AIntegerLiteralExpression) pLeafExpression;
      if (expression.getValue().equals(BigInteger.ZERO)) {
        return pAssumeTruth
            ? ExpressionTrees.<LeafType>getFalse()
            : ExpressionTrees.<LeafType>getTrue();
      }
      return pAssumeTruth
          ? ExpressionTrees.<LeafType>getTrue()
          : ExpressionTrees.<LeafType>getFalse();
    }
    return new LeafExpression<>(pLeafExpression, pAssumeTruth);
  }

}

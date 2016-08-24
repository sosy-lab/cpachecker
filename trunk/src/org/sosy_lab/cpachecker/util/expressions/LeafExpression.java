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
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

import java.math.BigInteger;
import java.util.Objects;

public class LeafExpression<LeafType> extends AbstractExpressionTree<LeafType> {

  public static ExpressionTree<AExpression> fromStatement(
      AStatement pStatement, CBinaryExpressionBuilder pBinaryExpressionBuilder) {
    if (pStatement instanceof AExpressionStatement) {
      return of(((AExpressionStatement) pStatement).getExpression());
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

  private final int hashCode;

  private LeafExpression(LeafType pExpression, boolean pAssumeTruth, int pHashCode) {
    this.expression = Objects.requireNonNull(pExpression);
    this.assumeTruth = pAssumeTruth;
    this.hashCode = pHashCode;
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
    return hashCode;
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
    return new LeafExpression<>(expression, !assumeTruth, -hashCode);
  }

  public static <LeafType> ExpressionTree<LeafType> of(LeafType pLeafExpression) {
    return of(pLeafExpression, true);
  }

  @SuppressWarnings("unchecked")
  public static <LeafType> ExpressionTree<LeafType> of(
      LeafType pLeafExpression, boolean pAssumeTruth) {
    LeafType leafExpression = pLeafExpression;
    boolean assumeTruth = pAssumeTruth;
    if (leafExpression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) pLeafExpression;
      if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
        assumeTruth = !assumeTruth;
        leafExpression =
            (LeafType)
                new CBinaryExpression(
                    binaryExpression.getFileLocation(),
                    binaryExpression.getExpressionType(),
                    binaryExpression.getCalculationType(),
                    binaryExpression.getOperand1(),
                    binaryExpression.getOperand2(),
                    BinaryOperator.EQUALS);
      }
    }
    if (leafExpression instanceof AIntegerLiteralExpression) {
      AIntegerLiteralExpression expression = (AIntegerLiteralExpression) pLeafExpression;
      if (expression.getValue().equals(BigInteger.ZERO)) {
        return assumeTruth
            ? ExpressionTrees.<LeafType>getFalse()
            : ExpressionTrees.<LeafType>getTrue();
      }
      return assumeTruth
          ? ExpressionTrees.<LeafType>getTrue()
          : ExpressionTrees.<LeafType>getFalse();
    }
    return new LeafExpression<>(leafExpression, assumeTruth, assumeTruth ? leafExpression.hashCode() : -leafExpression.hashCode());
  }

}

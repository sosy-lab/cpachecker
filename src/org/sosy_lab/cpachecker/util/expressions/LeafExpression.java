// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
        return of(assumeExp);
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

  public static <LeafType> ExpressionTree<LeafType> of(
      LeafType pLeafExpression, boolean pAssumeTruth) {
    if (pLeafExpression instanceof CBinaryExpression) {
      // TODO: Find a way to make this work with parsing ExpressionTrees to Cexpressions without
      // having the negation go away
      //      CBinaryExpression binaryExpression = (CBinaryExpression) pLeafExpression;
      //      if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
      //        assumeTruth = !assumeTruth;
      //        leafExpression =
      //            (LeafType)
      //                new CBinaryExpression(
      //                    binaryExpression.getFileLocation(),
      //                    binaryExpression.getExpressionType(),
      //                    binaryExpression.getCalculationType(),
      //                    binaryExpression.getOperand1(),
      //                    binaryExpression.getOperand2(),
      //                    BinaryOperator.EQUALS);
      //      }
    }
    if (pLeafExpression instanceof AIntegerLiteralExpression) {
      AIntegerLiteralExpression expression = (AIntegerLiteralExpression) pLeafExpression;
      if (expression.getValue().equals(BigInteger.ZERO)) {
        return pAssumeTruth ? ExpressionTrees.getFalse() : ExpressionTrees.getTrue();
      }
      return pAssumeTruth ? ExpressionTrees.getTrue() : ExpressionTrees.getFalse();
    }
    if (pLeafExpression instanceof String) {
      String expressionString = (String) pLeafExpression;
      if (expressionString.equals("0")) {
        return pAssumeTruth ? ExpressionTrees.getFalse() : ExpressionTrees.getTrue();
      }
      if (expressionString.equals("1")) {
        return pAssumeTruth ? ExpressionTrees.getTrue() : ExpressionTrees.getFalse();
      }
    }
    return new LeafExpression<>(
        pLeafExpression,
        pAssumeTruth,
        pAssumeTruth ? pLeafExpression.hashCode() : -pLeafExpression.hashCode());
  }
}

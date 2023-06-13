// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

/** Visitor that converts ACSL predicates to expression trees. */
public class ACSLPredicateToExpressionTreeVisitor
    implements ACSLPredicateVisitor<ExpressionTree<Object>, UnrecognizedCodeException> {

  private final ACSLTermToCExpressionVisitor termVisitor;

  public ACSLPredicateToExpressionTreeVisitor(ACSLTermToCExpressionVisitor pTermVisitor) {
    termVisitor = pTermVisitor;
  }

  @Override
  public ExpressionTree<Object> visitTrue() {
    return ExpressionTrees.getTrue();
  }

  @Override
  public ExpressionTree<Object> visitFalse() {
    return ExpressionTrees.getFalse();
  }

  @Override
  public ExpressionTree<Object> visit(ACSLSimplePredicate pred) throws UnrecognizedCodeException {
    return LeafExpression.of(pred.getTerm().accept(termVisitor), !pred.isNegated());
  }

  @Override
  public ExpressionTree<Object> visit(ACSLLogicalPredicate pred) throws UnrecognizedCodeException {
    ExpressionTree<Object> leftTree;
    ExpressionTree<Object> rightTree;
    switch (pred.getOperator()) {
      case AND:
        if (pred.isNegated()) {
          leftTree = pred.getLeft().negate().accept(this);
          rightTree = pred.getRight().negate().accept(this);
          return Or.of(leftTree, rightTree);
        }
        leftTree = pred.getLeft().accept(this);
        rightTree = pred.getRight().accept(this);
        return And.of(leftTree, rightTree);
      case OR:
        if (pred.isNegated()) {
          leftTree = pred.getLeft().negate().accept(this);
          rightTree = pred.getRight().negate().accept(this);
          return And.of(leftTree, rightTree);
        }
        leftTree = pred.getLeft().accept(this);
        rightTree = pred.getRight().accept(this);
        return Or.of(leftTree, rightTree);
      default:
        throw new AssertionError("Operator should be AND or OR");
    }
  }

  @Override
  public ExpressionTree<Object> visit(ACSLTernaryCondition pred) throws UnrecognizedCodeException {
    if (pred.isNegated()) {
      ExpressionTree<Object> left =
          Or.of(pred.getCondition().negate().accept(this), pred.getThen().negate().accept(this));
      ExpressionTree<Object> right =
          Or.of(pred.getCondition().accept(this), pred.getOtherwise().negate().accept(this));
      return And.of(left, right);
    }
    ExpressionTree<Object> left =
        And.of(pred.getCondition().accept(this), pred.getThen().accept(this));
    ExpressionTree<Object> right =
        And.of(pred.getCondition().negate().accept(this), pred.getOtherwise().accept(this));
    return Or.of(left, right);
  }

  @Override
  public ExpressionTree<Object> visit(PredicateAt pred) {
    throw new UnsupportedOperationException(
        "There is currently no concrete translation of \\at available");
  }
}

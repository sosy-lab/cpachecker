// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Iterator;

public class Or<LeafType> extends AbstractExpressionTree<LeafType>
    implements Iterable<ExpressionTree<LeafType>> {

  private final ImmutableSet<ExpressionTree<LeafType>> operands;

  private final int hashCode;

  private Or(ImmutableSet<ExpressionTree<LeafType>> pOperands) {
    assert Iterables.size(pOperands) >= 2;
    assert !Iterables.contains(pOperands, ExpressionTrees.getFalse());
    assert !Iterables.contains(pOperands, ExpressionTrees.getTrue());
    assert !FluentIterable.from(pOperands).anyMatch(Predicates.instanceOf(Or.class));
    operands = pOperands;
    hashCode = operands.hashCode();
  }

  @Override
  public Iterator<ExpressionTree<LeafType>> iterator() {
    return operands.iterator();
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
    if (pObj instanceof Or) {
      Or<?> other = (Or<?>) pObj;
      return hashCode == other.hashCode && operands.equals(other.operands);
    }
    return false;
  }

  public static <LeafType> ExpressionTree<LeafType> of(
      ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return of(ImmutableList.of(pOp1, pOp2));
  }

  public static <LeafType> ExpressionTree<LeafType> of(
      Iterable<ExpressionTree<LeafType>> pOperands) {
    // Filter out trivial operands and flatten the hierarchy
    ImmutableSet.Builder<ExpressionTree<LeafType>> builder = ImmutableSet.builder();
    for (ExpressionTree<LeafType> operand : pOperands) {
      if (ExpressionTrees.getTrue().equals(operand)) {
        // If one of the operands is true, return true
        return ExpressionTrees.getTrue();
      } else if (!ExpressionTrees.getFalse().equals(operand)) {
        if (operand instanceof Or) {
          builder.addAll(((Or<LeafType>) operand).operands);
        } else {
          builder.add(operand);
        }
      }
    }
    ImmutableSet<ExpressionTree<LeafType>> operands = builder.build();

    // If there are no operands, return the neutral element
    if (operands.isEmpty()) {
      return ExpressionTrees.getFalse();
    }
    // If there is only one operand, return it
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    return new Or<>(operands);
  }
}

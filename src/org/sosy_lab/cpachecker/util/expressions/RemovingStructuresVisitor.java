// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RemovingStructuresVisitor<LeafType, E extends Throwable>
    implements ExpressionTreeVisitor<LeafType, ExpressionTree<LeafType>, E> {

  private Function<LeafType, Boolean> removeLeaf;

  public RemovingStructuresVisitor(Function<LeafType, Boolean> pRemoveLeaf) {
    removeLeaf = pRemoveLeaf;
  }

  private List<ExpressionTree<LeafType>> getRemainingOperands(
      Iterator<ExpressionTree<LeafType>> iterator) throws E {
    List<ExpressionTree<LeafType>> newOperands = new ArrayList<>();
    while (iterator.hasNext()) {
      ExpressionTree<LeafType> operand = iterator.next();
      if (operand instanceof LeafExpression<LeafType> leaf) {
        if (removeLeaf.apply(leaf.getExpression())) {
          continue;
        }
      } else {
        operand = operand.accept(this);
      }
      newOperands.add(operand);
    }
    return newOperands;
  }

  @Override
  public ExpressionTree<LeafType> visit(And<LeafType> pAnd) throws E {
    return And.of(getRemainingOperands(pAnd.iterator()));
  }

  @Override
  public ExpressionTree<LeafType> visit(Or<LeafType> pOr) throws E {
    return Or.of(getRemainingOperands(pOr.iterator()));
  }

  @Override
  public ExpressionTree<LeafType> visit(LeafExpression<LeafType> pLeafExpression) throws E {
    if (removeLeaf.apply(pLeafExpression.getExpression())) {
      if (pLeafExpression.assumeTruth()) {
        return ExpressionTrees.getTrue();
      } else {
        return ExpressionTrees.getFalse();
      }
    }

    return pLeafExpression;
  }

  @Override
  public ExpressionTree<LeafType> visitTrue() throws E {
    return ExpressionTrees.getTrue();
  }

  @Override
  public ExpressionTree<LeafType> visitFalse() throws E {
    return ExpressionTrees.getFalse();
  }
}

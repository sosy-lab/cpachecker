// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is the default implementation of {@linkplain ExpressionTreeFactory}, which uses a cache in
 * order to reuse generated {@link ExpressionTree}s when they are requested again.
 *
 * @param <LeafType> the type to use for the leaves of the {@link ExpressionTree} that this factory
 *     generates
 */
public class CachingExpressionTreeFactory<LeafType> implements ExpressionTreeFactory<LeafType> {

  private final Map<ExpressionTree<LeafType>, ExpressionTree<LeafType>> leafCache = new HashMap<>();
  private final Map<Set<ExpressionTree<LeafType>>, ExpressionTree<LeafType>> andCache =
      new HashMap<>();
  private final Map<Set<ExpressionTree<LeafType>>, ExpressionTree<LeafType>> orCache =
      new HashMap<>();

  @Override
  public ExpressionTree<LeafType> leaf(LeafType pLeafType) {
    return leaf(pLeafType, true);
  }

  @Override
  public ExpressionTree<LeafType> leaf(LeafType pLeafExpression, boolean pAssumeTruth) {
    ExpressionTree<LeafType> potentialResult = LeafExpression.of(pLeafExpression, pAssumeTruth);
    return leafCache.computeIfAbsent(potentialResult, ignore -> potentialResult);
  }

  @Override
  public ExpressionTree<LeafType> and(
      ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return and(ImmutableSet.of(pOp1, pOp2));
  }

  @Override
  public ExpressionTree<LeafType> and(Collection<ExpressionTree<LeafType>> pOperands) {
    switch (pOperands.size()) {
      case 0:
        return ExpressionTrees.getTrue();
      case 1:
        return Iterables.getOnlyElement(pOperands);
      default:
        final Set<ExpressionTree<LeafType>> key = ImmutableSet.copyOf(pOperands);
        return andCache.computeIfAbsent(key, ignore -> And.of(key));
    }
  }

  @Override
  public ExpressionTree<LeafType> or(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return or(ImmutableSet.of(pOp1, pOp2));
  }

  @Override
  public ExpressionTree<LeafType> or(Collection<ExpressionTree<LeafType>> pOperands) {
    switch (pOperands.size()) {
      case 0:
        return ExpressionTrees.getFalse();
      case 1:
        return Iterables.getOnlyElement(pOperands);
      default:
        final Set<ExpressionTree<LeafType>> key = ImmutableSet.copyOf(pOperands);
        return orCache.computeIfAbsent(key, ignore -> Or.of(key));
    }
  }
}

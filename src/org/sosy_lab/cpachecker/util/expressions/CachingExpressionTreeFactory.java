/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the default implementation of {@linkplain ExpressionTreeFactory}, which uses a cache
 * in order to reuse generated {@link ExpressionTree}s when they are requested again.
 * @param <LeafType> the type to use for the leaves of the {@link ExpressionTree} that this factory generates
 */
public class CachingExpressionTreeFactory<LeafType> implements ExpressionTreeFactory<LeafType> {

  private final Map<Object, ExpressionTree<LeafType>> leafCache = new HashMap<>();
  private final Map<Object, ExpressionTree<LeafType>> andCache = new HashMap<>();
  private final Map<Object, ExpressionTree<LeafType>> orCache = new HashMap<>();

  @Override
  public ExpressionTree<LeafType> leaf(LeafType pLeafType) {
    return leaf(pLeafType, true);
  }

  @Override
  public ExpressionTree<LeafType> leaf(LeafType pLeafExpression, boolean pAssumeTruth) {
    ExpressionTree<LeafType> potentialResult = LeafExpression.of(pLeafExpression, pAssumeTruth);
    ExpressionTree<LeafType> cachedResult = leafCache.get(potentialResult);
    if (cachedResult == null) {
      leafCache.put(potentialResult, potentialResult);
      return potentialResult;
    }
    return cachedResult;
  }

  @Override
  public ExpressionTree<LeafType> and(
      ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return and(ImmutableSet.of(pOp1, pOp2));
  }

  @Override
  public ExpressionTree<LeafType> and(Iterable<ExpressionTree<LeafType>> pOperands) {
    final Set<ExpressionTree<LeafType>> key;
    if (pOperands instanceof Set) {
      key = (Set<ExpressionTree<LeafType>>) pOperands;
    } else {
      Iterator<ExpressionTree<LeafType>> operandIterator = pOperands.iterator();
      if (!operandIterator.hasNext()) {
        return ExpressionTrees.getTrue();
      }
      ExpressionTree<LeafType> first = operandIterator.next();
      if (!operandIterator.hasNext()) {
        return first;
      }
      ImmutableSet.Builder<ExpressionTree<LeafType>> keyBuilder = ImmutableSet.builder();
      keyBuilder.add(first);
      while (operandIterator.hasNext()) {
        keyBuilder.add(operandIterator.next());
      }
      key = keyBuilder.build();
    }
    ExpressionTree<LeafType> result = andCache.get(key);
    if (result != null) {
      return result;
    }
    result = And.of(key);
    andCache.put(key, result);
    return result;
  }

  @Override
  public ExpressionTree<LeafType> or(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return or(ImmutableSet.of(pOp1, pOp2));
  }

  @Override
  public ExpressionTree<LeafType> or(Iterable<ExpressionTree<LeafType>> pOperands) {
    final Set<ExpressionTree<LeafType>> key;
    if (pOperands instanceof Set) {
      key = (Set<ExpressionTree<LeafType>>) pOperands;
    } else {
      Iterator<ExpressionTree<LeafType>> operandIterator = pOperands.iterator();
      if (!operandIterator.hasNext()) {
        return ExpressionTrees.getFalse();
      }
      ExpressionTree<LeafType> first = operandIterator.next();
      if (!operandIterator.hasNext()) {
        return first;
      }
      ImmutableSet.Builder<ExpressionTree<LeafType>> keyBuilder = ImmutableSet.builder();
      keyBuilder.add(first);
      while (operandIterator.hasNext()) {
        keyBuilder.add(operandIterator.next());
      }
      key = keyBuilder.build();
    }
    ExpressionTree<LeafType> result = orCache.get(key);
    if (result != null) {
      return result;
    }
    result = Or.of(key);
    orCache.put(key, result);
    return result;
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class CachingRelevantPredicatesComputer implements RefineableRelevantPredicatesComputer {

  private final Map<Pair<Block, ImmutableSet<AbstractionPredicate>>, ImmutableSet<AbstractionPredicate>> irrelevantCache = Maps.newHashMap();
  private final Map<Pair<Block, ImmutableSet<AbstractionPredicate>>, ImmutableSet<AbstractionPredicate>> relevantCache = Maps.newHashMap();

  private final RelevantPredicatesComputer delegate;

  public CachingRelevantPredicatesComputer(RelevantPredicatesComputer pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public Set<AbstractionPredicate> getIrrelevantPredicates(
      Block pContext, Collection<AbstractionPredicate> pPredicates) {

    if (pPredicates.isEmpty()) {
      return Collections.emptySet();
    }

    ImmutableSet<AbstractionPredicate> predicates = ImmutableSet.copyOf(pPredicates);
    Pair<Block, ImmutableSet<AbstractionPredicate>> key = Pair.of(pContext, predicates);

    ImmutableSet<AbstractionPredicate> result = irrelevantCache.get(key);
    if (result == null) {
      result = ImmutableSet.copyOf(delegate.getIrrelevantPredicates(pContext, predicates));
      irrelevantCache.put(key, result);
    }

    return result;
  }

  @Override
  public Set<AbstractionPredicate> getRelevantPredicates(Block pContext,
      Collection<AbstractionPredicate> pPredicates) {

    if (pPredicates.isEmpty()) {
      return Collections.emptySet();
    }

    ImmutableSet<AbstractionPredicate> predicates = ImmutableSet.copyOf(pPredicates);
    Pair<Block, ImmutableSet<AbstractionPredicate>> key = Pair.of(pContext, predicates);

    ImmutableSet<AbstractionPredicate> result = relevantCache.get(key);
    if (result == null) {
      result = ImmutableSet.copyOf(delegate.getRelevantPredicates(pContext, predicates));
      relevantCache.put(key, result);
    }

    return result;
  }

  @Override
  public void considerPredicateAsRelevant(Block pBlock, AbstractionPredicate pPredicate) {
    if (delegate instanceof RefineableRelevantPredicatesComputer) {
      RefineableRelevantPredicatesComputer refineableDelegate = (RefineableRelevantPredicatesComputer)delegate;
      refineableDelegate.considerPredicateAsRelevant(pBlock, pPredicate);
      removeCacheEntriesForBlock(pBlock, irrelevantCache);
      removeCacheEntriesForBlock(pBlock, relevantCache);
    }
  }

  static <U, V> void removeCacheEntriesForBlock(Block pBlock, Map<Pair<Block, U>, V> pCache) {
    Collection<Pair<Block, U>> removeKeys = new ArrayList<>();
    for (Pair<Block, U> key : pCache.keySet()) {
      if (key.getFirst().equals(pBlock)) {
        removeKeys.add(key);
      }
    }
    for (Pair<Block, U> key : removeKeys) {
      pCache.remove(key);
    }
  }
}

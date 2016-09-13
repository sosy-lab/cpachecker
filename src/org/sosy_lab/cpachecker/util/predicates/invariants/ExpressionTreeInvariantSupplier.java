/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.invariants;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.LazyLocationMapping;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ReachedSetBasedExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExpressionTreeInvariantSupplier implements ExpressionTreeSupplier {

  private final AggregatedReachedSets aggregatedReached;
  private final CFA cfa;

  private Set<UnmodifiableReachedSet> lastUsedReachedSets = Collections.emptySet();
  private ExpressionTreeSupplier lastInvariantSupplier = TrivialInvariantSupplier.INSTANCE;

  private final Map<UnmodifiableReachedSet, ExpressionTreeSupplier> singleInvariantSuppliers =
      new HashMap<>();

  public ExpressionTreeInvariantSupplier(AggregatedReachedSets pAggregated, CFA pCFA) {
    aggregatedReached = pAggregated;
    cfa = pCFA;
    updateInvariants(); // at initialization we want to update the invariants the first time
  }

  @Override
  public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
    return lastInvariantSupplier.getInvariantFor(pNode);
  }

  public void updateInvariants() {
    Set<UnmodifiableReachedSet> newReached = aggregatedReached.snapShot();
    if (!newReached.equals(lastUsedReachedSets)) {
      // if we have a former aggregated supplier we do only replace the changed parts
      Set<UnmodifiableReachedSet> oldElements = Sets.difference(lastUsedReachedSets, newReached);
      Set<UnmodifiableReachedSet> newElements = Sets.difference(newReached, lastUsedReachedSets);

      oldElements.forEach(r -> singleInvariantSuppliers.remove(r));
      newElements.forEach(
          r ->
              singleInvariantSuppliers.put(
                  r, new ReachedSetBasedExpressionTreeSupplier(new LazyLocationMapping(r), cfa)));

      lastUsedReachedSets = newReached;
      lastInvariantSupplier =
          new AggregatedExpressionTreeSupplier(
              ImmutableSet.copyOf(singleInvariantSuppliers.values()));
    }
  }

  private static class AggregatedExpressionTreeSupplier implements ExpressionTreeSupplier {

    private final Collection<ExpressionTreeSupplier> invariantSuppliers;

    private AggregatedExpressionTreeSupplier(
        ImmutableCollection<ExpressionTreeSupplier> pInvariantSuppliers) {
      invariantSuppliers = checkNotNull(pInvariantSuppliers);
    }

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
      return invariantSuppliers
          .stream()
          .map(s -> s.getInvariantFor(pNode))
          .reduce(ExpressionTrees.getTrue(), And::of);
    }
  }
}

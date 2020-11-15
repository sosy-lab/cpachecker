// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.invariants;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

public class ExpressionTreeInvariantSupplier implements ExpressionTreeSupplier {

  private final AggregatedReachedSets aggregatedReached;
  private final CFA cfa;

  private Set<UnmodifiableReachedSet> lastUsedReachedSets = ImmutableSet.of();
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

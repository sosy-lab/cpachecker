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
package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.FormulaAndTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.LazyLocationMapping;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class AggregatedReachedSets {
  protected final Set<UnmodifiableReachedSet> reachedSets;

  private @Nullable Set<UnmodifiableReachedSet> lastUsedReachedSets = Collections.emptySet();
  private @Nullable AggregatedInvariantSupplier lastInvariantSupplier = null;

  private @Nullable final CFA cfa;
  private final Map<UnmodifiableReachedSet, FormulaAndTreeSupplier> singleInvariantSuppliers =
      new HashMap<>();

  public AggregatedReachedSets() {
    reachedSets = Collections.emptySet();
    cfa = null;
  }

  public AggregatedReachedSets(Set<UnmodifiableReachedSet> pReachedSets, CFA pCfa) {
    reachedSets = pReachedSets;
    cfa = pCfa;
  }

  public Set<UnmodifiableReachedSet> snapShot() {
    synchronized (reachedSets) {
      return ImmutableSet.copyOf(reachedSets);
    }
  }

  public synchronized InvariantSupplier asInvariantSupplier() {
    Set<UnmodifiableReachedSet> tmp = snapShot();
    if (tmp.equals(lastUsedReachedSets)) {
      if (lastInvariantSupplier != null) {
        return lastInvariantSupplier;
      } else {
        return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
      }
    }
    return asSupplier(tmp);
  }

  public synchronized ExpressionTreeSupplier asExpressionTreeSupplier() {
    Set<UnmodifiableReachedSet> tmp = snapShot();
    if (tmp.equals(lastUsedReachedSets)) {
      if (lastInvariantSupplier != null) {
        return lastInvariantSupplier;
      } else {
        return ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
      }
    }
    return asSupplier(tmp);
  }

  private synchronized AggregatedInvariantSupplier asSupplier(Set<UnmodifiableReachedSet> pReachedSets) {
    Preconditions.checkArgument(!pReachedSets.isEmpty());

    // if we have a former aggregated supplier we do only replace the changed parts
    if (lastUsedReachedSets != null) {
      Set<UnmodifiableReachedSet> oldElements = Sets.difference(lastUsedReachedSets, pReachedSets);
      Set<UnmodifiableReachedSet> newElements = Sets.difference(pReachedSets, lastUsedReachedSets);

      oldElements.forEach(r -> singleInvariantSuppliers.remove(r));
      newElements.forEach(
          r ->
              singleInvariantSuppliers.put(
                  r, new FormulaAndTreeSupplier(new LazyLocationMapping(r), cfa)));
    } else {
      pReachedSets.forEach(r -> new FormulaAndTreeSupplier(new LazyLocationMapping(r), cfa));
    }

    lastUsedReachedSets = pReachedSets;
    lastInvariantSupplier = new AggregatedInvariantSupplier(ImmutableSet.copyOf(singleInvariantSuppliers.values()));

    return lastInvariantSupplier;
  }

  private static class AggregatedInvariantSupplier
      implements InvariantSupplier, ExpressionTreeSupplier {

    private final Collection<FormulaAndTreeSupplier> invariantSuppliers;
    private final Map<InvariantsCacheKey, BooleanFormula> cache = new HashMap<>();


    private AggregatedInvariantSupplier(ImmutableCollection<FormulaAndTreeSupplier> pInvariantSuppliers) {
      invariantSuppliers = checkNotNull(pInvariantSuppliers);
    }

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
      InvariantsCacheKey key = new InvariantsCacheKey(pNode, pFmgr, pPfmgr, pContext);
      if (cache.containsKey(key)) {
        return cache.get(key);
      }
      final BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
      BooleanFormula formula =
          invariantSuppliers
              .stream()
              .map(s -> s.getInvariantFor(pNode, pFmgr, pPfmgr, pContext))
              .filter(f -> !bfmgr.isTrue(f))
              .reduce(bfmgr.makeBoolean(true), bfmgr::and);
      cache.put(key, formula);
      return formula;
    }

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
      return invariantSuppliers
          .stream()
          .map(s -> s.getInvariantFor(pNode))
          .reduce(ExpressionTrees.getTrue(), And::of);
    }
  }

  private static class InvariantsCacheKey {
    private final CFANode node;
    private final FormulaManagerView fmgr;
    private final PathFormulaManager pfmgr;
    private final PathFormula context;

    public InvariantsCacheKey(
        CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
      node = pNode;
      fmgr = pFmgr;
      pfmgr = pPfmgr;
      context = pContext;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hashCode(context);
      result = prime * result + Objects.hashCode(fmgr);
      result = prime * result + Objects.hashCode(node);
      result = prime * result + Objects.hashCode(pfmgr);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof InvariantsCacheKey)) {
        return false;
      }

      InvariantsCacheKey other = (InvariantsCacheKey) obj;
      return Objects.equals(node, other.node)
          && Objects.equals(fmgr, other.fmgr)
          && Objects.equals(pfmgr, other.pfmgr)
          && Objects.equals(context, other.context);
    }
  }

  private static class AggregatedThreadedReachedSets extends AggregatedReachedSets {
    private final ReentrantReadWriteLock lock;
    private final List<AggregatedThreadedReachedSets> otherAggregators = new ArrayList<>();

    private AggregatedThreadedReachedSets(final ReentrantReadWriteLock pLock, CFA pCfa, Set<UnmodifiableReachedSet> pReachedSets) {
      super(pReachedSets, pCfa);
      lock = pLock;
    }

    @Override
    public Set<UnmodifiableReachedSet> snapShot() {
      lock.readLock().lock();
      try {
        return Sets.union(
            super.snapShot(),
            otherAggregators
                .stream()
                .flatMap(s -> s.snapShot().stream())
                .collect(Collectors.toSet()));
      } finally {
        lock.readLock().unlock();
      }
    }

    public void concat(AggregatedThreadedReachedSets other) {
      otherAggregators.add(other);
    }
  }

  public static class AggregatedReachedSetManager {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AggregatedThreadedReachedSets reachedView;
    private final Set<UnmodifiableReachedSet> reachedSets = ConcurrentHashMap.newKeySet();

    public AggregatedReachedSetManager(CFA pCfa) {
      reachedView = new AggregatedThreadedReachedSets(lock, pCfa, reachedSets);
    }

    public void addReachedSet(UnmodifiableReachedSet reached) {
      lock.writeLock().lock();
      try {
        reachedSets.add(reached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public void updateReachedSet(
        UnmodifiableReachedSet oldReached, UnmodifiableReachedSet newReached) {
      lock.writeLock().lock();
      try {
        reachedSets.remove(oldReached);
        reachedSets.add(newReached);
      } finally {
        lock.writeLock().unlock();
      }
    }

    public AggregatedReachedSets asView() {
      return reachedView;
    }

    public synchronized void addAggregated(AggregatedReachedSets pAggregatedReachedSets) {
      lock.writeLock().lock();

      try {
        if (pAggregatedReachedSets instanceof AggregatedThreadedReachedSets) {
          reachedView.concat((AggregatedThreadedReachedSets) pAggregatedReachedSets);

        } else {
          reachedSets.addAll(pAggregatedReachedSets.reachedSets);
        }

      } finally {
        lock.writeLock().unlock();
      }
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * Contains all information about an interpolation sequence that was the result from a spurious
 * counterexample.
 */
class InterpolationSequence {

  static class Builder {

    private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

    private final NavigableSet<IndexedAbstractionPredicate> navigablePredicates;

    private final Multimap<CFANode, IndexedAbstractionPredicate> localPredicates;
    private final Multimap<CFANode, AbstractionPredicate> localPredCache;

    /**
     * Builder for {@link InterpolationSequence} that stores all predicates only once and in the
     * order in which they first appear.
     */
    Builder() {
      // To achieve the ordering for predicates a keyset with naturally-ordered tree-structure is
      // used. The predicates get numerically indexed when added to the respective collection.

      // The predicates are mapped to a set of location nodes. They are the scope in which the
      // predicates may hold.
      navigablePredicates = new TreeSet<>();

      localPredicates = MultimapBuilder.treeKeys().hashSetValues().build();
      localPredCache = HashMultimap.create();
    }

    @CanIgnoreReturnValue
    Builder addPredicates(
        LocationInstance pLocInstance, Collection<AbstractionPredicate> pPredicates) {
      for (AbstractionPredicate abstractionPredicate : pPredicates) {
        if (localPredCache.put(pLocInstance.getLocation(), abstractionPredicate)) {
          // There was no such value previously associated with the given key, meaning
          // this specific key-value pair is now added for the first time.
          IndexedAbstractionPredicate indexedPred =
              new IndexedAbstractionPredicate(ID_GENERATOR.getFreshId(), abstractionPredicate);

          // verify that the added predicate is unique and has not already been there before
          verify(navigablePredicates.add(indexedPred));
          verify(localPredicates.put(pLocInstance.getLocation(), indexedPred));
        }
      }
      return this;
    }

    InterpolationSequence build() {
      return new InterpolationSequence(navigablePredicates, localPredicates);
    }
  }

  /**
   * Set containing predicates in the order in which the solver has returned them as part of a
   * counterexample.
   */
  private final ImmutableSortedSet<IndexedAbstractionPredicate> orderedPredicates;

  private final ImmutableSetMultimap<CFANode, IndexedAbstractionPredicate> localPredicates;

  private InterpolationSequence(
      NavigableSet<IndexedAbstractionPredicate> pNavigablePredicates,
      Multimap<CFANode, IndexedAbstractionPredicate> pLocalPredicates) {
    orderedPredicates = ImmutableSortedSet.copyOfSorted(pNavigablePredicates);

    localPredicates = ImmutableSetMultimap.copyOf(pLocalPredicates);
  }

  private ImmutableSet<IndexedAbstractionPredicate> getPredicates(
      LocationInstance pLocationInstance) {
    return localPredicates.get(pLocationInstance.getLocation());
  }

  boolean isInScopeOf(LocationInstance pLocationInstance) {
    return !getPredicates(pLocationInstance).isEmpty();
  }

  Optional<IndexedAbstractionPredicate> getFirst(LocationInstance PLocationInstance) {
    return getPredicates(PLocationInstance).stream().findFirst();
  }

  Optional<IndexedAbstractionPredicate> getNextIndex(IndexedAbstractionPredicate pPredicate) {
    return Optional.ofNullable(orderedPredicates.higher(pPredicate));
  }

  /**
   * Returns the next {@link IndexedAbstractionPredicate} from this sequence that has a different
   * {@link AbstractionPredicate} than the one passed to this method.
   *
   * @return An {@link Optional} containing the next such {@link IndexedAbstractionPredicate}, or
   *     {@link Optional#empty()} if no such predicate is available.
   */
  Optional<IndexedAbstractionPredicate> getNextPredicate(IndexedAbstractionPredicate pPredicate) {
    IndexedAbstractionPredicate curPred = pPredicate;

    while (true) {
      IndexedAbstractionPredicate nextPred = orderedPredicates.higher(curPred);
      if (nextPred == null) {
        return Optional.empty();
      }

      if (pPredicate.getPredicate().equals(nextPred.getPredicate())) {
        curPred = nextPred;
        continue;
      }

      return Optional.of(nextPred);
    }
  }

  boolean isStrictSubsetOf(InterpolationSequence pOtherSequence) {
    if (equals(pOtherSequence)) {
      return false;
    }

    // TODO: implement a better data structure that allows more easily to check
    // for a subset.
    // The implementation below is likely quite inefficient and hence only a workaround for now.
    return convertedPredicates(pOtherSequence.localPredicates.entries())
        .containsAll(convertedPredicates(localPredicates.entries()));
  }

  private ImmutableSet<Entry<CFANode, AbstractionPredicate>> convertedPredicates(
      ImmutableSet<Entry<CFANode, IndexedAbstractionPredicate>> entries) {
    return transformedImmutableSetCopy(
        entries, x -> Map.entry(x.getKey(), x.getValue().getPredicate()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderedPredicates, localPredicates);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (!(pObj instanceof InterpolationSequence)) {
      return false;
    }
    InterpolationSequence other = (InterpolationSequence) pObj;
    return Objects.equals(orderedPredicates, other.orderedPredicates)
        && Objects.equals(localPredicates, other.localPredicates);
  }

  @Override
  public String toString() {
    return Joiner.on("\n").join(localPredicates.entries());
  }
}

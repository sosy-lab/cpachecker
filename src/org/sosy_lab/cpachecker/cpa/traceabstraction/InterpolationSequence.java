// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * Contains all information about an interpolation sequence that was the result from a spurious
 * counterexample.
 */
class InterpolationSequence {

  static class Builder {

    private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

    private final Multimap<String, IndexedAbstractionPredicate> functionPredicates;
    private final Set<IndexedAbstractionPredicate> globalPredicates;

    private final Multimap<String, AbstractionPredicate> functionPredCache;
    private final Set<AbstractionPredicate> globalPredCache;

    /**
     * Builder for {@link InterpolationSequence} in which all predicates are stored only once and in
     * the ordering in which they first appear in the respective collection.
     */
    Builder() {
      // To achieve the ordering a tree-structure for the collections are used.
      // The predicates are numerically indexed once they are put into the collection.
      functionPredicates = MultimapBuilder.linkedHashKeys().treeSetValues().build();
      globalPredicates = new TreeSet<>();

      functionPredCache = HashMultimap.create();
      globalPredCache = new HashSet<>();
    }

    Builder addFunctionPredicates(
        String pFunctionName, Set<AbstractionPredicate> pFunctionPredicates) {
      for (AbstractionPredicate abstractionPredicate : pFunctionPredicates) {
        if (functionPredCache.put(pFunctionName, abstractionPredicate)) {
          // There was no such value previously associated with the given key, meaning
          // this specific key-value pair is now added for the first time.
          functionPredicates.put(
              pFunctionName,
              new IndexedAbstractionPredicate(ID_GENERATOR.getFreshId(), abstractionPredicate));
        }
      }
      return this;
    }

    Builder addGlobalPredicate(AbstractionPredicate pGlobalPredicate) {
      if (globalPredCache.add(pGlobalPredicate)) {
        globalPredicates.add(
            new IndexedAbstractionPredicate(ID_GENERATOR.getFreshId(), pGlobalPredicate));
      }
      return this;
    }

    InterpolationSequence build() {
      assert sanityCheck()
          : "InterpolationSequence consists of more than *one* set of predicates "
              + "(either one of global-, local-, or function-predicates are allowed)";
      return new InterpolationSequence(functionPredicates, globalPredicates);
    }

    private boolean sanityCheck() {
      int count = 0;
      if (!functionPredicates.isEmpty()) {
        count++;
      }
      if (!globalPredicates.isEmpty()) {
        count++;
      }
      return count == 1;
    }
  }

  private final ImmutableSetMultimap<String, IndexedAbstractionPredicate> functionPredicates;
  private final ImmutableSet<IndexedAbstractionPredicate> globalPredicates;

  private InterpolationSequence(
      Multimap<String, IndexedAbstractionPredicate> pFunctionPredicates,
      Set<IndexedAbstractionPredicate> pGlobalPredicates) {
    functionPredicates =
        ImmutableSetMultimap.<String, IndexedAbstractionPredicate>builder()
            .orderValuesBy(IndexedAbstractionPredicate::compareTo)
            .putAll(pFunctionPredicates)
            .build();
    globalPredicates = ImmutableSet.copyOf(pGlobalPredicates);

    verify(
        functionPredicates
            .asMap()
            .values()
            .stream()
            .allMatch(x -> x instanceof ImmutableSortedSet));
  }

  private ImmutableSet<IndexedAbstractionPredicate> getPredicates(
      LocationInstance pLocationInstance) {
    ImmutableSet<IndexedAbstractionPredicate> result =
        functionPredicates.get(pLocationInstance.getFunctionName());
    if (result.isEmpty()) {
      result = globalPredicates;
    }
    return result;
  }

  boolean isInScopeOf(LocationInstance pLocationInstance) {
    return !getPredicates(pLocationInstance).isEmpty();
  }

  Optional<IndexedAbstractionPredicate> getFirst(LocationInstance PLocationInstance) {
    return getPredicates(PLocationInstance).stream().findFirst();
  }

  Optional<IndexedAbstractionPredicate> getNext(
      LocationInstance pLocationInstance, IndexedAbstractionPredicate pPredicate) {
    ImmutableSortedSet<IndexedAbstractionPredicate> predicates =
        (ImmutableSortedSet<IndexedAbstractionPredicate>)
            functionPredicates.asMap().get(pLocationInstance.getFunctionName());

    return Optional.ofNullable(predicates.higher(pPredicate));
  }

  boolean isStrictSubsetOf(InterpolationSequence pOtherSequence) {
    if (this.equals(pOtherSequence)) {
      return false;
    }
    return pOtherSequence.getGlobalPredicates().containsAll(getGlobalPredicates())
        && pOtherSequence
            .getFunctionPredicates()
            .entries()
            .containsAll(getFunctionPredicates().entries());
  }

  private ImmutableSet<IndexedAbstractionPredicate> getGlobalPredicates() {
    return globalPredicates;
  }

  private ImmutableSetMultimap<String, IndexedAbstractionPredicate> getFunctionPredicates() {
    return functionPredicates;
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionPredicates, globalPredicates);
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
    return Objects.equals(functionPredicates, other.functionPredicates)
        && Objects.equals(globalPredicates, other.globalPredicates);
  }

  @Override
  public String toString() {
    if (!functionPredicates.isEmpty()) {
      return functionPredicates.toString();
    }
    if (!globalPredicates.isEmpty()) {
      return globalPredicates.toString();
    }
    throw new AssertionError(
        InterpolationSequence.class.getSimpleName() + " should not have empty predicates");
  }
}

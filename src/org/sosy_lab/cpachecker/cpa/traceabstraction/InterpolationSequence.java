// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * Contains all information about an interpolation sequence that was the result from a spurious
 * counterexample.
 */
class InterpolationSequence {

  static class Builder {

    private final Multimap<String, AbstractionPredicate> functionPredicates;
    private final Set<AbstractionPredicate> globalPredicates;

    Builder() {
      // We want all predicates to be stored only once and in the ordering in which they first
      // appeared in the respective collection. This yields for every location type
      // (function, global, etc.)
      functionPredicates = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
      globalPredicates = new LinkedHashSet<>();
    }

    Builder addFunctionPredicates(
        String pFunctionName, Set<AbstractionPredicate> pFunctionPredicates) {
      assert checkOrdering(functionPredicates.get(pFunctionName), pFunctionPredicates)
          : "Sequence of interpolants is inconsistent";
      functionPredicates.putAll(pFunctionName, pFunctionPredicates);
      return this;
    }

    Builder addGlobalPredicate(AbstractionPredicate pGlobalPredicate) {
      assert checkOrdering(globalPredicates, pGlobalPredicate)
          : "Sequence of interpolants is inconsistent";
      globalPredicates.add(pGlobalPredicate);
      return this;
    }

    InterpolationSequence build() {
      assert sanityCheck()
          : "InterpolationSequence consists of more than *one* set of predicates "
              + "(either one of global-, local-, or function-predicates are allowed)";
      return new InterpolationSequence(functionPredicates, globalPredicates);
    }

    private boolean checkOrdering(
        Collection<AbstractionPredicate> pPredicates, Set<AbstractionPredicate> pPredicatesToAdd) {
      if (pPredicatesToAdd.size() > 1) {
        throw new UnsupportedOperationException(
            "InterpolationSequence only allows to add one new predicate at a time");
      }

      return checkOrdering(pPredicates, Iterables.getOnlyElement(pPredicatesToAdd));
    }

    /**
     * Check that if a predicate is already contained in the collection, then the position is always
     * only the direct predecessor
     *
     * <p>In other words, if the interpolants a -> a -> b -> b -> c were given before, and c is
     * again given as interpolant, then c may not appear before the last occurrence of b
     */
    private boolean checkOrdering(
        Collection<AbstractionPredicate> pPredicates, AbstractionPredicate pPredicateToAdd) {
      return !pPredicates.contains(pPredicateToAdd)
          || Iterables.getLast(pPredicates).equals(pPredicateToAdd);
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

  private final ImmutableSetMultimap<String, AbstractionPredicate> functionPredicates;
  private final ImmutableSet<AbstractionPredicate> globalPredicates;

  private InterpolationSequence(
      Multimap<String, AbstractionPredicate> pFunctionPredicates,
      Set<AbstractionPredicate> pGlobalPredicates) {
    functionPredicates = ImmutableSetMultimap.copyOf(pFunctionPredicates);
    globalPredicates = ImmutableSet.copyOf(pGlobalPredicates);
  }

  ImmutableSet<AbstractionPredicate> getPredicates(CFANode loc, int locInstance) {
    return getPredicates(new LocationInstance(loc, locInstance));
  }

  ImmutableSet<AbstractionPredicate> getPredicates(LocationInstance locationInstance) {
    ImmutableSet<AbstractionPredicate> result =
        functionPredicates.get(locationInstance.getFunctionName());
    if (result.isEmpty()) {
      result = globalPredicates;
    }
    return result;
  }

  AbstractionPredicate getFirst(LocationInstance locationInstance) {
    return getPredicates(locationInstance).stream().findFirst().orElseThrow();
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionPredicates, globalPredicates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InterpolationSequence)) {
      return false;
    }
    InterpolationSequence other = (InterpolationSequence) obj;
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

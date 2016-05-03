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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents the precision of the PredicateCPA.
 * It is basically a map which assigns to each node in the CFA a (possibly empty)
 * set of predicates which should be used at this location.
 * Additionally, there may be some predicates which are used for all locations
 * ("global" predicates), and some predicates which are used for all locations
 * within a specific function.
 *
 * All instances of this class are immutable.
 */
public class PredicatePrecision implements Precision {

  private final ImmutableSetMultimap<Pair<CFANode, Integer>, AbstractionPredicate> mLocationInstancePredicates;
  private final ImmutableSetMultimap<CFANode, AbstractionPredicate> mLocalPredicates;
  private final ImmutableSetMultimap<String, AbstractionPredicate> mFunctionPredicates;
  private final ImmutableSet<AbstractionPredicate> mGlobalPredicates;

  private static final PredicatePrecision EMPTY =
      new PredicatePrecision(
          ImmutableList.<Map.Entry<Pair<CFANode, Integer>, AbstractionPredicate>>of(),
          ImmutableList.<Map.Entry<CFANode, AbstractionPredicate>>of(),
          ImmutableList.<Map.Entry<String, AbstractionPredicate>>of(),
          ImmutableList.<AbstractionPredicate>of());

  public PredicatePrecision(
      Multimap<Pair<CFANode, Integer>, AbstractionPredicate> pLocationInstancePredicates,
      Multimap<CFANode, AbstractionPredicate> pLocalPredicates,
      Multimap<String, AbstractionPredicate> pFunctionPredicates,
      Iterable<AbstractionPredicate> pGlobalPredicates) {
    this(
        pLocationInstancePredicates.entries(),
        pLocalPredicates.entries(),
        pFunctionPredicates.entries(),
        pGlobalPredicates);
  }

  public PredicatePrecision(
      Iterable<Map.Entry<Pair<CFANode, Integer>, AbstractionPredicate>> pLocationInstancePredicates,
      Iterable<Map.Entry<CFANode, AbstractionPredicate>> pLocalPredicates,
      Iterable<Map.Entry<String, AbstractionPredicate>> pFunctionPredicates,
      Iterable<AbstractionPredicate> pGlobalPredicates) {
    // We want the precision to have
    // - no duplicate predicates,
    // - deterministic order, and
    // - fast, easy, and consistent lookup.
    // The latter means that for example the functionPredicates should include globalPredicates
    // for all functions where there is some specific predicate,
    // such that we need to look at globalPredicates only if there is no entry for a given function
    // in functionPredicates (and the same for localPredicates and locationInstancePredicates).
    // In other words, we compute the union over globalPredicates, functionPredicates, etc. eagerly.

    // The following code achieves all these points by sorting keys by their natural order,
    // keeping the iteration order of the predicate sets as they are,
    // and merging the sets where necessary.
    // It relies on the fact that a Multimap with treeKeys() and arrayListValues()
    // produces exactly the iteration order that we want,
    // and ImmutableSetMultimap.copyOf() preserves the order and removes duplicates.
    // We do two copies of each map here, but we cannot do better for creating
    // a sorted and immutable Multimap (even ImmutableMultimap.Builder copies twice when sorting).
    // Accepting Iterable<Map.Entry<...>> as parameters is no disadvantage here,
    // and makes mergeWith() and the various addSomethingPredicates() methods more efficient.

    mGlobalPredicates = ImmutableSet.copyOf(pGlobalPredicates);

    Multimap<String, AbstractionPredicate> functionPredicates =
        MultimapBuilder.treeKeys().arrayListValues().build();
    putAll(pFunctionPredicates, functionPredicates);
    for (String function : functionPredicates.keySet()) {
      functionPredicates.putAll(function, mGlobalPredicates);
    }
    mFunctionPredicates = ImmutableSetMultimap.copyOf(functionPredicates);

    Multimap<CFANode, AbstractionPredicate> localPredicates =
        MultimapBuilder.treeKeys().arrayListValues().build();
    putAll(pLocalPredicates, localPredicates);
    for (CFANode node : localPredicates.keySet()) {
      localPredicates.putAll(node, mFunctionPredicates.get(node.getFunctionName()));
      localPredicates.putAll(node, mGlobalPredicates);
    }
    mLocalPredicates = ImmutableSetMultimap.copyOf(localPredicates);

    Multimap<Pair<CFANode, Integer>, AbstractionPredicate> locationInstancePredicates =
        MultimapBuilder.treeKeys(Pair.<CFANode, Integer>lexicographicalNaturalComparator())
            .arrayListValues()
            .build();
    putAll(pLocationInstancePredicates, locationInstancePredicates);
    for (Pair<CFANode, Integer> location : locationInstancePredicates.keySet()) {
      locationInstancePredicates.putAll(location, mLocalPredicates.get(location.getFirst()));
      locationInstancePredicates.putAll(
          location, mFunctionPredicates.get(location.getFirst().getFunctionName()));
      locationInstancePredicates.putAll(location, mGlobalPredicates);
    }
    mLocationInstancePredicates = ImmutableSetMultimap.copyOf(locationInstancePredicates);
  }

  private static <K, V> void putAll(Iterable<Map.Entry<K, V>> entries, Multimap<K, V> map) {
    for (Map.Entry<K, V> entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Create a new, empty precision.
   */
  public static PredicatePrecision empty() {
    return EMPTY;
  }

  /**
   * Return a table of the location-instance-specific predicates.
   * These are the predicates that should be used at the n-th instance
   * of an abstraction location l in the current path.
   */
  public final ImmutableSetMultimap<Pair<CFANode, Integer>, AbstractionPredicate>
      getLocationInstancePredicates() {
    return mLocationInstancePredicates;
  }

  /**
   * Return a map view of the location-specific predicates of this precision.
   */
  public final ImmutableSetMultimap<CFANode, AbstractionPredicate> getLocalPredicates() {
    return mLocalPredicates;
  }

  /**
   * Return a map view of the function-specific predicates of this precision.
   */
  public final ImmutableSetMultimap<String, AbstractionPredicate> getFunctionPredicates() {
    return mFunctionPredicates;
  }

  /**
   * Return all global predicates in this precision.
   */
  public final ImmutableSet<AbstractionPredicate> getGlobalPredicates() {
    return mGlobalPredicates;
  }

  /**
   * Return all predicates for one specific location in this precision.
   * @param loc A CFA location.
   * @param locInstance How often this location has appeared in the current path.
   */
  public final ImmutableSet<AbstractionPredicate> getPredicates(CFANode loc, Integer locInstance) {
    ImmutableSet<AbstractionPredicate> result =
        getLocationInstancePredicates().get(Pair.of(loc, locInstance));
    if (result.isEmpty()) {
      result = getLocalPredicates().get(loc);
    }
    if (result.isEmpty()) {
      result = getFunctionPredicates().get(loc.getFunctionName());
    }
    if (result.isEmpty()) {
      result = getGlobalPredicates();
    }
    return result;
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional global predicates.
   */
  public PredicatePrecision addGlobalPredicates(Collection<AbstractionPredicate> newPredicates) {
    return new PredicatePrecision(
        getLocationInstancePredicates(),
        getLocalPredicates(),
        getFunctionPredicates(),
        Iterables.concat(getGlobalPredicates(), newPredicates));
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional function-specific predicates.
   */
  public PredicatePrecision addFunctionPredicates(
      Iterable<Map.Entry<String, AbstractionPredicate>> newPredicates) {
    if (Iterables.isEmpty(newPredicates)) {
      return this;
    }
    return new PredicatePrecision(
        getLocationInstancePredicates().entries(),
        getLocalPredicates().entries(),
        Iterables.concat(getFunctionPredicates().entries(), newPredicates),
        getGlobalPredicates());
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional location-specific predicates.
   */
  public PredicatePrecision addLocalPredicates(
      Iterable<Map.Entry<CFANode, AbstractionPredicate>> newPredicates) {
    if (Iterables.isEmpty(newPredicates)) {
      return this;
    }
    return new PredicatePrecision(
        getLocationInstancePredicates().entries(),
        Iterables.concat(getLocalPredicates().entries(), newPredicates),
        getFunctionPredicates().entries(),
        getGlobalPredicates());
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional location-instance-specific predicates.
   */
  public PredicatePrecision addLocationInstancePredicates(
      Iterable<Map.Entry<Pair<CFANode, Integer>, AbstractionPredicate>> newPredicates) {
    if (Iterables.isEmpty(newPredicates)) {
      return this;
    }
    return new PredicatePrecision(
        Iterables.concat(getLocationInstancePredicates().entries(), newPredicates),
        getLocalPredicates().entries(),
        getFunctionPredicates().entries(),
        getGlobalPredicates());
  }

  /**
   * Create a new precision which contains all predicates of this precision
   * and a second one.
   */
  public PredicatePrecision mergeWith(PredicatePrecision prec) {
    return new PredicatePrecision(
        Iterables.concat(
            getLocationInstancePredicates().entries(),
            prec.getLocationInstancePredicates().entries()),
        Iterables.concat(getLocalPredicates().entries(), prec.getLocalPredicates().entries()),
        Iterables.concat(getFunctionPredicates().entries(), prec.getFunctionPredicates().entries()),
        Iterables.concat(getGlobalPredicates(), prec.getGlobalPredicates()));
  }

  /**
   * Calculates a "difference" from this precision to another precision.
   * The difference is the number of predicates which are present in this precision,
   * and are missing in the other precision.
   * If a predicate is present in both precisions, but for different locations,
   * this counts as a difference.
   *
   * Note that this difference is not symmetric!
   * (Similar to {@link Sets#difference(Set, Set)}).
   */
  public int calculateDifferenceTo(PredicatePrecision other) {
    int difference = 0;
    difference += Sets.difference(this.getGlobalPredicates(),
                                  other.getGlobalPredicates()).size();

    difference += Sets.difference(this.getFunctionPredicates().entries(),
                                  other.getFunctionPredicates().entries()).size();

    difference += Sets.difference(this.getLocalPredicates().entries(),
                                  other.getLocalPredicates().entries()).size();

    difference += Sets.difference(this.getLocationInstancePredicates().entries(),
                                  other.getLocationInstancePredicates().entries()).size();
    return difference;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getGlobalPredicates(),
                             getFunctionPredicates(),
                             getLocalPredicates(),
                             getLocationInstancePredicates());
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj == null) {
      return false;
    } else if (!(pObj.getClass().equals(this.getClass()))) {
      return false;
    } else {
      PredicatePrecision other = (PredicatePrecision)pObj;
      return getLocationInstancePredicates().equals(other.getLocationInstancePredicates())
          && getLocalPredicates().equals(other.getLocalPredicates())
          && getFunctionPredicates().equals(other.getFunctionPredicates())
          && getGlobalPredicates().equals(other.getGlobalPredicates());
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (!getGlobalPredicates().isEmpty())  {
      sb.append("global predicates: ");
      sb.append(getGlobalPredicates());
    }
    if (!getFunctionPredicates().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("function predicates: ");
      sb.append(getFunctionPredicates());
    }
    if (!getLocalPredicates().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("local predicates: ");
      sb.append(getLocalPredicates());
    }
    if (!getLocationInstancePredicates().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("location-instance predicates: ");
      sb.append(getLocationInstancePredicates());
    }

    if (sb.length() == 0) {
      return "empty";
    } else {
      return sb.toString();
    }
  }
}
/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This class represents the precision of the PredicateCPA.
 * It is basically a map which assigns to each node in the CFA a (possibly empty)
 * set of predicates which should be used at this location.
 * Additionally, there may be some predicates which are used for all locations
 * ("global" predicates).
 *
 * All instances of this class are immutable.
 */
public class PredicatePrecision implements Precision {

  private final ImmutableSetMultimap<CFANode, AbstractionPredicate> localPredicates;
  private final ImmutableSet<AbstractionPredicate> globalPredicates;
  private final int id = idCounter++;
  private static int idCounter = 0;

  public PredicatePrecision(Multimap<CFANode, AbstractionPredicate> pLocalPredicates,
      Collection<AbstractionPredicate> pGlobalPredicates) {
    localPredicates = ImmutableSetMultimap.copyOf(pLocalPredicates);
    globalPredicates = ImmutableSet.copyOf(pGlobalPredicates);
  }

  /**
   * Create a new, empty precision.
   */
  public static PredicatePrecision empty() {
    return new PredicatePrecision(
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>of(),
        ImmutableSet.<AbstractionPredicate>of());
  }

  /**
   * Return a map view of the location-specific predicates of this precision.
   */
  public SetMultimap<CFANode, AbstractionPredicate> getLocalPredicates() {
    return localPredicates;
  }

  /**
   * Return all global predicates in this precision.
   */
  public Set<AbstractionPredicate> getGlobalPredicates() {
    return globalPredicates;
  }

  /**
   * Return all predicates for one specific location in this precision.
   * Note that this may be difference from <code>getPredicateMap().get(loc)</code>
   * if there are global predicates.
   * @param loc A CFA location.
   */
  public Set<AbstractionPredicate> getPredicates(CFANode loc) {
    Set<AbstractionPredicate> result = localPredicates.get(loc);
    if (result.isEmpty()) {
      result = globalPredicates;
    }
    return result;
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional global predicates.
   */
  public PredicatePrecision addGlobalPredicates(Collection<AbstractionPredicate> newPredicates) {
    List<AbstractionPredicate> predicates = Lists.newArrayList(globalPredicates);
    predicates.addAll(newPredicates);
    return new PredicatePrecision(localPredicates, predicates);
  }

  /**
   * Create a new precision which is a copy of the current one with some
   * additional location-specific predicates.
   */
  public PredicatePrecision addLocalPredicates(Multimap<CFANode, AbstractionPredicate> newPredicates) {
    Multimap<CFANode, AbstractionPredicate> predicates = ArrayListMultimap.create(localPredicates);
    predicates.putAll(newPredicates);

    // During lookup, we do not look into globalPredicates,
    // if there is something for the key in predicates.
    // Thus, we copy the relevant items into the predicates set here.
    if (!globalPredicates.isEmpty()) {
      for (CFANode newLoc : newPredicates.keySet()) {
        predicates.putAll(newLoc, globalPredicates);
      }
    }

    return new PredicatePrecision(predicates, globalPredicates);
  }

  /**
   * Create a new precision which contains all predicates of this precision
   * and a second one.
   */
  public PredicatePrecision mergeWith(PredicatePrecision prec) {
    Collection<AbstractionPredicate> newGlobalPredicates = Lists.newArrayList(globalPredicates);
    newGlobalPredicates.addAll(prec.globalPredicates);
    newGlobalPredicates = ImmutableSet.copyOf(newGlobalPredicates);

    Multimap<CFANode, AbstractionPredicate> newLocalPredicates = ArrayListMultimap.create(localPredicates);
    newLocalPredicates.putAll(prec.localPredicates);

    if (!newGlobalPredicates.isEmpty()) {
      for (CFANode loc : newLocalPredicates.keySet()) {
        newLocalPredicates.putAll(loc, newGlobalPredicates);
      }
    }

    return new PredicatePrecision(newLocalPredicates, newGlobalPredicates);
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
    difference += Sets.difference(this.globalPredicates,
                                  other.globalPredicates).size();

    difference += Sets.difference(this.functionPredicates.entries(),
                                  other.functionPredicates.entries()).size();

    difference += Sets.difference(this.localPredicates.entries(),
                                  other.localPredicates.entries()).size();
    return difference;
  }

  @Override
  public int hashCode() {
    return localPredicates.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (!(pObj instanceof PredicatePrecision)) {
      return false;
    } else {
      return localPredicates.equals(((PredicatePrecision)pObj).localPredicates);
    }
  }

  @Override
  public String toString() {
    return (globalPredicates.isEmpty() ? "" : "global predicates: " + globalPredicates + ", ")
        + "local predicates: " + localPredicates;
  }

  /**
   * Get the unique id of this precision object.
   */
  public int getId() {
    return id;
  }
}
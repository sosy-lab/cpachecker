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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

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
public class PredicatePrecision implements Precision, AdjustablePrecision {
  /**
   * This class identifies a position in the ARG where predicates can be applied.
   * It matches the n-th occurrence of a given CFANode on an ARGPath.
   */
  public static final class LocationInstance implements Comparable<LocationInstance> {
    private final CFANode location;
    private final int instance;

    LocationInstance(CFANode pLocation, int pInstance) {
      location = checkNotNull(pLocation);
      if (pInstance < 0) {
        throw new IllegalArgumentException(
            "Invalid LocationInstance with negative count " + pInstance);
      }
      instance = pInstance;
    }

    public CFANode getLocation() {
      return location;
    }

    public int getInstance() {
      return instance;
    }

    public String getFunctionName() {
      return location.getFunctionName();
    }

    @Override
    public int hashCode() {
      return (31 + instance) + location.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LocationInstance)) {
        return false;
      }
      LocationInstance other = (LocationInstance) obj;
      return instance == other.instance && location.equals(other.location);
    }

    @Override
    public int compareTo(LocationInstance other) {
      return ComparisonChain.start()
          .compare(location, other.location)
          .compare(instance, other.instance)
          .result();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("location", location)
          .add("instance", instance)
          .toString();
    }
  }

  private ImmutableSetMultimap<LocationInstance, AbstractionPredicate>
      mLocationInstancePredicates;
  private ImmutableSetMultimap<CFANode, AbstractionPredicate> mLocalPredicates;
  private ImmutableSetMultimap<String, AbstractionPredicate> mFunctionPredicates;
  private ImmutableSet<AbstractionPredicate> mGlobalPredicates;

  private static final PredicatePrecision EMPTY =
      new PredicatePrecision(
          ImmutableList.<Map.Entry<LocationInstance, AbstractionPredicate>>of(),
          ImmutableList.<Map.Entry<CFANode, AbstractionPredicate>>of(),
          ImmutableList.<Map.Entry<String, AbstractionPredicate>>of(),
          ImmutableList.<AbstractionPredicate>of());

  public PredicatePrecision(
      Multimap<LocationInstance, AbstractionPredicate> pLocationInstancePredicates,
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
      Iterable<Map.Entry<LocationInstance, AbstractionPredicate>> pLocationInstancePredicates,
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

    Multimap<LocationInstance, AbstractionPredicate> locationInstancePredicates =
        MultimapBuilder.treeKeys().arrayListValues().build();
    putAll(pLocationInstancePredicates, locationInstancePredicates);
    for (LocationInstance location : locationInstancePredicates.keySet()) {
      locationInstancePredicates.putAll(location, mLocalPredicates.get(location.getLocation()));
      locationInstancePredicates.putAll(
          location, mFunctionPredicates.get(location.getFunctionName()));
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
   * Create a new precision that is the union of all given precisions.
   */
  public static PredicatePrecision unionOf(Collection<PredicatePrecision> precisions) {
    if (precisions.isEmpty()) {
      return empty();
    }
    if (precisions.size() == 1) {
      return Iterables.getOnlyElement(precisions);
    }

    return new PredicatePrecision(
        from(precisions).transformAndConcat(prec -> prec.getLocationInstancePredicates().entries()),
        from(precisions).transformAndConcat(prec -> prec.getLocalPredicates().entries()),
        from(precisions).transformAndConcat(prec -> prec.getFunctionPredicates().entries()),
        from(precisions).transformAndConcat(prec -> prec.getGlobalPredicates()));
  }

  /**
   * Create a new precision that is the union of the predicate precisions of all given precisions.
   * This method can be called even with a lot of duplicate precisions in the input
   * (for example, all precisions occurring in the reached set)
   * and will handle the union computation efficiently.
   */
  public static PredicatePrecision unionOf(Iterable<Precision> precisions) {
    // Find all distinct elements to avoid computing the union over lots of identical objects.
    Set<PredicatePrecision> predicatePrecisions = Sets.newIdentityHashSet();
    for (Precision prec : precisions) {
      predicatePrecisions.add(Precisions.extractPrecisionByType(prec, PredicatePrecision.class));
    }
    predicatePrecisions.remove(EMPTY); // union over empty precision is pointless
    return unionOf(predicatePrecisions);
  }

  /**
   * Return a table of the location-instance-specific predicates.
   * These are the predicates that should be used at the n-th instance
   * of an abstraction location l in the current path.
   */
  public final ImmutableSetMultimap<LocationInstance, AbstractionPredicate>
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
  public final ImmutableSet<AbstractionPredicate> getPredicates(CFANode loc, int locInstance) {
    return getPredicates(new LocationInstance(loc, locInstance));
  }

  /**
   * Return all predicates for one specific location in this precision.
   */
  public final ImmutableSet<AbstractionPredicate> getPredicates(LocationInstance locationInstance) {
    ImmutableSet<AbstractionPredicate> result =
        getLocationInstancePredicates().get(locationInstance);
    if (result.isEmpty()) {
      result = getLocalPredicates().get(locationInstance.getLocation());
    }
    if (result.isEmpty()) {
      result = getFunctionPredicates().get(locationInstance.getFunctionName());
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
      Iterable<Map.Entry<LocationInstance, AbstractionPredicate>> newPredicates) {
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
    if (this.isEmpty()) {
      return prec;
    }
    if (prec.isEmpty()) {
      return this;
    }
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

  public boolean isEmpty() {
    return getGlobalPredicates().isEmpty()
        && getFunctionPredicates().isEmpty()
        && getLocalPredicates().isEmpty()
        && getLocationInstancePredicates().isEmpty();
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

  /*static ListMultimap<String, AbstractionPredicate> mergePredicatesPerFunction(
      Multimap<Pair<CFANode, Integer>, AbstractionPredicate> newPredicates) {

    return transformAndMergeKeys(newPredicates,
        Functions.compose(CFAUtils.GET_FUNCTION,
                          Pair.<CFANode>getProjectionToFirst()));
  }

  static ListMultimap<CFANode, AbstractionPredicate> mergePredicatesPerLocation(
      Multimap<Pair<CFANode, Integer>, AbstractionPredicate> newPredicates) {

    return transformAndMergeKeys(newPredicates, Pair.<CFANode>getProjectionToFirst());
  }*/

  private static <K1, K2, V> ListMultimap<K2, V> transformAndMergeKeys(Multimap<K1, V> input,
      Function<? super K1, K2> transformFunction) {

    ListMultimap<K2, V> result = ArrayListMultimap.create();
    for (Map.Entry<K1, Collection<V>> entry : input.asMap().entrySet()) {
      result.putAll(transformFunction.apply(entry.getKey()), entry.getValue());
    }
    return result;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision otherPrecision) {
    return mergeWith((PredicatePrecision)otherPrecision);
  }

  @Override
  public void clear() {
    mLocalPredicates = ImmutableSetMultimap.of();
    mLocationInstancePredicates = ImmutableSetMultimap.of();
    mFunctionPredicates = ImmutableSetMultimap.of();
    mGlobalPredicates = ImmutableSet.of();
  }

  @Override
  public boolean subtract(AdjustablePrecision otherPrecision) {
    assert otherPrecision.getClass().equals(this.getClass());

    if (!mLocalPredicates.isEmpty())
    {
      SetMultimap<CFANode,AbstractionPredicate> removed =
        ((PredicatePrecision)otherPrecision).getLocalPredicates();
      SetMultimap<CFANode, AbstractionPredicate> localPredicates =
         HashMultimap.create(mLocalPredicates);
      for (CFANode cfaNode : mLocalPredicates.keys()) {
        ImmutableSet<AbstractionPredicate> tmpAbstractionPredicates = mLocalPredicates.get(cfaNode);
        if (removed.containsKey(cfaNode) && removed.get(cfaNode).equals(tmpAbstractionPredicates))
        {
          localPredicates.removeAll(cfaNode);
        }
      }
      mLocalPredicates = ImmutableSetMultimap.copyOf(localPredicates);
    }

    if (!mLocationInstancePredicates.isEmpty())
    {
      ImmutableSetMultimap<LocationInstance, AbstractionPredicate> removed =
        ((PredicatePrecision)otherPrecision).getLocationInstancePredicates();
      SetMultimap<LocationInstance, AbstractionPredicate> localPredicates =
        HashMultimap.create(mLocationInstancePredicates);
      for (LocationInstance key : mLocationInstancePredicates.keys()) {
        ImmutableSet<AbstractionPredicate> tmpAbstractionPredicates = mLocationInstancePredicates.get(key);
        if (removed.containsKey(key) && removed.get(key).equals(tmpAbstractionPredicates))
        {
          localPredicates.removeAll(key);
        }
      }
      mLocationInstancePredicates = ImmutableSetMultimap.copyOf(localPredicates);
    }

    if (!mFunctionPredicates.isEmpty())
    {
      SetMultimap<String, AbstractionPredicate> removed =
        ((PredicatePrecision)otherPrecision).getFunctionPredicates();
      SetMultimap<String, AbstractionPredicate> localPredicates =
        HashMultimap.create(mFunctionPredicates);
      for (String key : mFunctionPredicates.keys()) {
        ImmutableSet<AbstractionPredicate> tmpAbstractionPredicates = mFunctionPredicates.get(key);
        if (removed.containsKey(key) && removed.get(key).equals(tmpAbstractionPredicates))
        {
          localPredicates.removeAll(key);
        }
      }
      mFunctionPredicates = ImmutableSetMultimap.copyOf(localPredicates);
    }

    if (!mGlobalPredicates.isEmpty())
    {
      Set<AbstractionPredicate> removed =
        ((PredicatePrecision)otherPrecision).getGlobalPredicates();
      Set<AbstractionPredicate> localPredicates = new HashSet<>(mGlobalPredicates);
      for (AbstractionPredicate abstractionPredicate : mGlobalPredicates.asList()) {
        if (removed.contains(abstractionPredicate))
        {
          localPredicates.remove(abstractionPredicate);
        }
      }
      mGlobalPredicates = ImmutableSet.copyOf(localPredicates);
    }

    return false;
  }
}
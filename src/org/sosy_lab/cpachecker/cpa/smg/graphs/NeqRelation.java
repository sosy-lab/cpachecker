/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

/**
 * This class tracks Pairs of Integers. Implemented as an immutable map.
 * The Multimap is used as Bi-Map, i.e. for each pair (K,V) there exists also a pair (V,K).
 * For memory-reasons we only store one of the Pairs and check for both if needed.
 */
final class NeqRelation {

  private final PersistentMap<Integer, ImmutableSet<Integer>> smgValues;

  public NeqRelation() {
    smgValues = PathCopyingPersistentTreeMap.of();
  }

  private NeqRelation(PersistentMap<Integer, ImmutableSet<Integer>> pMap) {
    smgValues = pMap;
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    Set<Integer> neqs = smgValues.get(pV);
    Builder<Integer> builder = ImmutableSet.builder();
    if (neqs != null) {
      builder.addAll(neqs);
    }
    // add backwards mappings
    for (Entry<Integer, ImmutableSet<Integer>> e : smgValues.entrySet()) {
      if (e.getValue().contains(pV)) {
        builder.add(e.getKey());
      }
    }
    return builder.build();
  }

  public NeqRelation addRelationAndCopy(Integer pOne, Integer pTwo) {

    // swap if A>B
    if (pOne.compareTo(pTwo) > 0) {
      Integer tmp = pOne;
      pOne = pTwo;
      pTwo = tmp;
    }

    ImmutableSet<Integer> set = smgValues.get(pOne);
    if (set == null) {
      set = ImmutableSet.of(pTwo);
    } else {
      set = ImmutableSet.<Integer>builder().addAll(set).add(pTwo).build();
    }

    return new NeqRelation(smgValues.putAndCopy(pOne, set));
  }

  public NeqRelation removeRelationAndCopy(Integer pOne, Integer pTwo) {

    // swap if A>B
    if (pOne.compareTo(pTwo) > 0) {
      Integer tmp = pOne;
      pOne = pTwo;
      pTwo = tmp;
    }

    ImmutableSet<Integer> set = smgValues.get(pOne);
    if (set == null || !set.contains(pTwo)) {
      return this;
    }

    final Integer pTwoFinal = pTwo;
    set = ImmutableSet.<Integer>builder().addAll(Iterables.filter(set, i -> !i.equals(pTwoFinal))).build();
    if (set.isEmpty()) {
      return new NeqRelation(smgValues.removeAndCopy(pOne));
    } else {
      return new NeqRelation(smgValues.putAndCopy(pOne, set));
    }
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {

    // swap if A>B
    if (pOne.compareTo(pTwo) > 0) {
      Integer tmp = pOne;
      pOne = pTwo;
      pTwo = tmp;
    }

    Set<Integer> set = smgValues.get(pOne);
    return set != null && set.contains(pTwo);
  }

  public NeqRelation removeValueAndCopy(Integer pOne) {
    // first delete forward matches
    PersistentMap<Integer, ImmutableSet<Integer>> newSet = smgValues.removeAndCopy(pOne);

    // then handle backwards matches
    for (Entry<Integer, ImmutableSet<Integer>> e : smgValues.entrySet()) {
      if (e.getKey().compareTo(pOne) > 0) {
        break;
      }
      if (e.getValue().contains(pOne)) {
        ImmutableSet<Integer> cp = ImmutableSet.copyOf(Iterables.filter(e.getValue(), i -> !i.equals(pOne)));
        if (cp.isEmpty()) {
          newSet = newSet.removeAndCopy(e.getKey());
        } else {
          newSet = newSet.putAndCopy(e.getKey(), cp);
        }
      }
    }
    return new NeqRelation(newSet);
  }

  /** transform all relations from (A->C) towards (A->B) and delete C */
  public NeqRelation mergeValuesAndCopy(Integer pB, Integer pC) {
    NeqRelation result = removeValueAndCopy(pC);
    for (Integer value : getNeqsForValue(pC)) {
      result = result.addRelationAndCopy(pB, value);
    }
    return result;
  }

  @Override
  public String toString() {
    return "neq_rel=" + smgValues.toString();
  }

  @Override
  public int hashCode() {
    return smgValues.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NeqRelation other = (NeqRelation) obj;
    return other.smgValues != null && smgValues.equals(other.smgValues);
  }
}
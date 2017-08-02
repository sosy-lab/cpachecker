/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;

/**
 * This class tracks Pairs of Integers. Implemented as an immutable map.
 * The Multimap is used as Bi-Map, i.e. for each pair (K,V) there exists also a pair (V,K).
 */
final class NeqRelation {

  private final PersistentMultimap<Integer, Integer> smgValues;

  public NeqRelation() {
    smgValues = PersistentMultimap.of();
  }

  private NeqRelation(PersistentMultimap<Integer, Integer> pMap) {
    smgValues = pMap;
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    return smgValues.get(pV);
  }

  public NeqRelation addRelationAndCopy(Integer pOne, Integer pTwo) {
    return new NeqRelation(smgValues.putAndCopy(pOne, pTwo).putAndCopy(pTwo, pOne));
  }

  public NeqRelation removeRelationAndCopy(Integer pOne, Integer pTwo) {
    return new NeqRelation(smgValues.removeAndCopy(pOne, pTwo).removeAndCopy(pTwo, pOne));
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {
    return smgValues.get(pOne).contains(pTwo);
  }

  public NeqRelation removeValueAndCopy(Integer pOne) {
    PersistentMultimap<Integer, Integer> newSet = smgValues.removeAndCopy(pOne);
    for (Integer pTwo : smgValues.get(pOne)) {
      newSet = newSet.removeAndCopy(pTwo, pOne);
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
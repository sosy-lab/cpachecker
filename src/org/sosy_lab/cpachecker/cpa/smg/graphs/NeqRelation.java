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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

import java.util.Collections;
import java.util.List;
import java.util.Set;

final class NeqRelation {

  /** The Multimap is used as Bi-Map, i.e. each pair (K,V) is also
   * inserted as pair (V,K). We avoid self-references like (A,A). */
  private final SetMultimap<Integer, Integer> smgValues = HashMultimap.create();

  @Override
  public int hashCode() {
    return smgValues.hashCode();
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    return Collections.unmodifiableSet(smgValues.get(pV));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NeqRelation other = (NeqRelation) obj;
    return other.smgValues != null && smgValues.equals(other.smgValues);
  }

  public void add_relation(Integer pOne, Integer pTwo) {

    // we do not want self-references
    if(pOne.intValue() == pTwo.intValue()) {
      return;
    }

    smgValues.put(pOne, pTwo);
    smgValues.put(pTwo, pOne);
  }

  public void putAll(NeqRelation pNeq) {
    smgValues.putAll(pNeq.smgValues);
  }

  public void remove_relation(Integer pOne, Integer pTwo) {
    smgValues.remove(pOne, pTwo);
    smgValues.remove(pTwo, pOne);
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {
    return smgValues.containsEntry(pOne, pTwo);
  }

  public void removeValue(Integer pOne) {
    for (Integer other : smgValues.get(pOne)) {
      smgValues.get(other).remove(pOne);
    }
    smgValues.removeAll(pOne);
  }

  /** transform all relations from (A->C) towards (A->B) and delete C */
  public void mergeValues(Integer pB, Integer pC) {
    List<Integer> values = ImmutableList.copyOf(smgValues.get(pC));
    removeValue(pC);
    for (Integer value : values) {
      add_relation(pB, value);
    }
  }

  @Override
  public String toString() {
    return "neq_rel=" + smgValues.toString();
  }

  public void clear() {
    smgValues.clear();
  }
}
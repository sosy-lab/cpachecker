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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

final class NeqRelation {
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((smgValues == null) ? 0 : smgValues.hashCode());
    return result;
  }

  public Set<Integer> getNeqsForValue(Integer pV) {
    if (smgValues.containsKey(pV)) {
      return Collections.unmodifiableSet(new HashSet<>(smgValues.get(pV)));
    }
    return Collections.unmodifiableSet(new HashSet<Integer>());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NeqRelation other = (NeqRelation) obj;
    if (smgValues == null) {
      if (other.smgValues != null) {
        return false;
      }
    } else if (!smgValues.equals(other.smgValues)) {
      return false;
    }
    return true;
  }

  private final Map<Integer, List<Integer>> smgValues = new HashMap<>();

  public void add_relation(Integer pOne, Integer pTwo) {

    if(pOne.intValue() == pTwo.intValue()) {
      return;
    }

    if (! smgValues.containsKey(pOne)) {
      smgValues.put(pOne, new ArrayList<Integer>());
    }
    if (! smgValues.containsKey(pTwo)) {
      smgValues.put(pTwo, new ArrayList<Integer>());
    }

    if (! smgValues.get(pOne).contains(pTwo)) {
      smgValues.get(pOne).add(pTwo);
      smgValues.get(pTwo).add(pOne);
    }
  }

  public void putAll(NeqRelation pNeq) {
    for (Integer key : pNeq.smgValues.keySet()) {
      smgValues.put(key, new ArrayList<Integer>());
      smgValues.get(key).addAll(pNeq.smgValues.get(key));
    }
  }

  public void remove_relation(Integer pOne, Integer pTwo) {
    if (smgValues.containsKey(pOne) && smgValues.containsKey(pTwo)) {
      List<Integer> listOne = smgValues.get(pOne);
      List<Integer> listTwo = smgValues.get(pTwo);

      if (listOne.contains(pTwo)) {
        listOne.remove(pTwo);
        listTwo.remove(pOne);
      }
    }
  }

  public boolean neq_exists(Integer pOne, Integer pTwo) {
    if (smgValues.containsKey(pOne) && smgValues.get(pOne).contains(pTwo)) {
      return true;
    }
    return false;
  }

  public void removeValue(Integer pOne) {
    if (smgValues.containsKey(pOne)) {
      for (Integer other : smgValues.get(pOne)) {
        smgValues.get(other).remove(pOne);
      }
      smgValues.remove(pOne);
    }
  }

  public void mergeValues(Integer pOne, Integer pTwo) {
    if (! smgValues.containsKey(pOne)) {
      smgValues.put(pOne, new ArrayList<Integer>());
    }
    if (! smgValues.containsKey(pTwo)) {
      smgValues.put(pTwo, new ArrayList<Integer>());
    }

    List<Integer> values = ImmutableList.copyOf(smgValues.get(pTwo));
    removeValue(pTwo);

    List<Integer> my = smgValues.get(pOne);
    for (Integer value : values) {
      if(!smgValues.containsKey(value)) {
        continue;
      }

      List<Integer> other = smgValues.get(value);
      if ((! value.equals(pOne)) && (! other.contains(pOne))) {
        other.add(pOne);
        my.add(value);
      }
    }
  }
}
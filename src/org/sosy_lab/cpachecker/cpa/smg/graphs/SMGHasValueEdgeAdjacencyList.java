/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class SMGHasValueEdgeAdjacencyList implements SMGHasValueEdges {

  private Map<SMGObject, Map<Integer, SMGEdgeHasValue>> objectToHveEdgeMap = new HashMap<>();
  private SetMultimap<Integer, SMGEdgeHasValue> valueToHveEdgeMap = HashMultimap.create();

  public SMGHasValueEdgeAdjacencyList() {

  }

  @Override
  public SMGHasValueEdges copy() {
    SMGHasValueEdgeAdjacencyList copy = new SMGHasValueEdgeAdjacencyList();
    copy.addAll(this);
    return copy;
  }

  private void addAll(SMGHasValueEdgeAdjacencyList pAdjacencyList) {
    valueToHveEdgeMap.putAll(pAdjacencyList.valueToHveEdgeMap);

    for (Entry<SMGObject, Map<Integer, SMGEdgeHasValue>> entry : pAdjacencyList.objectToHveEdgeMap
        .entrySet()) {

      SMGObject key = entry.getKey();
      Map<Integer, SMGEdgeHasValue> value = entry.getValue();

      if (objectToHveEdgeMap.containsKey(key)) {
        objectToHveEdgeMap.get(key).putAll(value);
      } else {
        HashMap<Integer, SMGEdgeHasValue> newValue = new HashMap<>();
        newValue.putAll(value);
        objectToHveEdgeMap.put(key, newValue);
      }
    }
  }

  public boolean performConsistencyCheck() {

    for (SMGEdgeHasValue hv : valueToHveEdgeMap.values()) {
      if (objectToHveEdgeMap.containsKey(hv.getObject())) {
        Map<Integer, SMGEdgeHasValue> hvem = objectToHveEdgeMap.get(hv.getObject());
        if (hvem.containsKey(hv.getOffset())) {
          if (!hvem.get(hv.getOffset()).equals(hv)) {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    }

    for (Map<Integer, SMGEdgeHasValue> c : objectToHveEdgeMap.values()) {
      for (SMGEdgeHasValue c2 : c.values()) {
        if (!valueToHveEdgeMap.get(c2.getValue()).contains(c2)) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public void removeAllEdgesOfObject(SMGObject pObj) {
    if (objectToHveEdgeMap.containsKey(pObj)) {
      Collection<SMGEdgeHasValue> toRemove = objectToHveEdgeMap.get(pObj).values();
      objectToHveEdgeMap.remove(pObj);

      for (SMGEdgeHasValue hve : toRemove) {
        valueToHveEdgeMap.remove(hve.getValue(), hve);
      }
    }
  }

  @Override
  public void addEdge(SMGEdgeHasValue pEdge) {
    SMGObject obj = pEdge.getObject();
    int offset = pEdge.getOffset();
    int value = pEdge.getValue();

    valueToHveEdgeMap.put(value, pEdge);

    Map<Integer, SMGEdgeHasValue> offsetToHveMap;

    if (!objectToHveEdgeMap.containsKey(obj)) {
      offsetToHveMap = new HashMap<>();
      objectToHveEdgeMap.put(obj, offsetToHveMap);
    } else {
      offsetToHveMap = objectToHveEdgeMap.get(obj);
    }

    if (offsetToHveMap.containsKey(offset)) {
      SMGEdgeHasValue hve2 = offsetToHveMap.get(offset);
      if (!pEdge.equals(hve2)) {
        valueToHveEdgeMap.remove(hve2.getValue(), hve2);
      }
    }

    offsetToHveMap.put(offset, pEdge);
  }

  @Override
  public void removeEdge(SMGEdgeHasValue pEdge) {
    SMGObject obj = pEdge.getObject();
    int offset = pEdge.getOffset();
    int value = pEdge.getValue();

    valueToHveEdgeMap.remove(value, pEdge);

    if (objectToHveEdgeMap.containsKey(obj)) {
      objectToHveEdgeMap.get(obj).remove(offset, pEdge);
    }
  }

  @Override
  public void replaceHvEdges(Set<SMGEdgeHasValue> pNewHV) {
    valueToHveEdgeMap.clear();
    objectToHveEdgeMap.clear();

    for (SMGEdgeHasValue edge : pNewHV) {
      addEdge(edge);
    }
  }

  @Override
  public String toString() {
    return valueToHveEdgeMap.values().toString();
  }

  @Override
  public Set<SMGEdgeHasValue> getHvEdges() {
    return ImmutableSet.copyOf(valueToHveEdgeMap.values());
  }

  private Set<SMGEdgeHasValue> getHvEdges(SMGObject pObject, Integer pOffset) {
    if (objectToHveEdgeMap.containsKey(pObject)) {
      Map<Integer, SMGEdgeHasValue> offsetToHveEdge = objectToHveEdgeMap.get(pObject);
      if (offsetToHveEdge.containsKey(pOffset)) {
        SMGEdgeHasValue edge = offsetToHveEdge.get(pOffset);
        return ImmutableSet.of(edge);
      }
    }

    return ImmutableSet.of();
  }

  private Set<SMGEdgeHasValue> getHvEdges(Integer pValue) {
    return ImmutableSet.copyOf(valueToHveEdgeMap.get(pValue));
  }

  private Set<SMGEdgeHasValue> getHvEdges(SMGObject pObject) {

    if (objectToHveEdgeMap.containsKey(
        pObject)) {
      return ImmutableSet.copyOf(objectToHveEdgeMap.get(pObject).values());
    }

    return ImmutableSet.of();
  }

  @Override
  public void clear() {
    objectToHveEdgeMap.clear();
    valueToHveEdgeMap.clear();
  }

  @Override
  public Set<SMGEdgeHasValue> filter(SMGEdgeHasValueFilter pFilter) {

    if (pFilter.isFilteringByObject() && pFilter.isFilteringAtOffset()) {
      return pFilter.filterSet(getHvEdges(pFilter.filtersByObject(), pFilter.filtersAtOffset()));
    }

    if(pFilter.isFilteringHavingValue()) {
      return pFilter.filterSet(getHvEdges(pFilter.filtersHavingValue()));
    }

    if (pFilter.isFilteringByObject()) {
      return pFilter.filterSet(getHvEdges(pFilter.filtersByObject()));
    }

    return pFilter.filterSet(getHvEdges());
  }

  @Override
  public Set<SMGEdgeHasValue> getEdgesForObject(SMGObject pObject) {
    Map<Integer, SMGEdgeHasValue> offsetToHveMap = objectToHveEdgeMap.get(pObject);
    if (offsetToHveMap != null) {
      return ImmutableSet.copyOf(offsetToHveMap.values());
    }
    return ImmutableSet.of();
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectToHveEdgeMap, valueToHveEdgeMap);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof SMGHasValueEdgeAdjacencyList) {
      SMGHasValueEdgeAdjacencyList other = (SMGHasValueEdgeAdjacencyList) pObj;
      return objectToHveEdgeMap.equals(other.objectToHveEdgeMap)
          && valueToHveEdgeMap.equals(other.valueToHveEdgeMap);
    }
    return false;
  }
}
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

public class SMGPointsToMap implements SMGPointsToEdges {

  private final PersistentMap<Integer, SMGEdgePointsTo> map;

  public SMGPointsToMap() {
    map = PathCopyingPersistentTreeMap.of();
  }

  private SMGPointsToMap(PersistentMap<Integer, SMGEdgePointsTo> pMap) {
    map = pMap;
  }

  @Override
  public SMGPointsToMap addAndCopy(SMGEdgePointsTo pEdge) {
    return new SMGPointsToMap(map.putAndCopy(pEdge.getValue(), pEdge));
  }

  @Override
  public SMGPointsToMap removeAndCopy(SMGEdgePointsTo pEdge) {
    return removeEdgeWithValueAndCopy(pEdge.getValue());
  }

  @Override
  public SMGPointsToMap removeAllEdgesOfObjectAndCopy(SMGObject pObj) {
    Set<SMGEdgePointsTo> toRemove = filter(SMGEdgePointsToFilter.targetObjectFilter(pObj));

    PersistentMap<Integer, SMGEdgePointsTo> tmp = map;
    for (SMGEdgePointsTo edge : toRemove) {
      tmp = tmp.removeAndCopy(edge.getValue());
    }
    return new SMGPointsToMap(tmp);
  }

  @Override
  public SMGPointsToMap removeEdgeWithValueAndCopy(int pValue) {
    return new SMGPointsToMap(map.removeAndCopy(pValue));
  }

  @Override
  public boolean containsEdgeWithValue(Integer pValue) {
    return map.containsKey(pValue);
  }

  @Override
  public SMGEdgePointsTo getEdgeWithValue(Integer pValue) {
    return map.get(pValue);
  }

  @Override
  public Set<SMGEdgePointsTo> filter(SMGEdgePointsToFilter pFilter) {

    if (pFilter.isFilteringAtValue()) {
      int value = pFilter.filtersHavingValue();
      if (map.containsKey(value)) {
        return ImmutableSet.of(map.get(value));
      } else {
        return ImmutableSet.of();
      }
    }

    return pFilter.filterSet(asSet());
  }

  @Override
  public ImmutableSet<SMGEdgePointsTo> asSet() {
    return ImmutableSet.copyOf(map.values());
  }

  @Override
  public ImmutableMap<Integer, SMGEdgePointsTo> asMap() {
    return ImmutableMap.copyOf(map);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof SMGPointsToMap && map.equals(((SMGPointsToMap)o).map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
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

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SMGPointsToMap extends HashMap<Integer, SMGEdgePointsTo> implements SMGPointsToEdges {

  private static final long serialVersionUID = 3838999122926869288L;

  @Override
  public SMGPointsToEdges copy() {
    SMGPointsToMap copy = new SMGPointsToMap();
    copy.putAll(this);
    return copy;
  }

  @Override
  public void add(SMGEdgePointsTo pEdge) {
    put(pEdge.getValue(), pEdge);
  }

  @Override
  public void remove(SMGEdgePointsTo pEdge) {
    remove(pEdge.getValue());
  }

  @Override
  public void removeAllEdgesOfObject(SMGObject pObj) {
    Set<SMGEdgePointsTo> toRemove = filter(SMGEdgePointsToFilter.targetObjectFilter(pObj));

    for (SMGEdgePointsTo edge : toRemove) {
      remove(edge.getValue());
    }
  }

  @Override
  public void removeEdgeWithValue(int pValue) {
    remove(pValue);
  }

  @Override
  public Map<Integer, SMGEdgePointsTo> asMap() {
    return ImmutableMap.copyOf(this);
  }

  @Override
  public boolean containsEdgeWithValue(Integer pValue) {
    return containsKey(pValue);
  }

  @Override
  public SMGEdgePointsTo getEdgeWithValue(Integer pValue) {
    return get(pValue);
  }

  @Override
  public Set<SMGEdgePointsTo> filter(SMGEdgePointsToFilter pFilter) {

    if (pFilter.isFilteringAtValue()) {
      int value = pFilter.filtersHavingValue();

      if (containsKey(value)) {
        return ImmutableSet.of(get(value));
      } else {
        return ImmutableSet.of();
      }

    }

    return pFilter.filterSet(asSet());
  }

  @Override
  public Set<SMGEdgePointsTo> asSet() {
    return ImmutableSet.copyOf(values());
  }
}
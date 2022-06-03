// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.Iterator;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGPointsToMap implements SMGPointsToEdges {

  private final PersistentMap<SMGValue, SMGEdgePointsTo> map;

  public SMGPointsToMap() {
    map = PathCopyingPersistentTreeMap.of();
  }

  private SMGPointsToMap(PersistentMap<SMGValue, SMGEdgePointsTo> pMap) {
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
    PersistentMap<SMGValue, SMGEdgePointsTo> tmp = map;
    for (SMGEdgePointsTo edge : SMGEdgePointsToFilter.targetObjectFilter(pObj).filter(this)) {
      tmp = tmp.removeAndCopy(edge.getValue());
    }
    return new SMGPointsToMap(tmp);
  }

  @Override
  public SMGPointsToMap removeEdgeWithValueAndCopy(SMGValue pValue) {
    return new SMGPointsToMap(map.removeAndCopy(pValue));
  }

  @Override
  public boolean containsEdgeWithValue(SMGValue pValue) {
    return map.containsKey(pValue);
  }

  @Override
  public SMGEdgePointsTo getEdgeWithValue(SMGValue pValue) {
    return map.get(pValue);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof SMGPointsToMap && map.equals(((SMGPointsToMap) o).map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.values().toString();
  }

  @Override
  public Iterator<SMGEdgePointsTo> iterator() {
    return map.values().iterator();
  }
}

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

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;

public class SMGHasValueEdgeSet implements SMGHasValueEdges {

  private final PersistentMultimap<SMGObject, SMGEdgeHasValue> map;

  public SMGHasValueEdgeSet() {
    map = PersistentMultimap.of();
  }

  private SMGHasValueEdgeSet(PersistentMultimap<SMGObject, SMGEdgeHasValue> pMap) {
    map = pMap;
  }

  @Override
  public SMGHasValueEdgeSet removeAllEdgesOfObjectAndCopy(SMGObject obj) {
    return new SMGHasValueEdgeSet(map.removeAndCopy(obj));
  }

  @Override
  public SMGHasValueEdgeSet addEdgeAndCopy(SMGEdgeHasValue pEdge) {
    return new SMGHasValueEdgeSet(map.putAndCopy(pEdge.getObject(), pEdge));
  }

  @Override
  public SMGHasValueEdgeSet removeEdgeAndCopy(SMGEdgeHasValue pEdge) {
    PersistentMultimap<SMGObject, SMGEdgeHasValue> updated =
        map.removeAndCopy(pEdge.getObject(), pEdge);
    if (map == updated) {
      return this;
    } else {
      return new SMGHasValueEdgeSet(updated);
    }
  }

  @Override
  public ImmutableSet<SMGEdgeHasValue> getHvEdges() {
    return map.values();
  }

  @Override
  public ImmutableSet<SMGEdgeHasValue> getEdgesForObject(SMGObject pObject) {
    @Nullable ImmutableSet<SMGEdgeHasValue> edges = map.get(pObject);
    return edges == null ? ImmutableSet.of() : edges;
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj instanceof SMGHasValueEdgeSet) {
      SMGHasValueEdgeSet other = (SMGHasValueEdgeSet) pObj;
      return map.equals(other.map);
    }
    return false;
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
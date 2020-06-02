// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
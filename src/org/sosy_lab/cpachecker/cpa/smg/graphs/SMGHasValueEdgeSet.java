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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGHasValueEdgeSet implements SMGHasValueEdges {

  private final PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> map;
  private int size = 0;

  public SMGHasValueEdgeSet() {
    map = PathCopyingPersistentTreeMap.of();
  }

  private SMGHasValueEdgeSet(PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> pMap, int pSize) {
    map = pMap;
    size = pSize;
  }

  @Override
  public SMGHasValueEdgeSet removeAllEdgesOfObjectAndCopy(SMGObject obj) {
    PersistentSortedMap<Long, SMGEdgeHasValue> edgesForObject = map.get(obj);
    if (edgesForObject == null) {
      return this;
    }
    int pSize = size - edgesForObject.size();
    return new SMGHasValueEdgeSet(map.removeAndCopy(obj), pSize);
  }

  @Override
  public SMGHasValueEdgeSet addEdgeAndCopy(SMGEdgeHasValue pEdge) {
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pEdge.getObject());
    if (sortedByOffsets == null) {
      sortedByOffsets = PathCopyingPersistentTreeMap.of();
    }
    int pSize = size - sortedByOffsets.size();
    sortedByOffsets = sortedByOffsets.putAndCopy(pEdge.getOffset(), pEdge);
    pSize += sortedByOffsets.size();
    return new SMGHasValueEdgeSet(map.putAndCopy(pEdge.getObject(), sortedByOffsets), pSize);
  }

  @Override
  public SMGHasValueEdgeSet removeEdgeAndCopy(SMGEdgeHasValue pEdge) {

    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pEdge.getObject());
    if (sortedByOffsets == null) {
      return this;
    } else {
      PersistentSortedMap<Long, SMGEdgeHasValue> updated = sortedByOffsets.removeAndCopy(pEdge.getOffset());
      if (updated == sortedByOffsets) {
        return this;
      } else {
        int pSize = size - 1;
        if (updated.size() > 0) {
          return new SMGHasValueEdgeSet(map.putAndCopy(pEdge.getObject(), updated), pSize);
        } else {
          return new SMGHasValueEdgeSet(map.removeAndCopy(pEdge.getObject()), pSize);
        }
      }
    }
  }

  @Override
  public SMGHasValueEdgeSet getHvEdges() {
    return this;
  }

  @Override
  public SMGHasValueEdges filter(SMGEdgeHasValueFilter pFilter) {
    SMGHasValueEdgeSet filtered = this;
    SMGObject object = pFilter.getObject();
    Long offset = pFilter.getOffset();
    SMGValue value = pFilter.getValue();
    long filterSize = pFilter.getSize();
    if (object != null) {
      filtered = getEdgesForObject(object);
      if (offset == null && value == null && filterSize == -1) {
        return filtered;
      }
    }
    NavigableSet<Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>>> entries =
        filtered.map.entrySet();
    for (Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> entry : entries) {
      filtered = filtered.removeAllEdgesOfObjectAndCopy(entry.getKey());
      if (offset != null) {
        SMGEdgeHasValue candidate = entry.getValue().get(offset);
        if (candidate != null && pFilter.holdsFor(candidate)) {
          filtered = filtered.addEdgeAndCopy(candidate);
        }
      } else {
        for (SMGEdgeHasValue candidate : entry.getValue().values()) {
          if (pFilter.holdsFor(candidate)) {
            filtered = filtered.addEdgeAndCopy(candidate);
          }
        }
      }
    }
    return filtered;
  }

  @Override
  public SMGHasValueEdgeSet getEdgesForObject(SMGObject pObject) {
    PersistentSortedMap<Long, SMGEdgeHasValue> edges = map.get(pObject);
    PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> newMap = PathCopyingPersistentTreeMap.of();
    int newSize = 0;
    if (edges != null && edges.size() > 0) {
      newSize = edges.size();
      newMap = newMap.putAndCopy(pObject, edges);
    }
    return new SMGHasValueEdgeSet(newMap, newSize);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean contains(SMGEdgeHasValue pHv) {
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pHv.getObject());
    if (sortedByOffsets == null) {
      return false;
    }
    return pHv.equals(sortedByOffsets.get(pHv.getOffset()));
  }

  @Override
  public Iterable<SMGEdgeHasValue> getOverlapping(
      SMGEdgeHasValue pNew_edge) {
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets =
        map.get(pNew_edge.getObject());
    if (sortedByOffsets == null) {
      return ImmutableSet.of();
    }
    long startOffset = pNew_edge.getOffset();
    long endOffset = startOffset + pNew_edge.getSizeInBits();
    Entry<Long, SMGEdgeHasValue> floorEntryCandidate = sortedByOffsets.floorEntry(startOffset);
    if (floorEntryCandidate != null) {
      SMGEdgeHasValue edgeCandidate = floorEntryCandidate.getValue();
      long edgeCandidateOffset = edgeCandidate.getOffset();
      long edgeCandidateEndOffset = edgeCandidateOffset + edgeCandidate.getSizeInBits();
      if (edgeCandidateEndOffset > startOffset) {
        startOffset = edgeCandidateOffset;
      }
    }
    NavigableMap<Long, SMGEdgeHasValue> filteredMap = sortedByOffsets.subMap(startOffset, endOffset);
    return filteredMap.values();
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

  @Override
  public Iterator<SMGEdgeHasValue> iterator() {
    return new SMGHasValueEdgeSetIterator(this);
  }

  private static class SMGHasValueEdgeSetIterator implements Iterator<SMGEdgeHasValue> {
    Iterator<PersistentSortedMap<Long, SMGEdgeHasValue>> firstLevelIterator;
    Iterator<SMGEdgeHasValue> secondLevelIterator = null;

    protected SMGHasValueEdgeSetIterator(SMGHasValueEdgeSet pSMGEdgeHasValues) {
      firstLevelIterator = pSMGEdgeHasValues.map.values().iterator();
      if (firstLevelIterator.hasNext()) {
        PersistentSortedMap<Long, SMGEdgeHasValue> next = firstLevelIterator.next();
        secondLevelIterator = next.values().iterator();
      }
    }

    @Override
    public boolean hasNext() {
      if (secondLevelIterator == null) {
        return false;
      }
      return secondLevelIterator.hasNext();
    }

    @Override
    public SMGEdgeHasValue next() {
      if (secondLevelIterator == null) {
        throw new NoSuchElementException();
      }
      SMGEdgeHasValue result = secondLevelIterator.next();
      if (!secondLevelIterator.hasNext()) {
        if (firstLevelIterator.hasNext()) {
          secondLevelIterator = firstLevelIterator.next().values().iterator();
        } else {
          secondLevelIterator = null;
        }
      }
      return result;
    }
  }
}
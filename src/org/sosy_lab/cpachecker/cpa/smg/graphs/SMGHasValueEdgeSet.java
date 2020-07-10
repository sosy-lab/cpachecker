// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.base.Preconditions.checkArgument;

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
  private final PersistentSortedMap<SMGObject, Integer> sizesMap;
  private int size = 0;

  public SMGHasValueEdgeSet() {
    map = PathCopyingPersistentTreeMap.of();
    sizesMap = PathCopyingPersistentTreeMap.of();
  }

  private SMGHasValueEdgeSet(
      PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> pMap,
      PersistentSortedMap<SMGObject, Integer> pSizesMap,
      int pSize) {
    map = pMap;
    sizesMap = pSizesMap;
    size = pSize;
  }

  @Override
  public SMGHasValueEdgeSet removeAllEdgesOfObjectAndCopy(SMGObject obj) {
    PersistentSortedMap<Long, SMGEdgeHasValue> edgesForObject = map.get(obj);
    if (edgesForObject == null) {
      return this;
    }
    PersistentSortedMap<SMGObject, Integer> newSizesMap = sizesMap.removeAndCopy(obj);
    int pSize = size - sizesMap.get(obj);
    return new SMGHasValueEdgeSet(map.removeAndCopy(obj), newSizesMap, pSize);
  }

  @Override
  public SMGHasValueEdgeSet addEdgeAndCopy(SMGEdgeHasValue pEdge) {
    SMGHasValueEdgeSet result = this;
    Integer sizeForObject = sizesMap.getOrDefault(pEdge.getObject(), 0);
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets =
        map.getOrDefault(pEdge.getObject(), PathCopyingPersistentTreeMap.of());

    // Check on overlapping edges
    Entry<Long, SMGEdgeHasValue> ceilingEntry = sortedByOffsets.ceilingEntry(pEdge.getOffset());
    long endOffset = pEdge.getSizeInBits() + pEdge.getOffset();
    assert (ceilingEntry == null || endOffset <= ceilingEntry.getKey());

    Entry<Long, SMGEdgeHasValue> floorEntry = sortedByOffsets.lowerEntry(endOffset);
    assert (floorEntry == null
        || pEdge.getOffset()
            >= floorEntry.getValue().getSizeInBits() + floorEntry.getValue().getOffset());

    // Merge zero edges
    if (pEdge.getValue().isZero()) {
      if (ceilingEntry != null
          && ceilingEntry.getKey() == endOffset
          && ceilingEntry.getValue().getValue().isZero()) {
        pEdge =
            new SMGEdgeHasValue(
                pEdge.getSizeInBits() + ceilingEntry.getValue().getSizeInBits(),
                pEdge.getOffset(),
                pEdge.getObject(),
                pEdge.getValue());
        result = result.removeEdgeAndCopy(ceilingEntry.getValue());
      }
      if (floorEntry != null
          && pEdge.getOffset()
              == floorEntry.getValue().getSizeInBits() + floorEntry.getValue().getOffset()
          && floorEntry.getValue().getValue().isZero()) {
        pEdge =
            new SMGEdgeHasValue(
                pEdge.getSizeInBits() + floorEntry.getValue().getSizeInBits(),
                floorEntry.getKey(),
                pEdge.getObject(),
                pEdge.getValue());
        result = result.removeEdgeAndCopy(floorEntry.getValue());
      }
      sizeForObject = result.sizesMap.getOrDefault(pEdge.getObject(), 0);
      sortedByOffsets =
          result.map.getOrDefault(pEdge.getObject(), PathCopyingPersistentTreeMap.of());
    }

    sortedByOffsets = sortedByOffsets.putAndCopy(pEdge.getOffset(), pEdge);
    PersistentSortedMap<SMGObject, Integer> newSizesMap =
        result.sizesMap.putAndCopy(pEdge.getObject(), sizeForObject + 1);
    return new SMGHasValueEdgeSet(
        result.map.putAndCopy(pEdge.getObject(), sortedByOffsets), newSizesMap, result.size + 1);
  }

  @Override
  public SMGHasValueEdgeSet removeEdgeAndCopy(SMGEdgeHasValue pEdge) {

    int sizeForObject = sizesMap.getOrDefault(pEdge.getObject(), 0);
    PersistentSortedMap<Long, SMGEdgeHasValue> updated;
    int pSize = size;

    if (sizeForObject == 0) {
      throw new AssertionError();
    } else {
      PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pEdge.getObject());
      if (pEdge.getValue().isZero()) {
        Entry<Long, SMGEdgeHasValue> floorEntry = sortedByOffsets.floorEntry(pEdge.getOffset());
        if (floorEntry != null) {
          SMGEdgeHasValue removingEdge = floorEntry.getValue();
          if (removingEdge.getOffset() + removingEdge.getSizeInBits() <= pEdge.getOffset()
              && (removingEdge.getOffset() != pEdge.getOffset())) {
            throw new AssertionError();
          } else {
            updated = sortedByOffsets.removeAndCopy(removingEdge.getOffset());
            pSize--;
            sizeForObject--;
            if (removingEdge.getOffset() < pEdge.getOffset()) {
              updated =
                  updated.putAndCopy(
                      removingEdge.getOffset(),
                      new SMGEdgeHasValue(
                          Math.toIntExact(pEdge.getOffset() - removingEdge.getOffset()),
                          removingEdge.getOffset(),
                          pEdge.getObject(),
                          pEdge.getValue()));
              pSize++;
              sizeForObject++;
            }
            if (removingEdge.getOffset() + removingEdge.getSizeInBits()
                > pEdge.getOffset() + pEdge.getSizeInBits()) {
              updated =
                  updated.putAndCopy(
                      pEdge.getOffset() + pEdge.getSizeInBits(),
                      new SMGEdgeHasValue(
                          Math.toIntExact(removingEdge.getOffset() - pEdge.getOffset())
                              + removingEdge.getSizeInBits()
                              - pEdge.getSizeInBits(),
                          pEdge.getOffset() + pEdge.getSizeInBits(),
                          pEdge.getObject(),
                          pEdge.getValue()));
              pSize++;
              sizeForObject++;
            }
          }
        } else {
          throw new AssertionError();
        }
      } else {
        updated = sortedByOffsets.removeAndCopy(pEdge.getOffset());
        pSize--;
        sizeForObject--;
      }

      if (updated == sortedByOffsets) {
        throw new AssertionError();
      } else {
        if (updated.isEmpty()) {
          return new SMGHasValueEdgeSet(
              map.removeAndCopy(pEdge.getObject()),
              sizesMap.removeAndCopy(pEdge.getObject()),
              pSize);
        } else {
          return new SMGHasValueEdgeSet(
              map.putAndCopy(pEdge.getObject(), updated),
              sizesMap.putAndCopy(pEdge.getObject(), sizeForObject),
              pSize);
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
    SMGObject filterObject = pFilter.getObject();
    Long filterOffset = pFilter.getOffset();
    SMGValue filterValue = pFilter.getValue();
    long filterSize = pFilter.getSize();
    if (filterObject != null) {
      filtered = getEdgesForObject(filterObject);
      if (filterOffset == null && filterValue == null && filterSize == -1) {
        return filtered;
      }
    }
    NavigableSet<Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>>> entries =
        filtered.map.entrySet();
    for (Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> entry : entries) {
      filtered = filtered.removeAllEdgesOfObjectAndCopy(entry.getKey());
      PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = entry.getValue();
      if (filterOffset != null) {
        Entry<Long, SMGEdgeHasValue> candidateEntry = sortedByOffsets.floorEntry(filterOffset);
        if (candidateEntry != null) {
          SMGEdgeHasValue candidate = candidateEntry.getValue();

          // also return part of zero edge
          if (candidate.getValue().isZero()) {
            long newSize = candidate.getSizeInBits() - (filterOffset - candidate.getOffset());
            if (filterSize >= 0 && newSize > filterSize) {
              newSize = filterSize;
            }
            if (newSize > 0 || (newSize == 0 && candidate.getSizeInBits() == 0)) {
              candidate =
                  new SMGEdgeHasValue(
                      newSize, filterOffset, candidate.getObject(), candidate.getValue());
            } else {
              continue;
            }
          }
          if (pFilter.holdsFor(candidate)) {
            filtered = filtered.addEdgeAndCopy(candidate);
          }
        }
      } else {
        for (SMGEdgeHasValue candidate : sortedByOffsets.values()) {
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
    PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> newMap =
        PathCopyingPersistentTreeMap.of();
    PersistentSortedMap<SMGObject, Integer> newSizesMap = PathCopyingPersistentTreeMap.of();
    int newSize = 0;
    if (edges != null && !edges.isEmpty()) {
      newSize = sizesMap.get(pObject);
      newSizesMap = newSizesMap.putAndCopy(pObject, newSize);
      newMap = newMap.putAndCopy(pObject, edges);
    }
    return new SMGHasValueEdgeSet(newMap, newSizesMap, newSize);
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
    if (pHv.getValue().isZero()) {
      Entry<Long, SMGEdgeHasValue> floorEntryCandidate =
          sortedByOffsets.floorEntry(pHv.getOffset());
      if (floorEntryCandidate != null) {
        SMGEdgeHasValue edgeCandidate = floorEntryCandidate.getValue();
        if (edgeCandidate.getValue().isZero()) {
          long edgeCandidateOffset = edgeCandidate.getOffset();
          long edgeCandidateEndOffset = edgeCandidateOffset + edgeCandidate.getSizeInBits();
          return pHv.getOffset() + pHv.getSizeInBits() <= edgeCandidateEndOffset;
        }
      }
    }
    return pHv.equals(sortedByOffsets.get(pHv.getOffset()));
  }

  @Override
  public Iterable<SMGEdgeHasValue> getOverlapping(SMGEdgeHasValue pNew_edge) {
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pNew_edge.getObject());
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
    NavigableMap<Long, SMGEdgeHasValue> filteredMap =
        sortedByOffsets.subMap(startOffset, endOffset);
    return filteredMap.values();
  }

  @Override
  public boolean overlapsWith(SMGEdgeHasValue pNewHve) {
    PersistentSortedMap<Long, SMGEdgeHasValue> sortedByOffsets = map.get(pNewHve.getObject());
    if (sortedByOffsets == null) {
      return false;
    }
    long startOffset = pNewHve.getOffset();
    long endOffset = startOffset + pNewHve.getSizeInBits();
    Entry<Long, SMGEdgeHasValue> floorEntryCandidate = sortedByOffsets.floorEntry(startOffset);
    if (floorEntryCandidate != null) {
      SMGEdgeHasValue edgeCandidate = floorEntryCandidate.getValue();
      long edgeCandidateOffset = edgeCandidate.getOffset();
      long edgeCandidateEndOffset = edgeCandidateOffset + edgeCandidate.getSizeInBits();
      if (edgeCandidateEndOffset > startOffset) {
        return true;
      }
    }
    NavigableMap<Long, SMGEdgeHasValue> filteredMap =
        sortedByOffsets.subMap(startOffset, endOffset);
    return !filteredMap.isEmpty();
  }

  @Override
  public SMGHasValueEdgeSet addEdgesForObject(SMGHasValueEdges pEdgesSet) {
    checkArgument(
        (pEdgesSet instanceof SMGHasValueEdgeSet),
        "Can't use different SMGHasValueEdges implementations");
    SMGHasValueEdgeSet edgesSet = (SMGHasValueEdgeSet) pEdgesSet;
    NavigableSet<Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>>> entries =
        edgesSet.map.entrySet();
    assert entries.size() == 1;

    Entry<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> entry = entries.first();

    assert !map.containsKey(entry.getKey());
    PersistentSortedMap<SMGObject, PersistentSortedMap<Long, SMGEdgeHasValue>> newMap =
        map.putAndCopy(entry.getKey(), entry.getValue());
    PersistentSortedMap<SMGObject, Integer> newSizesMap =
        sizesMap.putAndCopy(entry.getKey(), edgesSet.size);
    return new SMGHasValueEdgeSet(newMap, newSizesMap, size + edgesSet.size);
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
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
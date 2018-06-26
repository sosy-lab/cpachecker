/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

class SMGJoinFields {
  private final UnmodifiableSMG newSMG1;
  private final UnmodifiableSMG newSMG2;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  public SMGJoinFields(
      final UnmodifiableSMG pSMG1, final UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.getSize() != pObj2.getSize()) {
      throw new IllegalArgumentException("SMGJoinFields object arguments need to have identical size");
    }
    if (!(pSMG1.getObjects().contains(pObj1) && pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException("SMGJoinFields object arguments need to be included in parameter SMGs");
    }

    final SMG joinedSmg1 = pSMG1.copyOf();
    final SMG joinedSmg2 = pSMG2.copyOf();

    setCompatibleHVEdgesToSMG(joinedSmg1, pSMG2, pObj1, pObj2);
    setCompatibleHVEdgesToSMG(joinedSmg2, pSMG1, pObj2, pObj1);

    status = joinFieldsRelaxStatus(pSMG1, joinedSmg1, status, SMGJoinStatus.RIGHT_ENTAIL, pObj1);
    status = joinFieldsRelaxStatus(pSMG2, joinedSmg2, status, SMGJoinStatus.LEFT_ENTAIL, pObj2);

    Set<SMGEdgeHasValue> smg2Extension = mergeNonNullHasValueEdges(pSMG1, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> smg1Extension = mergeNonNullHasValueEdges(pSMG2, pSMG1, pObj2, pObj1);

    for (SMGEdgeHasValue edge : smg1Extension) {
      joinedSmg1.addValue(edge.getValue());
      joinedSmg1.addHasValueEdge(edge);
    }
    for (SMGEdgeHasValue edge : smg2Extension) {
      joinedSmg2.addValue(edge.getValue());
      joinedSmg2.addHasValueEdge(edge);
    }

    newSMG1 = joinedSmg1;
    newSMG2 = joinedSmg2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public UnmodifiableSMG getSMG1() {
    return newSMG1;
  }

  public UnmodifiableSMG getSMG2() {
    return newSMG2;
  }

  public static Set<SMGEdgeHasValue> mergeNonNullHasValueEdges(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();

    SMGEdgeHasValueFilter filterForSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    filterForSMG1.filterNotHavingValue(SMGZeroValue.INSTANCE);

    for (SMGEdgeHasValue edge : pSMG1.getHVEdges(filterForSMG1)) {
      filterForSMG2.filterAtOffset(edge.getOffset());
      if (pSMG2.getHVEdges(filterForSMG2).size() == 0) {
        returnSet.add(
            new SMGEdgeHasValue(
                edge.getType(), edge.getOffset(), pObj2, SMGKnownSymValue.of()));
      }
    }

    return Collections.unmodifiableSet(returnSet);
  }

  public static SMGJoinStatus joinFieldsRelaxStatus(
      UnmodifiableSMG pOrigSMG,
      UnmodifiableSMG pNewSMG,
      SMGJoinStatus pCurStatus,
      SMGJoinStatus pNewStatus,
      SMGObject pObject) {
    TreeMap<Long, Integer> origNullEdges = pOrigSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    TreeMap<Long, Integer> newNullEdges = pNewSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    for (Entry<Long, Integer> origEdge : origNullEdges.entrySet()) {
      Entry<Long, Integer> newFloorEntry = newNullEdges.floorEntry(origEdge.getKey());
      if (newFloorEntry == null || newFloorEntry.getValue() + newFloorEntry.getKey() <
                                    origEdge.getValue() + origEdge.getKey()) {
        return SMGJoinStatus.updateStatus(pCurStatus, pNewStatus);
      }
    }
    return pCurStatus;
  }

  @VisibleForTesting
  public static void setCompatibleHVEdgesToSMG(
      SMG pSMG, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    SMGEdgeHasValueFilter nullValueFilter = SMGEdgeHasValueFilter.objectFilter(pObj1);
    nullValueFilter.filterHavingValue(SMGZeroValue.INSTANCE);

    Set<SMGEdgeHasValue> edgesToRemove = pSMG.getHVEdges(nullValueFilter);
    Set<SMGEdgeHasValue> edgesToAdd1 = SMGJoinFields.getHVSetOfCommonNullValues(pSMG, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> edgesToAdd2 = SMGJoinFields.getHVSetOfMissingNullValues(pSMG, pSMG2, pObj1,pObj2);

    for (SMGEdgeHasValue edge : edgesToRemove) {
      pSMG.removeHasValueEdge(edge);
    }
    for (SMGEdgeHasValue edge : edgesToAdd1) {
      pSMG.addHasValueEdge(edge);
    }
    for (SMGEdgeHasValue edge : edgesToAdd2) {
      pSMG.addHasValueEdge(edge);
    }
  }

  @VisibleForTesting
  public static Set<SMGEdgeHasValue> getHVSetOfMissingNullValues(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();

    SMGEdgeHasValueFilter nonNullPtrInSmg2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    nonNullPtrInSmg2.filterNotHavingValue(SMGZeroValue.INSTANCE);

    SMGEdgeHasValueFilter nonNullPtrInSmg1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    nonNullPtrInSmg1.filterNotHavingValue(SMGZeroValue.INSTANCE);

    for (SMGEdgeHasValue edge : pSMG2.getHVEdges(nonNullPtrInSmg2)) {
      if (! pSMG2.isPointer(edge.getValue())) {
        continue;
      }

      nonNullPtrInSmg1.filterAtOffset(edge.getOffset());

      if (pSMG1.getHVEdges(nonNullPtrInSmg1).size() == 0) {

        TreeMap <Long, Integer> newNullEdgesOffsetToSize =
            pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);

        long min = edge.getOffset();
        long max = edge.getOffset() + edge.getSizeInBits(pSMG1.getMachineModel());

        Entry<Long, Integer> floorEntry = newNullEdgesOffsetToSize.floorEntry(min);
        if (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= max ) {
          retset.add(new SMGEdgeHasValue(edge.getType(), edge.getOffset(), pObj1, SMGZeroValue.INSTANCE));
        }
      }
    }
    return retset;
  }

  static private SMGEdgeHasValue getNullEdgesIntersection(Entry<Long, Integer> first, Entry<Long,
      Integer> next, SMGObject pObj1) {
    long resultOffset = Long.max(first.getKey(), next.getKey());
    int resultSize = Math.toIntExact(Long.min(first.getValue() + first.getKey(), next.getValue()
        + next.getKey()) - resultOffset);
    return new SMGEdgeHasValue(resultSize, resultOffset, pObj1, SMGZeroValue.INSTANCE);
  }

  public static Set<SMGEdgeHasValue> getHVSetOfCommonNullValues(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();
    TreeMap<Long, Integer> map1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Long, Integer> map2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);
    for (Entry<Long, Integer> entry1 : map1.entrySet()) {
      NavigableMap<Long, Integer> subMap =
          map2.subMap(entry1.getKey(), true, entry1.getKey() + entry1.getValue(), false);
      for (Entry<Long, Integer> entry2 : subMap.entrySet()) {
        retset.add(getNullEdgesIntersection(entry1, entry2, pObj1));
      }
    }
    for (Entry<Long, Integer> entry2 : map2.entrySet()) {
      NavigableMap<Long, Integer> subMap =
          map1.subMap(entry2.getKey(), false, entry2.getKey() + entry2.getValue(), false);
      for (Entry<Long, Integer> entry1 : subMap.entrySet()) {
        retset.add(getNullEdgesIntersection(entry2, entry1, pObj1));
      }
    }

    return Collections.unmodifiableSet(retset);
  }

  private static void checkResultConsistencySingleSide(
      UnmodifiableSMG pSMG1,
      SMGEdgeHasValueFilter nullEdges1,
      UnmodifiableSMG pSMG2,
      SMGObject pObj2,
      TreeMap<Long, Integer> nullEdgesInSMG2)
      throws SMGInconsistentException {
    for (SMGEdgeHasValue edgeInSMG1 : pSMG1.getHVEdges(nullEdges1)) {
      long start = edgeInSMG1.getOffset();
      long byte_after_end = start + edgeInSMG1.getSizeInBits(pSMG1.getMachineModel());
      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObj2)
                                                          .filterAtOffset(edgeInSMG1.getOffset())
                                                          .filterByType(edgeInSMG1.getType());
      Set<SMGEdgeHasValue> hvInSMG2Set = pSMG2.getHVEdges(filter);

      SMGEdgeHasValue hvInSMG2;
      if (hvInSMG2Set.size() > 0) {
        hvInSMG2 = Iterables.getOnlyElement(hvInSMG2Set);
      } else {
        hvInSMG2 = null;
      }

      Entry<Long, Integer> floorEntry = nullEdgesInSMG2.floorEntry(start);
      long nextNotNullBit = (floorEntry == null) ? start : Long.max(start, floorEntry.getKey() +
          floorEntry.getValue());
      if (hvInSMG2 == null || ( nextNotNullBit < byte_after_end && ! pSMG2.isPointer(hvInSMG2.getValue()))) {
        throw new SMGInconsistentException("SMGJoinFields output assertions do not hold");
      }
    }
  }

  public static void checkResultConsistency(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2)
      throws SMGInconsistentException {
    SMGEdgeHasValueFilter nullEdges1 = SMGEdgeHasValueFilter.objectFilter(pObj1).filterHavingValue(SMGZeroValue.INSTANCE);
    SMGEdgeHasValueFilter nullEdges2 = SMGEdgeHasValueFilter.objectFilter(pObj2).filterHavingValue(SMGZeroValue.INSTANCE);
    TreeMap<Long, Integer> nullEdgesInSMG1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Long, Integer> nullEdgesInSMG2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);

    if (pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1)).size() != pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)).size()) {
      throw new SMGInconsistentException("SMGJoinFields output assertion does not hold: the objects do not have identical sets of fields");
    }

    checkResultConsistencySingleSide(pSMG1, nullEdges1, pSMG2, pObj2, nullEdgesInSMG2);
    checkResultConsistencySingleSide(pSMG2, nullEdges2, pSMG1, pObj1, nullEdgesInSMG1);
  }
}

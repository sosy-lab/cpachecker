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

import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

class SMGJoinFields {
  private final SMG newSMG1;
  private final SMG newSMG2;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  public SMGJoinFields(final SMG pSMG1, final SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.getSize() != pObj2.getSize()) {
      throw new IllegalArgumentException("SMGJoinFields object arguments need to have identical size");
    }
    if (! (pSMG1.getObjects().contains(pObj1) && pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException("SMGJoinFields object arguments need to be included in parameter SMGs");
    }

    final SMG origSMG1 = new SMG(pSMG1);
    final SMG origSMG2 = new SMG(pSMG2);

    Set<SMGEdgeHasValue> H1Prime = getCompatibleHVEdgeSet(pSMG1, pSMG2, pObj1, pObj2);
    pSMG1.replaceHVSet(H1Prime);
    Set<SMGEdgeHasValue> H2Prime = getCompatibleHVEdgeSet(pSMG2, pSMG1, pObj2, pObj1);
    pSMG2.replaceHVSet(H2Prime);

    status = joinFieldsRelaxStatus(origSMG1, pSMG1, status, SMGJoinStatus.RIGHT_ENTAIL, pObj1);
    status = joinFieldsRelaxStatus(origSMG2, pSMG2, status, SMGJoinStatus.LEFT_ENTAIL, pObj2);

    Set<SMGEdgeHasValue> smg2Extension = mergeNonNullHasValueEdges(pSMG1, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> smg1Extension = mergeNonNullHasValueEdges(pSMG2, pSMG1, pObj2, pObj1);

    H1Prime.addAll(smg1Extension);
    H2Prime.addAll(smg2Extension);

    pSMG1.replaceHVSet(H1Prime);
    pSMG2.replaceHVSet(H2Prime);

    newSMG1 = pSMG1;
    newSMG2 = pSMG2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getSMG1() {
    return newSMG1;
  }

  public SMG getSMG2() {
    return newSMG2;
  }

  public static Set<SMGEdgeHasValue> mergeNonNullHasValueEdges(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> returnSet = new HashSet<>();

    SMGEdgeHasValueFilter filterForSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    filterForSMG1.filterNotHavingValue(pSMG1.getNullValue());

    for (SMGEdgeHasValue edge : pSMG1.getHVEdges(filterForSMG1)) {
      filterForSMG2.filterAtOffset(edge.getOffset());
      if (pSMG2.getHVEdges(filterForSMG2).size() == 0) {
        returnSet.add(new SMGEdgeHasValue(edge.getType(), edge.getOffset(), pObj2, SMGValueFactory.getNewValue()));
      }
    }

    return Collections.unmodifiableSet(returnSet);
  }

  public static SMGJoinStatus joinFieldsRelaxStatus(SMG pOrigSMG, SMG pNewSMG,
      SMGJoinStatus pCurStatus, SMGJoinStatus pNewStatus, SMGObject pObject) {
    TreeMap<Integer, Integer> origNullEdges = pOrigSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    TreeMap<Integer, Integer> newNullEdges = pNewSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    for (Entry<Integer, Integer> origEdge : origNullEdges.entrySet()) {
      Entry<Integer, Integer> newFloorEntry = newNullEdges.floorEntry(origEdge.getKey());
      if (newFloorEntry == null || newFloorEntry.getValue() + newFloorEntry.getKey() <
                                    origEdge.getValue() + origEdge.getKey()) {
        return SMGJoinStatus.updateStatus(pCurStatus, pNewStatus);
      }
    }
    return pCurStatus;
  }

  static public Set<SMGEdgeHasValue> getCompatibleHVEdgeSet(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> newHVSet = SMGJoinFields.getHVSetWithoutNullValuesOnObject(pSMG1, pObj1);

    newHVSet.addAll(SMGJoinFields.getHVSetOfCommonNullValues(pSMG1, pSMG2, pObj1, pObj2));
    newHVSet.addAll(SMGJoinFields.getHVSetOfMissingNullValues(pSMG1, pSMG2, pObj1, pObj2));

    return newHVSet;
  }

  static public Set<SMGEdgeHasValue> getHVSetOfMissingNullValues(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();

    SMGEdgeHasValueFilter nonNullPtrInSmg2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    nonNullPtrInSmg2.filterNotHavingValue(pSMG2.getNullValue());

    SMGEdgeHasValueFilter nonNullPtrInSmg1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    nonNullPtrInSmg1.filterNotHavingValue(pSMG1.getNullValue());

    for (SMGEdgeHasValue edge : pSMG2.getHVEdges(nonNullPtrInSmg2)) {
      if (! pSMG2.isPointer(edge.getValue())) {
        continue;
      }

      nonNullPtrInSmg1.filterAtOffset(edge.getOffset());

      if (pSMG1.getHVEdges(nonNullPtrInSmg1).size() == 0) {

        TreeMap <Integer, Integer> newNullEdgesOffsetToSize =
            pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);

        int min = edge.getOffset();
        int max = edge.getOffset() + edge.getSizeInBits(pSMG1.getMachineModel());

        Entry<Integer, Integer> floorEntry = newNullEdgesOffsetToSize.floorEntry(min);
        if (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= max ) {
          retset.add(new SMGEdgeHasValue(edge.getType(), edge.getOffset(), pObj1, pSMG1.getNullValue()));
        }
      }
    }
    return retset;
  }

  static private SMGEdgeHasValue getNullEdgesIntersection(Entry<Integer, Integer> first, Entry<Integer,
      Integer> next, SMGObject pObj1, SMG pSMG1) {
    int resultOffset = Integer.max(first.getKey(), next.getKey());
    int resultSize = Integer.min(first.getValue() + first.getKey(), next.getValue() + next.getKey()) - resultOffset;
    return new SMGEdgeHasValue(resultSize, resultOffset, pObj1, pSMG1.getNullValue());
  }

  static public Set<SMGEdgeHasValue> getHVSetOfCommonNullValues(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();
    TreeMap<Integer, Integer> map1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Integer, Integer> map2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);
    for (Entry<Integer, Integer> entry1 : map1.entrySet()) {
      NavigableMap<Integer, Integer> subMap =
          map2.subMap(entry1.getKey(), true, entry1.getKey() + entry1.getValue(), false);
      for (Entry<Integer, Integer> entry2 : subMap.entrySet()) {
        retset.add(getNullEdgesIntersection(entry1, entry2, pObj1, pSMG1));
      }
    }
    for (Entry<Integer, Integer> entry2 : map2.entrySet()) {
      NavigableMap<Integer, Integer> subMap =
          map1.subMap(entry2.getKey(), false, entry2.getKey() + entry2.getValue(), false);
      for (Entry<Integer, Integer> entry1 : subMap.entrySet()) {
        retset.add(getNullEdgesIntersection(entry2, entry1, pObj1, pSMG1));
      }
    }

    return Collections.unmodifiableSet(retset);
  }

  static public Set<SMGEdgeHasValue> getHVSetWithoutNullValuesOnObject(SMG pSMG, SMGObject pObj) {
    Set<SMGEdgeHasValue> retset = new HashSet<>();
    retset.addAll(pSMG.getHVEdges());

    SMGEdgeHasValueFilter nullValueFilter = SMGEdgeHasValueFilter.objectFilter(pObj);
    nullValueFilter.filterHavingValue(pSMG.getNullValue());

    retset.removeAll(pSMG.getHVEdges(nullValueFilter));

    return retset;
  }

  private static void checkResultConsistencySingleSide(SMG pSMG1, SMGEdgeHasValueFilter nullEdges1,
                                                       SMG pSMG2, SMGObject pObj2, TreeMap<Integer, Integer> nullEdgesInSMG2) throws SMGInconsistentException {
    for (SMGEdgeHasValue edgeInSMG1 : pSMG1.getHVEdges(nullEdges1)) {
      int start = edgeInSMG1.getOffset();
      int byte_after_end = start + edgeInSMG1.getSizeInBits(pSMG1.getMachineModel());
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

      Entry<Integer, Integer> floorEntry = nullEdgesInSMG2.floorEntry(start);
      int nextNotNullBit = (floorEntry == null) ? start : Integer.max(start, floorEntry.getKey() +
          floorEntry.getValue());
      if (hvInSMG2 == null || ( nextNotNullBit < byte_after_end && ! pSMG2.isPointer(hvInSMG2.getValue()))) {
        throw new SMGInconsistentException("SMGJoinFields output assertions do not hold");
      }
    }
  }

  public static void checkResultConsistency(SMG pSMG1, SMG pSMG2, SMGObject pObj1, SMGObject pObj2) throws SMGInconsistentException {
    SMGEdgeHasValueFilter nullEdges1 = SMGEdgeHasValueFilter.objectFilter(pObj1).filterHavingValue(pSMG1.getNullValue());
    SMGEdgeHasValueFilter nullEdges2 = SMGEdgeHasValueFilter.objectFilter(pObj2).filterHavingValue(pSMG2.getNullValue());
    TreeMap<Integer, Integer> nullEdgesInSMG1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Integer, Integer> nullEdgesInSMG2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);

    if (pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1)).size() != pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)).size()) {
      throw new SMGInconsistentException("SMGJoinFields output assertion does not hold: the objects do not have identical sets of fields");
    }

    checkResultConsistencySingleSide(pSMG1, nullEdges1, pSMG2, pObj2, nullEdgesInSMG2);
    checkResultConsistencySingleSide(pSMG2, nullEdges2, pSMG1, pObj1, nullEdgesInSMG1);
  }
}

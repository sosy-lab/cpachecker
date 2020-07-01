/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.LinkedHashSet;
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
  private final SMG newSMG1;
  private final SMG newSMG2;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  /** Algorithm 3 from FIT-TR-2012-04 */
  public SMGJoinFields(
      final UnmodifiableSMG pSMG1, final UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {

    Preconditions.checkArgument(
        pObj1.getSize() == pObj2.getSize(),
        "SMGJoinFields object arguments need to have identical size");
    Preconditions.checkArgument(
        pSMG1.getObjects().contains(pObj1) && pSMG2.getObjects().contains(pObj2),
        "SMGJoinFields object arguments need to be included in parameter SMGs");

    newSMG1 = pSMG1.copyOf();
    newSMG2 = pSMG2.copyOf();

    // Algorithm 3 from FIT-TR-2012-04, line 2
    setCompatibleHVEdgesToSMG(newSMG1, pSMG2, pObj1, pObj2);
    setCompatibleHVEdgesToSMG(newSMG2, pSMG1, pObj2, pObj1);

    // Algorithm 3 from FIT-TR-2012-04, line 4
    joinFieldsRelaxStatus(pSMG1, newSMG1, SMGJoinStatus.LEFT_ENTAIL, pObj1);
    joinFieldsRelaxStatus(pSMG2, newSMG2, SMGJoinStatus.RIGHT_ENTAIL, pObj2);

    Set<SMGEdgeHasValue> smg2Extension = mergeNonNullHasValueEdges(pSMG1, newSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> smg1Extension = mergeNonNullHasValueEdges(pSMG2, newSMG1, pObj2, pObj1);

    // Algorithm 3 from FIT-TR-2012-04, line 5
    for (SMGEdgeHasValue edge : smg1Extension) {
      newSMG1.addValue(edge.getValue());
      newSMG1.addHasValueEdge(edge);
    }
    for (SMGEdgeHasValue edge : smg2Extension) {
      newSMG2.addValue(edge.getValue());
      newSMG2.addHasValueEdge(edge);
    }
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

  @VisibleForTesting
  public static Set<SMGEdgeHasValue> mergeNonNullHasValueEdges(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> returnSet = new LinkedHashSet<>();

    SMGEdgeHasValueFilter filterForSMG1 = SMGEdgeHasValueFilter.objectFilter(pObj1);
    SMGEdgeHasValueFilter filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);
    filterForSMG1.filterNotHavingValue(SMGZeroValue.INSTANCE);

    for (SMGEdgeHasValue edge : pSMG1.getHVEdges(filterForSMG1)) {
      filterForSMG2.filterAtOffset(edge.getOffset());
      if (pSMG2.getHVEdges(filterForSMG2).isEmpty()) {
        returnSet.add(
            new SMGEdgeHasValue(
                edge.getSizeInBits(),
                edge.getOffset(),
                pObj2,
                SMGKnownSymValue.of()));
      }
    }

    return returnSet;
  }

  // Checks whether the nullified blocks are same in the new SMG as in the original SMG
  // or they got shrank / removed
  @VisibleForTesting
  void joinFieldsRelaxStatus(
      UnmodifiableSMG pOrigSMG,
      UnmodifiableSMG pNewSMG,
      SMGJoinStatus pNewStatus,
      SMGObject pObject) {
    // consecutive null edge block maps (offset, length)
    TreeMap<Long, Integer> origNullBlocks = pOrigSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    TreeMap<Long, Integer> newNullBlocks = pNewSMG.getNullEdgesMapOffsetToSizeForObject(pObject);

    // important: the new null edge block can only by same size or smaller!

    // for each consecutive null edge block, that was originally there
    for (Entry<Long, Integer> origEdge : origNullBlocks.entrySet()) {
      // find a null edge block that is in the modified SMG, and starts at the same offset
      Integer newNullBlock = newNullBlocks.get(origEdge.getKey());
      if (// if there is none (meaning the block got shortened from the start)
          newNullBlock == null ||
          // or the new block has different size (got shortened from the end)
          newNullBlock.intValue() != origEdge.getValue().intValue()) {

        // check whether the block really got smaller and not bigger on its end
        // there is also un-checked possibility that the block got bigger on its start
        Preconditions.checkState(newNullBlock == null || newNullBlock < origEdge.getValue());

        // then update the status accordingly
        status = status.updateWith(pNewStatus);
      }
    }
  }

  /**
   * split nullified fields such that each (partial) field corresponds to a field in the other SMG.
   */
  @VisibleForTesting
  static void setCompatibleHVEdgesToSMG(
      SMG pSMG, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {

    SMGEdgeHasValueFilter nullValueFilter =
        SMGEdgeHasValueFilter.objectFilter(pObj1).filterHavingValue(SMGZeroValue.INSTANCE);
    Set<SMGEdgeHasValue> edgesToRemove = pSMG.getHVEdges(nullValueFilter);
    Set<SMGEdgeHasValue> edgesToAdd1 = getHVSetOfCommonNullValues(pSMG, pSMG2, pObj1, pObj2);
    Set<SMGEdgeHasValue> edgesToAdd2 = getHVSetOfMissingNullValues(pSMG, pSMG2, pObj1, pObj2);

    // step 2a
    for (SMGEdgeHasValue edge : edgesToRemove) {
      pSMG.removeHasValueEdge(edge);
    }
    // step 2b
    for (SMGEdgeHasValue edge : edgesToAdd1) {
      pSMG.addHasValueEdge(edge);
    }
    // step 2c
    for (SMGEdgeHasValue edge : edgesToAdd2) {
      pSMG.addHasValueEdge(edge);
    }
  }

  /**
   * get all HV-Edges that are part of both SMGs, such that in one SMG the edge points to NULL and
   * in the other one the edge points to a non-NULL value.
   */
  @VisibleForTesting
  static Set<SMGEdgeHasValue> getHVSetOfMissingNullValues(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new LinkedHashSet<>();

    SMGEdgeHasValueFilter nonNullPtrInSmg2 =
        SMGEdgeHasValueFilter.objectFilter(pObj2).filterNotHavingValue(SMGZeroValue.INSTANCE);
    SMGEdgeHasValueFilter nonNullPtrInSmg1 =
        SMGEdgeHasValueFilter.objectFilter(pObj1).filterNotHavingValue(SMGZeroValue.INSTANCE);

    for (SMGEdgeHasValue edge : pSMG2.getHVEdges(nonNullPtrInSmg2)) {
      nonNullPtrInSmg1.filterAtOffset(edge.getOffset());

      if (pSMG1.getHVEdges(nonNullPtrInSmg1).isEmpty()) {
        TreeMap <Long, Integer> newNullEdgesOffsetToSize =
            pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);

        long min = edge.getOffset();
        long max = edge.getOffset() + edge.getSizeInBits();

        Entry<Long, Integer> floorEntry = newNullEdgesOffsetToSize.floorEntry(min);
        if (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= max ) {
          retset.add(
              new SMGEdgeHasValue(
                  edge.getSizeInBits(),
                  edge.getOffset(),
                  pObj1,
                  SMGZeroValue.INSTANCE));
        }
      }
    }
    return retset;
  }

  private static SMGEdgeHasValue getNullEdgesIntersection(
      Entry<Long, Integer> first, Entry<Long, Integer> next, SMGObject pObj1) {
    long resultOffset = Long.max(first.getKey(), next.getKey());
    int resultSize = Math.toIntExact(Long.min(first.getValue() + first.getKey(), next.getValue()
        + next.getKey()) - resultOffset);
    return new SMGEdgeHasValue(resultSize, resultOffset, pObj1, SMGZeroValue.INSTANCE);
  }

  /** get all HV-Edges that are common in both SMGs, i.e. where both objects point to NULL. */
  @VisibleForTesting
  static Set<SMGEdgeHasValue> getHVSetOfCommonNullValues(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new LinkedHashSet<>();
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

    return retset;
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
      long byte_after_end = start + edgeInSMG1.getSizeInBits();
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(pObj2)
              .filterAtOffset(edgeInSMG1.getOffset())
              .filterBySize(edgeInSMG1.getSizeInBits());
      Set<SMGEdgeHasValue> hvInSMG2Set = pSMG2.getHVEdges(filter);

      SMGEdgeHasValue hvInSMG2;
      if (!hvInSMG2Set.isEmpty()) {
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

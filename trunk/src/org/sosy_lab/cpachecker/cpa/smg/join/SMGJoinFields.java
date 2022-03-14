// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdgeSet;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
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

    SMGHasValueEdges smg2Extension = mergeNonNullHasValueEdges(pSMG1, newSMG2, pObj1, pObj2);
    SMGHasValueEdges smg1Extension = mergeNonNullHasValueEdges(pSMG2, newSMG1, pObj2, pObj1);

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
  public static SMGHasValueEdges mergeNonNullHasValueEdges(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    SMGHasValueEdges returnSet = new SMGHasValueEdgeSet();

    if (pSMG1
        .getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1))
        .equals(pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)))) {
      return returnSet;
    }

    SMGEdgeHasValueFilter filterForSMG1 =
        SMGEdgeHasValueFilter.objectFilter(pObj1).filterNotHavingValue(SMGZeroValue.INSTANCE);
    SMGEdgeHasValueFilter filterForSMG2 = SMGEdgeHasValueFilter.objectFilter(pObj2);

    for (SMGEdgeHasValue edge : pSMG1.getHVEdges(filterForSMG1)) {
      if (!pSMG2
          .getHVEdges(
              filterForSMG2.overlapsWith(
                  new SMGEdgeHasValue(
                      edge.getSizeInBits(), edge.getOffset(), pObj2, edge.getValue())))
          .iterator()
          .hasNext()) {
        returnSet =
            returnSet.addEdgeAndCopy(
                new SMGEdgeHasValue(
                    edge.getSizeInBits(), edge.getOffset(), pObj2, SMGKnownSymValue.of()));
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
    TreeMap<Long, Long> origNullBlocks = pOrigSMG.getNullEdgesMapOffsetToSizeForObject(pObject);
    TreeMap<Long, Long> newNullBlocks = pNewSMG.getNullEdgesMapOffsetToSizeForObject(pObject);

    // important: the new null edge block can only by same size or smaller!

    // for each consecutive null edge block, that was originally there
    for (Entry<Long, Long> origEdge : origNullBlocks.entrySet()) {
      // find a null edge block that is in the modified SMG, and starts at the same offset
      Long newNullBlock = newNullBlocks.get(origEdge.getKey());
      if ( // if there is none (meaning the block got shortened from the start)
      newNullBlock == null
          ||
          // or the new block has different size (got shortened from the end)
          newNullBlock.longValue() != origEdge.getValue().longValue()) {

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

    if (pSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1))
        .equals(pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)))) {
      return;
    }

    SMGEdgeHasValueFilter nullValueFilter =
        SMGEdgeHasValueFilter.objectFilter(pObj1)
            .filterHavingValue(SMGZeroValue.INSTANCE)
            .filterWithoutSize();
    Iterable<SMGEdgeHasValue> edgesToRemove = pSMG.getHVEdges(nullValueFilter);
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
      nonNullPtrInSmg1 = nonNullPtrInSmg1.filterAtOffset(edge.getOffset());

      if (!pSMG1.getHVEdges(nonNullPtrInSmg1).iterator().hasNext()) {
        TreeMap<Long, Long> newNullEdgesOffsetToSize =
            pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);

        long min = edge.getOffset();
        long max = edge.getOffset() + edge.getSizeInBits();

        Entry<Long, Long> floorEntry = newNullEdgesOffsetToSize.floorEntry(min);
        if (floorEntry != null && floorEntry.getValue() + floorEntry.getKey() >= max) {
          retset.add(
              new SMGEdgeHasValue(
                  edge.getSizeInBits(), edge.getOffset(), pObj1, SMGZeroValue.INSTANCE));
        }
      }
    }
    return retset;
  }

  private static SMGEdgeHasValue getNullEdgesIntersection(
      Entry<Long, Long> first, Entry<Long, Long> next, SMGObject pObj1) {
    long resultOffset = Long.max(first.getKey(), next.getKey());
    long resultSize =
        Long.min(first.getValue() + first.getKey(), next.getValue() + next.getKey()) - resultOffset;
    return new SMGEdgeHasValue(resultSize, resultOffset, pObj1, SMGZeroValue.INSTANCE);
  }

  /** get all HV-Edges that are common in both SMGs, i.e. where both objects point to NULL. */
  @VisibleForTesting
  static Set<SMGEdgeHasValue> getHVSetOfCommonNullValues(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2) {
    Set<SMGEdgeHasValue> retset = new LinkedHashSet<>();
    TreeMap<Long, Long> map1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Long, Long> map2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);
    for (Entry<Long, Long> entry1 : map1.entrySet()) {
      NavigableMap<Long, Long> subMap =
          map2.subMap(entry1.getKey(), true, entry1.getKey() + entry1.getValue(), false);
      for (Entry<Long, Long> entry2 : subMap.entrySet()) {
        retset.add(getNullEdgesIntersection(entry1, entry2, pObj1));
      }
    }
    for (Entry<Long, Long> entry2 : map2.entrySet()) {
      NavigableMap<Long, Long> subMap =
          map1.subMap(entry2.getKey(), false, entry2.getKey() + entry2.getValue(), false);
      for (Entry<Long, Long> entry1 : subMap.entrySet()) {
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
      TreeMap<Long, Long> nullEdgesInSMG2)
      throws SMGInconsistentException {
    for (SMGEdgeHasValue edgeInSMG1 : pSMG1.getHVEdges(nullEdges1)) {
      long start = edgeInSMG1.getOffset();
      long byte_after_end = start + edgeInSMG1.getSizeInBits();
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(pObj2)
              .filterAtOffset(edgeInSMG1.getOffset())
              .filterBySize(edgeInSMG1.getSizeInBits());
      Iterable<SMGEdgeHasValue> hvInSMG2Set = pSMG2.getHVEdges(filter);

      SMGEdgeHasValue hvInSMG2;
      if (hvInSMG2Set.iterator().hasNext()) {
        hvInSMG2 = Iterables.getOnlyElement(hvInSMG2Set);
      } else {
        hvInSMG2 = null;
      }

      Entry<Long, Long> floorEntry = nullEdgesInSMG2.floorEntry(start);
      long nextNotNullBit =
          (floorEntry == null)
              ? start
              : Long.max(start, floorEntry.getKey() + floorEntry.getValue());
      if (hvInSMG2 == null
          || (nextNotNullBit < byte_after_end && !pSMG2.isPointer(hvInSMG2.getValue()))) {
        throw new SMGInconsistentException("SMGJoinFields output assertions do not hold");
      }
    }
  }

  public static void checkResultConsistency(
      UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2, SMGObject pObj1, SMGObject pObj2)
      throws SMGInconsistentException {
    SMGEdgeHasValueFilter nullEdges1 =
        SMGEdgeHasValueFilter.objectFilter(pObj1)
            .filterHavingValue(SMGZeroValue.INSTANCE)
            .filterWithoutSize();
    SMGEdgeHasValueFilter nullEdges2 =
        SMGEdgeHasValueFilter.objectFilter(pObj2)
            .filterHavingValue(SMGZeroValue.INSTANCE)
            .filterWithoutSize();
    TreeMap<Long, Long> nullEdgesInSMG1 = pSMG1.getNullEdgesMapOffsetToSizeForObject(pObj1);
    TreeMap<Long, Long> nullEdgesInSMG2 = pSMG2.getNullEdgesMapOffsetToSizeForObject(pObj2);

    if (Iterables.size(
            pSMG1.getHVEdges(
                SMGEdgeHasValueFilter.objectFilter(pObj1)
                    .filterNotHavingValue(SMGZeroValue.INSTANCE)))
        != Iterables.size(
            pSMG2.getHVEdges(
                SMGEdgeHasValueFilter.objectFilter(pObj2)
                    .filterNotHavingValue(SMGZeroValue.INSTANCE)))) {
      throw new SMGInconsistentException(
          "SMGJoinFields output assertion does not hold: the objects do not have identical sets of"
              + " fields");
    }

    checkResultConsistencySingleSide(pSMG1, nullEdges1, pSMG2, pObj2, nullEdgesInSMG2);
    checkResultConsistencySingleSide(pSMG2, nullEdges2, pSMG1, pObj1, nullEdgesInSMG1);
  }
}

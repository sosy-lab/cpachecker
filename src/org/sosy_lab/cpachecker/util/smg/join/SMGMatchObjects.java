// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains utility classes for program slicing.
 *
 * @see org.sosy_lab.cpachecker.util.dependencegraph
 */
package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.OffsetAndSize;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.4) Algorithm 8. */
public class SMGMatchObjects extends SMGAbstractJoin {

  public SMGMatchObjects(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGObject pTargetObject1,
      SMGObject pTargetObject2) {
    super(pStatus, pInputSMG1, pInputSMG2, pDestSMG, pMapping1, pMapping2);
    // Precondition check: o1 in O1 and o2 in O2
    checkArgument(
        inputSMG1.getObjects().contains(pTargetObject1)
            && inputSMG2.getObjects().contains(pTargetObject2),
        "Only objects of givens SMGs can be joined.");
    // step 1-8
    if (!checkIfObjectsCanBeJoined(pTargetObject1, pTargetObject2)) {
      setBottomState();
      return;
    }
    // step 9-10
    updateJoinStatus(pTargetObject1, pTargetObject2);
  }

  /**
   * Utility function to update the join status for two join able objects. Implementation of
   * Algorithm 8 steps 9-10.
   *
   * @param pObj1 first input
   * @param pObj2 second input
   */
  private void updateJoinStatus(SMGObject pObj1, SMGObject pObj2) {
    int length1 =
        isDLLS(pObj1) ? ((SMGDoublyLinkedListSegment) pObj1).getMinLength() : Integer.MAX_VALUE;
    int length2 =
        isDLLS(pObj2) ? ((SMGDoublyLinkedListSegment) pObj2).getMinLength() : Integer.MAX_VALUE;
    if (length1 < length2) {
      status = status.updateWith(SMGJoinStatus.RIGHT_ENTAIL);
    } else if (length1 > length2) {
      status = status.updateWith(SMGJoinStatus.LEFT_ENTAIL);
    }
  }

  /**
   * Utility function to check whether all existing HasValueEdges with equal offset and size labels
   * are either unmapped or mapped with same value. Implementation of Algorithm 8 steps 7-8.
   *
   * @param pObj1 first parameter
   * @param pObj2 second parameter
   * @return true if there are no existing mappings which are not equal, false else
   */
  private boolean checkIfMappingsForHVEdgesMatches(SMGObject pObj1, SMGObject pObj2) {
    Map<OffsetAndSize, SMGValue> offsetAndSizeToValue1 =
        mapOffsetAndSizeToValue(inputSMG1.getEdges(pObj1));
    Map<OffsetAndSize, SMGValue> offsetAndSizeToValue2 =
        mapOffsetAndSizeToValue(inputSMG2.getEdges(pObj2));
    for (Map.Entry<OffsetAndSize, SMGValue> entry : offsetAndSizeToValue1.entrySet()) {
      if (offsetAndSizeToValue2.containsKey(entry.getKey())) {
        SMGValue v1 = entry.getValue();
        SMGValue v2 = offsetAndSizeToValue2.get(entry.getKey());
        if (!v1.isZero()
            && mapping1.hasMapping(v1)
            && mapping2.hasMapping(v2)
            && !mapping1.getMappedValue(v1).equals(mapping2.getMappedValue(v2))) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Utility function create a mapping with Offset and Size label to the related value of a
   * collection of edges.
   *
   * @param edges the input collection
   * @return injective mapping of offset and size to value
   */
  private Map<OffsetAndSize, SMGValue> mapOffsetAndSizeToValue(Collection<SMGHasValueEdge> edges) {
    return edges.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                hvEdge -> new OffsetAndSize(hvEdge.getOffset(), hvEdge.getSizeInBits()),
                hvEdge -> hvEdge.hasValue()));
  }

  /**
   * Utility function to check whether to objects are join able. Implementation of Algorithm 8 steps
   * 1-8.
   *
   * @param pObj1 first parameter
   * @param pObj2 second parameter
   * @return pObj1 is join able with pObj2
   */
  private boolean checkIfObjectsCanBeJoined(SMGObject pObj1, SMGObject pObj2) {
    // step 1
    if (pObj1.isZero() || pObj2.isZero()) {
      return false;
    }
    // step 2
    if (mapping1.hasMapping(pObj1)
        && mapping2.hasMapping(pObj2)
        && !mapping1.getMappedObject(pObj1).equals(mapping2.getMappedObject(pObj2))) {
      return false;
    }
    // step 3
    if (mapping1.hasMapping(pObj1) && mapping2.mappingExists(mapping1.getMappedObject(pObj1))) {
      return false;
    }
    // step 4
    if (mapping2.hasMapping(pObj2) && mapping1.mappingExists(mapping2.getMappedObject(pObj2))) {
      return false;
    }
    // step 5
    if (!pObj1.getSize().equals(pObj2.getSize())
        || inputSMG1.isValid(pObj1) != inputSMG2.isValid(pObj2)) {
      return false;
    }
    // step 6
    if (isDLLS(pObj1)
        && isDLLS(pObj2)
        && !checkIfLabelMatches(
            (SMGDoublyLinkedListSegment) pObj1, (SMGDoublyLinkedListSegment) pObj2)) {
      return false;
    }
    // step7-8
    return checkIfMappingsForHVEdgesMatches(pObj1, pObj2);
  }

  /**
   * Utility function which compares the labels of the dlls.
   *
   * @param dlls1 first argument
   * @param dlls2 second argument
   * @return true if all dlls labels are equal, false else.
   */
  private boolean checkIfLabelMatches(
      SMGDoublyLinkedListSegment dlls1, SMGDoublyLinkedListSegment dlls2) {
    return dlls1.getHeadOffset().equals(dlls2.getHeadOffset())
        && dlls1.getPrevOffset().equals(dlls2.getPrevOffset())
        && dlls1.getNextOffset().equals(dlls2.getNextOffset());
  }
}

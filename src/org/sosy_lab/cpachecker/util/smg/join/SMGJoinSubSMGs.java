// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.exception.SMGJoinException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.2) */
public class SMGJoinSubSMGs extends SMGAbstractJoin {

  public SMGJoinSubSMGs(
      SMGJoinStatus initialStatus,
      SMG pSMG1,
      SMG pSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGObject pObj1,
      SMGObject pObj2,
      SMGObject pNewObject,
      int pLDiff) {
    super(initialStatus, pSMG1, pSMG2, pDestSMG, pMapping1, pMapping2);
    joinFields(pObj1, pObj2);
    joinValues(pObj1, pObj2, pNewObject, pLDiff);
  }

  /**
   * Implementation of Algorithm 4 - Step 1.
   *
   * @param pObj1 - SMGObject of smg1
   * @param pObj2 - SMGObject of smg2
   */
  private void joinFields(SMGObject pObj1, SMGObject pObj2) {
    SMGJoinFields joinFields = new SMGJoinFields(inputSMG1, inputSMG2);
    joinFields.joinFields(pObj1, pObj2);
    inputSMG1 = joinFields.getSmg1();
    inputSMG2 = joinFields.getSmg2();
    status = status.updateWith(joinFields.getStatus());
    // check consistency here?
  }

  /**
   * Implementation of Algorithm 4 - Steps 2-4.
   *
   * @param pObj1 - SMGObject of smg1
   * @param pObj2 - SMGObject of smg2
   */
  private void joinValues(
      SMGObject pObj1, SMGObject pObj2, SMGObject pNewObject, int nestingLevelDiff) {

    for (SMGHasValueEdge edge1 : inputSMG1.getEdges(pObj1)) {
      // find edge in obj2 with same offset. After performing join fields there must exist such
      // edges.
      SMGHasValueEdge edge2 =
          inputSMG2
              .getHasValueEdgeByPredicate(pObj2, o -> o.getOffset().equals(edge1.getOffset()))
              .orElseThrow(
                  () ->
                      new SMGJoinException(
                          pObj2 + " misses edge with offset " + edge1.getOffset()));

      SMGValue value1 = edge1.hasValue();
      SMGValue value2 = edge2.hasValue();
      int newNestingLevelDiff = updateNestinglevelDiff(pObj1, edge1, nestingLevelDiff, 1);
      newNestingLevelDiff = updateNestinglevelDiff(pObj2, edge2, newNestingLevelDiff, -1);

      SMGJoinValues joinValues =
          new SMGJoinValues(
              status,
              inputSMG1,
              inputSMG2,
              destSMG,
              mapping1,
              mapping2,
              value1,
              value2,
              newNestingLevelDiff);

      status = joinValues.getStatus();

      /*
       * If the join of the values is not defined and can't be recovered through abstraction, the
       * join fails.
       */
      // Step 3.4
      if (!joinValues.isDefined() && !joinValues.isRecoverableFailur()) {
        status = SMGJoinStatus.INCOMPARABLE;
        return;
      }
      // set result Step 4
      inputSMG1 = joinValues.getInputSMG1();
      inputSMG2 = joinValues.getInputSMG2();
      destSMG = joinValues.getDestinationSMG();
      value = joinValues.getValue();
      // add new edge to resulting SMG Step 3.5
      SMGHasValueEdge newHVEdge =
          new SMGHasValueEdge(joinValues.getValue(), edge1.getOffset(), edge1.getSizeInBits());

      destSMG = destSMG.copyAndAddHVEdge(newHVEdge, pNewObject);
    }
  }

  /**
   * Update computation for nesting level difference. Algorithm 4 - Steps 3.2 and 3.3.
   *
   * @param pObj - the object to be considered
   * @param pEdge - the edge (for offset and size)
   * @param pLevel - the old level
   * @param pAccumulator - the accumulator to update the old level (+1/-1)
   * @return the updated level
   */
  private int updateNestinglevelDiff(
      SMGObject pObj, SMGHasValueEdge pEdge, int pLevel, int pAccumulator) {

    if (pObj instanceof SMGDoublyLinkedListSegment) {
      SMGDoublyLinkedListSegment doublyLinkedListSegment = (SMGDoublyLinkedListSegment) pObj;
      if (matchesOffsetAndSize(doublyLinkedListSegment, pEdge)) {
        pLevel += pAccumulator;
      }
    }

    return pLevel;
  }

  /**
   * Checks if a edges offset and size matches with a given DLLS's size and not with prev/next
   * offset.
   *
   * @param dlls - the DLLS
   * @param edge - the edge
   * @return true if sizes are equal and prev/head offset not equal to edge offset.
   */
  private boolean matchesOffsetAndSize(SMGDoublyLinkedListSegment dlls, SMGHasValueEdge edge) {
    if (dlls.getSize().equals(edge.getSizeInBits())) {
      return !dlls.getNextOffset().equals(edge.getOffset())
          && !dlls.getPrevOffset().equals(edge.getOffset());
    }
    return false;
  }
}

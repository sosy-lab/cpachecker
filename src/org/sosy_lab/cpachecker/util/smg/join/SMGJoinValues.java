// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.3) */
public class SMGJoinValues extends SMGAbstractJoin {

  public SMGJoinValues(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGValue pValue1,
      SMGValue pValue2,
      int pNestingLevelDiff) {
    super(pStatus, pInputSMG1, pInputSMG2, pDestSMG, pMapping1, pMapping2);
    joinValues(pValue1, pValue2, pNestingLevelDiff);
  }

  /**
   * Join two values, implementation of Algorithm 5.
   *
   * @param pValue1 - the first value of inputSMG1
   * @param pValue2 - the second value of inputSMG2
   * @param pNestingLevelDiff - nesting level difference
   */
  public void joinValues(SMGValue pValue1, SMGValue pValue2, int pNestingLevelDiff) {
    // both values are equal Algorithm 5 Step 1
    if (pValue1.equals(pValue2)) {
      value = pValue1;
      return;
    }
    // values already joined Algorithm 5 Step 2
    SMGValue mappingV1 = mapping1.getMappedValue(pValue1);
    SMGValue mappingV2 = mapping2.getMappedValue(pValue2);
    if (valuesEqualsAndNotZero(mappingV1, mappingV2)) {
      value = mappingV1;
      return;
    }

    Optional<SMGPointsToEdge> edgeOptionalV1 = inputSMG1.getPTEdge(pValue1);
    Optional<SMGPointsToEdge> edgeOptionalV2 = inputSMG2.getPTEdge(pValue2);

    if (edgeOptionalV1.isEmpty() && edgeOptionalV2.isEmpty()) {
      // no value is pointer value Algorithm 5 Step 3
      joinNonPointerValues(pValue1, pValue2, pNestingLevelDiff);
    } else if (edgeOptionalV1.isEmpty() || edgeOptionalV2.isEmpty()) {
      // one value is pointer Algorithm 5 Step 4
      setBottomState();
    } else {
      joinPointerValues(pValue1, pValue2, pNestingLevelDiff);
    }
  }

  public boolean isNullPointer(SMGPointsToEdge ptEdge) {
    return ptEdge == null || ptEdge.pointsTo().equals(SMGObject.nullInstance());
  }

  /**
   * Join two values that are pointer values, implementation of Algorithm 5 Step 5-8.
   *
   * @param pValue1 - the first value of inputSMG1
   * @param pValue2 - the second value of inputSMG2
   * @param pNestingLevelDiff - nesting level difference
   */
  private void joinPointerValues(SMGValue pValue1, SMGValue pValue2, int pNestingLevelDiff) {
    SMGJoinTargetObjects joinTargetObjects =
        new SMGJoinTargetObjects(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            pValue1,
            pValue2,
            pNestingLevelDiff);

    // step 5
    if (!joinTargetObjects.isDefined()) {
      setBottomState();
      return;
    }
    if (!joinTargetObjects.isRecoverableFailur()) {
      copyJoinState(joinTargetObjects);
      return;
    }

    // step 6
    Optional<SMGPointsToEdge> edgeOptionalV1 = inputSMG1.getPTEdge(pValue1);
    Optional<SMGPointsToEdge> edgeOptionalV2 = inputSMG2.getPTEdge(pValue2);

    checkArgument(edgeOptionalV1.isPresent() && edgeOptionalV2.isPresent());

    SMGObject obj1 = edgeOptionalV1.orElseThrow().pointsTo();
    SMGObject obj2 = edgeOptionalV2.orElseThrow().pointsTo();
    // step 7 left insert and join
    if (obj1 instanceof SMGDoublyLinkedListSegment) {
      SMGInsertLeftDlsAndJoin jDlsAndJoin =
          new SMGInsertLeftDlsAndJoin(
              status,
              inputSMG1,
              inputSMG2,
              destSMG,
              mapping1,
              mapping2,
              pValue1,
              pValue2,
              pNestingLevelDiff);
      if (!jDlsAndJoin.isDefined()) {
        setBottomState();
        return;
      }
      if (!jDlsAndJoin.isRecoverableFailur()) {
        copyJoinState(joinTargetObjects);
        return;
      }
    }
    // step 8 right insert and join
    if (obj2 instanceof SMGDoublyLinkedListSegment) {
      // THIS NEEDS TO BE DOUBLE CHECKED!! (The paper misses info on RightJoin)
      SMGInsertLeftDlsAndJoin jrightDlsAndJoin =
          new SMGInsertLeftDlsAndJoin(
              status,
              inputSMG2,
              inputSMG1,
              destSMG,
              mapping2,
              mapping1,
              pValue2,
              pValue1,
              pNestingLevelDiff);
      if (!jrightDlsAndJoin.isDefined() || jrightDlsAndJoin.isRecoverableFailur()) {
        status = SMGJoinStatus.INCOMPARABLE;
        isDefined = jrightDlsAndJoin.isDefined;
        isRecoverableFailure = jrightDlsAndJoin.isRecoverableFailure;
        return;
      }
      copyJoinState(joinTargetObjects);
    }
  }

  /**
   * Join two values that are no address values, implementation of Algorithm 5 Step 3.
   *
   * @param pValue1 - the first value of inputSMG1
   * @param pValue2 - the second value of inputSMG2
   * @param pNestingLevelDiff - nesting level difference
   */
  private void joinNonPointerValues(SMGValue pValue1, SMGValue pValue2, int pNestingLevelDiff) {
    SMGValue mappingV1 = mapping1.getMappedValue(pValue1);
    SMGValue mappingV2 = mapping2.getMappedValue(pValue2);
    // Bottom check - Algorithm 5 Step 3-1.
    if (!isBottom(mappingV1, mappingV2)) {
      setBottomState();
      return;
    }
    // compute new level step 3-2
    int newLevel = Integer.max(pValue1.getNestingLevel(), pValue2.getNestingLevel());
    // Step 3-6 return pValue1 - this is considered as typo
    value = SMGValue.of(newLevel);
    // add mappings step 3-3
    mapping1.addMapping(pValue1, value);
    mapping2.addMapping(pValue2, value);
    // update status step 3-4 and 3-5
    int levelDiffV1AndV2 = pValue1.getNestingLevel() - pValue2.getNestingLevel();
    if (levelDiffV1AndV2 < pNestingLevelDiff) {
      status = status.updateWith(SMGJoinStatus.LEFT_ENTAIL);
    } else if (levelDiffV1AndV2 > pNestingLevelDiff) {
      status = status.updateWith(SMGJoinStatus.RIGHT_ENTAIL);
    }
    destSMG = destSMG.copyAndAddValue(value);
  }

  private boolean isBottom(SMGValue... values) {
    for (SMGValue v : values) {
      if (v != null && !v.isZero()) {
        return false;
      }
    }
    return true;
  }

  private boolean valuesEqualsAndNotZero(SMGValue v1, SMGValue v2) {
    return v1 != null && v2 != null && !v1.isZero() && v1.equals(v2);
  }
}

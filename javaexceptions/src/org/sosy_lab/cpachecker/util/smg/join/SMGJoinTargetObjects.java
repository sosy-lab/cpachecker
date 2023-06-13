// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/** Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.4) */
public class SMGJoinTargetObjects extends SMGAbstractJoin {

  public SMGJoinTargetObjects(
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
    joinTargetObjects(pValue1, pValue2, pNestingLevelDiff);
  }

  /**
   * Join two pointer addresses, implementation of Algorithm 6.
   *
   * @param pValue1 - the first value of inputSMG1
   * @param pValue2 - the second value of inputSMG2
   * @param pNestingLevelDiff - nesting level difference
   */
  public void joinTargetObjects(SMGValue pValue1, SMGValue pValue2, int pNestingLevelDiff) {
    Optional<SMGPointsToEdge> edgeOptionalV1 = inputSMG1.getPTEdge(pValue1);
    Optional<SMGPointsToEdge> edgeOptionalV2 = inputSMG2.getPTEdge(pValue2);

    checkArgument(edgeOptionalV1.isPresent() && edgeOptionalV2.isPresent());

    SMGPointsToEdge pToEdge1 = edgeOptionalV1.orElseThrow();
    SMGPointsToEdge pToEdge2 = edgeOptionalV2.orElseThrow();

    // step 1
    if (!checkCompatibility(pToEdge1, pToEdge2, pValue1, pValue2, pNestingLevelDiff)) {
      isDefined = false;
      isRecoverableFailure = true;
      return;
    }

    // step 3
    if (checkForJoinedTargetObjects(pToEdge1.pointsTo(), pToEdge2.pointsTo())) {
      SMGMapTargetAddress mapTargetAddress =
          new SMGMapTargetAddress(
              status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pValue1, pValue2);
      copyJoinState(mapTargetAddress);
      return;
    }
    // step 4-6
    if (checkForRecoverableFailure(pToEdge1, pToEdge2)) {
      isDefined = false;
      isRecoverableFailure = true;
      return;
    }
    // step 7-11
    SMGObject newObject = createNewObject(pToEdge1.pointsTo(), pToEdge2.pointsTo());

    destSMG = destSMG.copyAndAddObject(newObject);
    if (!inputSMG1.isValid(pToEdge1.pointsTo())
        && !isDLLS(pToEdge1.pointsTo())
        && !inputSMG2.isValid(pToEdge2.pointsTo())) {
      destSMG = destSMG.copyAndInvalidateObject(newObject);
    }
    // add mappings step 12
    mapping1.addMapping(pToEdge1.pointsTo(), newObject);
    mapping2.addMapping(pToEdge2.pointsTo(), newObject);
    // create new target address step 13
    SMGMapTargetAddress targetAddress =
        new SMGMapTargetAddress(
            status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pValue1, pValue2);
    // step 14
    SMGJoinSubSMGs joinSubSMGs =
        new SMGJoinSubSMGs(
            status,
            inputSMG1,
            inputSMG2,
            targetAddress.destSMG,
            targetAddress.getMapping1(),
            targetAddress.getMapping2(),
            pToEdge1.pointsTo(),
            pToEdge2.pointsTo(),
            newObject,
            pNestingLevelDiff);
    if (!joinSubSMGs.isDefined) {
      setBottomState();
    } else {
      copyJoinState(joinSubSMGs);
      value = targetAddress.getValue();
    }
  }

  /**
   * New target object creation Algorithm 6 steps 7-11.
   *
   * @param obj1 - target of pointer1
   * @param obj2 - target of pointer2
   * @return new object where the joined pointer points to
   */
  private SMGObject createNewObject(SMGObject obj1, SMGObject obj2) {
    // step 10
    int level = Integer.max(obj1.getNestingLevel(), obj2.getNestingLevel());
    SMGObject newObject;
    // step 7-9
    if (isDLLS(obj1) || isDLLS(obj2)) {
      newObject = createDLLSCopyLabeling(obj1, obj2, level);
    } else {
      newObject = SMGObject.of(level, obj1.getSize(), obj1.getOffset());
    }
    destSMG = destSMG.copyAndAddObject(newObject);
    // step 11
    delayedJoin(mapping1.getMappedObject(obj1), newObject);
    delayedJoin(mapping2.getMappedObject(obj2), newObject);
    return newObject;
  }

  /**
   * Utility function for Algorithm 6 steps 7-9.
   *
   * @param obj1 - object of inputSMG1
   * @param obj2 - object of inputSMG2
   * @param pNestingLevel - the new level
   * @return new SMGDoublyLinkedListSegment with copied labels
   */
  private SMGDoublyLinkedListSegment createDLLSCopyLabeling(
      SMGObject obj1, SMGObject obj2, int pNestingLevel) {
    int length1 = 0;
    int length2 = 0;
    BigInteger headOffset = null;
    BigInteger nextOffset = null;
    BigInteger prevOffset = null;
    BigInteger pSize = null;
    BigInteger pOffset = null;
    if (isDLLS(obj1)) {
      length1 = ((SMGDoublyLinkedListSegment) obj1).getMinLength();
      headOffset = ((SMGDoublyLinkedListSegment) obj1).getHeadOffset();
      nextOffset = ((SMGDoublyLinkedListSegment) obj1).getNextOffset();
      prevOffset = ((SMGDoublyLinkedListSegment) obj1).getPrevOffset();
      pSize = obj1.getSize();
      pOffset = obj1.getOffset();
    }
    if (isDLLS(obj2)) {
      length2 = ((SMGDoublyLinkedListSegment) obj2).getMinLength();
      if (headOffset == null) {
        headOffset = ((SMGDoublyLinkedListSegment) obj2).getHeadOffset();
        nextOffset = ((SMGDoublyLinkedListSegment) obj2).getNextOffset();
        prevOffset = ((SMGDoublyLinkedListSegment) obj2).getPrevOffset();
        pSize = obj2.getSize();
        pOffset = obj2.getOffset();
      }
    }
    return new SMGDoublyLinkedListSegment(
        pNestingLevel,
        pSize,
        pOffset,
        headOffset,
        nextOffset,
        prevOffset,
        Integer.min(length1, length2));
  }

  /**
   * Delayed join implementation Algorithm 6 steps 11.
   *
   * @param oldObject - the old mapping to be replaced in destSMG
   * @param newObject - the replacement object in destSMG
   */
  private void delayedJoin(SMGObject oldObject, SMGObject newObject) {
    if (oldObject == null) {
      return;
    }
    destSMG = destSMG.copyAndReplaceObject(oldObject, newObject);
  }

  /**
   * Check for recoverable failure Algorithm 6 steps 4-6.
   *
   * @param pEdge1 - SMGPointsToEdge value1
   * @param pEdge2 - SMGPointsToEdge value2
   * @return true if failure and recoverable
   */
  private boolean checkForRecoverableFailure(SMGPointsToEdge pEdge1, SMGPointsToEdge pEdge2) {
    SMGObject targetObject1 = pEdge1.pointsTo();
    SMGObject targetObject2 = pEdge2.pointsTo();

    // step 4
    if (targetObject1.getClass().equals(targetObject2.getClass())
        && !pEdge1.targetSpecifier().equals(pEdge2.targetSpecifier())) {
      return true;
    }
    // step 5
    if (!targetObject1.getClass().equals(targetObject2.getClass())
        && mapping1
            .getMappedObject(targetObject1)
            .equals(mapping2.getMappedObject(targetObject1))) {
      return true;
    }
    // step 6
    SMGMatchObjects matchObjects =
        new SMGMatchObjects(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            targetObject1,
            targetObject2);
    return matchObjects.getStatus().equals(SMGJoinStatus.INCOMPARABLE);
  }

  /**
   * Check for already joined target objects Algorithm 6 step 3.
   *
   * @param obj1 - object 1
   * @param obj2 - object 2
   * @return true if both objects are already joined
   */
  private boolean checkForJoinedTargetObjects(SMGObject obj1, SMGObject obj2) {
    return (obj1.isZero() && obj1.equals(obj2)) || checkMappings(obj1, obj2);
  }

  private boolean checkMappings(SMGObject obj1, SMGObject obj2) {
    SMGObject mapped1 = mapping1.getMappedObject(obj1);
    SMGObject mapped2 = mapping2.getMappedObject(obj2);
    return mapped1 != null && mapped2 != null && mapped1.equals(mapped2);
  }

  /**
   * Compatibility check for Algorithm 6 step 1.
   *
   * @param pToEdge1 - edge for value1
   * @param pToEdge2 - edge for value2
   * @param pValue1 - value1
   * @param pValue2 - value2
   * @param pNestingLevelDiff - expected nesting level difference
   * @return true if pValue1 and pValue2 joinable
   */
  private boolean checkCompatibility(
      SMGPointsToEdge pToEdge1,
      SMGPointsToEdge pToEdge2,
      SMGValue pValue1,
      SMGValue pValue2,
      int pNestingLevelDiff) {
    if (pValue1.getNestingLevel() - pValue2.getNestingLevel() != pNestingLevelDiff) {
      return false;
    }

    return pToEdge1 != null
        && pToEdge2 != null
        && pToEdge1.getOffset().equals(pToEdge2.getOffset());
  }
}

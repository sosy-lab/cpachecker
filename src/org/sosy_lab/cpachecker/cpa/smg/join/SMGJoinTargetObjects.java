// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

final class SMGJoinTargetObjects {
  private SMGJoinStatus status;
  private boolean defined = false;
  private boolean recoverable = false;
  private final UnmodifiableSMG inputSMG1;
  private final UnmodifiableSMG inputSMG2;
  private SMG destSMG;
  private SMGValue value;
  @VisibleForTesting final SMGNodeMapping mapping1;
  @VisibleForTesting final SMGNodeMapping mapping2;

  private List<SMGGenericAbstractionCandidate> abstractionCandidates;

  private boolean matchOffsets(SMGEdgePointsTo pt1, SMGEdgePointsTo pt2) {
    if (pt1.getOffset() != pt2.getOffset()) {
      defined = false;
      recoverable = true;
      return true;
    }

    return false;
  }

  private boolean checkAlreadyJoined(
      SMGObject pObj1, SMGObject pObj2, SMGValue pAddress1, SMGValue pAddress2) {
    if ((pObj1 == SMGNullObject.INSTANCE && pObj2 == SMGNullObject.INSTANCE)
        || (mapping1.containsKey(pObj1)
            && mapping2.containsKey(pObj2)
            && mapping1.get(pObj1) == mapping2.get(pObj2))) {
      SMGJoinMapTargetAddress mta =
          new SMGJoinMapTargetAddress(
              inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pAddress1, pAddress2);
      defined = true;
      destSMG = mta.getSMG();
      value = mta.getValue();
      return true;
    }

    return false;
  }

  private boolean checkObjectMatch(SMGObject pObj1, SMGObject pObj2) {
    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(status, inputSMG1, inputSMG2, mapping1, mapping2, pObj1, pObj2);
    if (!mo.isDefined()) {
      defined = false;
      recoverable = true;
      return true;
    }

    status = mo.getStatus();
    return false;
  }

  /** Algorithm 6 from FIT-TR-2012-04 */
  public SMGJoinTargetObjects(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pSMG1,
      UnmodifiableSMG pSMG2,
      SMG pDestSMG,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      SMGLevelMapping pLevelMapping,
      SMGValue pAddress1,
      SMGValue pAddress2,
      int pLevel1,
      int pLevel2,
      int ldiff,
      boolean identicalInputSmgs,
      UnmodifiableSMGState pSmgState1,
      UnmodifiableSMGState pSmgState2)
      throws SMGInconsistentException {

    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    destSMG = pDestSMG;
    status = pStatus;

    SMGEdgePointsTo pt1 = inputSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = inputSMG2.getPointer(pAddress2);

    // Algorithm 6 from FIT-TR-2012-04, line 1
    if (pLevel1 - pLevel2 != ldiff) {
      defined = false;
      recoverable = true;
      return;
    }

    // Algorithm 6 from FIT-TR-2012-04, line 1
    if (matchOffsets(pt1, pt2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    SMGObject target1 = pt1.getObject();
    SMGObject target2 = pt2.getObject();

    // Algorithm 6 from FIT-TR-2012-04, line 2
    if (checkAlreadyJoined(target1, target2, pAddress1, pAddress2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    // Algorithm 6 from FIT-TR-2012-04, line 5
    if (target1.getKind() != target2.getKind()
        && mapping1.containsKey(target1)
        && mapping2.containsKey(target2)
        && !mapping1.get(target1).equals(mapping2.get(target2))) {
      recoverable = true;
      defined = false;
      return;
    }

    // Algorithm 6 from FIT-TR-2012-04, line 4
    if (target1.getKind() == target2.getKind()
        && pt1.getTargetSpecifier() != pt2.getTargetSpecifier()) {
      recoverable = true;
      defined = false;
      return;
    }

    // Algorithm 6 from FIT-TR-2012-04, line 6
    if (checkObjectMatch(target1, target2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    // Algorithm 6 from FIT-TR-2012-04, line 7
    SMGObject newObject =
        target1.join(target2, pLevelMapping.get(SMGJoinLevel.valueOf(pLevel1, pLevel2)));

    if (destSMG instanceof CLangSMG) {
      ((CLangSMG) destSMG).addHeapObject(newObject);
    } else {
      destSMG.addObject(newObject);
    }

    destSMG.setValidity(newObject, inputSMG1.isObjectValid(target1));

    // Algorithm 6 from FIT-TR-2012-04, line 11
    delayedJoin(target1, target2, newObject);

    // Algorithm 6 from FIT-TR-2012-04, line 12
    mapping1.map(target1, newObject);
    mapping2.map(target2, newObject);

    // Algorithm 6 from FIT-TR-2012-04, line 13
    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(
            inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pAddress1, pAddress2);
    destSMG = mta.getSMG();
    value = mta.getValue();

    // Algorithm 6 from FIT-TR-2012-04, line 14
    SMGJoinSubSMGs jss =
        new SMGJoinSubSMGs(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            pLevelMapping,
            target1,
            target2,
            newObject,
            ldiff,
            identicalInputSmgs,
            pSmgState1,
            pSmgState2);

    if (jss.isDefined()) {
      defined = true;
      status = jss.getStatus();
      abstractionCandidates = jss.getSubSmgAbstractionCandidates();
    }

    abstractionCandidates = ImmutableList.of();
  }

  private void delayedJoin(SMGObject pTarget1, SMGObject pTarget2, SMGObject targetObject) {

    if (mapping1.containsKey(pTarget1)) {
      SMGObject oldTarget = mapping1.get(pTarget1);
      Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(oldTarget, destSMG);
      removeSubSmgAndMappping(oldTarget, mapping1);

      for (SMGEdgePointsTo ptE : pointer) {
        destSMG.addPointsToEdge(
            new SMGEdgePointsTo(
                ptE.getValue(), targetObject, ptE.getOffset(), ptE.getTargetSpecifier()));
      }
    }

    if (mapping2.containsKey(pTarget2)) {
      SMGObject oldTarget = mapping2.get(pTarget2);
      Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(oldTarget, destSMG);
      removeSubSmgAndMappping(oldTarget, mapping2);

      for (SMGEdgePointsTo ptE : pointer) {
        destSMG.addPointsToEdge(
            new SMGEdgePointsTo(
                ptE.getValue(), targetObject, ptE.getOffset(), ptE.getTargetSpecifier()));
      }
    }
  }

  private void removeSubSmgAndMappping(SMGObject targetObject, SMGNodeMapping pMapping) {
    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    reached.add(targetObject);

    Set<SMGObject> toCheck = new HashSet<>();

    pMapping.removeValue(targetObject);
    SMGHasValueEdges hves = destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(targetObject));

    destSMG.markObjectDeletedAndRemoveEdges(targetObject);

    Set<Long> restricted = new HashSet<>();

    switch (targetObject.getKind()) {
      case DLL:
        restricted.add(((SMGDoublyLinkedList) targetObject).getNfo());
        restricted.add(((SMGDoublyLinkedList) targetObject).getPfo());
        break;
      case SLL:
        restricted.add(((SMGSingleLinkedList) targetObject).getNfo());
        break;
      default:
        return;
    }

    for (SMGEdgeHasValue hve : hves) {
      SMGValue val = hve.getValue();
      // TODO what does this code? why do we have restricted offsets?
      if (!restricted.contains(hve.getOffset()) && !val.isZero()) {

        if (destSMG.isPointer(val)) {
          SMGObject reachedObject = destSMG.getPointer(val).getObject();
          if (!reached.contains(reachedObject)
              && reachedObject.getLevel() <= targetObject.getLevel()) {
            toBeChecked.add(reachedObject);
            reached.add(reachedObject);
            pMapping.removeValue(val);
          }
        } else {
          pMapping.removeValue(val);
        }
      }
    }

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        removeObjectAndNodesFromDestSMG(
            objToCheck, reached, toBeChecked, targetObject.getLevel(), pMapping);
      }
    }
  }

  private void removeObjectAndNodesFromDestSMG(
      SMGObject pObjToCheck,
      Set<SMGObject> pReached,
      Set<SMGObject> pToBeChecked,
      int pLevel,
      SMGNodeMapping pMapping) {

    pMapping.removeValue(pObjToCheck);
    for (SMGEdgeHasValue hve :
        destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck))) {
      SMGValue val = hve.getValue();

      if (!val.isZero()) {

        if (destSMG.isPointer(val)) {
          SMGObject reachedObject = destSMG.getPointer(val).getObject();
          if (!pReached.contains(reachedObject) && reachedObject.getLevel() <= pLevel) {
            pToBeChecked.add(reachedObject);
            pReached.add(reachedObject);
            pMapping.removeValue(val);
          }
        } else {
          pMapping.removeValue(val);
        }
      }
    }

    destSMG.markObjectDeletedAndRemoveEdges(pObjToCheck);
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public UnmodifiableSMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMGValue getValue() {
    return value;
  }

  public boolean isRecoverable() {
    return recoverable;
  }

  public UnmodifiableSMG getInputSMG2() {
    return inputSMG2;
  }

  public List<SMGGenericAbstractionCandidate> getAbstractionCandidates() {
    return abstractionCandidates;
  }
}

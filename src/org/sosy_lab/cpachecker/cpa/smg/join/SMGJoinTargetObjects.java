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

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGLevelMapping.SMGJoinLevel;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SMGJoinTargetObjects {
  private SMGJoinStatus status;
  private boolean defined = false;
  private boolean recoverable = false;
  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;
  private Integer value;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  private List<SMGGenericAbstractionCandidate> abstractionCandidates;

  private static boolean matchOffsets(SMGJoinTargetObjects pJto, SMGEdgePointsTo pt1, SMGEdgePointsTo pt2) {
    if (pt1.getOffset() != pt2.getOffset()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    return false;
  }

  private static boolean checkAlreadyJoined(SMGJoinTargetObjects pJto, SMGObject pObj1, SMGObject pObj2,
                                            Integer pAddress1, Integer pAddress2) {
    if ((!pObj1.notNull() && !pObj2.notNull())
        || (pJto.mapping1.containsKey(pObj1)
            && pJto.mapping2.containsKey(pObj2)
            && pJto.mapping1.get(pObj1) == pJto.mapping2.get(pObj2))) {
      SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(pJto.inputSMG1, pJto.inputSMG2, pJto.destSMG, pJto.mapping1,
                                                        pJto.mapping2, pAddress1,
                                                        pAddress2);
      pJto.defined = true;
      pJto.destSMG = mta.getSMG();
      pJto.mapping1 = mta.getMapping1();
      pJto.mapping2 = mta.getMapping2();
      pJto.value = mta.getValue();
      return true;
    }

    return false;
  }

  private static boolean checkObjectMatch(SMGJoinTargetObjects pJto, SMGObject pObj1, SMGObject pObj2) {
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(pJto.status, pJto.inputSMG1, pJto.inputSMG2, pJto.mapping1, pJto.mapping2, pObj1, pObj2);
    if (! mo.isDefined()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    pJto.status = mo.getStatus();
    return false;
  }

  public SMGJoinTargetObjects(SMGJoinStatus pStatus,
                              SMG pSMG1, SMG pSMG2, SMG pDestSMG,
                              SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMapping,
                              Integer pAddress1, Integer pAddress2, int pLevel1, int pLevel2, int ldiff, boolean identicalInputSmgs, SMGState pSmgState1, SMGState pSmgState2) throws SMGInconsistentException {

    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    destSMG = pDestSMG;
    status = pStatus;

    SMGEdgePointsTo pt1 = inputSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = inputSMG2.getPointer(pAddress2);

    if(pLevel1 - pLevel2 != ldiff) {
      defined = false;
      recoverable = true;
      return;
    }

    if (SMGJoinTargetObjects.matchOffsets(this, pt1, pt2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    SMGObject target1 = pt1.getObject();
    SMGObject target2 = pt2.getObject();

    if (SMGJoinTargetObjects.checkAlreadyJoined(this, target1, target2, pAddress1, pAddress2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    if (target1.getKind() != target2.getKind() && mapping1.containsKey(target1)
        && mapping2.containsKey(target2)
        && !mapping1.get(target1).equals(mapping2.get(target2))) {
      recoverable = true;
      defined = false;
      return;
    }

    if (target1.getKind() == target2.getKind()
        && pt1.getTargetSpecifier() != pt2.getTargetSpecifier()) {
      recoverable = true;
      defined = false;
      return;
    }

    if (SMGJoinTargetObjects.checkObjectMatch(this, target1, target2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    SMGObject newObject = target1.join(target2, pLevelMapping.get(SMGJoinLevel.valueOf(pLevel1, pLevel2)));

    if (destSMG instanceof CLangSMG) {
      ((CLangSMG)destSMG).addHeapObject(newObject);
    } else {
      destSMG.addObject(newObject);
    }

    destSMG.setValidity(newObject, inputSMG1.isObjectValid(target1));

    delayedJoin(target1, target2, newObject);

    mapping1.map(target1, newObject);
    mapping2.map(target2, newObject);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pAddress1, pAddress2);
    destSMG = mta.getSMG();
    mapping1 = mta.getMapping1();
    mapping2 = mta.getMapping2();
    value = mta.getValue();

    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, inputSMG1, inputSMG2, destSMG,
        mapping1, mapping2, pLevelMapping,
        target1, target2, newObject, ldiff, identicalInputSmgs, pSmgState1, pSmgState2);

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
        destSMG.addPointsToEdge(new SMGEdgePointsTo(ptE.getValue(), targetObject, ptE.getOffset(),
            ptE.getTargetSpecifier()));
      }
    }

    if (mapping2.containsKey(pTarget2)) {
      SMGObject oldTarget = mapping2.get(pTarget2);
      Set<SMGEdgePointsTo> pointer = SMGUtils.getPointerToThisObject(oldTarget, destSMG);
      removeSubSmgAndMappping(oldTarget, mapping2);

      for (SMGEdgePointsTo ptE : pointer) {
        destSMG.addPointsToEdge(
            new SMGEdgePointsTo(ptE.getValue(), targetObject, ptE.getOffset(),
                ptE.getTargetSpecifier()));
      }
    }
  }

  private void removeSubSmgAndMappping(SMGObject targetObject, SMGNodeMapping pMapping) {
    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    reached.add(targetObject);

    Set<SMGObject> toCheck = new HashSet<>();

    pMapping.removeValue(targetObject);
    Set<SMGEdgeHasValue> hves =
        destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(targetObject));

    destSMG.removeObjectAndEdges(targetObject);

    Set<Integer> restricted = new HashSet<>();

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
      Integer val = hve.getValue();

      if (!restricted.contains(val) && val != 0) {

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

    while(!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for(SMGObject objToCheck : toCheck) {
        removeObjectAndNodesFromDestSMG(objToCheck, reached, toBeChecked, targetObject.getLevel(), pMapping);
      }
    }
  }

  private void removeObjectAndNodesFromDestSMG(SMGObject pObjToCheck, Set<SMGObject> pReached,
      Set<SMGObject> pToBeChecked, int pLevel, SMGNodeMapping pMapping) {

    pMapping.removeValue(pObjToCheck);
    Set<SMGEdgeHasValue> hves = destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {
      Integer val = hve.getValue();

      if (val != 0) {

        if (destSMG.isPointer(val)) {
          SMGObject reachedObject = destSMG.getPointer(val).getObject();
          if (!pReached.contains(reachedObject)
              && reachedObject.getLevel() <= pLevel) {
            pToBeChecked.add(reachedObject);
            pReached.add(reachedObject);
            pMapping.removeValue(val);
          }
        } else {
          pMapping.removeValue(val);
        }
      }
    }


    destSMG.removeObjectAndEdges(pObjToCheck);
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public Integer getValue() {
    return value;
  }

  public boolean isRecoverable() {
    return recoverable;
  }

  public SMG getInputSMG2() {
    return inputSMG2;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public List<SMGGenericAbstractionCandidate> getAbstractionCandidates() {
    return abstractionCandidates;
  }
}

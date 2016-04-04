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
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;

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
                                            Integer pAddress1, Integer pAddress2, boolean pRelabel) {
    if ((! pObj1.notNull()) && (! pObj2.notNull()) ||
        (pJto.mapping1.containsKey(pObj1) && pJto.mapping2.containsKey(pObj2) && pJto.mapping1.get(pObj1) == pJto.mapping2.get(pObj2))) {
      SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(pJto.inputSMG1, pJto.destSMG, pJto.mapping1,
                                                        pJto.mapping2, pAddress1,
                                                        pAddress2, pRelabel);
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
                              SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                              Integer pAddress1, Integer pAddress2, int pLevel1, int pLevel2, int ldiff, boolean identicalInputSmgs, boolean pIncreaseLevelAndRelabel, SMGState pSmgState1, SMGState pSmgState2) throws SMGInconsistentException {

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

    if (SMGJoinTargetObjects.checkAlreadyJoined(this, target1, target2, pAddress1, pAddress2, pIncreaseLevelAndRelabel)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    if (target1.getClass() != target2.getClass() && mapping1.containsKey(pAddress1)
        && mapping2.containsKey(pAddress2)
        && !mapping1.get(target1).equals(mapping2.get(target2))) {
      recoverable = true;
      defined = false;
      return;
    }

    if (target1.getClass() == target2.getClass()
        && pt1.getTargetSpecifier() != pt2.getTargetSpecifier()) {
      recoverable = true;
      defined = false;
      return;
    }

    if (SMGJoinTargetObjects.checkObjectMatch(this, target1, target2)) {
      abstractionCandidates = ImmutableList.of();
      return;
    }

    SMGObject objectToJoin1;
    SMGObject objectToJoin2;

    /* If destination Smg already contains target1, it can't be used
     * as join Object, a new object has to be created.
     */
    if (destSMG.getObjects().contains(target1)
        && (target1 instanceof SMGDoublyLinkedList || !(target2 instanceof SMGDoublyLinkedList))) {
      objectToJoin1 = target1.copy();
      objectToJoin2 = target2;
    } else if (destSMG.getObjects().contains(target2) && target2 instanceof SMGDoublyLinkedList
        && !(target1 instanceof SMGDoublyLinkedList)) {
      objectToJoin2 = target2.copy();
      objectToJoin1 = target1;
    } else {
      objectToJoin1 = target1;
      objectToJoin2 = target2;
    }

    SMGObject newObject = objectToJoin1.join(objectToJoin2, pIncreaseLevelAndRelabel);

    if (destSMG instanceof CLangSMG) {
      ((CLangSMG)destSMG).addHeapObject(newObject);
    } else {
      destSMG.addObject(newObject);
    }

    if (mapping1.containsKey(target1)) {
      throw new UnsupportedOperationException("Delayed join not yet implemented");
    }

    delayedJoin(target1, target2);

    mapping1.map(target1, newObject);
    mapping2.map(target2, newObject);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(inputSMG1, destSMG, mapping1, mapping2, pAddress1, pAddress2, pIncreaseLevelAndRelabel);
    destSMG = mta.getSMG();
    mapping1 = mta.getMapping1();
    mapping2 = mta.getMapping2();
    value = mta.getValue();

    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, inputSMG1, inputSMG2, destSMG,
                                            mapping1, mapping2,
                                            target1, target2, newObject, 0, false, identicalInputSmgs, pSmgState1, pSmgState2);
    if (jss.isDefined()) {
      defined = true;
      status = jss.getStatus();
      abstractionCandidates = jss.getSubSmgAbstractionCandidates();
    }

    abstractionCandidates = ImmutableList.of();
  }

  private void delayedJoin(SMGObject pTarget1, SMGObject pTarget2) {

    if (mapping1.containsKey(pTarget1)) {
      removeSubSmgAndMappping(mapping1.get(pTarget1));
    }

    if (mapping2.containsKey(pTarget2)) {
      removeSubSmgAndMappping(mapping1.get(pTarget2));
    }
  }

  private void removeSubSmgAndMappping(SMGObject targetObject) {
    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    toBeChecked.add(targetObject);
    reached.add(targetObject);

    Set<SMGObject> toCheck = new HashSet<>();

    while(!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for(SMGObject objToCheck : toCheck) {
        removeObjectAndNodesFromDestSMG(objToCheck, reached, toBeChecked);
      }
    }
  }

  private void removeObjectAndNodesFromDestSMG(SMGObject pObjToCheck, Set<SMGObject> pReached,
      Set<SMGObject> pToBeChecked) {

    mapping1.removeValue(pObjToCheck);
    Set<SMGEdgeHasValue> hves = destSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      Integer val = hve.getValue();

      mapping1.removeValue(val);

      if (destSMG.isPointer(hve.getValue())) {
        SMGObject reachedObject = destSMG.getPointer(hve.getValue()).getObject();
        if (!pReached.contains(reachedObject)) {
          pToBeChecked.add(reachedObject);
          pReached.add(reachedObject);
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

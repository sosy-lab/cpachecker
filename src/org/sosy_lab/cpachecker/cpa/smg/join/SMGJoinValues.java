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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

final class SMGJoinValues {
  private SMGJoinStatus status;
  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;
  private Integer value;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;
  private boolean defined = false;

  private final SMGState smgState1;
  private final SMGState smgState2;

  private List<SMGGenericAbstractionCandidate> abstractionCandidates;
  private boolean recoverable;

  private static boolean joinValuesIdentical(SMGJoinValues pJV, Integer pV1, Integer pV2) {
    if (pV1.equals(pV2) ) {
      pJV.value = pV1;
      pJV.defined = true;
      pJV.mapping1.map(pV1, pJV.getValue());
      pJV.mapping2.map(pV2, pJV.getValue());
      return true;
    }

    return false;
  }

  private static boolean joinValuesAlreadyJoined(SMGJoinValues pJV, Integer pV1, Integer pV2) {
    if (pJV.mapping1.containsKey(pV1) && pJV.mapping2.containsKey(pV2) &&
        pJV.mapping1.get(pV1).equals(pJV.mapping2.get(pV2))) {
      pJV.value = pJV.mapping1.get(pV1);
      pJV.defined = true;
      return true;
    }

    return false;
  }

  private static boolean joinValuesNonPointers(SMGJoinValues pJV, Integer pV1, Integer pV2, int pLevelV1, int pLevelV2, int lDiff) {
    if ((! pJV.inputSMG1.isPointer(pV1)) && (! pJV.inputSMG2.isPointer(pV2))) {
      if (pJV.mapping1.containsKey(pV1) || pJV.mapping2.containsKey(pV2)) {
        return true;
      }

      Integer newValue;

      if(pV1.equals(pV2)) {
        newValue = pV1;
      } else {
        newValue = SMGValueFactory.getNewValue();

        if (pJV.smgState1 == null || pJV.smgState2 == null) {
          pJV.status = SMGJoinStatus.updateStatus(pJV.status, SMGJoinStatus.INCOMPARABLE);
        } else {
          SMGJoinStatus v1isLessOrEqualV2 = pJV.smgState1.valueIsLessOrEqual(
              SMGKnownSymValue.valueOf(pV1), SMGKnownSymValue.valueOf(pV2), pJV.smgState2);
          SMGJoinStatus v2isLessOrEqualV1 = pJV.smgState2.valueIsLessOrEqual(
              SMGKnownSymValue.valueOf(pV2), SMGKnownSymValue.valueOf(pV1), pJV.smgState1);

          if (v1isLessOrEqualV2 != SMGJoinStatus.INCOMPARABLE) {
            pJV.status = SMGJoinStatus.updateStatus(pJV.status, v1isLessOrEqualV2);
          } else if (v1isLessOrEqualV2 == SMGJoinStatus.RIGHT_ENTAIL) {
            pJV.status = SMGJoinStatus.updateStatus(pJV.status, SMGJoinStatus.LEFT_ENTAIL);
          } else {
            pJV.status = SMGJoinStatus.updateStatus(pJV.status, v2isLessOrEqualV1);
          }
        }

      }

      if (pLevelV1 - pLevelV2 < lDiff) {
        pJV.status = SMGJoinStatus.updateStatus(pJV.status, SMGJoinStatus.LEFT_ENTAIL);
      } else if (pLevelV1 - pLevelV2 > lDiff) {
        pJV.status = SMGJoinStatus.updateStatus(pJV.status, SMGJoinStatus.RIGHT_ENTAIL);
      }

      pJV.destSMG.addValue(newValue);
      pJV.mapping1.map(pV1, newValue);
      pJV.mapping2.map(pV2, newValue);
      pJV.defined = true;
      pJV.value = newValue;
      return true;
    }
    return false;
  }

  private static boolean joinValuesMixedPointers(SMGJoinValues pJV, Integer pV1, Integer pV2) {
    return ((! pJV.inputSMG1.isPointer(pV1)) || (! pJV.inputSMG2.isPointer(pV2)));
  }

  private static boolean joinValuesPointers(SMGJoinValues pJV, Integer pV1, Integer pV2, int pLevel1, int pLevel2, int ldiff, boolean identicalInputSmg, boolean increaseLevelAndRelabel) throws SMGInconsistentException {
    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(pJV.status,
                                                        pJV.inputSMG1, pJV.inputSMG2, pJV.destSMG,
                                                        pJV.mapping1, pJV.mapping2,
                                                        pV1, pV2,pLevel1, pLevel2, ldiff, identicalInputSmg, increaseLevelAndRelabel, pJV.smgState1, pJV.smgState2);
    if (jto.isDefined()) {
      pJV.status = jto.getStatus();
      pJV.inputSMG1 = jto.getInputSMG1();
      pJV.inputSMG2 = jto.getInputSMG2();
      pJV.destSMG = jto.getDestinationSMG();
      pJV.mapping1 = jto.getMapping1();
      pJV.mapping2 = jto.getMapping2();
      pJV.value = jto.getValue();
      pJV.defined = true;
      pJV.abstractionCandidates = jto.getAbstractionCandidates();
      pJV.recoverable = jto.isRecoverable();
      return true;
    }

    if (jto.isRecoverable()) {
      return false;
    }

    pJV.defined = false;
    pJV.abstractionCandidates = ImmutableList.of();
    return true;
  }

  public SMGJoinValues(SMGJoinStatus pStatus,
                        SMG pSMG1, SMG pSMG2, SMG pDestSMG,
                        SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                        Integer pValue1, Integer pValue2, int pLDiff, boolean pIncreaseLevelAndRelabelTargetSpc, boolean identicalInputSmg, int levelV1, int levelV2, SMGState pStateOfSmg1, SMGState pStateOfSmg2) throws SMGInconsistentException {
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    status = pStatus;
    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    destSMG = pDestSMG;
    smgState1 = pStateOfSmg1;
    smgState2 = pStateOfSmg2;


    if ( identicalInputSmg && SMGJoinValues.joinValuesIdentical(this, pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      return;
    }

    if (SMGJoinValues.joinValuesAlreadyJoined(this, pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      return;
    }

    if (SMGJoinValues.joinValuesNonPointers(this, pValue1, pValue2, levelV1, levelV2, pLDiff)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      return;
    }

    if (SMGJoinValues.joinValuesMixedPointers(this, pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = true;
      return;
    }

    if (SMGJoinValues.joinValuesPointers(this, pValue1, pValue2, levelV1, levelV2, pLDiff, identicalInputSmg, pIncreaseLevelAndRelabelTargetSpc)) {

      if(recoverable) {

        SMGObject target1 = inputSMG1.getObjectPointedBy(pValue1);
        SMGObject target2 = inputSMG2.getObjectPointedBy(pValue2);

        if(target1 instanceof SMGDoublyLinkedList || target2 instanceof SMGDoublyLinkedList) {

          if (target1 instanceof SMGDoublyLinkedList) {
            Pair<Boolean, Boolean> result = insertDlsAndJoin(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pValue1,
                pValue2, (SMGDoublyLinkedList) target1, pLDiff, levelV1, levelV2,
                pIncreaseLevelAndRelabelTargetSpc, identicalInputSmg);

            if(result.getSecond()) {
              if(result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }

          if (target2 instanceof SMGDoublyLinkedList) {
            Pair<Boolean, Boolean> result = insertDlsAndJoin(status, inputSMG2, inputSMG1, destSMG, mapping2, mapping1, pValue2,
                pValue1, (SMGDoublyLinkedList) target2, pLDiff, levelV2, levelV1,
                pIncreaseLevelAndRelabelTargetSpc, identicalInputSmg);

            if(result.getSecond()) {
              if(result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }
        }  else {
          recoverable = false;
          return;
        }
      } else {
        recoverable = false;
        return;
      }
    }

    abstractionCandidates = ImmutableList.of();
    recoverable = false;
  }

  private Pair<Boolean, Boolean> insertDlsAndJoin(SMGJoinStatus pStatus, SMG pInputSMG1 , SMG  pInputSMG2 , SMG pDestSMG, SMGNodeMapping pMapping1 , SMGNodeMapping pMapping2 , Integer pointer1, Integer pointer2, SMGDoublyLinkedList pTarget1, int ldiff, int level1, int level2, boolean pIncreaseLevelAndRelabelTargetSpc, boolean identicalInputSmg) throws SMGInconsistentException {

    SMGEdgePointsTo ptEdge = pInputSMG1.getPointer(pointer1);
    SMGJoinStatus status = pStatus;
    SMG inputSMG1 = pInputSMG1;
    SMG  inputSMG2 = pInputSMG2;
    SMG destSMG = pDestSMG;
    SMGNodeMapping mapping1 = pMapping1;
    SMGNodeMapping mapping2 = pMapping2;

    int nf;

    switch(ptEdge.getTargetSpecifier()) {
      case FIRST:
        nf = pTarget1.getNfo();
        break;
      case LAST:
        nf = pTarget1.getPfo();
        break;
      default :
        return Pair.of(false, true);
    }

    SMGEdgeHasValue nextPointer = Iterables.getOnlyElement(
        inputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget1).filterAtOffset(nf)));

    if(mapping1.containsKey(pTarget1)) {
      SMGDoublyLinkedList jointDls = (SMGDoublyLinkedList) mapping1.get(pTarget1);
      if(mapping2.containsValue(jointDls)) {
        return Pair.of(false, true);
      }

      if(!mapping1.containsKey(pointer1)) {

        Integer resultPointer = SMGValueFactory.getNewValue();
        SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, jointDls, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
        destSMG.addValue(resultPointer);
        destSMG.addPointsToEdge(newJointPtEdge);

        mapping1.map(pointer1, resultPointer);
      } else {
        this.value = mapping1.get(pointer1);
        return Pair.of(true, true);
      }

      SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, ptEdge.getValue(), pointer2, ldiff, pIncreaseLevelAndRelabelTargetSpc, identicalInputSmg, level1, level2, smgState1, smgState2);

      if(jv.isDefined()) {

        status = jv.getStatus();
        inputSMG1 = jv.getInputSMG1();
        inputSMG2 = jv.getInputSMG2();
        destSMG = jv.getDestinationSMG();
        mapping1 = jv.getMapping1();
        mapping2 = jv.getMapping2();

      } else {
        return Pair.of(false, false);
      }
    }

    //TODO v1 == v2 Identical in conditions??
    if (mapping1.containsKey(nextPointer.getValue()) && mapping2.containsKey(pointer2) && !mapping1
        .get(pointer2).equals(mapping1.get(nextPointer.getValue()))) {
      return Pair.of(false, true);
    }

    SMGJoinStatus newJoinStatus =
        pTarget1.getMinimumLength() == 0 ? SMGJoinStatus.LEFT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    status = SMGJoinStatus.updateStatus(status, newJoinStatus);

    copyDlsSubSmgToDestSMG(pTarget1, mapping1, inputSMG1, destSMG, nf);

    SMGDoublyLinkedList dls = (SMGDoublyLinkedList) mapping1.get(pTarget1);
    int offset = ptEdge.getOffset();

    Set<SMGEdgeHasValue> hveForAddress = pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(dls).filterAtOffset(offset));

    Integer resultPointer;

    if(hveForAddress.isEmpty()) {
      resultPointer = SMGValueFactory.getNewValue();
      SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, dls, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
      destSMG.addValue(resultPointer);
      destSMG.addPointsToEdge(newJointPtEdge);
      mapping1.map(pointer1, resultPointer);

    }

    SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, ptEdge.getValue(), pointer2, ldiff, pIncreaseLevelAndRelabelTargetSpc, identicalInputSmg, level1, level2, smgState1, smgState2);

    Integer newAdressToDLS;

    if (jv.isDefined()) {

      this.status = jv.getStatus();
      this.inputSMG1 = jv.getInputSMG1();
      this.inputSMG2 = jv.getInputSMG2();
      this.destSMG = jv.getDestinationSMG();
      this.mapping1 = jv.getMapping1();
      this.mapping2 = jv.getMapping2();
      newAdressToDLS = jv.getValue();
      this.value = newAdressToDLS;

    } else {
      return Pair.of(false, false);
    }

    pDestSMG.addHasValueEdge(new SMGEdgeHasValue( new CPointerType(false, false, new CSimpleType(false, false, CBasicType.UNSPECIFIED, false, false, false, false, false, false, false)), nf, dls, newAdressToDLS));

    return Pair.of(true, true);
  }

  private void copyDlsSubSmgToDestSMG(SMGDoublyLinkedList pTarget1, SMGNodeMapping pMapping1, SMG pInputSMG1, SMG pDestSMG, int pNf) {

    Set<SMGObject> toBeChecked = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    if (!pMapping1.containsKey(pTarget1)) {
      SMGDoublyLinkedList copy = new SMGDoublyLinkedList(pTarget1.getSize(), pTarget1.getHfo(),
          pTarget1.getNfo(), pTarget1.getPfo(), 0, pTarget1.getLevel());
      pDestSMG.addObject(copy);
    }

    reached.add(pTarget1);

    Set<SMGEdgeHasValue> hves = pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget1));

    for (SMGEdgeHasValue hve : hves) {

      if(hve.getOffset() != pNf && !pMapping1.containsKey(hve.getValue())) {
        Integer newVal = SMGValueFactory.getNewValue();
        SMGObject newObj = pMapping1.get(pTarget1);
        pDestSMG.addValue(newVal);
        pDestSMG.addHasValueEdge(new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newObj, newVal));
        pMapping1.map(hve.getValue(), newVal);

        if (pInputSMG1.isPointer(hve.getValue())) {
          SMGObject reachedObject = pInputSMG1.getPointer(hve.getValue()).getObject();
          if (!reached.contains(reachedObject)) {
            SMGObject newReachedObj = reachedObject.copy();
            pMapping1.map(reachedObject, newReachedObj);
            pDestSMG.addObject(newReachedObj);
            toBeChecked.add(reachedObject);
            reached.add(reachedObject);
          }
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while(!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for(SMGObject objToCheck : toCheck) {
        copyObjectAndNodesIntoDestSMG(objToCheck, reached, toBeChecked, pInputSMG1, pDestSMG, pMapping1);
      }
    }

  }

  private void copyObjectAndNodesIntoDestSMG(SMGObject pObjToCheck, Set<SMGObject> pReached,
      Set<SMGObject> pToBeChecked, SMG pInputSMG1, SMG pDestSMG, SMGNodeMapping pMapping1) {

    SMGObject newObj = pMapping1.get(pObjToCheck);

    Set<SMGEdgeHasValue> hves = pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      if(!pMapping1.containsKey(hve.getValue())) {
        Integer newVal = SMGValueFactory.getNewValue();
        pDestSMG.addValue(newVal);
        pDestSMG.addHasValueEdge(new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newObj, newVal));
        pMapping1.map(hve.getValue(), newVal);

        if (pInputSMG1.isPointer(hve.getValue())) {
          SMGObject reachedObject = pInputSMG1.getPointer(hve.getValue()).getObject();
          if (!pReached.contains(reachedObject)) {
            SMGObject newReachedObj = reachedObject.copy();
            pMapping1.map(reachedObject, newReachedObj);
            pDestSMG.addObject(newReachedObj);
            pToBeChecked.add(reachedObject);
            pReached.add(reachedObject);
          }
        }
      }
    }
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getInputSMG2() {
    return inputSMG2;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public Integer getValue() {
    return value;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public boolean isDefined() {
    return defined;
  }

  /**
   * Signifies, if the part of the sub-smg rooted at the
   * given value can possibly be joined through abstraction.
   *
   * @return true, if join is defined, or join through abstraction may be a possibility,
   * false otherwise.
   */
  public boolean isRecoverable() {
    return recoverable;
  }

  public boolean subSmgHasAbstractionsCandidates() {
    return false;
  }

  public List<SMGGenericAbstractionCandidate> getAbstractionCandidates() {
    return abstractionCandidates;
  }
}
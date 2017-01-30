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
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGLevelMapping.SMGJoinLevel;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.objects.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.objects.optional.SMGOptionalObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
          } else if (v2isLessOrEqualV1 == SMGJoinStatus.RIGHT_ENTAIL) {
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

  private static boolean joinValuesPointers(SMGJoinValues pJV, Integer pV1, Integer pV2, int pLevel1, int pLevel2, int ldiff, boolean identicalInputSmg, SMGLevelMapping pLevelMap) throws SMGInconsistentException {
    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(pJV.status,
                                                        pJV.inputSMG1, pJV.inputSMG2, pJV.destSMG,
                                                        pJV.mapping1, pJV.mapping2, pLevelMap,
                                                        pV1, pV2,pLevel1, pLevel2, ldiff, identicalInputSmg, pJV.smgState1, pJV.smgState2);
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
      pJV.recoverable = true;
      return true;
    }

    pJV.defined = false;
    pJV.recoverable = false;
    pJV.abstractionCandidates = ImmutableList.of();
    return false;
  }

  public SMGJoinValues(SMGJoinStatus pStatus,
                        final SMG pSMG1, final SMG pSMG2, SMG pDestSMG,
                        SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMap,
                        Integer pValue1, Integer pValue2, int pLDiff, boolean identicalInputSmg, int levelV1, int levelV2, int pPrevDestLevel, SMGState pStateOfSmg1, SMGState pStateOfSmg2) throws SMGInconsistentException {
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    status = pStatus;
    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    destSMG = pDestSMG;
    smgState1 = pStateOfSmg1;
    smgState2 = pStateOfSmg2;


    if (identicalInputSmg && SMGJoinValues.joinValuesIdentical(this, pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      mapping1.map(pValue1, pValue1);
      mapping2.map(pValue2, pValue1);
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

    if (SMGJoinValues.joinValuesPointers(this, pValue1, pValue2, levelV1, levelV2, pLDiff, identicalInputSmg, pLevelMap)) {

      if(defined) {
        abstractionCandidates = ImmutableList.of();
        recoverable = false;
        return;
      }

      if(recoverable) {

        SMGObject target1 = inputSMG1.getObjectPointedBy(pValue1);
        SMGObject target2 = inputSMG2.getObjectPointedBy(pValue2);

        if(target1.isAbstract() || target2.isAbstract()) {

          if (target1.getKind() == SMGObjectKind.DLL || target1.getKind() == SMGObjectKind.SLL) {

            Pair<Boolean, Boolean> result = insertLeftListAndJoin(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, pValue1,
                pValue2, target1, pLDiff, levelV1, levelV2,
                identicalInputSmg, pPrevDestLevel);

            if(result.getSecond()) {
              if(result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }

          if (target2.getKind() == SMGObjectKind.DLL || target2.getKind() == SMGObjectKind.SLL) {

            Pair<Boolean, Boolean> result = insertRightListAndJoin(status, inputSMG1, inputSMG2,
                destSMG, mapping1, mapping2, pLevelMap, pValue1,
                pValue2, target2, pLDiff, levelV1, levelV2,
                identicalInputSmg, pPrevDestLevel);

            if(result.getSecond()) {
              if(result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }
        }

        /*Try to create an optional object.*/
        Pair<Boolean, Boolean> result = insertLeftObjectAsOptional(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, pValue1,
            pValue2, target1, pLDiff, levelV1, levelV2,
            identicalInputSmg, pPrevDestLevel);

        if(result.getSecond()) {
          if(result.getFirst()) {
            return;
          }
        } else {
          recoverable = false;
          return;
        }

        result = insertRightObjectAsOptional(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, pValue1,
            pValue2, target2, pLDiff, levelV1, levelV2,
            identicalInputSmg, pPrevDestLevel);

        if(result.getSecond()) {
          if(result.getFirst()) {
            return;
          }
        } else {
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

  private Pair<Boolean, Boolean> insertRightObjectAsOptional(SMGJoinStatus pStatus, SMG pInputSMG1,
      SMG pInputSMG2, SMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMap,
      Integer pValue1, Integer pValue2, SMGObject pTarget, int pLDiff, int pLevelV1, int pLevelV2,
      boolean pIdenticalInputSmg, int pPrevDestLevel) throws SMGInconsistentException {

    switch (pTarget.getKind()) {
      case REG:
      case OPTIONAL:
        break;
      default:
        return Pair.of(false, true);
    }

    if (mapping2.containsKey(pTarget)) {
      SMGObject jointList = mapping2.get(pTarget);
      if (mapping1.containsValue(jointList)) {
        return Pair.of(false, true);
      }
    }

    /*Optional objects may be pointed to by one offset.*/
    Set<SMGEdgePointsTo> pointedToTarget = SMGUtils.getPointerToThisObject(pTarget, pInputSMG2);

    if(pointedToTarget.size() != 1) {
      return Pair.of(false, true);
    }

    SMGEdgePointsTo pointedToTargetEdge = Iterables.getOnlyElement(pointedToTarget);

    /*Fields of optional objects must have one pointer.*/
    Set<SMGEdgeHasValue> fieldsOfTarget = pInputSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget));

    if (fieldsOfTarget.isEmpty()) {
      return Pair.of(false, true);
    }

    Integer nextPointer;

    /* Null can be treated like a value, or like a pointer.
     * Only treat null like a pointer if we want to join with null.*/
    if (pValue1 == 0) {
      nextPointer = 0;
    } else {
      nextPointer = null;
    }

    for (SMGEdgeHasValue field : fieldsOfTarget) {
      int fieldValue = field.getValue();

      if (pInputSMG2.isPointer(fieldValue) && fieldValue != 0) {
        if (nextPointer == null) {
          nextPointer = fieldValue;
        } else if (nextPointer != fieldValue) {
          return Pair.of(false, true);
        }
      }
    }

    if(nextPointer == null) {
      return Pair.of(false, true);
    }

    /*Check if pointer was already joint*/
    if (pMapping2.containsKey(pTarget)) {
      if (pMapping2.containsKey(pValue2)) {
        this.value = pMapping2.get(pValue2);
        this.defined = true;
        this.inputSMG1 = pInputSMG1;
        this.inputSMG2 = pInputSMG2;
        this.destSMG = pDestSMG;
        this.mapping1 = pMapping1;
        this.mapping2 = pMapping2;
        this.status = pStatus;
        return Pair.of(true, true);
      } else {
        return Pair.of(false, true);
      }
    }

    int level = getLevelOfOptionalObject(pValue2, pLevelMap, inputSMG2, mapping2, pLevelV1, pLevelV2);

    SMGLevelMapping newLevelMap = new SMGLevelMapping();
    newLevelMap.putAll(pLevelMap);
    newLevelMap.put(SMGJoinLevel.valueOf(pLevelV1, pLevelV2), level);

    /*Create optional object*/
    SMGObject optionalObject = new SMGOptionalObject(pTarget.getSize(), level);
    pMapping2.map(pTarget, optionalObject);
    ((CLangSMG) pDestSMG).addHeapObject(optionalObject);

    /*Create pointer to optional object.*/
    int resultPointer = SMGValueFactory.getNewValue();
    SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, optionalObject, pointedToTargetEdge.getOffset(), SMGTargetSpecifier.OPT);
    pDestSMG.addValue(resultPointer);
    pDestSMG.addPointsToEdge(newJointPtEdge);
    pMapping2.map(pValue2, resultPointer);
    pDestSMG.setValidity(optionalObject, pInputSMG2.isObjectValid(pTarget));

    SMGJoinStatus newJoinStatus = pTarget.getKind() == SMGObjectKind.OPTIONAL
        ? SMGJoinStatus.RIGHT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    SMGJoinStatus status = SMGJoinStatus.updateStatus(pStatus, newJoinStatus);

    /*Join next pointer with value1. And insert optional object if succesfully joined.*/
    SMGJoinValues jv =
        new SMGJoinValues(status, pInputSMG1, pInputSMG2, pDestSMG, pMapping1,
            pMapping2, newLevelMap, pValue1, nextPointer, pLDiff, pIdenticalInputSmg, pLevelV1,
            pLevelV2, pPrevDestLevel, smgState1, smgState2);

    int newAddressFromOptionalObject;

    if (jv.isDefined()) {

      newAddressFromOptionalObject = jv.getValue();

      /*No double optional objects for the same address.*/
      if (pDestSMG.getPointer(newAddressFromOptionalObject)
          .getTargetSpecifier() == SMGTargetSpecifier.OPT) {
        return Pair.of(false, false);
      }

      this.status = jv.getStatus();
      this.inputSMG1 = jv.getInputSMG1();
      this.inputSMG2 = jv.getInputSMG2();
      this.destSMG = jv.getDestinationSMG();
      this.mapping1 = jv.getMapping1();
      this.mapping2 = jv.getMapping2();
      this.value = resultPointer;
      this.defined = jv.defined;

    } else {
      return Pair.of(false, true);
    }

    /*Copy values of optional object.*/

    for (SMGEdgeHasValue field : fieldsOfTarget) {

      SMGEdgeHasValue newHve;

      if (field.getValue() == nextPointer) {
        newHve = new SMGEdgeHasValue(field.getType(), field.getOffset(), optionalObject,
            newAddressFromOptionalObject);
      } else {

        Integer val = field.getValue();
        Integer newVal;

        if (pMapping1.containsKey(val) || val == 0) {
          newVal = val;
        } else {
          newVal = SMGValueFactory.getNewValue();
          pMapping2.map(val, newVal);
          pDestSMG.addValue(newVal);
        }

        newHve = new SMGEdgeHasValue(field.getType(), field.getOffset(), optionalObject,
            newVal);
      }
      pDestSMG.addHasValueEdge(newHve);
    }

    return Pair.of(true, true);
  }

  private int getLevelOfOptionalObject(int pDisplacedValue, SMGLevelMapping pLevelMap, SMG pDisplacedValueSmg, SMGNodeMapping pDisplacedValueNodeMapping, int pLevelV1, int pLevelV2) {

    /*If the target of an optional object insertion is 0,
     * always increase the level in case of a join with an abstract
     * object as its sole source, otherwise it can't be joined.*/
    if (pDisplacedValue != 0) {
      return pLevelMap.get(SMGJoinLevel.valueOf(pLevelV1, pLevelV2));
    } else {

      Set<SMGEdgeHasValue> edges = pDisplacedValueSmg.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pDisplacedValue));

      SMGObject sourceObject = edges.iterator().next().getObject();
      for(SMGEdgeHasValue edge : edges) {
        if(edge.getObject() != sourceObject) {
          return -1;
        }
      }

      SMGObject destSmgSourceObject = pDisplacedValueNodeMapping.get(sourceObject);

      if(destSmgSourceObject.isAbstract()) {

        /*Pick arbitrary offset of edges to see if you should increase the level.*/
        int arbitraryOffset = edges.iterator().next().getOffset();

        switch (destSmgSourceObject.getKind()) {
          case DLL:
            SMGDoublyLinkedList dll = (SMGDoublyLinkedList) destSmgSourceObject;

            if (arbitraryOffset != dll.getNfo()
                && arbitraryOffset != dll.getPfo()) {
              return destSmgSourceObject.getLevel() + 1;
            } else {
              return destSmgSourceObject.getLevel();
            }

          case SLL:
            SMGSingleLinkedList sll = (SMGSingleLinkedList) destSmgSourceObject;

            if (arbitraryOffset != sll.getNfo()) {
              return destSmgSourceObject.getLevel() + 1;
            } else {
              return destSmgSourceObject.getLevel();
            }

          default:
            return -1;
        }

      } else {
        return destSmgSourceObject.getLevel();
      }
    }
  }

  private Pair<Boolean, Boolean> insertLeftObjectAsOptional(SMGJoinStatus pStatus, SMG pInputSMG1,
      SMG pInputSMG2, SMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMapping,
      Integer pValue1, Integer pValue2, SMGObject pTarget, int pLDiff, int pLevelV1, int pLevelV2,
      boolean pIdenticalInputSmg, int pPrevDestLevel) throws SMGInconsistentException {

    switch (pTarget.getKind()) {
      case REG:
      case OPTIONAL:
        break;
      default:
        return Pair.of(false, true);
    }

    if(pMapping1.containsKey(pTarget)) {
      SMGObject jointObject = mapping1.get(pTarget);
      if(mapping2.containsValue(jointObject)) {
        return Pair.of(false, true);
      }
    }

    /*Optional objects may be pointed to by one offset.*/
    Set<SMGEdgePointsTo> pointedToTarget = SMGUtils.getPointerToThisObject(pTarget, pInputSMG1);

    if(pointedToTarget.size() != 1) {
      return Pair.of(false, true);
    }

    SMGEdgePointsTo pointedToTargetEdge = Iterables.getOnlyElement(pointedToTarget);

    /*Fields of optional objects must have one pointer.*/
    Set<SMGEdgeHasValue> fieldsOfTarget = pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget));

    if (fieldsOfTarget.isEmpty()) {
      return Pair.of(false, true);
    }

    Integer nextPointer;

    /* Null can be treated like a value, or like a pointer.
     * Only treat null like a pointer if we want to join with null.*/
    if (pValue2 == 0) {
      nextPointer = 0;
    } else {
      nextPointer = null;
    }

    for (SMGEdgeHasValue field : fieldsOfTarget) {
      int fieldValue = field.getValue();

      if (pInputSMG1.isPointer(fieldValue) && fieldValue != 0) {
        if (nextPointer == null) {
          nextPointer = fieldValue;
        } else if (nextPointer != fieldValue) {
          return Pair.of(false, true);
        }
      }
    }

    if(nextPointer == null) {
      return Pair.of(false, true);
    }

    /*Check if pointer was already joint*/
    if (pMapping1.containsKey(pTarget)) {
      if (pMapping1.containsKey(pValue1)) {
        this.value = pMapping1.get(pValue1);
        this.defined = true;
        this.inputSMG1 = pInputSMG1;
        this.inputSMG2 = pInputSMG2;
        this.destSMG = pDestSMG;
        this.mapping1 = pMapping1;
        this.mapping2 = pMapping2;
        this.status = pStatus;
        return Pair.of(true, true);
      } else {
        return Pair.of(false, true);
      }
    }

    int level = getLevelOfOptionalObject(pValue1, pLevelMapping, inputSMG1, mapping1, pLevelV1, pLevelV2);


    SMGLevelMapping newLevelMap = new SMGLevelMapping();
    newLevelMap.putAll(pLevelMapping);
    newLevelMap.put(SMGJoinLevel.valueOf(pLevelV1, pLevelV2), level);

    /*Create optional object*/
    SMGObject optionalObject = new SMGOptionalObject(pTarget.getSize(), level);
    pMapping1.map(pTarget, optionalObject);
    ((CLangSMG) pDestSMG).addHeapObject(optionalObject);
    pDestSMG.setValidity(optionalObject, pInputSMG1.isObjectValid(pTarget));

    /*Create pointer to optional object.*/
    int resultPointer = SMGValueFactory.getNewValue();
    SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, optionalObject, pointedToTargetEdge.getOffset(), SMGTargetSpecifier.OPT);
    pDestSMG.addValue(resultPointer);
    pDestSMG.addPointsToEdge(newJointPtEdge);
    pMapping1.map(pValue1, resultPointer);

    SMGJoinStatus newJoinStatus = pTarget.getKind() == SMGObjectKind.OPTIONAL
        ? SMGJoinStatus.LEFT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    SMGJoinStatus status = SMGJoinStatus.updateStatus(pStatus, newJoinStatus);

    /*Join next pointer with value2. And insert optional object if succesfully joined.*/
    SMGJoinValues jv = new SMGJoinValues(status, pInputSMG1, pInputSMG2, pDestSMG, pMapping1,
        pMapping2, newLevelMap, nextPointer, pValue2, pLDiff, pIdenticalInputSmg, pLevelV1,
        pLevelV2, pPrevDestLevel, smgState1, smgState2);

    int newAddressFromOptionalObject;

    if (jv.isDefined()) {

      newAddressFromOptionalObject = jv.getValue();

      /*No double optional objects for the same address.*/
      if (pDestSMG.getPointer(newAddressFromOptionalObject)
          .getTargetSpecifier() == SMGTargetSpecifier.OPT) {
        return Pair.of(false, false);
      }

      this.status = jv.getStatus();
      this.inputSMG1 = jv.getInputSMG1();
      this.inputSMG2 = jv.getInputSMG2();
      this.destSMG = jv.getDestinationSMG();
      this.mapping1 = jv.getMapping1();
      this.mapping2 = jv.getMapping2();
      this.value = resultPointer;
      this.defined = jv.defined;

    } else {
      return Pair.of(false, true);
    }

    /*Copy values of optional object.*/

    for (SMGEdgeHasValue field : fieldsOfTarget) {

      SMGEdgeHasValue newHve;

      if (field.getValue() == nextPointer) {
        newHve = new SMGEdgeHasValue(field.getType(), field.getOffset(), optionalObject,
            newAddressFromOptionalObject);
      } else {

        Integer val = field.getValue();
        Integer newVal;

        if (pMapping1.containsKey(val) || val == 0) {
          newVal = val;
        } else {
          newVal = SMGValueFactory.getNewValue();
          pMapping1.map(val, newVal);
          pDestSMG.addValue(newVal);
        }

        newHve = new SMGEdgeHasValue(field.getType(), field.getOffset(), optionalObject,
            newVal);
      }
      pDestSMG.addHasValueEdge(newHve);
    }

    return Pair.of(true, true);
  }

  private Pair<Boolean, Boolean> insertLeftListAndJoin(SMGJoinStatus pStatus, SMG pInputSMG1 , SMG  pInputSMG2 , SMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMap, Integer pointer1, Integer pointer2, SMGObject pTarget, int ldiff, int level1, int level2, boolean identicalInputSmg, int pPrevDestLevel) throws SMGInconsistentException {

    SMGEdgePointsTo ptEdge = pInputSMG1.getPointer(pointer1);
    SMGJoinStatus status = pStatus;
    SMG inputSMG1 = pInputSMG1;
    SMG  inputSMG2 = pInputSMG2;
    SMG destSMG = pDestSMG;
    SMGNodeMapping mapping1 = pMapping1;
    SMGNodeMapping mapping2 = pMapping2;

    int nf;
    int length;
    int hfo;
    int nfo;
    int pfo;

    switch (ptEdge.getTargetSpecifier()) {
      case FIRST:
        if (pTarget.getKind() == SMGObjectKind.DLL) {
          nf = ((SMGDoublyLinkedList) pTarget).getNfo();
          hfo = ((SMGDoublyLinkedList) pTarget).getHfo();
          nfo = nf;
          pfo = ((SMGDoublyLinkedList) pTarget).getPfo();
          length = ((SMGDoublyLinkedList) pTarget).getMinimumLength();
        } else {
          nf = ((SMGSingleLinkedList) pTarget).getNfo();
          hfo = ((SMGSingleLinkedList) pTarget).getHfo();
          nfo = nf;
          pfo = -1;
          length = ((SMGSingleLinkedList) pTarget).getMinimumLength();
        }
        break;
      case LAST:
        nf = ((SMGDoublyLinkedList) pTarget).getPfo();
        hfo = ((SMGDoublyLinkedList) pTarget).getHfo();
        nfo = ((SMGDoublyLinkedList) pTarget).getPfo();
        pfo = nf;
        length = ((SMGDoublyLinkedList) pTarget).getMinimumLength();
        break;
      default:
        return Pair.of(false, true);
    }

    Integer nextPointer;


    Set<SMGEdgeHasValue> hvesNp = inputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget).filterAtOffset(nf));

    if(hvesNp.isEmpty()) {
      // Edge lost due to join fields, should be zero
      nextPointer = 0;
    } else {
      nextPointer = Iterables.getOnlyElement(hvesNp).getValue();
    }

    if(mapping1.containsKey(pTarget)) {
      SMGObject jointList = mapping1.get(pTarget);
      if(mapping2.containsValue(jointList)) {
        return Pair.of(false, true);
      }

      if(!mapping1.containsKey(pointer1)) {

        Integer resultPointer = SMGValueFactory.getNewValue();
        SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, jointList, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
        destSMG.addValue(resultPointer);
        destSMG.addPointsToEdge(newJointPtEdge);

        mapping1.map(pointer1, resultPointer);
      } else {
        this.value = mapping1.get(pointer1);
        this.defined = true;
        this.inputSMG1 = inputSMG1;
        this.inputSMG2 = inputSMG2;
        this.destSMG = destSMG;
        this.mapping1 = mapping1;
        this.mapping2 = mapping2;
        this.status = status;
        return Pair.of(true, true);
      }

      SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, nextPointer, pointer2, ldiff, identicalInputSmg, level1, level2, pPrevDestLevel, smgState1, smgState2);

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
    if (mapping1.containsKey(nextPointer) && mapping2.containsKey(pointer2) && !mapping2
        .get(pointer2).equals(mapping1.get(nextPointer))) {
      return Pair.of(false, true);
    }

    SMGJoinStatus newJoinStatus =
        length == 0 ? SMGJoinStatus.LEFT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    status = SMGJoinStatus.updateStatus(status, newJoinStatus);

    int lvlDiff = pPrevDestLevel - pTarget.getLevel();

    if (level1 < level2) {
      lvlDiff = lvlDiff + 1;
    }

    copyDlsSubSmgToDestSMG(pTarget, mapping1, inputSMG1, destSMG, lvlDiff);

    SMGObject list = mapping1.get(pTarget);

    Integer resultPointer = null;

    Set<SMGEdgePointsTo> edges = pDestSMG.getPtEdges(
        SMGEdgePointsToFilter.targetObjectFilter(list).filterAtTargetOffset(ptEdge.getOffset())
            .filterByTargetSpecifier(ptEdge.getTargetSpecifier()));

    if (!edges.isEmpty()) {
      resultPointer = Iterables.getOnlyElement(edges).getValue();
    }

    if(resultPointer == null) {
      resultPointer = SMGValueFactory.getNewValue();
      SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, list, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
      destSMG.addValue(resultPointer);
      destSMG.addPointsToEdge(newJointPtEdge);
      mapping1.map(pointer1, resultPointer);
    }

    SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, nextPointer, pointer2, ldiff, identicalInputSmg, level1, level2, pPrevDestLevel, smgState1, smgState2);

    Integer newAdressFromDLS;

    if (jv.isDefined()) {

      this.status = jv.getStatus();
      this.inputSMG1 = jv.getInputSMG1();
      this.inputSMG2 = jv.getInputSMG2();
      this.destSMG = jv.getDestinationSMG();
      this.mapping1 = jv.getMapping1();
      this.mapping2 = jv.getMapping2();
      newAdressFromDLS = jv.getValue();
      this.value = resultPointer;
      this.defined = jv.defined;

    } else {
      return Pair.of(false, false);
    }

    CType nfType = getType(pTarget, nf, inputSMG1);

    SMGEdgeHasValue newHve = new SMGEdgeHasValue(nfType, nf, list, newAdressFromDLS);

    if (pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).filterAtOffset(nf).filterHavingValue(newAdressFromDLS)).isEmpty()) {
      pDestSMG.addHasValueEdge(newHve);
    }

    if (smgState1.getAddress(pTarget, hfo, SMGTargetSpecifier.FIRST) == null) {
      CType nfType2 = getType(pTarget, nfo, inputSMG1);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfType2, nfo, list, newAdressFromDLS);
      pDestSMG.addHasValueEdge(newHve2);
    }

    if (pTarget.getKind() == SMGObjectKind.DLL
        && smgState1.getAddress(pTarget, hfo, SMGTargetSpecifier.LAST) == null) {
      CType nfType2 = getType(pTarget, pfo, inputSMG1);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfType2, pfo, list, newAdressFromDLS);
      pDestSMG.addHasValueEdge(newHve2);
    }

    return Pair.of(true, true);
  }

  private CType getType(SMGObject pTarget, int pNf, SMG inputSMG1) {
    Set<SMGEdgeHasValue> oldNfEdge =
        inputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget).filterAtOffset(pNf));

    if (oldNfEdge.isEmpty()) {
      return new SMGEdgeHasValue(inputSMG1.getMachineModel().getBitSizeofPtr(), pNf, pTarget, 0)
          .getType();
    } else {
      return Iterables.getOnlyElement(oldNfEdge).getType();
    }
  }

  private Pair<Boolean, Boolean> insertRightListAndJoin(SMGJoinStatus pStatus, SMG pInputSMG1, SMG  pInputSMG2, SMG pDestSMG, SMGNodeMapping pMapping1, SMGNodeMapping pMapping2, SMGLevelMapping pLevelMap, Integer pointer1, Integer pointer2, SMGObject pTarget, int ldiff, int level1, int level2, boolean identicalInputSmg, int pPrevDestLevel) throws SMGInconsistentException {

    SMGEdgePointsTo ptEdge = pInputSMG2.getPointer(pointer2);
    SMGJoinStatus status = pStatus;
    SMG inputSMG1 = pInputSMG1;
    SMG  inputSMG2 = pInputSMG2;
    SMG destSMG = pDestSMG;
    SMGNodeMapping mapping1 = pMapping1;
    SMGNodeMapping mapping2 = pMapping2;

    int nf;
    int length;
    int hfo;
    int nfo;
    int pfo;

    switch (ptEdge.getTargetSpecifier()) {
      case FIRST:
        if (pTarget.getKind() == SMGObjectKind.DLL) {
          nf = ((SMGDoublyLinkedList) pTarget).getNfo();
          hfo = ((SMGDoublyLinkedList) pTarget).getHfo();
          nfo = nf;
          pfo = ((SMGDoublyLinkedList) pTarget).getPfo();
          length = ((SMGDoublyLinkedList) pTarget).getMinimumLength();
        } else {
          nf = ((SMGSingleLinkedList) pTarget).getNfo();
          hfo = ((SMGSingleLinkedList) pTarget).getHfo();
          nfo = nf;
          pfo = -1;
          length = ((SMGSingleLinkedList) pTarget).getMinimumLength();
        }
        break;
      case LAST:
        nf = ((SMGDoublyLinkedList) pTarget).getPfo();
        hfo = ((SMGDoublyLinkedList) pTarget).getHfo();
        nfo = ((SMGDoublyLinkedList) pTarget).getPfo();
        pfo = nf;
        length = ((SMGDoublyLinkedList) pTarget).getMinimumLength();
        break;
      default:
        return Pair.of(false, true);
    }

    Set<SMGEdgeHasValue> npHves = inputSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget).filterAtOffset(nf));

    Integer nextPointer;

    if(npHves.isEmpty()) {
      // nullified block, but lacks edge
      nextPointer = 0;
    } else {
      nextPointer = Iterables.getOnlyElement(npHves).getValue();
    }

    if(mapping2.containsKey(pTarget)) {
      SMGObject jointList = mapping2.get(pTarget);
      if(mapping1.containsValue(jointList)) {
        return Pair.of(false, true);
      }

      if(!mapping2.containsKey(pointer2)) {

        Integer resultPointer = SMGValueFactory.getNewValue();
        SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, jointList, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
        destSMG.addValue(resultPointer);
        destSMG.addPointsToEdge(newJointPtEdge);

        mapping2.map(pointer2, resultPointer);
      } else {
        this.value = mapping2.get(pointer2);
        this.defined = true;
        this.inputSMG1 = inputSMG1;
        this.inputSMG2 = inputSMG2;
        this.destSMG = destSMG;
        this.mapping1 = mapping1;
        this.mapping2 = mapping2;
        this.status = status;
        return Pair.of(true, true);
      }

      SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, pointer1, nextPointer, ldiff, identicalInputSmg, level1, level2, pPrevDestLevel, smgState1, smgState2);

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
    if (mapping2.containsKey(nextPointer) && mapping1.containsKey(pointer1) && !mapping1
        .get(pointer1).equals(mapping2.get(nextPointer))) {
      return Pair.of(false, true);
    }

    SMGJoinStatus newJoinStatus =
        length == 0 ? SMGJoinStatus.RIGHT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    status = SMGJoinStatus.updateStatus(status, newJoinStatus);

    int levelDiff = pPrevDestLevel - pTarget.getLevel();

    if (level1 > level2) {
      levelDiff = levelDiff + 1;
    }

    copyDlsSubSmgToDestSMG(pTarget, mapping2, inputSMG2, destSMG, levelDiff);

    SMGObject list = mapping2.get(pTarget);

    Integer resultPointer = null;

    Set<SMGEdgePointsTo> edges = pDestSMG.getPtEdges(
        SMGEdgePointsToFilter.targetObjectFilter(list).filterAtTargetOffset(ptEdge.getOffset())
            .filterByTargetSpecifier(ptEdge.getTargetSpecifier()));

    if (!edges.isEmpty()) {
      resultPointer = Iterables.getOnlyElement(edges).getValue();
    }

    if(resultPointer == null) {
      resultPointer = SMGValueFactory.getNewValue();
      SMGEdgePointsTo newJointPtEdge = new SMGEdgePointsTo(resultPointer, list, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
      destSMG.addValue(resultPointer);
      destSMG.addPointsToEdge(newJointPtEdge);
      mapping2.map(pointer2, resultPointer);
    }

    SMGJoinValues jv = new SMGJoinValues(status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pLevelMap, pointer1, nextPointer, ldiff, identicalInputSmg, level1, level2, pPrevDestLevel, smgState1, smgState2);

    Integer newAdressFromDLS;

    if (jv.isDefined()) {

      this.status = jv.getStatus();
      this.inputSMG1 = jv.getInputSMG1();
      this.inputSMG2 = jv.getInputSMG2();
      this.destSMG = jv.getDestinationSMG();
      this.mapping1 = jv.getMapping1();
      this.mapping2 = jv.getMapping2();
      newAdressFromDLS = jv.getValue();
      this.value = resultPointer;
      this.defined = jv.defined;

    } else {
      return Pair.of(false, false);
    }

    CType nfType = getType(pTarget, nf, inputSMG2);
    SMGEdgeHasValue newHve = new SMGEdgeHasValue(nfType, nf, list, newAdressFromDLS);

    if (pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).filterAtOffset(nf).filterHavingValue(newAdressFromDLS)).isEmpty()) {
      pDestSMG.addHasValueEdge(newHve);
    }

    if (smgState2.getAddress(pTarget, hfo, SMGTargetSpecifier.FIRST) == null) {
      CType nfType2 = getType(pTarget, nfo, inputSMG2);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfType2, nfo, list, newAdressFromDLS);
      pDestSMG.addHasValueEdge(newHve2);
    }

    if (pTarget.getKind() == SMGObjectKind.DLL
        && smgState2.getAddress(pTarget, hfo, SMGTargetSpecifier.LAST) == null) {
      CType nfType2 = getType(pTarget, nfo, inputSMG2);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfType2, pfo, list, newAdressFromDLS);
      pDestSMG.addHasValueEdge(newHve2);
    }

    return Pair.of(true, true);
  }

  private void copyDlsSubSmgToDestSMG(SMGObject pList, SMGNodeMapping pMapping, SMG pInputSMG1, SMG pDestSMG, int pLevelDiff) {

    Set<SMGObject> toBeChecked = new HashSet<>();

    int listLevel = pList.getLevel() + pLevelDiff;

    SMGObject listCopy;
    int nfo = -1;
    int pfo = -1;

    if (pMapping.containsKey(pList)) {
      listCopy = pMapping.get(pList);
    } else {

      switch (pList.getKind()) {
        case DLL:
          nfo = ((SMGDoublyLinkedList) pList).getNfo();
          pfo = ((SMGDoublyLinkedList) pList).getPfo();
          int hfo = ((SMGDoublyLinkedList) pList).getHfo();
          listCopy = new SMGDoublyLinkedList(pList.getSize(), hfo, nfo, pfo,
              0, listLevel);
          break;
        case SLL:
          nfo = ((SMGSingleLinkedList) pList).getNfo();
          hfo = ((SMGSingleLinkedList) pList).getHfo();
          listCopy = new SMGSingleLinkedList(pList.getSize(), hfo, nfo, 0, listLevel);
          break;
        default:
          throw new AssertionError();
      }

      pMapping.map(pList, listCopy);
      ((CLangSMG) pDestSMG).addHeapObject(listCopy);
    }

    Set<SMGEdgeHasValue> hves = pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pList));

    for (SMGEdgeHasValue hve : hves) {

      if(hve.getOffset() != pfo && hve.getOffset() != nfo) {

        int subDlsValue = hve.getValue();
        int newVal = subDlsValue;

        if (pInputSMG1.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSMG1.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();

          if(newVal != 0) {

            SMGObject copyOfReachedObject;

            if (!pMapping.containsKey(reachedObjectSubSmg)) {

              int newLevel = level + pLevelDiff;

              copyOfReachedObject = reachedObjectSubSmg.copy(newLevel);
              pMapping.map(reachedObjectSubSmg, copyOfReachedObject);
              ((CLangSMG) pDestSMG).addHeapObject(copyOfReachedObject);
              pDestSMG.setValidity(copyOfReachedObject, pInputSMG1.isObjectValid(reachedObjectSubSmg));
              toBeChecked.add(reachedObjectSubSmg);
            } else {
              copyOfReachedObject = pMapping.get(reachedObjectSubSmg);
            }

            if(pMapping.containsKey(subDlsValue)) {
              newVal = pMapping.get(subDlsValue);
            } else {
              newVal = SMGValueFactory.getNewValue();
              pDestSMG.addValue(newVal);
              pMapping.map(subDlsValue, newVal);

              SMGTargetSpecifier newTg;

              if (copyOfReachedObject instanceof SMGRegion) {
                newTg = SMGTargetSpecifier.REGION;
              } else {
                newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
              }

              SMGEdgePointsTo newPtEdge = new SMGEdgePointsTo(newVal, copyOfReachedObject, reachedObjectSubSmgPTEdge.getOffset(), newTg);
              pDestSMG.addPointsToEdge(newPtEdge);
            }
          }
        }

        if (pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(listCopy).filterAtOffset(hve.getOffset())).isEmpty()) {
          pDestSMG.addHasValueEdge(new SMGEdgeHasValue(hve.getType(), hve.getOffset(), listCopy, newVal));
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while(!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for(SMGObject objToCheck : toCheck) {
        copyObjectAndNodesIntoDestSMG(objToCheck, toBeChecked, pMapping, pInputSMG1, pDestSMG, pLevelDiff);
      }
    }
  }

  private void copyObjectAndNodesIntoDestSMG(SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked, SMGNodeMapping pMapping, SMG pInputSMG1, SMG pDestSMG, int pLevelDiff) {

    SMGObject newObj = pMapping.get(pObjToCheck);

    Set<SMGEdgeHasValue> hves = pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck));

    for (SMGEdgeHasValue hve : hves) {

      int subDlsValue = hve.getValue();
      int newVal = subDlsValue;

      if (pInputSMG1.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSMG1.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();

        if (newVal != 0) {

          SMGObject copyOfReachedObject;

          if (!pMapping.containsKey(reachedObjectSubSmg)) {

            int newLevel = level + pLevelDiff;

            copyOfReachedObject = reachedObjectSubSmg.copy(newLevel);
            pMapping.map(reachedObjectSubSmg, copyOfReachedObject);
            ((CLangSMG) pDestSMG).addHeapObject(copyOfReachedObject);
            pDestSMG.setValidity(copyOfReachedObject, pInputSMG1.isObjectValid(reachedObjectSubSmg));
            pToBeChecked.add(reachedObjectSubSmg);
          } else {
            copyOfReachedObject = pMapping.get(reachedObjectSubSmg);
          }

          if (pMapping.containsKey(subDlsValue)) {
            newVal = pMapping.get(subDlsValue);
          } else {
            newVal = SMGValueFactory.getNewValue();
            pDestSMG.addValue(newVal);
            pMapping.map(subDlsValue, newVal);

            SMGTargetSpecifier newTg;

            if (copyOfReachedObject instanceof SMGRegion) {
              newTg = SMGTargetSpecifier.REGION;
            } else {
              newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
            }

            SMGEdgePointsTo newPtEdge = new SMGEdgePointsTo(newVal, copyOfReachedObject,
                reachedObjectSubSmgPTEdge.getOffset(),
                newTg);
            pDestSMG.addPointsToEdge(newPtEdge);
          }
        }
      }

      if (pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(newObj).filterAtOffset(hve.getOffset())).isEmpty()) {
        pDestSMG.addHasValueEdge(new SMGEdgeHasValue(hve.getType(), hve.getOffset(), newObj, newVal));
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
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
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.SMGGenericAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.optional.SMGOptionalObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.util.Pair;

final class SMGJoinValues {
  private SMGJoinStatus status;
  private UnmodifiableSMG inputSMG1;
  private UnmodifiableSMG inputSMG2;
  private SMG destSMG;
  private SMGValue value;
  @VisibleForTesting final SMGNodeMapping mapping1;
  @VisibleForTesting final SMGNodeMapping mapping2;
  private boolean defined = false;

  private final UnmodifiableSMGState smgState1;
  private final UnmodifiableSMGState smgState2;

  private List<SMGGenericAbstractionCandidate> abstractionCandidates;
  private boolean recoverable;

  private boolean joinValuesIdentical(SMGValue pV1, SMGValue pV2) {
    if (pV1.equals(pV2)) {
      value = pV1;
      defined = true;
      return true;
    }

    return false;
  }

  private boolean joinValuesAlreadyJoined(SMGValue pV1, SMGValue pV2) {
    if (mapping1.containsKey(pV1)
        && mapping2.containsKey(pV2)
        && mapping1.get(pV1).equals(mapping2.get(pV2))) {
      value = mapping1.get(pV1);
      defined = true;
      return true;
    }

    return false;
  }

  private boolean joinValuesNonPointers(
      SMGValue pV1, SMGValue pV2, int pLevelV1, int pLevelV2, int lDiff) {
    if (!inputSMG1.isPointer(pV1) && !inputSMG2.isPointer(pV2)) {
      if (mapping1.containsKey(pV1) || mapping2.containsKey(pV2)) {
        return true;
      }

      SMGValue newValue;

      if (pV1.equals(pV2)) {
        newValue = pV1;
      } else {
        newValue = SMGKnownSymValue.of();

        if (smgState1 == null || smgState2 == null) {
          status = status.updateWith(SMGJoinStatus.INCOMPARABLE);
        } else {
          SMGJoinStatus v1isLessOrEqualV2 =
              valueIsLessOrEqual(
                  (SMGKnownSymbolicValue) pV1, (SMGKnownSymbolicValue) pV2, smgState1, smgState2);
          SMGJoinStatus v2isLessOrEqualV1 =
              valueIsLessOrEqual(
                  (SMGKnownSymbolicValue) pV2, (SMGKnownSymbolicValue) pV1, smgState2, smgState1);

          if (v1isLessOrEqualV2 != SMGJoinStatus.INCOMPARABLE) {
            status = status.updateWith(v1isLessOrEqualV2);
          } else if (v2isLessOrEqualV1 == SMGJoinStatus.RIGHT_ENTAIL) {
            status = status.updateWith(SMGJoinStatus.LEFT_ENTAIL);
          } else {
            status = status.updateWith(v2isLessOrEqualV1);
          }
        }
      }

      if (pLevelV1 - pLevelV2 < lDiff) {
        status = status.updateWith(SMGJoinStatus.LEFT_ENTAIL);
      } else if (pLevelV1 - pLevelV2 > lDiff) {
        status = status.updateWith(SMGJoinStatus.RIGHT_ENTAIL);
      }

      destSMG.addValue(newValue);
      mapping1.map(pV1, newValue);
      mapping2.map(pV2, newValue);
      defined = true;
      value = newValue;
      return true;
    }
    return false;
  }

  /**
   * Check if symbolic value1 of this smgState is less or equal to value2 of smgsState2.
   *
   * <p>A value is less or equal if every concrete value represented by value1 is also represented
   * by value2.
   *
   * <p>This check may be imprecise, but only insofar that equal symbolic values or symbolic values
   * that entail each other may be identified as incomparable, never the other way around.
   *
   * @param value1 Value of this smgState.
   * @param value2 Value of smgState2.
   * @param state1 this State.
   * @param state2 Another SMG State.
   * @return SMGJoinStatus.RIGHT_ENTAIL iff all values represented by value1 are also represented by
   *     value2. SMGJoinStatus.EQUAL iff values represented by value1 and value2 are equal.
   *     SMGJoinStatus.INCOMPARABLE otherwise.
   */
  private static SMGJoinStatus valueIsLessOrEqual(
      SMGKnownSymbolicValue value1,
      SMGKnownSymbolicValue value2,
      UnmodifiableSMGState state1,
      UnmodifiableSMGState state2) {

    if (value1.equals(value2)) {
      return SMGJoinStatus.EQUAL;
    }

    if (state2.isExplicit(value2)) {
      if (!state1.isExplicit(value1)) {
        return SMGJoinStatus.INCOMPARABLE;
      }

      if (!state2.getExplicit(value2).equals(state1.getExplicit(value1))) {
        return SMGJoinStatus.INCOMPARABLE;
      } else {
        // Same explicit values
        return SMGJoinStatus.EQUAL;
      }
    }

    for (SMGValue neqToVal2 : state2.getHeap().getNeqsForValue(value2)) {
      if (!state1.getHeap().haveNeqRelation(value1, neqToVal2)) {
        return SMGJoinStatus.INCOMPARABLE;
      }
    }

    if (state1.isExplicit(value1) || !state1.getHeap().getNeqsForValue(value1).isEmpty()) {
      return SMGJoinStatus.RIGHT_ENTAIL;
    }

    // Both values represent top
    return SMGJoinStatus.EQUAL;
  }

  private boolean joinValuesMixedPointers(SMGValue pV1, SMGValue pV2) {
    return !inputSMG1.isPointer(pV1) || !inputSMG2.isPointer(pV2);
  }

  private boolean joinValuesPointers(
      SMGValue pV1,
      SMGValue pV2,
      int pLevel1,
      int pLevel2,
      int ldiff,
      boolean identicalInputSmg,
      SMGLevelMapping pLevelMap)
      throws SMGInconsistentException {
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            pLevelMap,
            pV1,
            pV2,
            pLevel1,
            pLevel2,
            ldiff,
            identicalInputSmg,
            smgState1,
            smgState2);
    if (jto.isDefined()) {
      status = jto.getStatus();
      inputSMG1 = jto.getInputSMG1();
      inputSMG2 = jto.getInputSMG2();
      destSMG = jto.getDestinationSMG();
      value = jto.getValue();
      defined = true;
      abstractionCandidates = jto.getAbstractionCandidates();
      recoverable = jto.isRecoverable();
      return true;
    }

    if (jto.isRecoverable()) {
      recoverable = true;
      return true;
    }

    defined = false;
    recoverable = false;
    abstractionCandidates = ImmutableList.of();
    return false;
  }

  /** Algorithm 5 from FIT-TR-2012-04 */
  public SMGJoinValues(
      SMGJoinStatus pStatus,
      final UnmodifiableSMG pNewInputSMG1,
      final UnmodifiableSMG pNewInputSMG2,
      SMG pDestSMG,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      SMGLevelMapping pLevelMap,
      SMGValue pValue1,
      SMGValue pValue2,
      int pLDiff,
      boolean identicalInputSmg,
      int levelV1,
      int levelV2,
      int pPrevDestLevel,
      UnmodifiableSMGState pStateOfSmg1,
      UnmodifiableSMGState pStateOfSmg2)
      throws SMGInconsistentException {
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    status = pStatus;
    inputSMG1 = pNewInputSMG1;
    inputSMG2 = pNewInputSMG2;
    destSMG = pDestSMG;
    smgState1 = pStateOfSmg1;
    smgState2 = pStateOfSmg2;

    // Algorithm 5 from FIT-TR-2012-04, line 1, change: only for identical SMGs
    if (identicalInputSmg && joinValuesIdentical(pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      mapping1.map(pValue1, pValue1);
      mapping2.map(pValue2, pValue1);
      return;
    }

    // Algorithm 5 from FIT-TR-2012-04, line 2
    if (joinValuesAlreadyJoined(pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      return;
    }

    // Algorithm 5 from FIT-TR-2012-04, line 3
    if (joinValuesNonPointers(pValue1, pValue2, levelV1, levelV2, pLDiff)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = defined;
      return;
    }

    // Algorithm 5 from FIT-TR-2012-04, line 4
    if (joinValuesMixedPointers(pValue1, pValue2)) {
      abstractionCandidates = ImmutableList.of();
      recoverable = true;
      return;
    }

    // Algorithm 5 from FIT-TR-2012-04, line 5
    if (joinValuesPointers(
        pValue1, pValue2, levelV1, levelV2, pLDiff, identicalInputSmg, pLevelMap)) {

      if (defined) {
        abstractionCandidates = ImmutableList.of();
        recoverable = false;
        return;
      }

      if (recoverable) {

        // Algorithm 5 from FIT-TR-2012-04, line 6
        SMGObject target1 = inputSMG1.getObjectPointedBy(pValue1);
        SMGObject target2 = inputSMG2.getObjectPointedBy(pValue2);

        if (target1.isAbstract() || target2.isAbstract()) {

          // Algorithm 5 from FIT-TR-2012-04, line 7
          if (target1.getKind() == SMGObjectKind.DLL || target1.getKind() == SMGObjectKind.SLL) {

            Pair<Boolean, Boolean> result =
                insertLeftListAndJoin(
                    status,
                    inputSMG1,
                    inputSMG2,
                    destSMG,
                    pLevelMap,
                    pValue1,
                    pValue2,
                    target1,
                    pLDiff,
                    levelV1,
                    levelV2,
                    identicalInputSmg,
                    pPrevDestLevel);

            if (result.getSecond()) {
              if (result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }

          // Algorithm 5 from FIT-TR-2012-04, line 8
          if (target2.getKind() == SMGObjectKind.DLL || target2.getKind() == SMGObjectKind.SLL) {

            Pair<Boolean, Boolean> result =
                insertRightListAndJoin(
                    status,
                    inputSMG1,
                    inputSMG2,
                    destSMG,
                    pLevelMap,
                    pValue1,
                    pValue2,
                    target2,
                    pLDiff,
                    levelV1,
                    levelV2,
                    identicalInputSmg,
                    pPrevDestLevel);

            if (result.getSecond()) {
              if (result.getFirst()) {
                return;
              }
            } else {
              recoverable = false;
              return;
            }
          }
        }

        /*Try to create an optional object.*/
        Pair<Boolean, Boolean> result =
            insertLeftObjectAsOptional(
                status,
                inputSMG1,
                inputSMG2,
                destSMG,
                pLevelMap,
                pValue1,
                pValue2,
                target1,
                pLDiff,
                levelV1,
                levelV2,
                identicalInputSmg,
                pPrevDestLevel);

        if (result.getSecond()) {
          if (result.getFirst()) {
            return;
          }
        } else {
          recoverable = false;
          return;
        }

        result =
            insertRightObjectAsOptional(
                status,
                inputSMG1,
                inputSMG2,
                destSMG,
                pLevelMap,
                pValue1,
                pValue2,
                target2,
                pLDiff,
                levelV1,
                levelV2,
                identicalInputSmg,
                pPrevDestLevel);

        if (result.getSecond()) {
          if (result.getFirst()) {
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

  private Pair<Boolean, Boolean> insertRightObjectAsOptional(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pInputSMG1,
      UnmodifiableSMG pInputSMG2,
      SMG pDestSMG,
      SMGLevelMapping pLevelMap,
      SMGValue pValue1,
      SMGValue pValue2,
      SMGObject pTarget,
      int pLDiff,
      int pLevelV1,
      int pLevelV2,
      boolean pIdenticalInputSmg,
      int pPrevDestLevel)
      throws SMGInconsistentException {

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

    if (pointedToTarget.size() != 1) {
      return Pair.of(false, true);
    }

    SMGEdgePointsTo pointedToTargetEdge = Iterables.getOnlyElement(pointedToTarget);

    /*Fields of optional objects must have one pointer.*/
    SMGHasValueEdges fieldsOfTarget =
        pInputSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget));

    if (fieldsOfTarget.isEmpty()) {
      return Pair.of(false, true);
    }

    SMGValue nextPointer;

    /* Null can be treated like a value, or like a pointer.
     * Only treat null like a pointer if we want to join with null.*/
    if (pValue1.isZero()) {
      nextPointer = SMGZeroValue.INSTANCE;
    } else {
      nextPointer = null;
    }

    for (SMGEdgeHasValue field : fieldsOfTarget) {
      SMGValue fieldValue = field.getValue();

      if (pInputSMG2.isPointer(fieldValue) && !fieldValue.isZero()) {
        if (nextPointer == null) {
          nextPointer = fieldValue;
        } else if (nextPointer != fieldValue) {
          return Pair.of(false, true);
        }
      }
    }

    if (nextPointer == null) {
      return Pair.of(false, true);
    }

    /*Check if pointer was already joint*/
    if (mapping2.containsKey(pTarget)) {
      if (mapping2.containsKey(pValue2)) {
        value = mapping2.get(pValue2);
        defined = true;
        inputSMG1 = pInputSMG1;
        inputSMG2 = pInputSMG2;
        destSMG = pDestSMG;
        status = pStatus;
        return Pair.of(true, true);
      } else {
        return Pair.of(false, true);
      }
    }

    int level =
        getLevelOfOptionalObject(pValue2, pLevelMap, inputSMG2, mapping2, pLevelV1, pLevelV2);

    SMGLevelMapping newLevelMap = new SMGLevelMapping();
    newLevelMap.putAll(pLevelMap);
    newLevelMap.put(SMGJoinLevel.valueOf(pLevelV1, pLevelV2), level);

    /*Create optional object*/
    SMGObject optionalObject = new SMGOptionalObject(pTarget.getSize(), level);
    mapping2.map(pTarget, optionalObject);
    ((CLangSMG) pDestSMG).addHeapObject(optionalObject);

    /*Create pointer to optional object.*/
    SMGValue resultPointer = SMGKnownSymValue.of();
    SMGEdgePointsTo newJointPtEdge =
        new SMGEdgePointsTo(
            resultPointer, optionalObject, pointedToTargetEdge.getOffset(), SMGTargetSpecifier.OPT);
    pDestSMG.addValue(resultPointer);
    pDestSMG.addPointsToEdge(newJointPtEdge);
    mapping2.map(pValue2, resultPointer);
    pDestSMG.setValidity(optionalObject, pInputSMG2.isObjectValid(pTarget));

    SMGJoinStatus newJoinStatus =
        pTarget.getKind() == SMGObjectKind.OPTIONAL
            ? SMGJoinStatus.RIGHT_ENTAIL
            : SMGJoinStatus.INCOMPARABLE;

    SMGJoinStatus updatedStatus = pStatus.updateWith(newJoinStatus);

    /*Join next pointer with value1. And insert optional object if succesfully joined.*/
    SMGJoinValues jv =
        new SMGJoinValues(
            updatedStatus,
            pInputSMG1,
            pInputSMG2,
            pDestSMG,
            mapping1,
            mapping2,
            newLevelMap,
            pValue1,
            nextPointer,
            pLDiff,
            pIdenticalInputSmg,
            pLevelV1,
            pLevelV2,
            pPrevDestLevel,
            smgState1,
            smgState2);

    SMGValue newAddressFromOptionalObject;

    if (jv.isDefined()) {

      newAddressFromOptionalObject = jv.getValue();

      /*No double optional objects for the same address.*/
      if (pDestSMG.getPointer(newAddressFromOptionalObject).getTargetSpecifier()
          == SMGTargetSpecifier.OPT) {
        return Pair.of(false, false);
      }

      status = jv.getStatus();
      inputSMG1 = jv.getInputSMG1();
      inputSMG2 = jv.getInputSMG2();
      destSMG = jv.getDestinationSMG();
      value = resultPointer;
      defined = jv.defined;

    } else {
      return Pair.of(false, true);
    }

    /*Copy values of optional object.*/

    for (SMGEdgeHasValue field : fieldsOfTarget) {

      SMGEdgeHasValue newHve;

      if (field.getValue() == nextPointer) {
        newHve =
            new SMGEdgeHasValue(
                field.getSizeInBits(),
                field.getOffset(),
                optionalObject,
                newAddressFromOptionalObject);
      } else {

        SMGValue val = field.getValue();
        SMGValue newVal;

        if (mapping1.containsKey(val) || val.isZero()) {
          newVal = val;
        } else {
          newVal = SMGKnownSymValue.of();
          mapping2.map(val, newVal);
          pDestSMG.addValue(newVal);
        }

        newHve =
            new SMGEdgeHasValue(field.getSizeInBits(), field.getOffset(), optionalObject, newVal);
      }
      if (!pDestSMG.getValues().contains(newHve.getValue())) {
        pDestSMG.addValue(newHve.getValue());
      }
      if (!mapping1.containsKey(field.getValue())) {
        mapping1.map(field.getValue(), newHve.getValue());
      }
      pDestSMG.addHasValueEdge(newHve);
    }

    return Pair.of(true, true);
  }

  private int getLevelOfOptionalObject(
      SMGValue pDisplacedValue,
      SMGLevelMapping pLevelMap,
      UnmodifiableSMG pInputSMG1,
      SMGNodeMapping pDisplacedValueNodeMapping,
      int pLevelV1,
      int pLevelV2) {

    /*If the target of an optional object insertion is 0,
     * always increase the level in case of a join with an abstract
     * object as its sole source, otherwise it can't be joined.*/
    if (!pDisplacedValue.isZero()) {
      return pLevelMap.get(SMGJoinLevel.valueOf(pLevelV1, pLevelV2));
    } else {

      Iterable<SMGEdgeHasValue> edges =
          pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.valueFilter(pDisplacedValue));

      SMGObject sourceObject = edges.iterator().next().getObject();
      for (SMGEdgeHasValue edge : edges) {
        if (edge.getObject() != sourceObject) {
          return -1;
        }
      }

      SMGObject destSmgSourceObject = pDisplacedValueNodeMapping.get(sourceObject);

      if (destSmgSourceObject.isAbstract()) {

        /*Pick arbitrary offset of edges to see if you should increase the level.*/
        long arbitraryOffset = edges.iterator().next().getOffset();

        switch (destSmgSourceObject.getKind()) {
          case DLL:
            SMGDoublyLinkedList dll = (SMGDoublyLinkedList) destSmgSourceObject;
            if (arbitraryOffset != dll.getNfo() && arbitraryOffset != dll.getPfo()) {
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

  private Pair<Boolean, Boolean> insertLeftObjectAsOptional(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pInputSMG1,
      UnmodifiableSMG pInputSMG2,
      SMG pDestSMG,
      SMGLevelMapping pLevelMapping,
      SMGValue pValue1,
      SMGValue pValue2,
      SMGObject pTarget,
      int pLDiff,
      int pLevelV1,
      int pLevelV2,
      boolean pIdenticalInputSmg,
      int pPrevDestLevel)
      throws SMGInconsistentException {

    switch (pTarget.getKind()) {
      case REG:
      case OPTIONAL:
        break;
      default:
        return Pair.of(false, true);
    }

    if (mapping1.containsKey(pTarget)) {
      SMGObject jointObject = mapping1.get(pTarget);
      if (mapping2.containsValue(jointObject)) {
        return Pair.of(false, true);
      }
    }

    /*Optional objects may be pointed to by one offset.*/
    Set<SMGEdgePointsTo> pointedToTarget = SMGUtils.getPointerToThisObject(pTarget, pInputSMG1);

    if (pointedToTarget.size() != 1) {
      return Pair.of(false, true);
    }

    SMGEdgePointsTo pointedToTargetEdge = Iterables.getOnlyElement(pointedToTarget);

    /*Fields of optional objects must have one pointer.*/
    SMGHasValueEdges fieldsOfTarget =
        pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pTarget));

    if (fieldsOfTarget.isEmpty()) {
      return Pair.of(false, true);
    }

    SMGValue nextPointer;

    /* Null can be treated like a value, or like a pointer.
     * Only treat null like a pointer if we want to join with null.*/
    if (pValue2.isZero()) {
      nextPointer = SMGZeroValue.INSTANCE;
    } else {
      nextPointer = null;
    }

    for (SMGEdgeHasValue field : fieldsOfTarget) {
      SMGValue fieldValue = field.getValue();

      if (pInputSMG1.isPointer(fieldValue) && !fieldValue.isZero()) {
        if (nextPointer == null) {
          nextPointer = fieldValue;
        } else if (!nextPointer.equals(fieldValue)) {
          return Pair.of(false, true);
        }
      }
    }

    if (nextPointer == null) {
      return Pair.of(false, true);
    }

    /*Check if pointer was already joint*/
    if (mapping1.containsKey(pTarget)) {
      if (mapping1.containsKey(pValue1)) {
        value = mapping1.get(pValue1);
        defined = true;
        inputSMG1 = pInputSMG1;
        inputSMG2 = pInputSMG2;
        destSMG = pDestSMG;
        status = pStatus;
        return Pair.of(true, true);
      } else {
        return Pair.of(false, true);
      }
    }

    int level =
        getLevelOfOptionalObject(pValue1, pLevelMapping, inputSMG1, mapping1, pLevelV1, pLevelV2);

    SMGLevelMapping newLevelMap = new SMGLevelMapping();
    newLevelMap.putAll(pLevelMapping);
    newLevelMap.put(SMGJoinLevel.valueOf(pLevelV1, pLevelV2), level);

    /*Create optional object*/
    SMGObject optionalObject = new SMGOptionalObject(pTarget.getSize(), level);
    mapping1.map(pTarget, optionalObject);
    ((CLangSMG) pDestSMG).addHeapObject(optionalObject);
    pDestSMG.setValidity(optionalObject, pInputSMG1.isObjectValid(pTarget));

    /*Create pointer to optional object.*/
    SMGValue resultPointer = SMGKnownSymValue.of();
    SMGEdgePointsTo newJointPtEdge =
        new SMGEdgePointsTo(
            resultPointer, optionalObject, pointedToTargetEdge.getOffset(), SMGTargetSpecifier.OPT);
    pDestSMG.addValue(resultPointer);
    pDestSMG.addPointsToEdge(newJointPtEdge);
    mapping1.map(pValue1, resultPointer);

    SMGJoinStatus newJoinStatus =
        pTarget.getKind() == SMGObjectKind.OPTIONAL
            ? SMGJoinStatus.LEFT_ENTAIL
            : SMGJoinStatus.INCOMPARABLE;

    SMGJoinStatus updatedStatus = pStatus.updateWith(newJoinStatus);

    /*Join next pointer with value2. And insert optional object if succesfully joined.*/
    SMGJoinValues jv =
        new SMGJoinValues(
            updatedStatus,
            pInputSMG1,
            pInputSMG2,
            pDestSMG,
            mapping1,
            mapping2,
            newLevelMap,
            nextPointer,
            pValue2,
            pLDiff,
            pIdenticalInputSmg,
            pLevelV1,
            pLevelV2,
            pPrevDestLevel,
            smgState1,
            smgState2);

    SMGValue newAddressFromOptionalObject;

    if (jv.isDefined()) {

      newAddressFromOptionalObject = jv.getValue();

      /*No double optional objects for the same address.*/
      if (pDestSMG.getPointer(newAddressFromOptionalObject).getTargetSpecifier()
          == SMGTargetSpecifier.OPT) {
        return Pair.of(false, false);
      }

      status = jv.getStatus();
      inputSMG1 = jv.getInputSMG1();
      inputSMG2 = jv.getInputSMG2();
      destSMG = jv.getDestinationSMG();
      value = resultPointer;
      defined = jv.defined;

    } else {
      return Pair.of(false, true);
    }

    /*Copy values of optional object.*/

    for (SMGEdgeHasValue field : fieldsOfTarget) {

      SMGEdgeHasValue newHve;

      if (field.getValue().equals(nextPointer)) {
        newHve =
            new SMGEdgeHasValue(
                field.getSizeInBits(),
                field.getOffset(),
                optionalObject,
                newAddressFromOptionalObject);
      } else {

        SMGValue val = field.getValue();
        SMGValue newVal;

        if (mapping1.containsKey(val) || val.isZero()) {
          newVal = val;
        } else {
          newVal = SMGKnownSymValue.of();
          mapping1.map(val, newVal);
          pDestSMG.addValue(newVal);
        }

        newHve =
            new SMGEdgeHasValue(field.getSizeInBits(), field.getOffset(), optionalObject, newVal);
      }
      if (!pDestSMG.getValues().contains(newHve.getValue())) {
        pDestSMG.addValue(newHve.getValue());
      }
      if (!mapping1.containsKey(field.getValue())) {
        mapping1.map(field.getValue(), newHve.getValue());
      }
      pDestSMG.addHasValueEdge(newHve);
    }

    return Pair.of(true, true);
  }

  /** Algorithm 9 from FIT-TR-2012-04 */
  private Pair<Boolean, Boolean> insertLeftListAndJoin(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pInputSMG1,
      UnmodifiableSMG pInputSMG2,
      SMG pDestSMG,
      SMGLevelMapping pLevelMap,
      SMGValue pointer1,
      SMGValue pointer2,
      SMGObject pTarget,
      int ldiff,
      int level1,
      int level2,
      boolean identicalInputSmg,
      int pPrevDestLevel)
      throws SMGInconsistentException {

    SMGEdgePointsTo ptEdge = pInputSMG1.getPointer(pointer1);
    SMGJoinStatus newStatus = pStatus;
    UnmodifiableSMG newInputSMG1 = pInputSMG1;
    UnmodifiableSMG newInputSMG2 = pInputSMG2;
    SMG newDestSMG = pDestSMG;

    long nf;
    int length;
    long hfo;
    long nfo;
    long pfo;

    switch (ptEdge.getTargetSpecifier()) {
      case FIRST:
        if (pTarget.getKind() == SMGObjectKind.DLL) {
          final SMGDoublyLinkedList dll = (SMGDoublyLinkedList) pTarget;
          nf = dll.getNfo();
          hfo = dll.getHfo();
          nfo = nf;
          pfo = dll.getPfo();
          length = dll.getMinimumLength();
        } else {
          final SMGSingleLinkedList sll = (SMGSingleLinkedList) pTarget;
          nf = sll.getNfo();
          hfo = sll.getHfo();
          nfo = nf;
          pfo = -1;
          length = sll.getMinimumLength();
        }
        break;
      case LAST:
        final SMGDoublyLinkedList dll = (SMGDoublyLinkedList) pTarget;
        nf = dll.getPfo();
        hfo = dll.getHfo();
        nfo = dll.getPfo();
        pfo = nf;
        length = dll.getMinimumLength();
        break;
      default:
        return Pair.of(false, true);
    }

    SMGValue nextPointer;

    Iterable<SMGEdgeHasValue> hvesNp =
        newInputSMG1.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(pTarget)
                .filterAtOffset(nf)
                .filterBySize(newDestSMG.getSizeofPtrInBits()));

    if (!hvesNp.iterator().hasNext()) {
      // Edge lost due to join fields, should be zero
      nextPointer = SMGZeroValue.INSTANCE;
    } else {
      nextPointer = Iterables.getOnlyElement(hvesNp).getValue();
    }

    // Algorithm 9 from FIT-TR-2012-04, line 4
    if (mapping1.containsKey(pTarget)) {
      SMGObject jointList = mapping1.get(pTarget);
      if (mapping2.containsValue(jointList)) {
        return Pair.of(false, true);
      }

      if (!mapping1.containsKey(pointer1)) {

        SMGValue resultPointer = SMGKnownSymValue.of();
        SMGEdgePointsTo newJointPtEdge =
            new SMGEdgePointsTo(
                resultPointer, jointList, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
        newDestSMG.addValue(resultPointer);
        newDestSMG.addPointsToEdge(newJointPtEdge);

        mapping1.map(pointer1, resultPointer);
      } else {
        value = mapping1.get(pointer1);
        defined = true;
        inputSMG1 = newInputSMG1;
        inputSMG2 = newInputSMG2;
        destSMG = newDestSMG;
        status = newStatus;
        return Pair.of(true, true);
      }

      SMGJoinValues jv =
          new SMGJoinValues(
              newStatus,
              newInputSMG1,
              newInputSMG2,
              newDestSMG,
              mapping1,
              mapping2,
              pLevelMap,
              nextPointer,
              pointer2,
              ldiff,
              identicalInputSmg,
              level1,
              level2,
              pPrevDestLevel,
              smgState1,
              smgState2);

      if (jv.isDefined()) {

        newStatus = jv.getStatus();
        newInputSMG1 = jv.getInputSMG1();
        newInputSMG2 = jv.getInputSMG2();
        newDestSMG = jv.getDestinationSMG();

      } else {
        return Pair.of(false, false);
      }
    }

    // Algorithm 9 from FIT-TR-2012-04, line 5
    // TODO v1 == v2 Identical in conditions??
    if (mapping1.containsKey(nextPointer)
        && mapping2.containsKey(pointer2)
        && !mapping2.get(pointer2).equals(mapping1.get(nextPointer))) {
      return Pair.of(false, true);
    }

    // Algorithm 9 from FIT-TR-2012-04, line 6
    SMGJoinStatus newJoinStatus =
        length == 0 ? SMGJoinStatus.LEFT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    newStatus = newStatus.updateWith(newJoinStatus);

    int lvlDiff = pPrevDestLevel - pTarget.getLevel();

    if (level1 < level2) {
      lvlDiff = lvlDiff + 1;
    }

    // Algorithm 9 from FIT-TR-2012-04, line 7
    copyDlsSubSmgToDestSMG(pTarget, mapping1, newInputSMG1, newDestSMG, lvlDiff);

    SMGObject list = mapping1.get(pTarget);

    SMGValue resultPointer = null;

    Set<SMGEdgePointsTo> edges =
        pDestSMG.getPtEdges(
            SMGEdgePointsToFilter.targetObjectFilter(list)
                .filterAtTargetOffset(ptEdge.getOffset())
                .filterByTargetSpecifier(ptEdge.getTargetSpecifier()));

    if (!edges.isEmpty()) {
      resultPointer = Iterables.getOnlyElement(edges).getValue();
    }

    // Algorithm 9 from FIT-TR-2012-04, line 9
    if (resultPointer == null) {
      resultPointer = SMGKnownSymValue.of();
      SMGEdgePointsTo newJointPtEdge =
          new SMGEdgePointsTo(resultPointer, list, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
      newDestSMG.addValue(resultPointer);
      newDestSMG.addPointsToEdge(newJointPtEdge);
      mapping1.map(pointer1, resultPointer);
    }

    // Algorithm 9 from FIT-TR-2012-04, line 10
    SMGJoinValues jv =
        new SMGJoinValues(
            newStatus,
            newInputSMG1,
            newInputSMG2,
            newDestSMG,
            mapping1,
            mapping2,
            pLevelMap,
            nextPointer,
            pointer2,
            ldiff,
            identicalInputSmg,
            level1,
            level2,
            pPrevDestLevel,
            smgState1,
            smgState2);

    SMGValue newAdressFromDLS;

    if (jv.isDefined()) {

      status = jv.getStatus();
      inputSMG1 = jv.getInputSMG1();
      inputSMG2 = jv.getInputSMG2();
      destSMG = jv.getDestinationSMG();
      newAdressFromDLS = jv.getValue();
      value = resultPointer;
      defined = jv.defined;

    } else {
      return Pair.of(false, false);
    }

    long nfSize = getSize(pTarget, nf, newInputSMG1);

    // Algorithm 9 from FIT-TR-2012-04, line 11
    SMGEdgeHasValue newHve = new SMGEdgeHasValue(nfSize, nf, list, newAdressFromDLS);

    if (!pDestSMG
        .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve))
        .iterator()
        .hasNext()) {
      pDestSMG.addHasValueEdge(newHve);
    } else {
      for (SMGEdgeHasValue currentValue :
          pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve))) {
        if (!currentValue.getValue().equals(newAdressFromDLS)) {
          return Pair.of(false, false);
        }
      }
    }

    if (smgState1.getAddress(pTarget, hfo, SMGTargetSpecifier.FIRST) == null) {
      long nfSize2 = getSize(pTarget, nfo, newInputSMG1);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfSize2, nfo, list, newAdressFromDLS);
      if (!pDestSMG
          .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve2))
          .iterator()
          .hasNext()) {
        pDestSMG.addHasValueEdge(newHve2);
      }
    }

    if (pTarget.getKind() == SMGObjectKind.DLL
        && smgState1.getAddress(pTarget, hfo, SMGTargetSpecifier.LAST) == null) {
      long nfSize2 = getSize(pTarget, pfo, newInputSMG1);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfSize2, pfo, list, newAdressFromDLS);
      if (!pDestSMG
          .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve2))
          .iterator()
          .hasNext()) {
        pDestSMG.addHasValueEdge(newHve2);
      }
    }

    return Pair.of(true, true);
  }

  private long getSize(SMGObject pTarget, long pNf, UnmodifiableSMG pInputSMG1) {
    Iterable<SMGEdgeHasValue> oldNfEdge =
        pInputSMG1.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(pTarget).filterAtOffset(pNf).filterWithoutSize());

    if (!oldNfEdge.iterator().hasNext()) {
      return new SMGEdgeHasValue(
              pInputSMG1.getSizeofPtrInBits(), pNf, pTarget, SMGZeroValue.INSTANCE)
          .getSizeInBits();
    } else {
      return Iterables.getOnlyElement(oldNfEdge).getSizeInBits();
    }
  }

  private Pair<Boolean, Boolean> insertRightListAndJoin(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pInputSMG1,
      UnmodifiableSMG pInputSMG2,
      SMG pDestSMG,
      SMGLevelMapping pLevelMap,
      SMGValue pointer1,
      SMGValue pointer2,
      SMGObject pTarget,
      int ldiff,
      int level1,
      int level2,
      boolean identicalInputSmg,
      int pPrevDestLevel)
      throws SMGInconsistentException {

    SMGEdgePointsTo ptEdge = pInputSMG2.getPointer(pointer2);
    SMGJoinStatus newStatus = pStatus;
    UnmodifiableSMG newInputSMG1 = pInputSMG1;
    UnmodifiableSMG newInputSMG2 = pInputSMG2;
    SMG newDestSMG = pDestSMG;

    long nf;
    int length;
    long hfo;
    long nfo;
    long pfo;

    switch (ptEdge.getTargetSpecifier()) {
      case FIRST:
        if (pTarget.getKind() == SMGObjectKind.DLL) {
          final SMGDoublyLinkedList dll = (SMGDoublyLinkedList) pTarget;
          nf = dll.getNfo();
          hfo = dll.getHfo();
          nfo = nf;
          pfo = dll.getPfo();
          length = dll.getMinimumLength();
        } else {
          final SMGSingleLinkedList sll = (SMGSingleLinkedList) pTarget;
          nf = sll.getNfo();
          hfo = sll.getHfo();
          nfo = nf;
          pfo = -1;
          length = sll.getMinimumLength();
        }
        break;
      case LAST:
        final SMGDoublyLinkedList dll = (SMGDoublyLinkedList) pTarget;
        nf = dll.getPfo();
        hfo = dll.getHfo();
        nfo = dll.getPfo();
        pfo = nf;
        length = dll.getMinimumLength();
        break;
      default:
        return Pair.of(false, true);
    }

    Iterable<SMGEdgeHasValue> npHves =
        newInputSMG2.getHVEdges(
            SMGEdgeHasValueFilter.objectFilter(pTarget)
                .filterAtOffset(nf)
                .filterBySize(newInputSMG2.getSizeofPtrInBits()));

    SMGValue nextPointer;

    if (!npHves.iterator().hasNext()) {
      // nullified block, but lacks edge
      nextPointer = SMGZeroValue.INSTANCE;
    } else {
      nextPointer = Iterables.getOnlyElement(npHves).getValue();
    }

    if (mapping2.containsKey(pTarget)) {
      SMGObject jointList = mapping2.get(pTarget);
      if (mapping1.containsValue(jointList)) {
        return Pair.of(false, true);
      }

      if (!mapping2.containsKey(pointer2)) {

        SMGValue resultPointer = SMGKnownSymValue.of();
        SMGEdgePointsTo newJointPtEdge =
            new SMGEdgePointsTo(
                resultPointer, jointList, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
        newDestSMG.addValue(resultPointer);
        newDestSMG.addPointsToEdge(newJointPtEdge);

        mapping2.map(pointer2, resultPointer);
      } else {
        value = mapping2.get(pointer2);
        defined = true;
        inputSMG1 = newInputSMG1;
        inputSMG2 = newInputSMG2;
        destSMG = newDestSMG;
        status = newStatus;
        return Pair.of(true, true);
      }

      SMGJoinValues jv =
          new SMGJoinValues(
              newStatus,
              newInputSMG1,
              newInputSMG2,
              newDestSMG,
              mapping1,
              mapping2,
              pLevelMap,
              pointer1,
              nextPointer,
              ldiff,
              identicalInputSmg,
              level1,
              level2,
              pPrevDestLevel,
              smgState1,
              smgState2);

      if (jv.isDefined()) {

        newStatus = jv.getStatus();
        newInputSMG1 = jv.getInputSMG1();
        newInputSMG2 = jv.getInputSMG2();
        newDestSMG = jv.getDestinationSMG();
      } else {
        return Pair.of(false, false);
      }
    }

    // TODO v1 == v2 Identical in conditions??
    if (mapping2.containsKey(nextPointer)
        && mapping1.containsKey(pointer1)
        && !mapping1.get(pointer1).equals(mapping2.get(nextPointer))) {
      return Pair.of(false, true);
    }

    SMGJoinStatus newJoinStatus =
        length == 0 ? SMGJoinStatus.RIGHT_ENTAIL : SMGJoinStatus.INCOMPARABLE;

    newStatus = newStatus.updateWith(newJoinStatus);

    int levelDiff = pPrevDestLevel - pTarget.getLevel();

    if (level1 > level2) {
      levelDiff = levelDiff + 1;
    }

    copyDlsSubSmgToDestSMG(pTarget, mapping2, newInputSMG2, newDestSMG, levelDiff);

    SMGObject list = mapping2.get(pTarget);

    SMGValue resultPointer = null;

    Set<SMGEdgePointsTo> edges =
        pDestSMG.getPtEdges(
            SMGEdgePointsToFilter.targetObjectFilter(list)
                .filterAtTargetOffset(ptEdge.getOffset())
                .filterByTargetSpecifier(ptEdge.getTargetSpecifier()));

    if (!edges.isEmpty()) {
      resultPointer = Iterables.getOnlyElement(edges).getValue();
    }

    if (resultPointer == null) {
      resultPointer = SMGKnownSymValue.of();
      SMGEdgePointsTo newJointPtEdge =
          new SMGEdgePointsTo(resultPointer, list, ptEdge.getOffset(), ptEdge.getTargetSpecifier());
      newDestSMG.addValue(resultPointer);
      newDestSMG.addPointsToEdge(newJointPtEdge);
      mapping2.map(pointer2, resultPointer);
    }

    SMGJoinValues jv =
        new SMGJoinValues(
            newStatus,
            newInputSMG1,
            newInputSMG2,
            newDestSMG,
            mapping1,
            mapping2,
            pLevelMap,
            pointer1,
            nextPointer,
            ldiff,
            identicalInputSmg,
            level1,
            level2,
            pPrevDestLevel,
            smgState1,
            smgState2);

    SMGValue newAdressFromDLS;

    if (jv.isDefined()) {

      status = jv.getStatus();
      inputSMG1 = jv.getInputSMG1();
      inputSMG2 = jv.getInputSMG2();
      destSMG = jv.getDestinationSMG();
      newAdressFromDLS = jv.getValue();
      value = resultPointer;
      defined = jv.defined;

    } else {
      return Pair.of(false, false);
    }

    long nfSize = getSize(pTarget, nf, newInputSMG2);
    SMGEdgeHasValue newHve = new SMGEdgeHasValue(nfSize, nf, list, newAdressFromDLS);

    if (!pDestSMG
        .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve))
        .iterator()
        .hasNext()) {
      pDestSMG.addHasValueEdge(newHve);
    } else {
      for (SMGEdgeHasValue currentValue :
          pDestSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve))) {
        if (!currentValue.getValue().equals(newAdressFromDLS)) {
          return Pair.of(false, false);
        }
      }
    }

    if (smgState2.getAddress(pTarget, hfo, SMGTargetSpecifier.FIRST) == null) {
      long nfSize2 = getSize(pTarget, nfo, newInputSMG2);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfSize2, nfo, list, newAdressFromDLS);
      if (!pDestSMG
          .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve2))
          .iterator()
          .hasNext()) {
        pDestSMG.addHasValueEdge(newHve2);
      }
    }

    if (pTarget.getKind() == SMGObjectKind.DLL
        && smgState2.getAddress(pTarget, hfo, SMGTargetSpecifier.LAST) == null) {
      long nfSize2 = getSize(pTarget, nfo, newInputSMG2);
      SMGEdgeHasValue newHve2 = new SMGEdgeHasValue(nfSize2, pfo, list, newAdressFromDLS);
      if (!pDestSMG
          .getHVEdges(SMGEdgeHasValueFilter.objectFilter(list).overlapsWith(newHve2))
          .iterator()
          .hasNext()) {
        pDestSMG.addHasValueEdge(newHve2);
      }
    }

    return Pair.of(true, true);
  }

  private void copyDlsSubSmgToDestSMG(
      SMGObject pList,
      SMGNodeMapping pMapping,
      UnmodifiableSMG pInputSMG1,
      SMG pDestSMG,
      int pLevelDiff) {

    Set<SMGObject> toBeChecked = new HashSet<>();

    int listLevel = pList.getLevel() + pLevelDiff;

    SMGObject listCopy;
    long nfo = -1;
    long pfo = -1;

    if (pMapping.containsKey(pList)) {
      listCopy = pMapping.get(pList);
    } else {

      switch (pList.getKind()) {
        case DLL:
          {
            final SMGDoublyLinkedList dll = (SMGDoublyLinkedList) pList;
            nfo = dll.getNfo();
            pfo = dll.getPfo();
            long hfo = dll.getHfo();
            listCopy = new SMGDoublyLinkedList(pList.getSize(), hfo, nfo, pfo, 0, listLevel);
            break;
          }
        case SLL:
          {
            final SMGSingleLinkedList sll = (SMGSingleLinkedList) pList;
            nfo = sll.getNfo();
            long hfo = sll.getHfo();
            listCopy = new SMGSingleLinkedList(pList.getSize(), hfo, nfo, 0, listLevel);
            break;
          }
        default:
          throw new AssertionError();
      }

      pMapping.map(pList, listCopy);
      ((CLangSMG) pDestSMG).addHeapObject(listCopy);
    }

    for (SMGEdgeHasValue hve : pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pList))) {

      if (hve.getOffset() != pfo && hve.getOffset() != nfo) {

        SMGValue subDlsValue = hve.getValue();
        SMGValue newVal = subDlsValue;

        if (pInputSMG1.isPointer(subDlsValue)) {
          SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSMG1.getPointer(subDlsValue);
          SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
          int level = reachedObjectSubSmg.getLevel();

          if (!newVal.isZero()) {

            SMGObject copyOfReachedObject;

            if (!pMapping.containsKey(reachedObjectSubSmg)) {

              int newLevel = level + pLevelDiff;

              copyOfReachedObject = reachedObjectSubSmg.copy(newLevel);
              pMapping.map(reachedObjectSubSmg, copyOfReachedObject);
              ((CLangSMG) pDestSMG).addHeapObject(copyOfReachedObject);
              pDestSMG.setValidity(
                  copyOfReachedObject, pInputSMG1.isObjectValid(reachedObjectSubSmg));
              toBeChecked.add(reachedObjectSubSmg);
            } else {
              copyOfReachedObject = pMapping.get(reachedObjectSubSmg);
            }

            if (pMapping.containsKey(subDlsValue)) {
              newVal = pMapping.get(subDlsValue);
            } else {
              newVal = SMGKnownSymValue.of();
              pDestSMG.addValue(newVal);
              pMapping.map(subDlsValue, newVal);

              SMGTargetSpecifier newTg;
              if (copyOfReachedObject instanceof SMGRegion) {
                newTg = SMGTargetSpecifier.REGION;
              } else {
                newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
              }

              SMGEdgePointsTo newPtEdge =
                  new SMGEdgePointsTo(
                      newVal, copyOfReachedObject, reachedObjectSubSmgPTEdge.getOffset(), newTg);
              pDestSMG.addPointsToEdge(newPtEdge);
            }
          }
        }

        SMGEdgeHasValue newEdge =
            new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), listCopy, newVal);
        if (!pDestSMG
            .getHVEdges(SMGEdgeHasValueFilter.objectFilter(listCopy).overlapsWith(newEdge))
            .iterator()
            .hasNext()) {
          if (!pDestSMG.getValues().contains(newVal)) {
            pDestSMG.addValue(newVal);
          }
          if (!pMapping.containsKey(subDlsValue)) {
            pMapping.map(subDlsValue, newVal);
          }
          pDestSMG.addHasValueEdge(newEdge);
        }
      }
    }

    Set<SMGObject> toCheck = new HashSet<>();

    while (!toBeChecked.isEmpty()) {
      toCheck.clear();
      toCheck.addAll(toBeChecked);
      toBeChecked.clear();

      for (SMGObject objToCheck : toCheck) {
        copyObjectAndNodesIntoDestSMG(
            objToCheck, toBeChecked, pMapping, pInputSMG1, pDestSMG, pLevelDiff);
      }
    }
  }

  private void copyObjectAndNodesIntoDestSMG(
      SMGObject pObjToCheck,
      Set<SMGObject> pToBeChecked,
      SMGNodeMapping pMapping,
      UnmodifiableSMG pInputSMG1,
      SMG pDestSMG,
      int pLevelDiff) {

    SMGObject newObj = pMapping.get(pObjToCheck);

    for (SMGEdgeHasValue hve :
        pInputSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObjToCheck))) {

      SMGValue subDlsValue = hve.getValue();
      SMGValue newVal = subDlsValue;

      if (pInputSMG1.isPointer(subDlsValue)) {
        SMGEdgePointsTo reachedObjectSubSmgPTEdge = pInputSMG1.getPointer(subDlsValue);
        SMGObject reachedObjectSubSmg = reachedObjectSubSmgPTEdge.getObject();
        int level = reachedObjectSubSmg.getLevel();

        if (!newVal.isZero()) {

          SMGObject copyOfReachedObject;

          if (!pMapping.containsKey(reachedObjectSubSmg)) {

            int newLevel = level + pLevelDiff;

            copyOfReachedObject = reachedObjectSubSmg.copy(newLevel);
            // Avoid adding null object
            if (!copyOfReachedObject.equals(SMGNullObject.INSTANCE)) {
              pMapping.map(reachedObjectSubSmg, copyOfReachedObject);
              ((CLangSMG) pDestSMG).addHeapObject(copyOfReachedObject);
              pDestSMG.setValidity(
                  copyOfReachedObject, pInputSMG1.isObjectValid(reachedObjectSubSmg));
            }
            pToBeChecked.add(reachedObjectSubSmg);
          } else {
            copyOfReachedObject = pMapping.get(reachedObjectSubSmg);
          }

          if (pMapping.containsKey(subDlsValue)) {
            newVal = pMapping.get(subDlsValue);
          } else {
            newVal = SMGKnownSymValue.of();
            pDestSMG.addValue(newVal);
            pMapping.map(subDlsValue, newVal);

            SMGTargetSpecifier newTg;

            if (copyOfReachedObject instanceof SMGRegion) {
              newTg = SMGTargetSpecifier.REGION;
            } else {
              newTg = reachedObjectSubSmgPTEdge.getTargetSpecifier();
            }

            SMGEdgePointsTo newPtEdge =
                new SMGEdgePointsTo(
                    newVal, copyOfReachedObject, reachedObjectSubSmgPTEdge.getOffset(), newTg);
            pDestSMG.addPointsToEdge(newPtEdge);
          }
        }
      }

      if (!pDestSMG
          .getHVEdges(
              SMGEdgeHasValueFilter.objectFilter(newObj)
                  .filterAtOffset(hve.getOffset())
                  .filterWithoutSize())
          .iterator()
          .hasNext()) {
        if (!pDestSMG.getValues().contains(newVal)) {
          pDestSMG.addValue(newVal);
        }
        if (!pMapping.containsKey(subDlsValue)) {
          pMapping.map(subDlsValue, newVal);
        }
        pDestSMG.addHasValueEdge(
            new SMGEdgeHasValue(hve.getSizeInBits(), hve.getOffset(), newObj, newVal));
      }
    }
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public UnmodifiableSMG getInputSMG1() {
    return inputSMG1;
  }

  public UnmodifiableSMG getInputSMG2() {
    return inputSMG2;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMGValue getValue() {
    return value;
  }

  public boolean isDefined() {
    return defined;
  }

  /**
   * Signifies, if the part of the sub-smg rooted at the given value can possibly be joined through
   * abstraction.
   *
   * @return true, if join is defined, or join through abstraction may be a possibility, false
   *     otherwise.
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

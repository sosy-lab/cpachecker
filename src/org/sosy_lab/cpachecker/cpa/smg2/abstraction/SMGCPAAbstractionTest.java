// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPATest0;
import org.sosy_lab.cpachecker.cpa.smg2.SMGMergeOperator;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager.SMGCandidate;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/*
 * Tests (list) abstraction and materialization.
 */
public class SMGCPAAbstractionTest extends SMGCPATest0 {

  private static final CType LIST_POINTER_TYPE = CPointerType.POINTER_TO_VOID;

  /*
  TODO:
   * Implements the following example (Ending the list means value 0):
   * variable
   *     |
   *    [concrete] -> [2+]           [concrete] -> [2+]
   *     |             |    merge      |             |
   *    [2+]          [2+]           [concrete]     [2+]
   *
   * Expected result:
   *    [concrete] -> [2+]
   *     |             |
   *    [1+]          [2+]
   */

  /*
   * A nested SLL (top element 64 bits, nested 32) w variables pointing to the top and last
   * nested element.
   * Example (Ending the list means value 0):
   * variable
   *     |                          |
   *    [concrete] -> 0         [concrete] -> 0
   *     |                          |
   *    [2+]         merge      [concrete]
   *     |                          |
   * -> [concrete]           -> [concrete]
   *
   * Expected result:
   *     |
   *    [concrete] -> 0
   *     |
   *    [1+]
   *     |
   * -> [concrete]
   * By inserting a 0+ in the right list.
   */
  @Test
  public void simpleMergeTest() throws CPAException, InterruptedException {
    SMGState stateLeft = getFreshState();
    SMGState stateRight = getFreshState();
    NumericValue zero = new NumericValue(BigInteger.ZERO);
    NumericValue thirtyTwo = new NumericValue(BigInteger.valueOf(32));
    NumericValue sixtyFour = new NumericValue(BigInteger.valueOf(64));
    String variableName = "topList";
    String variableName2 = "nestedList";
    CType pointerType = CPointerType.POINTER_TO_VOID;

    // Init left with nested abstraction
    stateLeft = stateLeft.copyAndAddLocalVariable(thirtyTwo, variableName, pointerType);
    ValueAndSMGState addressAndState =
        evaluator.createHeapMemoryAndPointer(stateLeft, sixtyFour, pointerType);
    Value addressToNewRegion = addressAndState.getValue();
    stateLeft = addressAndState.getState();

    SMGObject variableMemory =
        stateLeft.getMemoryModel().getStackFrames().peek().getVariable(variableName);

    stateLeft =
        stateLeft.writeValueWithChecks(
            variableMemory, zero, thirtyTwo, addressToNewRegion, pointerType, null);
    ValueAndSMGState ptrToNested2PlusAndState =
        createSLLAndReturnPointer(stateLeft, 32, 2, 0, 0, 0);
    stateLeft = ptrToNested2PlusAndState.getState();
    // Nested below first, concrete, list elem
    Value ptrToNested2Plus = ptrToNested2PlusAndState.getValue();

    stateLeft =
        stateLeft
            .writeValueTo(
                addressToNewRegion, BigInteger.ZERO, thirtyTwo, zero, CNumericTypes.INT, null)
            .get(0);
    stateLeft =
        stateLeft
            .writeValueTo(
                addressToNewRegion,
                BigInteger.valueOf(32),
                thirtyTwo,
                ptrToNested2Plus,
                pointerType,
                null)
            .get(0);
    // Now put target of abstracted region to concrete that is also pointed to by a stack var
    stateLeft = stateLeft.copyAndAddLocalVariable(thirtyTwo, variableName2, pointerType);
    ValueAndSMGState addressAndStateVar2 =
        evaluator.createHeapMemoryAndPointer(stateLeft, thirtyTwo, pointerType);
    Value addressToNewNestedRegion = addressAndStateVar2.getValue();
    stateLeft = addressAndStateVar2.getState();

    SMGStateAndOptionalSMGObjectAndOffset newNestedRegionAndState =
        stateLeft.dereferencePointerWithoutMaterilization(addressToNewNestedRegion).orElseThrow();
    SMGObject nestedConcreteEndList = newNestedRegionAndState.getSMGObject();
    stateLeft = newNestedRegionAndState.getSMGState();
    stateLeft =
        stateLeft.writeValueWithChecks(
            nestedConcreteEndList, zero, thirtyTwo, zero, pointerType, null);

    SMGObject variableMemory2 =
        stateLeft.getMemoryModel().getStackFrames().peek().getVariable(variableName2);

    stateLeft =
        stateLeft.writeValueWithChecks(
            variableMemory2, zero, thirtyTwo, addressToNewNestedRegion, pointerType, null);

    SMGStateAndOptionalSMGObjectAndOffset nested2PlusAndState =
        stateLeft.dereferencePointerWithoutMaterilization(ptrToNested2Plus).orElseThrow();
    SMGObject nested2Plus = nested2PlusAndState.getSMGObject();
    stateLeft = nested2PlusAndState.getSMGState();
    stateLeft =
        stateLeft.writeValueWithChecks(
            nested2Plus, zero, thirtyTwo, addressToNewNestedRegion, pointerType, null);

    // Init right
    stateRight = stateRight.copyAndAddLocalVariable(thirtyTwo, variableName, pointerType);
    ValueAndSMGState addressFirstConcreteAndStateRight =
        evaluator.createHeapMemoryAndPointer(stateRight, sixtyFour, pointerType);
    Value addressToFirstConcreteRegionRight = addressFirstConcreteAndStateRight.getValue();
    stateRight = addressFirstConcreteAndStateRight.getState();

    SMGObject variableMemoryRight =
        stateRight.getMemoryModel().getStackFrames().peek().getVariable(variableName);
    stateRight =
        stateRight.writeValueWithChecks(
            variableMemoryRight,
            zero,
            thirtyTwo,
            addressToFirstConcreteRegionRight,
            pointerType,
            null);

    ValueAndSMGState addressNestedConcreteAndStateRight =
        evaluator.createHeapMemoryAndPointer(stateRight, thirtyTwo, pointerType);
    stateRight = addressNestedConcreteAndStateRight.getState();
    // Nested below first, concrete, list elem
    Value ptrToNestedRight = addressNestedConcreteAndStateRight.getValue();
    stateRight =
        stateRight
            .writeValueTo(
                addressToFirstConcreteRegionRight,
                BigInteger.ZERO,
                thirtyTwo,
                zero,
                CNumericTypes.INT,
                null)
            .get(0);
    stateRight =
        stateRight
            .writeValueTo(
                addressToFirstConcreteRegionRight,
                BigInteger.valueOf(32),
                thirtyTwo,
                ptrToNestedRight,
                pointerType,
                null)
            .get(0);
    // Now put target of abstracted region to concrete that is also pointed to by a stack var
    stateRight = stateRight.copyAndAddLocalVariable(thirtyTwo, variableName2, pointerType);
    ValueAndSMGState addressAndStateRightVar2 =
        evaluator.createHeapMemoryAndPointer(stateRight, thirtyTwo, pointerType);
    Value addressToSecondNestedRegion = addressAndStateRightVar2.getValue();
    stateRight = addressAndStateRightVar2.getState();

    SMGStateAndOptionalSMGObjectAndOffset lastNestedRegionAndStateRight =
        stateRight
            .dereferencePointerWithoutMaterilization(addressToSecondNestedRegion)
            .orElseThrow();
    SMGObject lastNestedRegionRight = lastNestedRegionAndStateRight.getSMGObject();
    stateRight = lastNestedRegionAndStateRight.getSMGState();
    stateRight =
        stateRight.writeValueWithChecks(
            lastNestedRegionRight, zero, thirtyTwo, zero, pointerType, null);

    SMGObject variableMemoryRight2 =
        stateRight.getMemoryModel().getStackFrames().peek().getVariable(variableName2);

    stateRight =
        stateRight.writeValueWithChecks(
            variableMemoryRight2, zero, thirtyTwo, addressToSecondNestedRegion, pointerType, null);

    stateRight =
        stateRight
            .writeValueTo(
                ptrToNestedRight,
                BigInteger.ZERO,
                thirtyTwo,
                addressToSecondNestedRegion,
                pointerType,
                null)
            .get(0);

    // Now merge
    SMGMergeOperator mergeOp = new SMGMergeOperator(new SMGCPAStatistics());
    SMGState mergedState = (SMGState) mergeOp.merge(stateLeft, stateRight, null);
    assertThat(mergedState).isNotEqualTo(stateLeft);
    // merge returns right for failing
    assertThat(mergedState).isNotEqualTo(stateRight);
  }

  private ValueAndSMGState createSLLAndReturnPointer(
      SMGState state,
      int sizeOfObject,
      int length,
      int nestingLevel,
      int headOffset,
      int nextOffset) {
    CType pointerType = CPointerType.POINTER_TO_VOID;
    SMGObject newObject =
        new SMGSinglyLinkedListSegment(
            nestingLevel,
            new NumericValue(BigInteger.valueOf(sizeOfObject)),
            BigInteger.ZERO,
            BigInteger.valueOf(headOffset),
            BigInteger.valueOf(nextOffset),
            BigInteger.ZERO,
            length);
    state = state.copyAndReplaceMemoryModel(state.getMemoryModel().copyAndAddHeapObject(newObject));
    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    // New regions always have offset 0
    return ValueAndSMGState.of(
        addressValue,
        state.createAndAddPointer(
            addressValue,
            newObject,
            pointerType,
            new NumericValue(BigInteger.ZERO),
            0,
            SMGTargetSpecifier.IS_FIRST_POINTER));
  }

  // test list specifier after normal abstraction
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void listAbstractionSpecifierSLLTest() throws SMGException, SMGSolverException {
    int listLength = 15;
    Value[] pointers = buildConcreteList(false, sllSize, listLength);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Check specifier for abstracted list
    for (int i = 0; i < listLength; i++) {
      Value ptr = pointers[i];
      Optional<SMGValue> smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      if (i == 0) {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      } else if (i == listLength - 1) {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      } else {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
      }
    }
    // Now materialize, check specifier, abstract again and check again
    SMGObject previousMemory = null;
    for (int i = 0; i < listLength; i++) {
      Value ptr;
      if (i == listLength - 1) {
        // The ptr from pointers is the last pointer that behaves differently
        ptr =
            currentState
                .readValueWithoutMaterialization(previousMemory, nfo, pointerSizeInBits, null)
                .getValue();
        SMGPointsToEdge nextPtrEdge =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(ptr).orElseThrow())
                .orElseThrow();
        SMGPointsToEdge lastPtrEdge =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(pointers[i]).orElseThrow())
                .orElseThrow();
        assertThat(nextPtrEdge).isNotEqualTo(lastPtrEdge);
      } else {
        ptr = pointers[i];
      }

      Optional<SMGValue> smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();

      assertThat(ptEdge.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

      List<SMGStateAndOptionalSMGObjectAndOffset> materializedMemoryList =
          currentState.dereferencePointer(ptr);

      SMGStateAndOptionalSMGObjectAndOffset materializedMemory;
      assertThat(materializedMemoryList).hasSize(1);
      assertThat(i).isLessThan(listLength);
      materializedMemory = materializedMemoryList.get(0);
      assertThat(materializedMemory.hasSMGObjectAndOffset()).isTrue();
      currentState = materializedMemory.getSMGState();
      previousMemory = materializedMemory.getSMGObject();
      smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      ptEdge = currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);

      if (i == listLength - 1) {
        // Force extension of list by reading the next ptr
        List<ValueAndSMGState> nextPtrs =
            currentState.readValue(materializedMemory.getSMGObject(), nfo, pointerSizeInBits, null);
        assertThat(nextPtrs).hasSize(2);
        // Check correct specifier for extended list
        // first is non-extended
        Value endingListPtr = nextPtrs.get(0).getValue();
        SMGState endingState = nextPtrs.get(0).getState();
        SMGValue endingLstSMGPtr =
            endingState.getMemoryModel().getSMGValueFromValue(endingListPtr).orElseThrow();
        assertThat(endingLstSMGPtr.isZero()).isTrue();
        // Check that the last ptr truly points to the last concrete element
        SMGValue lastSMGPtr =
            endingState
                .getMemoryModel()
                .getSMGValueFromValue(pointers[listLength - 1])
                .orElseThrow();
        assertThat(lastSMGPtr.isZero()).isFalse();
        SMGPointsToEdge lastPTE =
            endingState.getMemoryModel().getSmg().getPTEdge(lastSMGPtr).orElseThrow();
        assertThat(lastPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(lastPTE.pointsTo()).isEqualTo(materializedMemory.getSMGObject());

        // second is extended
        Value extendedListPtr = nextPtrs.get(1).getValue();
        SMGState extendedState = nextPtrs.get(1).getState();
        SMGValue extendedLstSMGPtr =
            extendedState.getMemoryModel().getSMGValueFromValue(extendedListPtr).orElseThrow();
        SMGPointsToEdge extendedPTE =
            extendedState.getMemoryModel().getSmg().getPTEdge(extendedLstSMGPtr).orElseThrow();
        // Points to a new object
        assertThat(extendedPTE.pointsTo()).isNotEqualTo(materializedMemory.getSMGObject());
        assertThat(extendedPTE.pointsTo()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(extendedPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        // Read the nfo and check that ptr is "next" and points to the same as last
        ValueAndSMGState nextPtrAndState =
            extendedState.readValueWithoutMaterialization(
                extendedPTE.pointsTo(), nfo, pointerSizeInBits, null);
        extendedState = nextPtrAndState.getState();
        Value nextValue = nextPtrAndState.getValue();
        SMGValue nextSMGValue =
            extendedState.getMemoryModel().getSMGValueFromValue(nextValue).orElseThrow();
        SMGPointsToEdge nextPTE =
            extendedState.getMemoryModel().getSmg().getPTEdge(nextSMGValue).orElseThrow();
        assertThat(nextPTE.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(nextPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

        // Check that the last ptr truly points to the last abstracted element
        lastSMGPtr =
            extendedState
                .getMemoryModel()
                .getSMGValueFromValue(pointers[listLength - 1])
                .orElseThrow();
        assertThat(lastSMGPtr.isZero()).isFalse();
        lastPTE = extendedState.getMemoryModel().getSmg().getPTEdge(lastSMGPtr).orElseThrow();
        assertThat(lastPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        assertThat(lastPTE.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(((SMGSinglyLinkedListSegment) lastPTE.pointsTo()).getMinLength()).isEqualTo(0);
        assertThat(lastPTE.pointsTo()).isEqualTo(nextPTE.pointsTo());
      }
    }
  }

  // test list specifier after normal abstraction
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void listAbstractionSpecifierDLLTest() throws SMGException, SMGSolverException {
    int listLength = 15;
    Value[] pointers = buildConcreteList(true, dllSize, listLength);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Check specifier for abstracted list
    for (int i = 0; i < listLength; i++) {
      Value ptr = pointers[i];
      Optional<SMGValue> smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      if (i == 0) {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      } else if (i == listLength - 1) {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      } else {
        assertThat(ptEdge.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
      }
    }
    // Now materialize, check specifier, abstract again and check again
    SMGObject previousMemory = null;
    for (int i = 0; i < listLength; i++) {
      Value ptr;
      if (i == listLength - 1) {
        // The ptr from pointers is the last pointer that behaves differently
        ptr =
            currentState
                .readValueWithoutMaterialization(previousMemory, nfo, pointerSizeInBits, null)
                .getValue();
        SMGPointsToEdge nextPtrEdge =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(ptr).orElseThrow())
                .orElseThrow();
        SMGPointsToEdge lastPtrEdge =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(pointers[i]).orElseThrow())
                .orElseThrow();
        assertThat(nextPtrEdge).isNotEqualTo(lastPtrEdge);
      } else {
        ptr = pointers[i];
      }

      Optional<SMGValue> smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

      List<SMGStateAndOptionalSMGObjectAndOffset> materializedMemoryList =
          currentState.dereferencePointer(ptr);

      SMGStateAndOptionalSMGObjectAndOffset materializedMemory;
      assertThat(materializedMemoryList).hasSize(1);
      assertThat(i).isLessThan(listLength);
      materializedMemory = materializedMemoryList.get(0);
      assertThat(materializedMemory.hasSMGObjectAndOffset()).isTrue();
      currentState = materializedMemory.getSMGState();
      previousMemory = materializedMemory.getSMGObject();
      smgValuePtr = currentState.getMemoryModel().getSMGValueFromValue(ptr);
      assertThat(smgValuePtr).isPresent();
      ptEdge = currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);

      if (i == listLength - 1) {
        // Force extension of list by reading the next ptr
        List<ValueAndSMGState> nextPtrs =
            currentState.readValue(materializedMemory.getSMGObject(), nfo, pointerSizeInBits, null);
        assertThat(nextPtrs).hasSize(2);
        // Check correct specifier for extended list
        // first is non-extended
        Value endingListPtr = nextPtrs.get(0).getValue();
        SMGState endingState = nextPtrs.get(0).getState();
        SMGValue endingLstSMGPtr =
            endingState.getMemoryModel().getSMGValueFromValue(endingListPtr).orElseThrow();
        assertThat(endingLstSMGPtr.isZero()).isTrue();
        // Check that the last ptr truly points to the last concrete element
        SMGValue lastSMGPtr =
            endingState
                .getMemoryModel()
                .getSMGValueFromValue(pointers[listLength - 1])
                .orElseThrow();
        assertThat(lastSMGPtr.isZero()).isFalse();
        SMGPointsToEdge lastPTE =
            endingState.getMemoryModel().getSmg().getPTEdge(lastSMGPtr).orElseThrow();
        assertThat(lastPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(lastPTE.pointsTo()).isEqualTo(materializedMemory.getSMGObject());

        // second is extended
        Value extendedListPtr = nextPtrs.get(1).getValue();
        SMGState extendedState = nextPtrs.get(1).getState();
        SMGValue extendedLstSMGPtr =
            extendedState.getMemoryModel().getSMGValueFromValue(extendedListPtr).orElseThrow();
        SMGPointsToEdge extendedPTE =
            extendedState.getMemoryModel().getSmg().getPTEdge(extendedLstSMGPtr).orElseThrow();
        // Points to a new object
        assertThat(extendedPTE.pointsTo()).isNotEqualTo(materializedMemory.getSMGObject());
        assertThat(extendedPTE.pointsTo()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(extendedPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        // Read the nfo and check that ptr is "next" and points to the same as last
        ValueAndSMGState nextPtrAndState =
            extendedState.readValueWithoutMaterialization(
                extendedPTE.pointsTo(), nfo, pointerSizeInBits, null);
        extendedState = nextPtrAndState.getState();
        Value nextValue = nextPtrAndState.getValue();
        SMGValue nextSMGValue =
            extendedState.getMemoryModel().getSMGValueFromValue(nextValue).orElseThrow();
        SMGPointsToEdge nextPTE =
            extendedState.getMemoryModel().getSmg().getPTEdge(nextSMGValue).orElseThrow();
        assertThat(nextPTE.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(nextPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

        // Check that the last ptr truly points to the last abstracted element
        lastSMGPtr =
            extendedState
                .getMemoryModel()
                .getSMGValueFromValue(pointers[listLength - 1])
                .orElseThrow();
        assertThat(lastSMGPtr.isZero()).isFalse();
        lastPTE = extendedState.getMemoryModel().getSmg().getPTEdge(lastSMGPtr).orElseThrow();
        assertThat(lastPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        assertThat(lastPTE.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(((SMGSinglyLinkedListSegment) lastPTE.pointsTo()).getMinLength()).isEqualTo(0);
        assertThat(lastPTE.pointsTo()).isEqualTo(nextPTE.pointsTo());
      }
    }
  }

  // test list specifier with a concrete element in the middle of 2 abstractions
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void listSpecifierConcreteInBetweenTwoAbstractedSLLTest()
      throws SMGException, SMGSolverException {

    int listLength = 10;
    Value[] pointersFirstHalf = buildConcreteList(false, sllSize, listLength);
    // Distinct middle segment
    Value[] pointersConcrete =
        buildConcreteListWithEqualValues(
            false, sllSize, 1, 111, BigInteger.ZERO, Optional.empty(), true);
    Value[] pointersSecondHalf = buildConcreteList(false, sllSize, listLength);
    // Now combine to 1 list
    assertThat(pointersConcrete).hasLength(1);
    SMGStateAndOptionalSMGObjectAndOffset lstFirstHalf =
        currentState
            .dereferencePointerWithoutMaterilization(pointersFirstHalf[listLength - 1])
            .orElseThrow();
    currentState = lstFirstHalf.getSMGState();
    SMGStateAndOptionalSMGObjectAndOffset concreteMiddle =
        currentState.dereferencePointerWithoutMaterilization(pointersConcrete[0]).orElseThrow();
    currentState = concreteMiddle.getSMGState();
    SMGStateAndOptionalSMGObjectAndOffset fstSecondHalf =
        currentState.dereferencePointerWithoutMaterilization(pointersSecondHalf[0]).orElseThrow();
    currentState = fstSecondHalf.getSMGState();
    SMGObject lastFirstObj = lstFirstHalf.getSMGObject();
    SMGObject middleObj = concreteMiddle.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            lastFirstObj,
            nfo,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(pointersConcrete[0]).orElseThrow());
    currentState =
        currentState.writeValueWithoutChecks(
            middleObj,
            nfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(pointersSecondHalf[0])
                .orElseThrow());

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    // Check correct ptr specifier of now abstracted list
    for (int i = 0; i < listLength * 2 + 1; i++) {
      Value ptrToCheck;
      SMGTargetSpecifier expectedSpecifier;
      if (i < listLength) {
        // first half
        ptrToCheck = pointersFirstHalf[i];
      } else if (i == listLength) {
        // Middle Obj
        ptrToCheck = pointersConcrete[0];
      } else {
        // second half
        ptrToCheck = pointersSecondHalf[i - listLength - 1];
      }
      if (i == 0 || i == listLength + 1) {
        expectedSpecifier = SMGTargetSpecifier.IS_FIRST_POINTER;
      } else if (i == listLength - 1 || i == listLength * 2) {
        expectedSpecifier = SMGTargetSpecifier.IS_LAST_POINTER;
      } else if (i == listLength) {
        expectedSpecifier = SMGTargetSpecifier.IS_REGION;
      } else {
        expectedSpecifier = SMGTargetSpecifier.IS_ALL_POINTER;
      }

      Optional<SMGValue> smgValuePtr =
          currentState.getMemoryModel().getSMGValueFromValue(ptrToCheck);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(expectedSpecifier);
    }

    // Change the middle value to the same as the list ones, abstract again and check specifier
    currentState =
        currentState.writeValueWithoutChecks(
            middleObj, hfo, pointerSizeInBits, SMGValue.zeroValue());
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    for (int i = 0; i < listLength * 2 + 1; i++) {
      Value ptrToCheck;
      SMGTargetSpecifier expectedSpecifier = SMGTargetSpecifier.IS_ALL_POINTER;
      if (i < listLength) {
        // first half
        ptrToCheck = pointersFirstHalf[i];
      } else if (i == listLength) {
        // Middle Obj
        ptrToCheck = pointersConcrete[0];
      } else {
        // second half
        ptrToCheck = pointersSecondHalf[i - listLength - 1];
      }
      if (i == 0) {
        expectedSpecifier = SMGTargetSpecifier.IS_FIRST_POINTER;
      } else if (i == listLength * 2) {
        expectedSpecifier = SMGTargetSpecifier.IS_LAST_POINTER;
      }

      Optional<SMGValue> smgValuePtr =
          currentState.getMemoryModel().getSMGValueFromValue(ptrToCheck);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(expectedSpecifier);
    }
  }

  // test list specifier with a concrete element in the middle of 2 abstractions
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void listSpecifierConcreteInBetweenTwoAbstractedDLLTest()
      throws SMGException, SMGSolverException {

    int listLength = 10;
    Value[] pointersFirstHalf = buildConcreteList(true, dllSize, listLength);
    // Distinct middle segment
    Value[] pointersConcrete =
        buildConcreteListWithEqualValues(
            true, dllSize, 1, 111, BigInteger.ZERO, Optional.of(BigInteger.ZERO), true);
    Value[] pointersSecondHalf = buildConcreteList(true, dllSize, listLength);
    // Now combine to 1 list
    assertThat(pointersConcrete).hasLength(1);
    SMGStateAndOptionalSMGObjectAndOffset lstFirstHalf =
        currentState
            .dereferencePointerWithoutMaterilization(pointersFirstHalf[listLength - 1])
            .orElseThrow();
    currentState = lstFirstHalf.getSMGState();
    SMGStateAndOptionalSMGObjectAndOffset concreteMiddle =
        currentState.dereferencePointerWithoutMaterilization(pointersConcrete[0]).orElseThrow();
    currentState = concreteMiddle.getSMGState();
    SMGStateAndOptionalSMGObjectAndOffset fstSecondHalf =
        currentState.dereferencePointerWithoutMaterilization(pointersSecondHalf[0]).orElseThrow();
    currentState = fstSecondHalf.getSMGState();
    SMGObject lastFirstObj = lstFirstHalf.getSMGObject();
    SMGObject middleObj = concreteMiddle.getSMGObject();
    SMGObject firstSecondHalfObj = fstSecondHalf.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            lastFirstObj,
            nfo,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(pointersConcrete[0]).orElseThrow());
    currentState =
        currentState.writeValueWithoutChecks(
            middleObj,
            nfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(pointersSecondHalf[0])
                .orElseThrow());
    // Backptr for dll
    currentState =
        currentState.writeValueWithoutChecks(
            middleObj,
            pfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(pointersFirstHalf[listLength - 1])
                .orElseThrow());
    currentState =
        currentState.writeValueWithoutChecks(
            firstSecondHalfObj,
            pfo,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(pointersConcrete[0]).orElseThrow());

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    // Check correct ptr specifier of now abstracted list
    for (int i = 0; i < listLength * 2 + 1; i++) {
      Value ptrToCheck;
      SMGTargetSpecifier expectedSpecifier;
      if (i < listLength) {
        // first half
        ptrToCheck = pointersFirstHalf[i];
      } else if (i == listLength) {
        // Middle Obj
        ptrToCheck = pointersConcrete[0];
      } else {
        // second half
        ptrToCheck = pointersSecondHalf[i - listLength - 1];
      }
      if (i == 0 || i == listLength + 1) {
        expectedSpecifier = SMGTargetSpecifier.IS_FIRST_POINTER;
      } else if (i == listLength - 1 || i == listLength * 2) {
        expectedSpecifier = SMGTargetSpecifier.IS_LAST_POINTER;
      } else if (i == listLength) {
        expectedSpecifier = SMGTargetSpecifier.IS_REGION;
      } else {
        expectedSpecifier = SMGTargetSpecifier.IS_ALL_POINTER;
      }

      Optional<SMGValue> smgValuePtr =
          currentState.getMemoryModel().getSMGValueFromValue(ptrToCheck);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(expectedSpecifier);
    }

    // Change the middle value to the same as the list ones, abstract again and check specifier
    currentState =
        currentState.writeValueWithoutChecks(
            middleObj, hfo, pointerSizeInBits, SMGValue.zeroValue());
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    for (int i = 0; i < listLength * 2 + 1; i++) {
      Value ptrToCheck;
      SMGTargetSpecifier expectedSpecifier = SMGTargetSpecifier.IS_ALL_POINTER;
      if (i < listLength) {
        // first half
        ptrToCheck = pointersFirstHalf[i];
      } else if (i == listLength) {
        // Middle Obj
        ptrToCheck = pointersConcrete[0];
      } else {
        // second half
        ptrToCheck = pointersSecondHalf[i - listLength - 1];
      }
      if (i == 0) {
        expectedSpecifier = SMGTargetSpecifier.IS_FIRST_POINTER;
      } else if (i == listLength * 2) {
        expectedSpecifier = SMGTargetSpecifier.IS_LAST_POINTER;
      }

      Optional<SMGValue> smgValuePtr =
          currentState.getMemoryModel().getSMGValueFromValue(ptrToCheck);
      assertThat(smgValuePtr).isPresent();
      Optional<SMGPointsToEdge> ptEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValuePtr.orElseThrow());
      assertThat(ptEdge).isPresent();
      if (expectedSpecifier.equals(SMGTargetSpecifier.IS_ALL_POINTER)
          && !ptEdge.orElseThrow().targetSpecifier().equals(SMGTargetSpecifier.IS_ALL_POINTER)) {
        assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(expectedSpecifier);
      }
      assertThat(ptEdge.orElseThrow().targetSpecifier()).isEqualTo(expectedSpecifier);
    }
  }

  // TODO: test list specifier after abstraction of 2 or more abstracted lists
  // TODO: test list specifier with 0+ in the beginning or end
  // TODO: test list specifier with a concrete element  and 0+s around it in the middle of 2
  // abstractions

  /**
   * Creates and tests lists that are barely not abstractable because of 1 value not being equal for
   * the given threshold.
   */
  @Test
  public void listNotAbstractableSLLTest() {
    resetSMGStateAndVisitor();
    // TODO: this and DLL version
  }

  /**
   * Creates a concrete list, then saves the start pointer to a nested list in EACH segment. Then
   * change 1 value in all the nested lists at different positions and check that no list is
   * abstractable.
   */
  @Test
  public void notEqualNestedListSLLTest() {
    resetSMGStateAndVisitor();
    // TODO: this and DLL version
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The pointers
   * saved however don't necessarily point to the beginning of the nested list! The lists are then
   * abstracted and checked. The top list is NOT supposed to be abstractable as the nested lists are
   * not equal as their subjective lengths differ for each element of the top list.
   */
  @Test
  public void nestedListMovingPointerSLLTest() {
    resetSMGStateAndVisitor();
    // TODO: this and DLL version
  }

  // DLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithPointerOffsetsAbstractionTest() throws SMGException, SMGSolverException {
    int listLength = 10;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> listPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            true,
            dllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.of(internalListPtrOffset),
            true);

    // Abstract, check that first and last are not abstracted
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    checkAbstractionOfLLWithConcreteFirstAndLast(
        true, listLength, listPtrs, otherPtrOffset, internalListPtrOffset);
  }

  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithPointerOffsetsAbstractionTest() throws SMGException, SMGSolverException {
    int listLength = 10;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> listPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            false,
            sllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.empty(),
            true);

    // Abstract, check that first and last are not abstracted
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    assertThat(
            currentState.getMemoryModel().getSmg().getObjects().stream()
                .anyMatch(
                    o ->
                        o instanceof SMGSinglyLinkedListSegment sll
                            && sll.getMinLength() == listLength))
        .isTrue();

    checkAbstractionOfLLWithConcreteFirstAndLast(
        false, listLength, listPtrs, otherPtrOffset, internalListPtrOffset);
  }

  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // Then nest the same in the top SLL with same length (abstractable for top)
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithPointerOffsetsNestedAbstractionTest() throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            false,
            sllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.empty(),
            true);

    for (int all = 0; all < listLength + 2; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              false,
              sllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.empty(),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    List<Set<SMGCandidate>> orderedListCandidatesByNesting = absFinder.getListCandidates();
    // There are 2 nesting levels, top and nested
    assertThat(orderedListCandidatesByNesting).hasSize(2);
    currentState = absFinder.findAndAbstractLists();
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
      assertThat(derefedTopElem).isPresent();
      assertThat(derefedTopElem.orElseThrow().hasSMGObjectAndOffset()).isTrue();
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      assertThat(derefedTopElem.orElseThrow().getOffsetForObject())
          .isEqualTo(new NumericValue(BigInteger.ZERO));

      ValueAndSMGState ptrInTopObjAndState =
          currentState.readValueWithoutMaterialization(
              topObj, BigInteger.ZERO, pointerSizeInBits, null);
      currentState = ptrInTopObjAndState.getState();
      Value ptrInTopObj = ptrInTopObjAndState.getValue();
      SMGValue smgPtrInTop =
          currentState.getMemoryModel().getSMGValueFromValue(ptrInTopObj).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgPtrInTop)).isTrue();
      SMGPointsToEdge ptrInTopPte =
          currentState.getMemoryModel().getSmg().getPTEdge(smgPtrInTop).orElseThrow();
      assertThat(ptrInTopPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(ptrInTopPte.getOffset()).isEqualTo(BigInteger.ZERO);
      SMGObject firstConcreteNestedObj = ptrInTopPte.pointsTo();

      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(firstConcreteNestedObj))
          .hasSize(1);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(firstConcreteNestedObj))
          .contains(topObj);

      ValueAndSMGState valueInNestedAndState =
          currentState.readValueWithoutMaterialization(
              firstConcreteNestedObj, BigInteger.ZERO, pointerSizeInBits, null);
      currentState = valueInNestedAndState.getState();
      Value valueInNested = valueInNestedAndState.getValue();
      assertThat(valueInNested.isNumericValue()).isTrue();
      assertThat(valueInNested.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);

      // This is the nfo towards the abstracted section
      ValueAndSMGState nfoPtrInNestedAndState =
          currentState.readValueWithoutMaterialization(
              firstConcreteNestedObj, nfo, pointerSizeInBits, null);
      currentState = nfoPtrInNestedAndState.getState();
      Value nfoPtrInNested = nfoPtrInNestedAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nfoPtrInNested)).isTrue();
      SMGValue smgNfoPtrInNested =
          currentState.getMemoryModel().getSMGValueFromValue(nfoPtrInNested).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgNfoPtrInNested)).isTrue();
      SMGPointsToEdge nfoPteInNested =
          currentState.getMemoryModel().getSmg().getPTEdge(smgNfoPtrInNested).orElseThrow();
      assertThat(nfoPteInNested.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      assertThat(nfoPteInNested.getOffset()).isEqualTo(otherPtrOffset);
      SMGObject abstrNestedObj = nfoPteInNested.pointsTo();
      assertThat(abstrNestedObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(abstrNestedObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) abstrNestedObj).getMinLength())
          .isEqualTo(listLength);
      assertThat(((SMGSinglyLinkedListSegment) abstrNestedObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
      assertThat(((SMGSinglyLinkedListSegment) abstrNestedObj).getNextOffset()).isEqualTo(nfo);
      assertThat(abstrNestedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(abstrNestedObj))
          .hasSize(1);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(abstrNestedObj))
          .contains(firstConcreteNestedObj);

      ValueAndSMGState nfoPtrInNestedAbstractedAndState =
          currentState.readValueWithoutMaterialization(
              abstrNestedObj, nfo, pointerSizeInBits, null);
      currentState = nfoPtrInNestedAbstractedAndState.getState();
      Value nfoPtrInAbstrNested = nfoPtrInNestedAbstractedAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nfoPtrInAbstrNested)).isTrue();
      SMGValue smgNfoPtrInAbstrNested =
          currentState.getMemoryModel().getSMGValueFromValue(nfoPtrInAbstrNested).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgNfoPtrInAbstrNested)).isTrue();
      SMGPointsToEdge nfoPteInAbstrNested =
          currentState.getMemoryModel().getSmg().getPTEdge(smgNfoPtrInAbstrNested).orElseThrow();
      assertThat(nfoPteInAbstrNested.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(nfoPteInAbstrNested.getOffset()).isEqualTo(BigInteger.ZERO);
      SMGObject lastConcreteNestedObj = nfoPteInAbstrNested.pointsTo();

      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(lastConcreteNestedObj))
          .hasSize(1);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(lastConcreteNestedObj))
          .contains(abstrNestedObj);

      ValueAndSMGState nfoPtrInLastNestedAndState =
          currentState.readValueWithoutMaterialization(
              nfoPteInAbstrNested.pointsTo(), nfo, pointerSizeInBits, null);
      currentState = nfoPtrInLastNestedAndState.getState();
      Value nfoPtrInLastNested = nfoPtrInLastNestedAndState.getValue();
      assertThat(nfoPtrInLastNested.isNumericValue()).isTrue();
      assertThat(nfoPtrInLastNested.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
    }
    // Top list is abstracted as well
    checkAbstractionOfLLWithConcreteFirstAndLast(
        false, listLength, topListPtrs, otherPtrOffset, internalListPtrOffset);
  }

  // DLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // Then nest the same in the top SLL with same length (abstractable for top)
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithPointerOffsetsNestedAbstractionTest() throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            true,
            dllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.of(internalListPtrOffset),
            true);

    for (int all = 0; all < listLength + 2; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              true,
              dllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.of(internalListPtrOffset),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    List<Set<SMGCandidate>> orderedListCandidatesByNesting = absFinder.getListCandidates();
    // There are 2 nesting levels, top and nested
    assertThat(orderedListCandidatesByNesting).hasSize(2);
    currentState = absFinder.findAndAbstractLists();
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
      assertThat(derefedTopElem).isPresent();
      assertThat(derefedTopElem.orElseThrow().hasSMGObjectAndOffset()).isTrue();
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      assertThat(derefedTopElem.orElseThrow().getOffsetForObject())
          .isEqualTo(new NumericValue(BigInteger.ZERO));

      ValueAndSMGState ptrInTopObjAndState =
          currentState.readValueWithoutMaterialization(
              topObj, BigInteger.ZERO, pointerSizeInBits, null);
      currentState = ptrInTopObjAndState.getState();
      Value ptrInTopObj = ptrInTopObjAndState.getValue();
      SMGValue smgPtrInTop =
          currentState.getMemoryModel().getSMGValueFromValue(ptrInTopObj).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgPtrInTop)).isTrue();
      SMGPointsToEdge ptrInTopPte =
          currentState.getMemoryModel().getSmg().getPTEdge(smgPtrInTop).orElseThrow();
      assertThat(ptrInTopPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(ptrInTopPte.getOffset()).isEqualTo(BigInteger.ZERO);
      SMGObject firstConcreteNestedObj = ptrInTopPte.pointsTo();
      // 2 because of dll and top ptr
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(firstConcreteNestedObj))
          .hasSize(2);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(firstConcreteNestedObj))
          .contains(topObj);

      ValueAndSMGState valueInNestedAndState =
          currentState.readValueWithoutMaterialization(
              firstConcreteNestedObj, BigInteger.ZERO, pointerSizeInBits, null);
      currentState = valueInNestedAndState.getState();
      Value valueInNested = valueInNestedAndState.getValue();
      assertThat(valueInNested.isNumericValue()).isTrue();
      assertThat(valueInNested.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);

      // This is the nfo towards the abstracted section
      ValueAndSMGState nfoPtrInNestedAndState =
          currentState.readValueWithoutMaterialization(
              firstConcreteNestedObj, nfo, pointerSizeInBits, null);
      currentState = nfoPtrInNestedAndState.getState();
      Value nfoPtrInNested = nfoPtrInNestedAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nfoPtrInNested)).isTrue();
      SMGValue smgNfoPtrInNested =
          currentState.getMemoryModel().getSMGValueFromValue(nfoPtrInNested).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgNfoPtrInNested)).isTrue();
      SMGPointsToEdge nfoPteInNested =
          currentState.getMemoryModel().getSmg().getPTEdge(smgNfoPtrInNested).orElseThrow();
      assertThat(nfoPteInNested.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      assertThat(nfoPteInNested.getOffset()).isEqualTo(otherPtrOffset);
      SMGObject abstrNestedObj = nfoPteInNested.pointsTo();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(firstConcreteNestedObj))
          .contains(abstrNestedObj);
      assertThat(abstrNestedObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGDoublyLinkedListSegment) abstrNestedObj).getMinLength())
          .isEqualTo(listLength);
      assertThat(((SMGDoublyLinkedListSegment) abstrNestedObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
      assertThat(((SMGDoublyLinkedListSegment) abstrNestedObj).getNextOffset()).isEqualTo(nfo);
      assertThat(abstrNestedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      // top + prev
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(abstrNestedObj))
          .hasSize(2);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(abstrNestedObj))
          .contains(firstConcreteNestedObj);

      ValueAndSMGState nfoPtrInNestedAbstractedAndState =
          currentState.readValueWithoutMaterialization(
              abstrNestedObj, nfo, pointerSizeInBits, null);
      currentState = nfoPtrInNestedAbstractedAndState.getState();
      Value nfoPtrInAbstrNested = nfoPtrInNestedAbstractedAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nfoPtrInAbstrNested)).isTrue();
      SMGValue smgNfoPtrInAbstrNested =
          currentState.getMemoryModel().getSMGValueFromValue(nfoPtrInAbstrNested).orElseThrow();
      // Check that it points to the concrete first, that at abstracted, that at concrete that at 0
      assertThat(currentState.getMemoryModel().getSmg().isPointer(smgNfoPtrInAbstrNested)).isTrue();
      SMGPointsToEdge nfoPteInAbstrNested =
          currentState.getMemoryModel().getSmg().getPTEdge(smgNfoPtrInAbstrNested).orElseThrow();
      assertThat(nfoPteInAbstrNested.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(nfoPteInAbstrNested.getOffset()).isEqualTo(BigInteger.ZERO);
      SMGObject lastConcreteNestedObj = nfoPteInAbstrNested.pointsTo();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(abstrNestedObj))
          .contains(lastConcreteNestedObj);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(lastConcreteNestedObj))
          .hasSize(1);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getAllSourcesForPointersPointingTowards(lastConcreteNestedObj))
          .contains(abstrNestedObj);

      ValueAndSMGState nfoPtrInLastNestedAndState =
          currentState.readValueWithoutMaterialization(
              nfoPteInAbstrNested.pointsTo(), nfo, pointerSizeInBits, null);
      currentState = nfoPtrInLastNestedAndState.getState();
      Value nfoPtrInLastNested = nfoPtrInLastNestedAndState.getValue();
      assertThat(nfoPtrInLastNested.isNumericValue()).isTrue();
      assertThat(nfoPtrInLastNested.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
    }
    // Top list is abstracted as well
    checkAbstractionOfLLWithConcreteFirstAndLast(
        true, listLength, topListPtrs, otherPtrOffset, internalListPtrOffset);
  }

  /*
   * SLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithSelfPointerWithOffsetsAbstractionTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {

        Value[] topListPtrs = buildConcreteList(false, listSize, listLength, false);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr;
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits);
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Check abstraction
        Value fstPtr =
            currentState
                .readValueWithoutMaterialization(stackObj, BigInteger.ZERO, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(fstPtr)).isTrue();
        SMGPointsToEdge fstPte =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(fstPtr).orElseThrow())
                .orElseThrow();
        assertThat(fstPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
        assertThat(fstPte.getOffset()).isEqualTo(BigInteger.ZERO);
        SMGObject abstractedObj = fstPte.pointsTo();
        assertThat(abstractedObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(abstractedObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
        SMGSinglyLinkedListSegment sllObj = (SMGSinglyLinkedListSegment) abstractedObj;
        assertThat(sllObj.getOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(sllObj.getNextOffset()).isEqualTo(changingNfo);
        assertThat(sllObj.getSize().asNumericValue().bigIntegerValue()).isEqualTo(listSize);
        assertThat(sllObj.getNextPointerTargetOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(sllObj.getMinLength()).isEqualTo(listLength);

        // next check
        Value nfoPtr =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, changingNfo, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(nfoPtr)).isTrue();
        assertThat(nfoPtr.isNumericValue()).isTrue();
        assertThat(nfoPtr.asNumericValue().bigIntegerValue().intValueExact()).isEqualTo(0);
        SMGPointsToEdge nfoPte =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nfoPtr).orElseThrow())
                .orElseThrow();
        assertThat(nfoPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(nfoPte.getOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(nfoPte.pointsTo().isZero()).isTrue();

        // check the 2 self pointers
        Value selfPtr1 =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, offsetFirstSelfPtr, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(selfPtr1)).isTrue();
        assertThat(selfPtr1.isNumericValue()).isFalse();
        SMGPointsToEdge selfPtrPte1 =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(selfPtr1).orElseThrow())
                .orElseThrow();
        assertThat(selfPtrPte1.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
        assertThat(selfPtrPte1.getOffset()).isEqualTo(targetOffsetsForSelfPtrs.get(0));
        assertThat(selfPtrPte1.pointsTo()).isEqualTo(abstractedObj);

        Value selfPtr2 =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, offsetSecondSelfPtr, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(selfPtr2)).isTrue();
        assertThat(selfPtr2.isNumericValue()).isFalse();
        SMGPointsToEdge selfPtrPte2 =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(selfPtr2).orElseThrow())
                .orElseThrow();
        assertThat(selfPtrPte2.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
        assertThat(selfPtrPte2.getOffset()).isEqualTo(targetOffsetsForSelfPtrs.get(1));
        assertThat(selfPtrPte2.pointsTo()).isEqualTo(abstractedObj);
      }
    }
  }

  /*
   * DLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithSelfPointerWithOffsetsAbstractionTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] topListPtrs = buildConcreteList(true, listSize, listLength, false);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr.add(pointerSizeInBits);
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }
        if (changingPfo.equals(listSize)) {
          continue;
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
        assertThat(changingPfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          Value prevPtr =
              currentState
                  .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                  .getValue();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          if (!changingPfo.equals(pfo)) {
            // Change PFO for list
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Check abstraction
        // Get ptr of the stack obj towards the abstracted and check the general abstraction
        Value fstPtr =
            currentState
                .readValueWithoutMaterialization(stackObj, BigInteger.ZERO, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(fstPtr)).isTrue();
        SMGPointsToEdge fstPte =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(fstPtr).orElseThrow())
                .orElseThrow();
        SMGObject abstractedObj = fstPte.pointsTo();
        assertThat(abstractedObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
        assertThat(fstPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
        assertThat(fstPte.getOffset()).isEqualTo(BigInteger.ZERO);
        SMGDoublyLinkedListSegment dllObj = (SMGDoublyLinkedListSegment) abstractedObj;
        assertThat(dllObj.getOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(dllObj.getNextOffset()).isEqualTo(changingNfo);
        assertThat(dllObj.getSize().asNumericValue().bigIntegerValue()).isEqualTo(listSize);
        assertThat(dllObj.getNextPointerTargetOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(dllObj.getMinLength()).isEqualTo(listLength);
        assertThat(dllObj.getPrevOffset()).isEqualTo(changingPfo);
        assertThat(dllObj.getPrevPointerTargetOffset()).isEqualTo(BigInteger.ZERO);

        // next check
        Value nfoPtr =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, changingNfo, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(nfoPtr)).isTrue();
        assertThat(nfoPtr.isNumericValue()).isTrue();
        assertThat(nfoPtr.asNumericValue().bigIntegerValue().intValueExact()).isEqualTo(0);
        SMGPointsToEdge nfoPte =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nfoPtr).orElseThrow())
                .orElseThrow();
        assertThat(nfoPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(nfoPte.getOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(nfoPte.pointsTo().isZero()).isTrue();

        // Prev check
        Value pfoPtr =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, changingPfo, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(pfoPtr)).isTrue();
        assertThat(pfoPtr.isNumericValue()).isTrue();
        assertThat(pfoPtr.asNumericValue().bigIntegerValue().intValueExact()).isEqualTo(0);
        SMGPointsToEdge pfoPte =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nfoPtr).orElseThrow())
                .orElseThrow();
        assertThat(pfoPte.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(pfoPte.getOffset()).isEqualTo(BigInteger.ZERO);
        assertThat(pfoPte.pointsTo().isZero()).isTrue();

        // check the 2 self pointers
        Value selfPtr1 =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, offsetFirstSelfPtr, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(selfPtr1)).isTrue();
        assertThat(selfPtr1.isNumericValue()).isFalse();
        SMGPointsToEdge selfPtrPte1 =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(selfPtr1).orElseThrow())
                .orElseThrow();
        assertThat(selfPtrPte1.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
        assertThat(selfPtrPte1.getOffset()).isEqualTo(targetOffsetsForSelfPtrs.get(0));
        assertThat(selfPtrPte1.pointsTo()).isEqualTo(abstractedObj);

        Value selfPtr2 =
            currentState
                .readValueWithoutMaterialization(
                    abstractedObj, offsetSecondSelfPtr, pointerSizeInBits, null)
                .getValue();
        assertThat(currentState.getMemoryModel().isPointer(selfPtr2)).isTrue();
        assertThat(selfPtr2.isNumericValue()).isFalse();
        SMGPointsToEdge selfPtrPte2 =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(selfPtr2).orElseThrow())
                .orElseThrow();
        assertThat(selfPtrPte2.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
        assertThat(selfPtrPte2.getOffset()).isEqualTo(targetOffsetsForSelfPtrs.get(1));
        assertThat(selfPtrPte2.pointsTo()).isEqualTo(abstractedObj);
      }
    }
  }

  /*
   * SLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithSelfPointerWithOffsetsLeftMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    BigInteger firstPtrTargetOffset = pointerSizeInBits;
    BigInteger secondPtrTargetOffset = pointerSizeInBits.add(pointerSizeInBits);
    // TODO: add multiple nextPtrOffset
    BigInteger nextPtrOffset = BigInteger.ZERO;
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrTargetOffset, firstPtrTargetOffset),
            ImmutableList.of(secondPtrTargetOffset, secondPtrTargetOffset),
            ImmutableList.of(firstPtrTargetOffset, secondPtrTargetOffset),
            ImmutableList.of(secondPtrTargetOffset, firstPtrTargetOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {

        Value[] topListPtrs = buildConcreteList(false, listSize, listLength, false);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr;
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits);
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check left hand sided materialization
        // Read ptr from stack var, this mats the list once
        List<ValueAndSMGState> fstPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(fstPtrsAndStates).hasSize(1);
        Value fstPtr = fstPtrsAndStates.get(0).getValue();
        currentState = fstPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> firstObjsList =
            currentState.dereferencePointer(fstPtr);
        assertThat(firstObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset firstObjAndState = firstObjsList.get(0);
        assertThat(firstObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = firstObjAndState.getSMGState();
        SMGObject fstObj = firstObjAndState.getSMGObject();
        assertThat(fstObj.isSLL()).isFalse();
        Value fstObjPtrOffset = firstObjAndState.getOffsetForObject();
        assertThat(fstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(fstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(fstObj)).hasSize(3);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(fstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(fstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        SMGObject prevListSegment = fstObj;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          List<ValueAndSMGState> nextListObjAndStates =
              currentState.readValue(prevListSegment, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(i < listLength - 1 ? 1 : 2);
          Value nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          if (i >= listLength - 1) {
            List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
                currentState.dereferencePointer(nextPtr);
            assertThat(nextObjsList).hasSize(1);
            SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = nextObjsList.get(0);
            assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = nextObjAndState.getSMGState();
            SMGObject nextObj = nextObjAndState.getSMGObject();
            Value nextObjPtrOffset = nextObjAndState.getOffsetForObject();
            assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
            assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
            assertThat(nextObj.isZero()).isTrue();

            nextPtr = nextListObjAndStates.get(1).getValue();
            currentState = nextListObjAndStates.get(1).getState();
          }

          List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
              currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = nextObjsList.get(0);
          assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = nextObjAndState.getSMGState();
          SMGObject nextObj = nextObjAndState.getSMGObject();
          assertThat(nextObj.isSLL()).isFalse();
          Value nextObjPtrOffset = nextObjAndState.getOffsetForObject();
          assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
          assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          // Next, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(nextObj)).hasSize(3);

          firstSelfPtrsAndState =
              currentState.readValue(nextObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(nextObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          prevListSegment = nextObj;
        }
      }
    }
  }

  /*
   * Looping SLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   * We test materialization from the left to the right, including 0+.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllLoopingWithSelfPointerWithOffsetsLeftMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    BigInteger firstPtrTargetOffset = pointerSizeInBits;
    BigInteger secondPtrTargetOffset = pointerSizeInBits.add(pointerSizeInBits);
    // TODO: add multiple nextPtrOffset
    BigInteger nextPtrOffset = BigInteger.ZERO;
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrTargetOffset, firstPtrTargetOffset),
            ImmutableList.of(secondPtrTargetOffset, secondPtrTargetOffset),
            ImmutableList.of(firstPtrTargetOffset, secondPtrTargetOffset),
            ImmutableList.of(secondPtrTargetOffset, firstPtrTargetOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {

        Value[] topListPtrs = buildConcreteList(false, listSize, listLength, false);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr;
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits);
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();

        for (int i = 0; i < listLength; i++) {
          Value ptr = topListPtrs[i];
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            if (i == listLength - 1) {
              // Looping
              nextPtr = topListPtrs[0];
            }
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check left hand sided materialization
        // Read ptr from stack var, this mats the list once
        List<ValueAndSMGState> fstPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(fstPtrsAndStates).hasSize(1);
        Value fstPtr = fstPtrsAndStates.get(0).getValue();
        currentState = fstPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> firstObjsList =
            currentState.dereferencePointer(fstPtr);
        assertThat(firstObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset firstObjAndState = firstObjsList.get(0);
        assertThat(firstObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = firstObjAndState.getSMGState();
        SMGObject fstObj = firstObjAndState.getSMGObject();
        assertThat(fstObj.isSLL()).isFalse();
        Value fstObjPtrOffset = firstObjAndState.getOffsetForObject();
        assertThat(fstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(fstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(fstObj)).hasSize(3);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(fstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(fstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        SMGObject initialObject = fstObj;
        SMGObject prevListSegment = fstObj;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          List<ValueAndSMGState> nextListObjAndStates =
              currentState.readValue(prevListSegment, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(1);
          Value nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
              currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(i < listLength - 1 ? 1 : 2);
          SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = nextObjsList.get(0);
          assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = nextObjAndState.getSMGState();
          SMGObject nextObj = nextObjAndState.getSMGObject();
          assertThat(nextObj.isSLL()).isFalse();
          Value nextObjPtrOffset = nextObjAndState.getOffsetForObject();
          assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
          assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          if (i >= listLength - 1) {
            assertThat(nextObj).isEqualTo(initialObject);
            nextObjAndState = nextObjsList.get(1);
            assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = nextObjAndState.getSMGState();
            nextObj = nextObjAndState.getSMGObject();
            assertThat(nextObj.isSLL()).isFalse();
            nextObjPtrOffset = nextObjAndState.getOffsetForObject();
            assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
            assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
          }

          // Next, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(nextObj)).hasSize(3);

          firstSelfPtrsAndState =
              currentState.readValue(nextObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(nextObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          prevListSegment = nextObj;
        }
      }
    }
  }

  /*
   * DLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   * Materializes the list from the left.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithSelfPointerWithOffsetsLeftMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    BigInteger prevPtrTargetOffset = BigInteger.ZERO;
    // TODO: rolling next and prev offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] topListPtrs = buildConcreteList(true, listSize, listLength, false);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr.add(pointerSizeInBits);
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }
        if (changingPfo.equals(listSize)) {
          continue;
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
        assertThat(changingPfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          Value prevPtr =
              currentState
                  .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                  .getValue();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          if (!changingPfo.equals(pfo)) {
            // Change PFO for list
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check left hand sided materialization
        // Read ptr from stack var, this mats the list once
        List<ValueAndSMGState> fstPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(fstPtrsAndStates).hasSize(1);
        Value fstPtr = fstPtrsAndStates.get(0).getValue();
        currentState = fstPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> firstObjsList =
            currentState.dereferencePointer(fstPtr);
        assertThat(firstObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset firstObjAndState = firstObjsList.get(0);
        assertThat(firstObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = firstObjAndState.getSMGState();
        SMGObject fstObj = firstObjAndState.getSMGObject();
        assertThat(fstObj.isSLL()).isFalse();
        Value fstObjPtrOffset = firstObjAndState.getOffsetForObject();
        assertThat(fstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(fstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, prev, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(fstObj)).hasSize(4);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(fstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(fstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        // Prev check
        List<ValueAndSMGState> prevListObjAndStates =
            currentState.readValue(fstObj, changingPfo, pointerSizeInBits, null);
        assertThat(prevListObjAndStates).hasSize(1);
        Value prevPtr = prevListObjAndStates.get(0).getValue();
        currentState = prevListObjAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
            currentState.dereferencePointer(prevPtr);
        assertThat(prevObjsList).hasSize(1);
        assertThat(prevObjsList.get(0).getSMGObject()).isEqualTo(SMGObject.nullInstance());
        assertThat(prevObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(prevPtrTargetOffset);

        SMGObject prevListSegment = fstObj;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          List<ValueAndSMGState> nextListObjAndStates =
              currentState.readValue(prevListSegment, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(i < listLength - 1 ? 1 : 2);
          Value nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
              currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = nextObjsList.get(0);
          assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = nextObjAndState.getSMGState();
          SMGObject nextObj = nextObjAndState.getSMGObject();
          assertThat(nextObj.isSLL()).isFalse();
          Value nextObjPtrOffset = nextObjAndState.getOffsetForObject();
          assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
          assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          if (i >= listLength - 1) {
            assertThat(nextObj.isZero()).isTrue();

            // Go to extending case
            nextPtr = nextListObjAndStates.get(1).getValue();
            currentState = nextListObjAndStates.get(1).getState();

            nextObjsList = currentState.dereferencePointer(nextPtr);
            nextObjAndState = nextObjsList.get(0);
            assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = nextObjAndState.getSMGState();
            nextObj = nextObjAndState.getSMGObject();
            assertThat(nextObj.isSLL()).isFalse();
            nextObjPtrOffset = nextObjAndState.getOffsetForObject();
            assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
            assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
          }

          // Prev check
          prevListObjAndStates =
              currentState.readValue(nextObj, changingPfo, pointerSizeInBits, null);
          assertThat(prevListObjAndStates).hasSize(1);
          prevPtr = prevListObjAndStates.get(0).getValue();
          currentState = prevListObjAndStates.get(0).getState();

          prevObjsList = currentState.dereferencePointer(prevPtr);
          assertThat(prevObjsList).hasSize(1);
          assertThat(prevObjsList.get(0).getSMGObject()).isEqualTo(prevListSegment);
          assertThat(prevObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
              .isEqualTo(prevPtrTargetOffset);

          // Next, prev, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(nextObj)).hasSize(4);

          firstSelfPtrsAndState =
              currentState.readValue(nextObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(nextObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          prevListSegment = nextObj;
        }
      }
    }
  }

  /*
   * Looping DLL that has 2 pointers with differing target offsets to itself in different offsets.
   * Is abstractable, but the self-pointers need to be correct.
   * Materializes the list from the left.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllLoopingWithSelfPointerWithOffsetsLeftMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    BigInteger prevPtrTargetOffset = BigInteger.ZERO;
    // TODO: rolling next and prev offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] topListPtrs = buildConcreteList(true, listSize, listLength, true);

        // Ptr to the top list from some stack obj
        // We want to test from left to right until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr.add(pointerSizeInBits);
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }
        if (changingPfo.equals(listSize)) {
          continue;
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
        assertThat(changingPfo.compareTo(listSize) < 0).isTrue();

        for (int i = 0; i < topListPtrs.length; i++) {
          Value ptr = topListPtrs[i];
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          Value prevPtr =
              currentState
                  .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                  .getValue();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            if (i == topListPtrs.length - 1) {
              // Looping nxt
              nextPtr = topListPtrs[0];
            }
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          if (!changingPfo.equals(pfo)) {
            // Change PFO for list
            if (i == 0) {
              // Looping nxt
              prevPtr = topListPtrs[topListPtrs.length - 1];
            }
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check left hand sided materialization
        // Read ptr from stack var, this mats the list once
        List<ValueAndSMGState> fstPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(fstPtrsAndStates).hasSize(1);
        Value fstPtr = fstPtrsAndStates.get(0).getValue();
        currentState = fstPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> firstObjsList =
            currentState.dereferencePointer(fstPtr);
        assertThat(firstObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset firstObjAndState = firstObjsList.get(0);
        assertThat(firstObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = firstObjAndState.getSMGState();
        SMGObject fstObj = firstObjAndState.getSMGObject();
        assertThat(fstObj.isSLL()).isFalse();
        Value fstObjPtrOffset = firstObjAndState.getOffsetForObject();
        assertThat(fstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(fstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, prev, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(fstObj)).hasSize(4);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(fstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(fstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(fstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        // Prev check (don't mat the last ptr!)
        ValueAndSMGState prevListObjAndStates =
            currentState.readValueWithoutMaterialization(
                fstObj, changingPfo, pointerSizeInBits, null);
        Value prevPtr = prevListObjAndStates.getValue();
        currentState = prevListObjAndStates.getState();

        Optional<SMGStateAndOptionalSMGObjectAndOffset> prevObjAndState =
            currentState.dereferencePointerWithoutMaterilization(prevPtr);
        assertThat(prevObjAndState).isPresent();
        currentState = prevObjAndState.orElseThrow().getSMGState();
        assertThat(prevObjAndState.orElseThrow().getSMGObject())
            .isEqualTo(
                currentState
                    .dereferencePointerWithoutMaterilization(topListPtrs[topListPtrs.length - 1])
                    .orElseThrow()
                    .getSMGObject());
        assertThat(
                prevObjAndState
                    .orElseThrow()
                    .getOffsetForObject()
                    .asNumericValue()
                    .bigIntegerValue())
            .isEqualTo(prevPtrTargetOffset);

        SMGObject initialObj = fstObj;
        SMGObject prevListSegment = fstObj;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          List<ValueAndSMGState> nextListObjAndStates =
              currentState.readValue(prevListSegment, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(i < listLength - 1 ? 1 : 2);
          Value nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
              currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = nextObjsList.get(0);
          assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = nextObjAndState.getSMGState();
          SMGObject nextObj = nextObjAndState.getSMGObject();
          assertThat(nextObj.isSLL()).isFalse();
          Value nextObjPtrOffset = nextObjAndState.getOffsetForObject();
          assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
          assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          // Prev check
          List<ValueAndSMGState> prevListObjsAndStates =
              currentState.readValue(nextObj, changingPfo, pointerSizeInBits, null);
          assertThat(prevListObjsAndStates).hasSize(1);
          prevPtr = prevListObjsAndStates.get(0).getValue();
          currentState = prevListObjsAndStates.get(0).getState();

          List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
              currentState.dereferencePointer(prevPtr);
          assertThat(prevObjsList).hasSize(1);
          assertThat(prevObjsList.get(0).getSMGObject()).isEqualTo(prevListSegment);
          assertThat(prevObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
              .isEqualTo(prevPtrTargetOffset);

          if (i >= listLength - 1) {
            assertThat(nextObj).isEqualTo(initialObj);
            nextPtr = nextListObjAndStates.get(1).getValue();
            currentState = nextListObjAndStates.get(1).getState();

            nextObjsList = currentState.dereferencePointer(nextPtr);
            nextObjAndState = nextObjsList.get(0);
            assertThat(nextObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = nextObjAndState.getSMGState();
            nextObj = nextObjAndState.getSMGObject();
            assertThat(nextObj.isSLL()).isFalse();
            nextObjPtrOffset = nextObjAndState.getOffsetForObject();
            assertThat(nextObjPtrOffset.isNumericValue()).isTrue();
            assertThat(nextObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
          }

          // Next, prev, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(nextObj)).hasSize(4);

          firstSelfPtrsAndState =
              currentState.readValue(nextObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(nextObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(nextObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          prevListSegment = nextObj;
        }
      }
    }
  }

  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithSelfPointerWithOffsetsRightMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    BigInteger prevPtrTargetOffset = BigInteger.ZERO;
    // TODO: rolling next and prev offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] topListPtrs = buildConcreteList(true, listSize, listLength, false);

        // Ptr to the top list from some stack obj (lst ptr)
        // We want to test from right to left until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(topListPtrs[topListPtrs.length - 1])
                    .orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr.add(pointerSizeInBits);
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }
        if (changingPfo.equals(listSize)) {
          continue;
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
        assertThat(changingPfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          Value prevPtr =
              currentState
                  .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                  .getValue();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          if (!changingPfo.equals(pfo)) {
            // Change PFO for list
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check left hand sided materialization
        // Read ptr from stack var, this mats the list once
        List<ValueAndSMGState> fstPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(fstPtrsAndStates).hasSize(1);
        Value lastPtr = fstPtrsAndStates.get(0).getValue();
        currentState = fstPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> lastObjsList =
            currentState.dereferencePointer(lastPtr);
        assertThat(lastObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset lastObjAndState = lastObjsList.get(0);
        assertThat(lastObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = lastObjAndState.getSMGState();
        SMGObject lstObj = lastObjAndState.getSMGObject();
        assertThat(lstObj.isSLL()).isFalse();
        Value lstObjPtrOffset = lastObjAndState.getOffsetForObject();
        assertThat(lstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(lstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, prev, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(lstObj)).hasSize(4);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(lstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(lstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(lstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(lstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        // Next check
        List<ValueAndSMGState> nextListObjAndStates =
            currentState.readValue(lstObj, changingNfo, pointerSizeInBits, null);
        assertThat(nextListObjAndStates).hasSize(1);
        Value nextPtr = nextListObjAndStates.get(0).getValue();
        currentState = nextListObjAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
            currentState.dereferencePointer(nextPtr);
        assertThat(nextObjsList).hasSize(1);
        assertThat(nextObjsList.get(0).getSMGObject()).isEqualTo(SMGObject.nullInstance());
        assertThat(nextObjsList.get(0).getOffsetForObject().isNumericValue()).isTrue();
        assertThat(nextObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(prevPtrTargetOffset);

        SMGObject previousListSegment = lstObj;
        boolean foundZeroPlusOnce = false;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          // Read prev ptr
          List<ValueAndSMGState> prevListObjAndStates =
              currentState.readValue(previousListSegment, changingPfo, pointerSizeInBits, null);
          assertThat(prevListObjAndStates).hasSize(i < listLength - 1 ? 1 : 2);
          Value prevPtr = prevListObjAndStates.get(0).getValue();
          currentState = prevListObjAndStates.get(0).getState();

          // deref prev ptr, get prev obj
          List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
              currentState.dereferencePointer(prevPtr);
          assertThat(prevObjsList).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset prevObjAndState = prevObjsList.get(0);
          assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = prevObjAndState.getSMGState();
          SMGObject prevObj = prevObjAndState.getSMGObject();
          assertThat(prevObj.isSLL()).isFalse();
          Value prevObjPtrOffset = prevObjAndState.getOffsetForObject();
          assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
          assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          // Next check of prev obj (should point to the one above or 0)
          if (i >= listLength - 1) {
            assertThat(prevObj.isZero()).isTrue();
            foundZeroPlusOnce = true;

            prevPtr = prevListObjAndStates.get(1).getValue();
            currentState = prevListObjAndStates.get(1).getState();
            prevObjsList = currentState.dereferencePointer(prevPtr);
            assertThat(prevObjsList).hasSize(1);
            prevObjAndState = prevObjsList.get(0);
            assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = prevObjAndState.getSMGState();
            prevObj = prevObjAndState.getSMGObject();
            assertThat(prevObj.isSLL()).isFalse();
            prevObjPtrOffset = prevObjAndState.getOffsetForObject();
            assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
            assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
          }

          nextListObjAndStates =
              currentState.readValue(prevObj, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(1);
          nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          nextObjsList = currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(1);
          assertThat(nextObjsList.get(0).getSMGObject()).isEqualTo(previousListSegment);
          assertThat(nextObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
              .isEqualTo(prevPtrTargetOffset);

          // Next, prev, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(prevObj)).hasSize(4);

          firstSelfPtrsAndState =
              currentState.readValue(prevObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(prevObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(prevObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(prevObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          previousListSegment = prevObj;
        }
        assertThat(foundZeroPlusOnce).isTrue();
      }
    }
  }

  // Looping sll with 2 self pointers in each list segment with multiple target offsets and ptr
  // offsets.
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithSelfPointerWithOffsetsRightMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    int numOfAbstrLists = 0;
    // TODO: rolling next and prev target offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        // We start from the right and have no back ptr, so we need stack objs for each element
        Value[] topListPtrs = buildConcreteList(false, listSize, listLength, true);

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr;
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits);
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();

        for (Value ptr : topListPtrs) {
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();
        numOfAbstrLists++;

        assertThat(currentState.getMemoryModel().getSmg().getNumberOfAbstractedLists())
            .isEqualTo(numOfAbstrLists);
        assertThat(topListPtrs.length).isEqualTo(listLength);
        for (int i = 0; i < listLength; i++) {
          SMGPointsToEdge ptrPTE =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(topListPtrs[i])
                          .orElseThrow())
                  .orElseThrow();
          if (i == 0) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
          } else if (i == listLength - 1) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
          } else {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
          }
        }

        // Abstraction is checked in the test for this abstraction

        // deref ptr, get prev obj to the one before (the ptr with the last specifier)
        Value currentPtr = topListPtrs[topListPtrs.length - 1];

        SMGPointsToEdge ptrPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(currentPtr).orElseThrow())
                .orElseThrow();
        assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
            currentState.dereferencePointer(currentPtr);
        assertThat(prevObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset prevObjAndState = prevObjsList.get(0);
        assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = prevObjAndState.getSMGState();

        SMGObject currentObj = prevObjAndState.getSMGObject();
        assertThat(currentObj.isSLL()).isFalse();
        Value prevObjPtrOffset = prevObjAndState.getOffsetForObject();
        assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
        assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

        List<ValueAndSMGState> nextListObjAndStates =
            currentState.readValue(currentObj, changingNfo, pointerSizeInBits, null);
        assertThat(nextListObjAndStates).hasSize(1);
        Value nextPtr = nextListObjAndStates.get(0).getValue();
        currentState = nextListObjAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
            currentState.dereferencePointer(nextPtr);
        assertThat(nextObjsList).hasSize(1);
        assertThat(nextObjsList.get(0).getSMGObject()).isEqualTo(SMGObject.nullInstance());
        assertThat(nextObjsList.get(0).getOffsetForObject().isNumericValue()).isTrue();
        assertThat(nextObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(nextPtrOffset);

        // Next, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(currentObj)).hasSize(3);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(currentObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(currentObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(currentObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(currentObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        for (int j = 0; j < listLength; j++) {
          ptrPTE =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(topListPtrs[j])
                          .orElseThrow())
                  .orElseThrow();
          // There is no last ptr anymore
          if (j == listLength - 1) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
          } else if (j == 0) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
          } else {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
          }
        }
      }
    }
  }

  // Looping dll with 2 self pointers in each list segment with multiple target offsets and ptr
  // offsets.
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllLoopingWithSelfPointersWithOffsetsRightMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    BigInteger prevPtrTargetOffset = BigInteger.ZERO;
    // TODO: rolling next and prev offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] topListPtrs = buildConcreteList(true, listSize, listLength, true);

        // Ptr to the top list from some stack obj (lst ptr)
        // We want to test from right to left until 0+ has been materialized at least once
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
        currentState = stackObjAndState.getState();
        SMGObject stackObj = stackObjAndState.getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                stackObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(topListPtrs[topListPtrs.length - 1])
                    .orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr.add(pointerSizeInBits);
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }
        if (changingPfo.equals(listSize)) {
          continue;
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingPfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
        assertThat(changingPfo.compareTo(listSize) < 0).isTrue();

        for (int i = 0; i < topListPtrs.length; i++) {
          Value ptr = topListPtrs[i];
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          Value prevPtr =
              currentState
                  .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                  .getValue();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          // Looping next
          if (i == topListPtrs.length - 1) {
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(topListPtrs[0])
                        .orElseThrow());
          }
          if (!changingPfo.equals(pfo)) {
            // Change PFO for list
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }
          // Looping prev
          if (i == 0) {
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingPfo,
                    pointerSizeInBits,
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(topListPtrs[topListPtrs.length - 1])
                        .orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        // Abstraction is checked in the test for this abstraction

        // Check right hand sided materialization
        // Read ptr from stack var, this mats the list once
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(
                        currentState
                            .getMemoryModel()
                            .getSMGValueFromValue(topListPtrs[topListPtrs.length - 1])
                            .orElseThrow())
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        List<ValueAndSMGState> lastPtrsAndStates =
            currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
        assertThat(lastPtrsAndStates).hasSize(1);
        Value lastPtr = lastPtrsAndStates.get(0).getValue();
        currentState = lastPtrsAndStates.get(0).getState();

        List<SMGStateAndOptionalSMGObjectAndOffset> lastObjsList =
            currentState.dereferencePointer(lastPtr);
        assertThat(lastObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset lastObjAndState = lastObjsList.get(0);
        assertThat(lastObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = lastObjAndState.getSMGState();
        SMGObject lstObj = lastObjAndState.getSMGObject();
        assertThat(lstObj.isSLL()).isFalse();
        Value lstObjPtrOffset = lastObjAndState.getOffsetForObject();
        assertThat(lstObjPtrOffset.isNumericValue()).isTrue();
        assertThat(lstObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Next, prev, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(lstObj)).hasSize(4);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(lstObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(lstObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(lstObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(lstObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        // Next check
        ValueAndSMGState nextListObjAndStatesWOMat =
            currentState.readValueWithoutMaterialization(
                lstObj, changingNfo, pointerSizeInBits, null);
        Value nextPtr = nextListObjAndStatesWOMat.getValue();
        currentState = nextListObjAndStatesWOMat.getState();

        Optional<SMGStateAndOptionalSMGObjectAndOffset> nextObjsListWOMat =
            currentState.dereferencePointerWithoutMaterilization(nextPtr);
        assertThat(nextObjsListWOMat).isPresent();
        assertThat(nextObjsListWOMat.orElseThrow().getSMGObject())
            .isInstanceOf(SMGDoublyLinkedListSegment.class);
        assertThat(
                ((SMGDoublyLinkedListSegment) nextObjsListWOMat.orElseThrow().getSMGObject())
                    .getMinLength())
            .isEqualTo(7);
        assertThat(nextObjsListWOMat.orElseThrow().getOffsetForObject().isNumericValue()).isTrue();
        assertThat(
                nextObjsListWOMat
                    .orElseThrow()
                    .getOffsetForObject()
                    .asNumericValue()
                    .bigIntegerValue())
            .isEqualTo(prevPtrTargetOffset);

        SMGObject firstObjDeref = lstObj;
        SMGObject previousListSegment = lstObj;
        boolean foundZeroPlusOnce = false;
        // We start with the first list segment already checked and the second ptr being read
        for (int i = 0; i < listLength; i++) {
          // Read prev ptr
          List<ValueAndSMGState> prevListObjAndStates =
              currentState.readValue(previousListSegment, changingPfo, pointerSizeInBits, null);
          assertThat(prevListObjAndStates).hasSize(i < listLength - 1 ? 1 : 2);
          Value prevPtr = prevListObjAndStates.get(0).getValue();
          currentState = prevListObjAndStates.get(0).getState();

          // deref prev ptr, get prev obj
          List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
              currentState.dereferencePointer(prevPtr);
          assertThat(prevObjsList).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset prevObjAndState = prevObjsList.get(0);
          assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
          currentState = prevObjAndState.getSMGState();
          SMGObject prevObj = prevObjAndState.getSMGObject();
          assertThat(prevObj.isSLL()).isFalse();
          Value prevObjPtrOffset = prevObjAndState.getOffsetForObject();
          assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
          assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

          // Next check of prev obj (should point to the very first obj we derefed because of end of
          // looping list)
          if (i >= listLength - 1) {
            assertThat(prevObj).isEqualTo(firstObjDeref);
            prevObjAndState = prevObjsList.get(0);
            assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = prevObjAndState.getSMGState();
            prevObj = prevObjAndState.getSMGObject();
            assertThat(prevObj.isSLL()).isFalse();
            prevObjPtrOffset = prevObjAndState.getOffsetForObject();
            assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
            assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
            foundZeroPlusOnce = true;

            prevPtr = prevListObjAndStates.get(1).getValue();
            currentState = prevListObjAndStates.get(1).getState();

            // deref prev ptr, get prev obj
            prevObjsList = currentState.dereferencePointer(prevPtr);
            assertThat(prevObjsList).hasSize(1);
            prevObjAndState = prevObjsList.get(0);
            assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
            currentState = prevObjAndState.getSMGState();
            prevObj = prevObjAndState.getSMGObject();
            assertThat(prevObj.isSLL()).isFalse();
            prevObjPtrOffset = prevObjAndState.getOffsetForObject();
            assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
            assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue())
                .isEqualTo(nextPtrOffset);
          }

          List<ValueAndSMGState> nextListObjAndStates =
              currentState.readValue(prevObj, changingNfo, pointerSizeInBits, null);
          assertThat(nextListObjAndStates).hasSize(1);
          nextPtr = nextListObjAndStates.get(0).getValue();
          currentState = nextListObjAndStates.get(0).getState();

          List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
              currentState.dereferencePointer(nextPtr);
          assertThat(nextObjsList).hasSize(1);
          assertThat(nextObjsList.get(0).getSMGObject()).isEqualTo(previousListSegment);
          assertThat(nextObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
              .isEqualTo(prevPtrTargetOffset);

          // Next, prev, and 2 self ptrs
          assertThat(currentState.getMemoryModel().getSmg().getEdges(prevObj)).hasSize(4);

          firstSelfPtrsAndState =
              currentState.readValue(prevObj, offsetFirstSelfPtr, pointerSizeInBits, null);
          assertThat(firstSelfPtrsAndState).hasSize(1);
          currentState = firstSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedFirstSelfPtr =
              currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
          assertThat(derefedFirstSelfPtr).hasSize(1);
          currentState = derefedFirstSelfPtr.get(0).getSMGState();
          assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(prevObj);
          assertThat(
                  derefedFirstSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(0));

          secondSelfPtrsAndState =
              currentState.readValue(prevObj, offsetSecondSelfPtr, pointerSizeInBits, null);
          assertThat(secondSelfPtrsAndState).hasSize(1);
          currentState = secondSelfPtrsAndState.get(0).getState();
          assertThat(
                  currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
              .isTrue();
          derefedSecondSelfPtr =
              currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
          assertThat(derefedSecondSelfPtr).hasSize(1);
          currentState = derefedSecondSelfPtr.get(0).getSMGState();
          assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
          assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(prevObj);
          assertThat(
                  derefedSecondSelfPtr
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(targetOffsetsForSelfPtrs.get(1));

          previousListSegment = prevObj;
        }
        assertThat(foundZeroPlusOnce).isTrue();
      }
    }
  }

  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllLoopingWithSelfPointersWithOffsetsRightMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    // Large enough for 3 pointers
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    BigInteger firstPtrOffset = pointerSizeInBits;
    BigInteger secondPtrOffset = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger nextPtrOffset = BigInteger.ZERO;
    // TODO: rolling next and prev target offsets
    for (List<BigInteger> targetOffsetsForSelfPtrs :
        ImmutableList.of(
            ImmutableList.of(firstPtrOffset, firstPtrOffset),
            ImmutableList.of(secondPtrOffset, secondPtrOffset),
            ImmutableList.of(firstPtrOffset, secondPtrOffset),
            ImmutableList.of(secondPtrOffset, firstPtrOffset))) {

      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        // We start from the right and have no back ptr, so we need stack objs for each element
        Value[] topListPtrs = buildConcreteList(false, listSize, listLength, true);

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        if (changingNfo.equals(offsetFirstSelfPtr)) {
          offsetFirstSelfPtr = offsetSecondSelfPtr;
          offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
        }
        if (changingNfo.equals(offsetSecondSelfPtr)) {
          offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
        }

        assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
        assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
        assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
        assertThat(changingNfo.compareTo(listSize) < 0).isTrue();

        for (int i = 0; i < topListPtrs.length; i++) {
          Value ptr = topListPtrs[i];
          // Write self pointers into every concrete obj
          SMGObject obj =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr)
                  .orElseThrow()
                  .getSMGObject();
          if (!changingNfo.equals(nfo)) {
            // Change NFO for list
            Value nextPtr =
                currentState
                    .readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
          }
          // Looping
          if (i == topListPtrs.length - 1) {
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo,
                    pointerSizeInBits,
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(topListPtrs[0])
                        .orElseThrow());
          }

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(0));
          currentState = selfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetFirstSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(selfPtrAndState.getValue())
                      .orElseThrow());

          ValueAndSMGState otherSelfPtrAndState =
              currentState.searchOrCreateAddress(
                  obj, CPointerType.POINTER_TO_VOID, targetOffsetsForSelfPtrs.get(1));
          currentState = otherSelfPtrAndState.getState();
          currentState =
              currentState.writeValueWithoutChecks(
                  obj,
                  offsetSecondSelfPtr,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(otherSelfPtrAndState.getValue())
                      .orElseThrow());
        }

        // Abstract
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
        currentState = absFinder.findAndAbstractLists();

        assertThat(topListPtrs.length).isEqualTo(listLength);
        // The first and last might be shifted due to the abstraction
        int firstIndex = -1;
        int lastIndex = -1;
        for (int i = 0; i < listLength; i++) {
          SMGPointsToEdge ptrPTE =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(topListPtrs[i])
                          .orElseThrow())
                  .orElseThrow();
          if (ptrPTE.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
            assertThat(firstIndex).isEqualTo(-1);
            firstIndex = i;
          } else if (ptrPTE.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
            assertThat(lastIndex).isEqualTo(-1);
            lastIndex = i;
          }
        }
        assertThat(firstIndex).isNotEqualTo(-1);
        assertThat(lastIndex).isNotEqualTo(-1);
        assertThat(firstIndex).isNotEqualTo(lastIndex);

        // Abstraction is checked in the test for this abstraction

        // deref ptr, get prev obj to the one before (the ptr with the last specifier)
        Value currentPtr = topListPtrs[listLength - 1];

        SMGPointsToEdge ptrPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(currentPtr).orElseThrow())
                .orElseThrow();
        assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        List<SMGStateAndOptionalSMGObjectAndOffset> prevObjsList =
            currentState.dereferencePointer(currentPtr);
        assertThat(prevObjsList).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset prevObjAndState = prevObjsList.get(0);
        assertThat(prevObjAndState.hasSMGObjectAndOffset()).isTrue();
        currentState = prevObjAndState.getSMGState();

        SMGObject currentObj = prevObjAndState.getSMGObject();
        assertThat(currentObj.isSLL()).isFalse();
        Value prevObjPtrOffset = prevObjAndState.getOffsetForObject();
        assertThat(prevObjPtrOffset.isNumericValue()).isTrue();
        assertThat(prevObjPtrOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrOffset);

        List<ValueAndSMGState> nextListObjAndStates =
            currentState.readValue(currentObj, changingNfo, pointerSizeInBits, null);
        assertThat(nextListObjAndStates).hasSize(1);
        Value nextPtr = nextListObjAndStates.get(0).getValue();
        currentState = nextListObjAndStates.get(0).getState();

        SMGObject expectedNextListSegment =
            currentState
                .dereferencePointerWithoutMaterilization(topListPtrs[firstIndex])
                .orElseThrow()
                .getSMGObject();

        List<SMGStateAndOptionalSMGObjectAndOffset> nextObjsList =
            currentState.dereferencePointer(nextPtr);
        assertThat(nextObjsList).hasSize(1);
        assertThat(nextObjsList.get(0).getSMGObject()).isEqualTo(expectedNextListSegment);
        assertThat(nextObjsList.get(0).getOffsetForObject().isNumericValue()).isTrue();
        assertThat(nextObjsList.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(nextPtrOffset);

        // Next, and 2 self ptrs
        assertThat(currentState.getMemoryModel().getSmg().getEdges(currentObj)).hasSize(3);

        List<ValueAndSMGState> firstSelfPtrsAndState =
            currentState.readValue(currentObj, offsetFirstSelfPtr, pointerSizeInBits, null);
        assertThat(firstSelfPtrsAndState).hasSize(1);
        currentState = firstSelfPtrsAndState.get(0).getState();
        assertThat(currentState.getMemoryModel().isPointer(firstSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedFirstSelfPtr =
            currentState.dereferencePointer(firstSelfPtrsAndState.get(0).getValue());
        assertThat(derefedFirstSelfPtr).hasSize(1);
        currentState = derefedFirstSelfPtr.get(0).getSMGState();
        assertThat(derefedFirstSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedFirstSelfPtr.get(0).getSMGObject()).isEqualTo(currentObj);
        assertThat(
                derefedFirstSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(0));

        List<ValueAndSMGState> secondSelfPtrsAndState =
            currentState.readValue(currentObj, offsetSecondSelfPtr, pointerSizeInBits, null);
        assertThat(secondSelfPtrsAndState).hasSize(1);
        currentState = secondSelfPtrsAndState.get(0).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(secondSelfPtrsAndState.get(0).getValue()))
            .isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedSecondSelfPtr =
            currentState.dereferencePointer(secondSelfPtrsAndState.get(0).getValue());
        assertThat(derefedSecondSelfPtr).hasSize(1);
        currentState = derefedSecondSelfPtr.get(0).getSMGState();
        assertThat(derefedSecondSelfPtr.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedSecondSelfPtr.get(0).getSMGObject()).isEqualTo(currentObj);
        assertThat(
                derefedSecondSelfPtr.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(targetOffsetsForSelfPtrs.get(1));

        for (int j = 0; j < listLength; j++) {
          ptrPTE =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(topListPtrs[j])
                          .orElseThrow())
                  .orElseThrow();
          if (j == lastIndex) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
          } else if (j == firstIndex) {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
          } else {
            assertThat(ptrPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_ALL_POINTER);
          }
        }
      }
    }
  }

  // A (abstractable) list with a root (in the looping list) that is not abstractable.
  // When materializing the root needs to be connected correctly to the list in both next and prev.
  @Ignore
  @Test
  public void loopingDllWithDistinctListElementRightMaterializationTest() {
    throw new RuntimeException("Implement me");
  }

  // TODO: the same with fst and lst as beginning ptr
  // A (abstractable) list with a root (in the looping list) that is not abstractable.
  // When materializing the root needs to be connected correctly to the list in both next and prev.
  // We make the non-abstractable look like a struct:
  // next *     // for an SLL of top elements. In the abstractable, this is a value.
  // nextDLL *  // next/prev and target offsets as in the abstractable list
  // prevDLL *
  @Ignore
  @Test
  public void loopingDllWithDistinctListElementLeftMaterializationTest()
      throws SMGException, SMGSolverException {
    // from linux-list-lst-data.c
    /* next pointer target offset 64
    loop::head: SymbolicIdentifier[0](Value3[0,64) -> Optional[ -> (reg) [0] SMGObject5[0, 128)]) in loop::head[0, 64)
    loop::now: SymbolicIdentifier[165](Value3251[0,64) -> Optional[ -> (lst) [0] 12+DLL 494]) in loop::now[0, 64)

    Value3 (0)    -> (reg) [0] SMGObject5[0, 128)[(ptr) Value8 [0; 64), (ptr) Value1059 [64; 64), (ptr) Value3252 [128; 64)]

    Value8 (11)   -> (fst) [64] 12+DLL 494[Value3250 [0; 32), (ptr) Value3252 [64; 64), (ptr) Value3 [128; 64)]
    Value3184 (1) -> (all) [64] 12+DLL 494[Value3250 [0; 32), (ptr) Value3252 [64; 64), (ptr) Value3 [128; 64)]
    Value3186 (0) -> (lst) [64] 12+DLL 494[Value3250 [0; 32), (ptr) Value3252 [64; 64), (ptr) Value3 [128; 64)]
    Value3251 (0) -> (lst) [0] 12+DLL 494[Value3250 [0; 32), (ptr) Value3252 [64; 64), (ptr) Value3 [128; 64)]

    Value1059 (0) -> (lst) [64] 0+DLL 473[Value1022 [0; 32), (ptr) Value3 [64; 64), (ptr) Value3186 [128; 64)]
    Value3252 (0) -> (fst) [64] 0+DLL 473[Value1022 [0; 32), (ptr) Value3 [64; 64), (ptr) Value3186 [128; 64)]

    Value3250 and Value1022 are distinct __VERIFIER_nondet_int();
    SMGObject5 is the distinct root with next and prev only.
         */
    int listLength = 8;
    BigInteger nextPtrTargetOffset = pointerSizeInBits;
    BigInteger prevPtrTargetOffset = pointerSizeInBits;
    // Value in offset 0 is 1
    Value[] listPtrs =
        buildConcreteListWithEqualValues(
            true,
            dllSize,
            listLength,
            1,
            nextPtrTargetOffset,
            Optional.of(prevPtrTargetOffset),
            false);

    // Head points to nonAbstractingListHead
    SMGObjectAndSMGState headObjAndState =
        currentState.copyAndAddStackObject(numericPointerSizeInBits);
    currentState = headObjAndState.getState();
    SMGObject headObj = headObjAndState.getSMGObject();

    // We make the non-abstractable look like a struct:
    // next *     // for an SLL of top elements. In the abstractable, this is a value
    // nextDLL *  // next/prev and target offsets as in the abstractable list
    // prevDLL *
    SMGObjectAndSMGState nonAbstractingListHeadAndState =
        currentState.copyAndAddNewHeapObject(dllSizeValue);
    currentState = nonAbstractingListHeadAndState.getState();
    SMGObject nonAbstractingListHead = nonAbstractingListHeadAndState.getSMGObject();
    ValueAndSMGState ptrToNonAbstractingListHead =
        currentState.searchOrCreateAddress(
            nonAbstractingListHead, CPointerType.POINTER_TO_VOID, BigInteger.ZERO);
    currentState = ptrToNonAbstractingListHead.getState();
    ValueAndSMGState nextPtrWOffsetToNonAbstractingListHead =
        currentState.searchOrCreateAddress(
            nonAbstractingListHead, CPointerType.POINTER_TO_VOID, nextPtrTargetOffset);
    currentState = nextPtrWOffsetToNonAbstractingListHead.getState();
    ValueAndSMGState prevPtrWOffsetToNonAbstractingListHead =
        currentState.searchOrCreateAddress(
            nonAbstractingListHead, CPointerType.POINTER_TO_VOID, prevPtrTargetOffset);
    currentState = prevPtrWOffsetToNonAbstractingListHead.getState();

    // Save ptr to this in head
    currentState =
        currentState.writeValueWithoutChecks(
            headObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(ptrToNonAbstractingListHead.getValue())
                .orElseThrow());
    // Build list element
    currentState =
        currentState.writeValueWithoutChecks(
            nonAbstractingListHead, BigInteger.ZERO, pointerSizeInBits, SMGValue.zeroValue());

    ValueAndSMGState nextPtrWOffsetToAbstractingFirst =
        currentState.searchOrCreateAddress(
            currentState
                .dereferencePointerWithoutMaterilization(listPtrs[0])
                .orElseThrow()
                .getSMGObject(),
            CPointerType.POINTER_TO_VOID,
            nextPtrTargetOffset);
    currentState = nextPtrWOffsetToAbstractingFirst.getState();
    currentState =
        currentState.writeValueWithoutChecks(
            nonAbstractingListHead,
            nfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(nextPtrWOffsetToAbstractingFirst.getValue())
                .orElseThrow());

    ValueAndSMGState prevPtrWOffsetToAbstractingLast =
        currentState.searchOrCreateAddress(
            currentState
                .dereferencePointerWithoutMaterilization(listPtrs[listPtrs.length - 1])
                .orElseThrow()
                .getSMGObject(),
            CPointerType.POINTER_TO_VOID,
            prevPtrTargetOffset);
    currentState = prevPtrWOffsetToAbstractingLast.getState();
    currentState =
        currentState.writeValueWithoutChecks(
            nonAbstractingListHead,
            pfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(prevPtrWOffsetToAbstractingLast.getValue())
                .orElseThrow());

    // Now fix next and prev of the list towards this new element
    currentState =
        currentState.writeValueWithoutChecks(
            currentState
                .dereferencePointerWithoutMaterilization(listPtrs[0])
                .orElseThrow()
                .getSMGObject(),
            pfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(prevPtrWOffsetToNonAbstractingListHead.getValue())
                .orElseThrow());
    currentState =
        currentState.writeValueWithoutChecks(
            currentState
                .dereferencePointerWithoutMaterilization(listPtrs[listPtrs.length - 1])
                .orElseThrow()
                .getSMGObject(),
            nfo,
            pointerSizeInBits,
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(nextPtrWOffsetToNonAbstractingListHead.getValue())
                .orElseThrow());

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    assertThat(currentState.getMemoryModel().getSmg().getNumberOfAbstractedLists()).isEqualTo(1);

    // Read headObj next dll w/o mat to check that the element is not abstracted
    ValueAndSMGState nonMatPtrToNonAbstrAndState =
        currentState.readValueWithoutMaterialization(
            headObj, BigInteger.ZERO, pointerSizeInBits, null);
    currentState = nonMatPtrToNonAbstrAndState.getState();
    Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNonAbstrObjAndState =
        currentState.dereferencePointerWithoutMaterilization(
            nonMatPtrToNonAbstrAndState.getValue());
    assertThat(maybeNonAbstrObjAndState).isPresent();
    assertThat(maybeNonAbstrObjAndState.orElseThrow().hasSMGObjectAndOffset()).isTrue();
    assertThat(maybeNonAbstrObjAndState.orElseThrow().getSMGObject().isSLL()).isFalse();

    // Read headObj next dll w mat and check everything
    List<ValueAndSMGState> matPtrToNonAbstrsAndStates =
        currentState.readValue(headObj, BigInteger.ZERO, pointerSizeInBits, null);
    assertThat(matPtrToNonAbstrsAndStates).hasSize(1);
    ValueAndSMGState matPtrToNonAbstrAndState = matPtrToNonAbstrsAndStates.get(0);
    currentState = matPtrToNonAbstrAndState.getState();

    Value nextPtr = matPtrToNonAbstrAndState.getValue();
    List<SMGStateAndOptionalSMGObjectAndOffset> derefedListElems =
        currentState.dereferencePointer(nextPtr);
    assertThat(derefedListElems).hasSize(1);
    currentState = derefedListElems.get(0).getSMGState();
    assertThat(derefedListElems.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    // This is the first, non-abstracted elem
    SMGObject derefedListElem = derefedListElems.get(0).getSMGObject();

    SMGObject nonAbstrListElem = derefedListElem;
    SMGState stateAfterMat = currentState;

    SMGObject previousConcreteListSegment = null;
    // Walk the list through next until we are repeating 0+
    for (int i = 0; i < listLength + 2; i++) {
      if (i != 0) {
        // 0 case being the initial segment that is not abstracted
        List<ValueAndSMGState> ptrToNextAndStates =
            currentState.readValue(derefedListElem, nfo, pointerSizeInBits, null);
        assertThat(ptrToNextAndStates).hasSize(i > listLength ? 2 : 1);
        ValueAndSMGState matPtrToAbstrAndState = ptrToNextAndStates.get(0);
        currentState = matPtrToAbstrAndState.getState();

        nextPtr = matPtrToAbstrAndState.getValue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedNextListElems =
            currentState.dereferencePointer(nextPtr);
        assertThat(derefedNextListElems).hasSize(1);
        currentState = derefedNextListElems.get(0).getSMGState();
        assertThat(
                derefedNextListElems.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(nextPtrTargetOffset);
        derefedListElem = derefedNextListElems.get(0).getSMGObject();
        Value targetOffset = derefedNextListElems.get(0).getOffsetForObject();

        if (i > listLength) {
          // 0+ case
          assertThat(derefedListElem).isEqualTo(nonAbstrListElem);
          assertThat(targetOffset.asNumericValue().bigIntegerValue())
              .isEqualTo(nextPtrTargetOffset);

          // Get extension case
          matPtrToAbstrAndState = ptrToNextAndStates.get(1);
          currentState = matPtrToAbstrAndState.getState();

          nextPtr = matPtrToAbstrAndState.getValue();
          derefedNextListElems = currentState.dereferencePointer(nextPtr);
          assertThat(derefedNextListElems).hasSize(1);
          currentState = derefedNextListElems.get(0).getSMGState();
          targetOffset = derefedNextListElems.get(0).getOffsetForObject();
          assertThat(
                  derefedNextListElems
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(nextPtrTargetOffset);
          derefedListElem = derefedNextListElems.get(0).getSMGObject();
          assertThat(derefedListElem).isNotEqualTo(nonAbstrListElem);
        }
        assertThat(targetOffset.asNumericValue().bigIntegerValue()).isEqualTo(nextPtrTargetOffset);
      }

      // Check correct value in offset 0
      List<ValueAndSMGState> readConstAndStateList =
          currentState.readValue(derefedListElem, BigInteger.ZERO, pointerSizeInBits, null);
      assertThat(readConstAndStateList).hasSize(1);
      if (i == 0) {
        assertThat(readConstAndStateList.get(0).getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
      } else {
        assertThat(readConstAndStateList.get(0).getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ONE);
      }

      // Check prev and next w/o mat
      ValueAndSMGState readNextNoMatAndStateList =
          currentState.readValueWithoutMaterialization(
              derefedListElem, nfo, pointerSizeInBits, null);
      currentState = readNextNoMatAndStateList.getState();
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObjWOMatAndState =
          currentState.dereferencePointerWithoutMaterilization(
              readNextNoMatAndStateList.getValue());
      assertThat(maybeNextObjWOMatAndState).isPresent();
      currentState = maybeNextObjWOMatAndState.orElseThrow().getSMGState();
      SMGObject nextObjWOMat = maybeNextObjWOMatAndState.orElseThrow().getSMGObject();
      assertThat(
              maybeNextObjWOMatAndState
                  .orElseThrow()
                  .getOffsetForObject()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(nextPtrTargetOffset);
      currentState = maybeNextObjWOMatAndState.orElseThrow().getSMGState();
      assertThat(nextObjWOMat).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(readNextNoMatAndStateList.getValue())
                          .orElseThrow())
                  .orElseThrow()
                  .targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      if (i >= listLength) {
        // 0+
        assertThat(((SMGSinglyLinkedListSegment) nextObjWOMat).getMinLength()).isEqualTo(0);
      }

      ValueAndSMGState readPrevNoMatAndStateList =
          currentState.readValueWithoutMaterialization(
              derefedListElem, pfo, pointerSizeInBits, null);
      currentState = readPrevNoMatAndStateList.getState();
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybePrevObjWOMatAndState =
          currentState.dereferencePointerWithoutMaterilization(
              readPrevNoMatAndStateList.getValue());
      assertThat(maybePrevObjWOMatAndState).isPresent();
      currentState = maybePrevObjWOMatAndState.orElseThrow().getSMGState();
      SMGObject prevObjWOMat = maybePrevObjWOMatAndState.orElseThrow().getSMGObject();
      assertThat(
              maybePrevObjWOMatAndState
                  .orElseThrow()
                  .getOffsetForObject()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(nextPtrTargetOffset);
      currentState = maybePrevObjWOMatAndState.orElseThrow().getSMGState();
      if (i == 0) {
        assertThat(prevObjWOMat).isEqualTo(nextObjWOMat);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(
                        currentState
                            .getMemoryModel()
                            .getSMGValueFromValue(readPrevNoMatAndStateList.getValue())
                            .orElseThrow())
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      } else {
        assertThat(prevObjWOMat).isEqualTo(previousConcreteListSegment);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(
                        currentState
                            .getMemoryModel()
                            .getSMGValueFromValue(readPrevNoMatAndStateList.getValue())
                            .orElseThrow())
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);
      }

      previousConcreteListSegment = derefedListElem;
    }

    // Same but walk through prev
    currentState = stateAfterMat;
    derefedListElem = nonAbstrListElem;
    previousConcreteListSegment = null;
    for (int i = 0; i < listLength + 2; i++) {
      if (i != 0) {
        // 0 case being the initial segment that is not abstracted
        List<ValueAndSMGState> ptrToPrevAndStates =
            currentState.readValue(derefedListElem, pfo, pointerSizeInBits, null);
        assertThat(ptrToPrevAndStates).hasSize(i > listLength ? 2 : 1);
        ValueAndSMGState matPtrToAbstrAndState = ptrToPrevAndStates.get(0);
        currentState = matPtrToAbstrAndState.getState();

        Value prevPtr = matPtrToAbstrAndState.getValue();
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedPrevListElems =
            currentState.dereferencePointer(prevPtr);
        assertThat(derefedPrevListElems).hasSize(1);
        currentState = derefedPrevListElems.get(0).getSMGState();
        derefedListElem = derefedPrevListElems.get(0).getSMGObject();
        Value targetOffset = derefedPrevListElems.get(0).getOffsetForObject();

        if (i > listLength) {
          // 0+ case
          assertThat(derefedListElem).isEqualTo(nonAbstrListElem);
          assertThat(targetOffset.asNumericValue().bigIntegerValue())
              .isEqualTo(prevPtrTargetOffset);

          // Get extension case
          matPtrToAbstrAndState = ptrToPrevAndStates.get(1);
          currentState = matPtrToAbstrAndState.getState();

          prevPtr = matPtrToAbstrAndState.getValue();
          derefedPrevListElems = currentState.dereferencePointer(prevPtr);
          assertThat(derefedPrevListElems).hasSize(1);
          currentState = derefedPrevListElems.get(0).getSMGState();
          targetOffset = derefedPrevListElems.get(0).getOffsetForObject();
          assertThat(
                  derefedPrevListElems
                      .get(0)
                      .getOffsetForObject()
                      .asNumericValue()
                      .bigIntegerValue())
              .isEqualTo(nextPtrTargetOffset);
          derefedListElem = derefedPrevListElems.get(0).getSMGObject();
          assertThat(derefedListElem).isNotEqualTo(nonAbstrListElem);
        }
        assertThat(targetOffset.asNumericValue().bigIntegerValue()).isEqualTo(prevPtrTargetOffset);
      }

      // Check correct value in offset 0
      List<ValueAndSMGState> readConstAndStateList =
          currentState.readValue(derefedListElem, BigInteger.ZERO, pointerSizeInBits, null);
      assertThat(readConstAndStateList).hasSize(1);
      if (i == 0) {
        assertThat(readConstAndStateList.get(0).getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
      } else {
        assertThat(readConstAndStateList.get(0).getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ONE);
      }

      // Check prev and next w/o mat
      ValueAndSMGState readNextNoMatAndStateList =
          currentState.readValueWithoutMaterialization(
              derefedListElem, nfo, pointerSizeInBits, null);
      currentState = readNextNoMatAndStateList.getState();
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObjWOMatAndState =
          currentState.dereferencePointerWithoutMaterilization(
              readNextNoMatAndStateList.getValue());
      assertThat(maybeNextObjWOMatAndState).isPresent();
      currentState = maybeNextObjWOMatAndState.orElseThrow().getSMGState();
      SMGObject nextObjWOMat = maybeNextObjWOMatAndState.orElseThrow().getSMGObject();
      assertThat(
              maybeNextObjWOMatAndState
                  .orElseThrow()
                  .getOffsetForObject()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(nextPtrTargetOffset);
      currentState = maybeNextObjWOMatAndState.orElseThrow().getSMGState();
      if (i == 0) {
        assertThat(nextObjWOMat).isInstanceOf(SMGDoublyLinkedListSegment.class);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(
                        currentState
                            .getMemoryModel()
                            .getSMGValueFromValue(readNextNoMatAndStateList.getValue())
                            .orElseThrow())
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      } else {
        assertThat(nextObjWOMat).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(
                        currentState
                            .getMemoryModel()
                            .getSMGValueFromValue(readNextNoMatAndStateList.getValue())
                            .orElseThrow())
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(nextObjWOMat).isEqualTo(previousConcreteListSegment);
      }

      ValueAndSMGState readPrevNoMatAndStateList =
          currentState.readValueWithoutMaterialization(
              derefedListElem, pfo, pointerSizeInBits, null);
      currentState = readPrevNoMatAndStateList.getState();
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybePrevObjWOMatAndState =
          currentState.dereferencePointerWithoutMaterilization(
              readPrevNoMatAndStateList.getValue());
      assertThat(maybePrevObjWOMatAndState).isPresent();
      currentState = maybePrevObjWOMatAndState.orElseThrow().getSMGState();
      SMGObject prevObjWOMat = maybePrevObjWOMatAndState.orElseThrow().getSMGObject();
      assertThat(
              maybePrevObjWOMatAndState
                  .orElseThrow()
                  .getOffsetForObject()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(nextPtrTargetOffset);
      currentState = maybePrevObjWOMatAndState.orElseThrow().getSMGState();
      if (i == 0) {
        assertThat(prevObjWOMat).isEqualTo(nextObjWOMat);
      }
      assertThat(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(
                      currentState
                          .getMemoryModel()
                          .getSMGValueFromValue(readPrevNoMatAndStateList.getValue())
                          .orElseThrow())
                  .orElseThrow()
                  .targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);

      if (i >= listLength) {
        // 0+
        assertThat(((SMGSinglyLinkedListSegment) prevObjWOMat).getMinLength()).isEqualTo(0);
      }

      previousConcreteListSegment = derefedListElem;
    }
  }

  // Normal SLL top list with 1 stack region that has a ptr to some list element.
  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // nested in each top list (abstractable for top)
  // We materialize the first pointer until 0+ materialization has occurred at least once
  // This test materializes only by dereference of pointers.
  @Ignore
  @Test
  public void sllWithPointerOffsetsNestedFstMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    Value[] topListPtrs = buildConcreteList(false, sllSize, listLength, false);

    for (int all = 0; all < listLength; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              false,
              sllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.empty(),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs[all]);
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Ptr to the top list from some stack obj
    // We want to test from left to right until 0+ has been materialized at least once
    SMGObjectAndSMGState stackObjAndState =
        currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
    currentState = stackObjAndState.getState();
    SMGObject stackObj = stackObjAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    Value nextPtr = topListPtrs[0];
    // Now deref each top list obj until we are repeating 0+
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      // We read in such a way that only the deref here materializes
      List<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElems =
          currentState.dereferencePointer(nextPtr);
      SMGObject nextListObj;
      if (all >= listLength) {
        // 0+
        assertThat(derefedTopElems).hasSize(2);
        SMGStateAndOptionalSMGObjectAndOffset derefedEndingList = derefedTopElems.get(0);
        currentState = derefedEndingList.getSMGState();
        assertThat(derefedEndingList.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedEndingList.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedEndingList.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        SMGObject endingObj = derefedEndingList.getSMGObject();
        assertThat(endingObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(endingObj.isZero()).isTrue();

        SMGStateAndOptionalSMGObjectAndOffset derefedZeroPlusCase = derefedTopElems.get(1);
        currentState = derefedZeroPlusCase.getSMGState();
        assertThat(derefedZeroPlusCase.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedZeroPlusCase.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedZeroPlusCase.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        SMGObject objPointingToZeroPlus = derefedZeroPlusCase.getSMGObject();
        nextListObj = objPointingToZeroPlus;

      } else {
        assertThat(derefedTopElems).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset derefedList = derefedTopElems.get(0);
        currentState = derefedList.getSMGState();
        assertThat(derefedList.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedList.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedList.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        nextListObj = derefedList.getSMGObject();
      }

      ValueAndSMGState readNext =
          currentState.readValueWithoutMaterialization(nextListObj, nfo, pointerSizeInBits, null);
      assertThat(readNext.getValue().isNumericValue()).isFalse();
      currentState = readNext.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNext.getValue())).isTrue();
      Optional<SMGPointsToEdge> pte =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readNext.getValue())
                      .orElseThrow());
      assertThat(pte).isPresent();
      assertThat(pte.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      assertThat(pte.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      SMGObject abstractedObj = pte.orElseThrow().pointsTo();
      assertThat(abstractedObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(abstractedObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getMinLength())
          .isEqualTo(Integer.max(0, listLength - all - 1));
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextPointerTargetOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(abstractedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getHeadOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextOffset()).isEqualTo(nfo);

      assertThat(nextListObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      ValueAndSMGState readNested =
          currentState.readValueWithoutMaterialization(nextListObj, hfo, pointerSizeInBits, null);
      currentState = readNested.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNested.getValue())).isTrue();

      // Check nested abstracted list
      checkAbstractionOfLLWithConcreteFirstAndLast(
          false, listLength, sllSize, readNested.getValue(), otherPtrOffset, internalListPtrOffset);

      nextPtr = readNext.getValue();
    }
  }

  // Normal SLL top list with 1 stack region that has a ptr to some list element.
  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // nested in each top list (abstractable for top)
  // We materialize the first pointer until 0+ materialization has occurred at least once
  // This test materializes by read and deref.
  @Ignore
  @Test
  public void sllWithPointerOffsetsNestedFstMaterializationTest2()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    Value[] topListPtrs = buildConcreteList(false, sllSize, listLength, false);

    for (int all = 0; all < listLength; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              false,
              sllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.empty(),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs[all]);
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Ptr to the top list from some stack obj
    // We want to test from left to right until 0+ has been materialized at least once
    SMGObjectAndSMGState stackObjAndState =
        currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
    currentState = stackObjAndState.getState();
    SMGObject stackObj = stackObjAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    // This read does mat the next obj
    List<ValueAndSMGState> readStackNextPtrAndState =
        currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
    assertThat(readStackNextPtrAndState).hasSize(1);
    currentState = readStackNextPtrAndState.get(0).getState();

    Value nextPtr = readStackNextPtrAndState.get(0).getValue();
    assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
    Optional<SMGPointsToEdge> pte =
        currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
    assertThat(pte).isPresent();
    assertThat(pte.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    assertThat(pte.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
    SMGObject materializedObj = pte.orElseThrow().pointsTo();
    assertThat(materializedObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(materializedObj.getOffset()).isEqualTo(BigInteger.ZERO);

    // Now read/deref each top list obj until we are repeating 0+
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      List<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElems =
          currentState.dereferencePointer(nextPtr);
      SMGObject nextListObj;

      // deref is not materializing, the read of the ptr is
      assertThat(derefedTopElems).hasSize(1);
      SMGStateAndOptionalSMGObjectAndOffset derefedList = derefedTopElems.get(0);
      currentState = derefedList.getSMGState();
      assertThat(derefedList.hasSMGObjectAndOffset()).isTrue();
      assertThat(derefedList.getOffsetForObject().isNumericValue()).isTrue();
      assertThat(derefedList.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      nextListObj = derefedList.getSMGObject();

      // This read materializes the target of the pointer read
      List<ValueAndSMGState> readNexts =
          currentState.readValue(nextListObj, nfo, pointerSizeInBits, null);
      ValueAndSMGState readNext;
      if (all >= listLength - 1) {
        assertThat(readNexts).hasSize(2);
        // ending 0+
        readNext = readNexts.get(0);
        assertThat(readNext.getValue().isNumericValue()).isTrue();
        assertThat(readNext.getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);

        readNext = readNexts.get(1);
      } else {
        assertThat(readNexts).hasSize(1);
        readNext = readNexts.get(0);
        assertThat(readNext.getValue().isNumericValue()).isFalse();
      }
      currentState = readNext.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNext.getValue())).isTrue();
      pte =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readNext.getValue())
                      .orElseThrow());
      assertThat(pte).isPresent();
      assertThat(pte.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      assertThat(pte.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      materializedObj = pte.orElseThrow().pointsTo();
      assertThat(materializedObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(materializedObj.getSize()).isEqualTo(sllSizeValue);

      // Look ahead into the abstracted obj
      ValueAndSMGState abstractedObjPointer =
          currentState.readValueWithoutMaterialization(
              materializedObj, nfo, pointerSizeInBits, null);
      currentState = abstractedObjPointer.getState();
      Value abstractedObjPtr = abstractedObjPointer.getValue();
      assertThat(currentState.getMemoryModel().isPointer(abstractedObjPtr)).isTrue();

      SMGStateAndOptionalSMGObjectAndOffset ptrToAbstrDeref =
          currentState.dereferencePointerWithoutMaterilization(abstractedObjPtr).orElseThrow();
      assertThat(ptrToAbstrDeref.hasSMGObjectAndOffset()).isTrue();
      assertThat(ptrToAbstrDeref.getOffsetForObject().isNumericValue()).isTrue();
      assertThat(ptrToAbstrDeref.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      SMGObject abstractedObj = ptrToAbstrDeref.getSMGObject();
      assertThat(abstractedObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(abstractedObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getMinLength())
          .isEqualTo(Integer.max(0, listLength - all - 2));
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextPointerTargetOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(abstractedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getHeadOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextOffset()).isEqualTo(nfo);

      assertThat(nextListObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      List<ValueAndSMGState> readNested =
          currentState.readValue(nextListObj, hfo, pointerSizeInBits, null);
      assertThat(readNested).hasSize(1);
      currentState = readNested.get(0).getState();
      assertThat(currentState.getMemoryModel().isPointer(readNested.get(0).getValue())).isTrue();

      // Check nested abstracted list
      checkAbstractionOfLLWithConcreteFirstAndLast(
          false,
          listLength,
          sllSize,
          readNested.get(0).getValue(),
          otherPtrOffset,
          internalListPtrOffset);

      nextPtr = readNext.getValue();
    }
  }

  // Normal DLL top list with 1 stack region that has a ptr to some list element.
  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // nested in each top list (abstractable for top)
  // We materialize the first pointer until 0+ materialization has occurred at least once
  // This test materializes only by dereference of pointers.
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithPointerOffsetsNestedFstMaterializationTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    Value[] topListPtrs = buildConcreteList(true, dllSize, listLength, false);

    for (int all = 0; all < listLength; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              true,
              dllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.of(internalListPtrOffset),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs[all]);
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Ptr to the top list from some stack obj
    // We want to test from left to right until 0+ has been materialized at least once
    SMGObjectAndSMGState stackObjAndState =
        currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
    currentState = stackObjAndState.getState();
    SMGObject stackObj = stackObjAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    Value nextPtr = topListPtrs[0];
    // Now deref each top list obj until we are repeating 0+
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      // We read in such a way that only the deref here materializes
      List<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElems =
          currentState.dereferencePointer(nextPtr);
      SMGObject nextListObj;
      if (all >= listLength) {
        // 0+
        assertThat(derefedTopElems).hasSize(2);
        SMGStateAndOptionalSMGObjectAndOffset derefedEndingList = derefedTopElems.get(0);
        currentState = derefedEndingList.getSMGState();
        assertThat(derefedEndingList.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedEndingList.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedEndingList.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        SMGObject endingObj = derefedEndingList.getSMGObject();
        assertThat(endingObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(endingObj.isZero()).isTrue();

        SMGStateAndOptionalSMGObjectAndOffset derefedZeroPlusCase = derefedTopElems.get(1);
        currentState = derefedZeroPlusCase.getSMGState();
        assertThat(derefedZeroPlusCase.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedZeroPlusCase.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedZeroPlusCase.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        SMGObject objPointingToZeroPlus = derefedZeroPlusCase.getSMGObject();
        nextListObj = objPointingToZeroPlus;

      } else {
        assertThat(derefedTopElems).hasSize(1);
        SMGStateAndOptionalSMGObjectAndOffset derefedList = derefedTopElems.get(0);
        currentState = derefedList.getSMGState();
        assertThat(derefedList.hasSMGObjectAndOffset()).isTrue();
        assertThat(derefedList.getOffsetForObject().isNumericValue()).isTrue();
        assertThat(derefedList.getOffsetForObject().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        nextListObj = derefedList.getSMGObject();
      }

      ValueAndSMGState readNext =
          currentState.readValueWithoutMaterialization(nextListObj, nfo, pointerSizeInBits, null);
      assertThat(readNext.getValue().isNumericValue()).isFalse();
      currentState = readNext.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNext.getValue())).isTrue();
      Optional<SMGPointsToEdge> pte =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readNext.getValue())
                      .orElseThrow());
      assertThat(pte).isPresent();
      assertThat(pte.orElseThrow().getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(pte.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      SMGObject abstractedObj = pte.orElseThrow().pointsTo();
      assertThat(abstractedObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGDoublyLinkedListSegment) abstractedObj).getMinLength())
          .isEqualTo(Integer.max(0, listLength - all - 1));
      assertThat(((SMGDoublyLinkedListSegment) abstractedObj).getNextPointerTargetOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(abstractedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(((SMGDoublyLinkedListSegment) abstractedObj).getHeadOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(((SMGDoublyLinkedListSegment) abstractedObj).getNextOffset()).isEqualTo(nfo);
      // Prev check
      ValueAndSMGState readPrevOfAbstr =
          currentState.readValueWithoutMaterialization(
              pte.orElseThrow().pointsTo(), pfo, pointerSizeInBits, null);
      assertThat(readPrevOfAbstr.getValue().isNumericValue()).isFalse();
      currentState = readPrevOfAbstr.getState();
      assertThat(currentState.getMemoryModel().isPointer(readPrevOfAbstr.getValue())).isTrue();
      Optional<SMGPointsToEdge> ptePrevAbstr =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readPrevOfAbstr.getValue())
                      .orElseThrow());
      assertThat(ptePrevAbstr).isPresent();
      assertThat(ptePrevAbstr.orElseThrow().pointsTo()).isEqualTo(nextListObj);
      assertThat(ptePrevAbstr.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(ptePrevAbstr.orElseThrow().getOffset()).isEqualTo(BigInteger.ZERO);

      assertThat(nextListObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      ValueAndSMGState readNested =
          currentState.readValueWithoutMaterialization(nextListObj, hfo, pointerSizeInBits, null);
      currentState = readNested.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNested.getValue())).isTrue();

      // Check nested abstracted list
      checkAbstractionOfLLWithConcreteFirstAndLast(
          true, listLength, dllSize, readNested.getValue(), otherPtrOffset, internalListPtrOffset);

      nextPtr = readNext.getValue();
    }
    Preconditions.checkNotNull(nextPtr);
  }

  // Normal DLL top list with 1 stack region that has a ptr to some list element.
  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // nested in each top list (abstractable for top)
  // We materialize the first pointer until 0+ materialization has occurred at least once
  // This test materializes by read and deref.
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllWithPointerOffsetsNestedFstMaterializationTest2()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    Value[] topListPtrs = buildConcreteList(true, dllSize, listLength, false);

    for (int all = 0; all < listLength; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              true,
              dllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.of(internalListPtrOffset),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs[all]);
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Ptr to the top list from some stack obj
    // We want to test from left to right until 0+ has been materialized at least once
    SMGObjectAndSMGState stackObjAndState =
        currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
    currentState = stackObjAndState.getState();
    SMGObject stackObj = stackObjAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(topListPtrs[0]).orElseThrow());

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    // This read does mat the next obj
    List<ValueAndSMGState> readStackNextPtrAndState =
        currentState.readValue(stackObj, BigInteger.ZERO, pointerSizeInBits, null);
    assertThat(readStackNextPtrAndState).hasSize(1);
    currentState = readStackNextPtrAndState.get(0).getState();

    Value nextPtr = readStackNextPtrAndState.get(0).getValue();
    assertThat(currentState.getMemoryModel().isPointer(nextPtr)).isTrue();
    Optional<SMGPointsToEdge> pte =
        currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
    assertThat(pte).isPresent();
    assertThat(pte.orElseThrow().getOffset()).isEqualTo(BigInteger.ZERO);
    assertThat(pte.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
    SMGObject materializedObj = pte.orElseThrow().pointsTo();
    assertThat(materializedObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(materializedObj.getOffset()).isEqualTo(BigInteger.ZERO);

    // Now read/deref each top list obj until we are repeating 0+
    for (int all = 0; all < listLength + 2; all++) {
      // Check abstracted nested based on the pointers of top
      List<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElems =
          currentState.dereferencePointer(nextPtr);
      SMGObject nextListObj;

      // deref is not materializing, the read of the ptr is
      assertThat(derefedTopElems).hasSize(1);
      SMGStateAndOptionalSMGObjectAndOffset derefedList = derefedTopElems.get(0);
      currentState = derefedList.getSMGState();
      assertThat(derefedList.hasSMGObjectAndOffset()).isTrue();
      assertThat(derefedList.getOffsetForObject().isNumericValue()).isTrue();
      assertThat(derefedList.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      nextListObj = derefedList.getSMGObject();

      // This read materializes the target of the pointer read
      List<ValueAndSMGState> readNexts =
          currentState.readValue(nextListObj, nfo, pointerSizeInBits, null);
      ValueAndSMGState readNext;
      if (all >= listLength - 1) {
        assertThat(readNexts).hasSize(2);
        // ending 0+
        readNext = readNexts.get(0);
        assertThat(readNext.getValue().isNumericValue()).isTrue();
        assertThat(readNext.getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);

        readNext = readNexts.get(1);
      } else {
        assertThat(readNexts).hasSize(1);
        readNext = readNexts.get(0);
        assertThat(readNext.getValue().isNumericValue()).isFalse();
      }
      currentState = readNext.getState();
      assertThat(currentState.getMemoryModel().isPointer(readNext.getValue())).isTrue();
      pte =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readNext.getValue())
                      .orElseThrow());
      assertThat(pte).isPresent();
      assertThat(pte.orElseThrow().getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(pte.orElseThrow().targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
      materializedObj = pte.orElseThrow().pointsTo();
      assertThat(materializedObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(materializedObj.getSize()).isEqualTo(dllSizeValue);

      // Look ahead into the abstracted obj
      ValueAndSMGState abstractedObjPointer =
          currentState.readValueWithoutMaterialization(
              materializedObj, nfo, pointerSizeInBits, null);
      currentState = abstractedObjPointer.getState();
      Value abstractedObjPtr = abstractedObjPointer.getValue();
      assertThat(currentState.getMemoryModel().isPointer(abstractedObjPtr)).isTrue();

      SMGStateAndOptionalSMGObjectAndOffset ptrToAbstrDeref =
          currentState.dereferencePointerWithoutMaterilization(abstractedObjPtr).orElseThrow();
      assertThat(ptrToAbstrDeref.hasSMGObjectAndOffset()).isTrue();
      assertThat(ptrToAbstrDeref.getOffsetForObject().isNumericValue()).isTrue();
      assertThat(ptrToAbstrDeref.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      SMGObject abstractedObj = ptrToAbstrDeref.getSMGObject();
      assertThat(abstractedObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getMinLength())
          .isEqualTo(Integer.max(0, listLength - all - 2));
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextPointerTargetOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(abstractedObj.getOffset()).isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getHeadOffset())
          .isEqualTo(BigInteger.ZERO);
      assertThat(((SMGSinglyLinkedListSegment) abstractedObj).getNextOffset()).isEqualTo(nfo);
      // Prev check
      ValueAndSMGState readPrevOfAbstr =
          currentState.readValueWithoutMaterialization(abstractedObj, pfo, pointerSizeInBits, null);
      assertThat(readPrevOfAbstr.getValue().isNumericValue()).isFalse();
      assertThat(currentState.getMemoryModel().getNestingLevel(readPrevOfAbstr.getValue()))
          .isEqualTo(0);
      currentState = readPrevOfAbstr.getState();
      assertThat(currentState.getMemoryModel().isPointer(readPrevOfAbstr.getValue())).isTrue();
      Optional<SMGPointsToEdge> ptePrevAbstr =
          currentState
              .getMemoryModel()
              .getSmg()
              .getPTEdge(
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(readPrevOfAbstr.getValue())
                      .orElseThrow());
      assertThat(ptePrevAbstr).isPresent();
      assertThat(ptePrevAbstr.orElseThrow().pointsTo()).isEqualTo(materializedObj);
      assertThat(ptePrevAbstr.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(ptePrevAbstr.orElseThrow().getOffset()).isEqualTo(BigInteger.ZERO);

      assertThat(nextListObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      List<ValueAndSMGState> readNested =
          currentState.readValue(nextListObj, hfo, pointerSizeInBits, null);
      assertThat(readNested).hasSize(1);
      currentState = readNested.get(0).getState();
      assertThat(currentState.getMemoryModel().isPointer(readNested.get(0).getValue())).isTrue();

      // Check nested abstracted list
      checkAbstractionOfLLWithConcreteFirstAndLast(
          true,
          listLength,
          dllSize,
          readNested.get(0).getValue(),
          otherPtrOffset,
          internalListPtrOffset);

      nextPtr = readNext.getValue();
    }
  }

  /*
   * List with a concrete, non abstractable element in the beginning and end, and then nest the
   * same structure. Stack objs pointing at only 1 top element.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllAbstractionNestingTest2() throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            true,
            dllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.of(internalListPtrOffset),
            false);

    SMGState stateBeforeStackObj = currentState;
    for (Value topListPtr : topListPtrs) {
      SMGObjectAndSMGState stackObjAndState =
          stateBeforeStackObj.copyAndAddStackObject(numericPointerSizeInBits);
      currentState = stackObjAndState.getState();
      SMGObject dummyStackObject = stackObjAndState.getSMGObject();
      currentState =
          currentState.writeValueWithChecks(
              dummyStackObject,
              new NumericValue(BigInteger.ZERO),
              numericPointerSizeInBits,
              topListPtr,
              null,
              dummyCDAEdge);

      for (int all = 0; all < listLength + 2; all++) {
        // put in abstractable nested in all
        List<Value> nestedListPtrs =
            buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
                true,
                dllSize,
                listLength + 2,
                otherPtrOffset,
                internalListPtrOffset,
                Optional.of(internalListPtrOffset),
                false);
        // Now write the first pointer into the top elem
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
            currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
        // State does not change because of no mat
        currentState = derefedTopElem.orElseThrow().getSMGState();
        SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                topObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(nestedListPtrs.get(0))
                    .orElseThrow());
      }

      // Abstract
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
      List<Set<SMGCandidate>> orderedListCandidatesByNesting = absFinder.getListCandidates();
      // There are 2 nesting levels, top and nested
      assertThat(orderedListCandidatesByNesting).hasSize(2);
      // The top list is level 0, so are the 2 nested lists with no abstracted top elements
      assertThat(orderedListCandidatesByNesting.get(0)).hasSize(3);
      assertThat(orderedListCandidatesByNesting.get(1)).hasSize(8);
    }
  }

  /*
   * List with a concrete, non abstractable element in the beginning and end, and then nest the
   * same structure. Stack objs pointing at every top list element.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void dllAbstractionNestingTest() throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            true,
            dllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.of(internalListPtrOffset),
            true);

    for (int all = 0; all < listLength + 2; all++) {
      // put in abstractable nested in all
      List<Value> nestedListPtrs =
          buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
              true,
              dllSize,
              listLength + 2,
              otherPtrOffset,
              internalListPtrOffset,
              Optional.of(internalListPtrOffset),
              false);
      // Now write the first pointer into the top elem
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
          currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
      // State does not change because of no mat
      currentState = derefedTopElem.orElseThrow().getSMGState();
      SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topObj,
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(nestedListPtrs.get(0))
                  .orElseThrow());
    }

    // Abstract
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    List<Set<SMGCandidate>> orderedListCandidatesByNesting = absFinder.getListCandidates();
    // There are 2 nesting levels, top and nested
    assertThat(orderedListCandidatesByNesting).hasSize(2);
    // The top list is level 0, so are the 2 nested lists with no abstracted top elements
    assertThat(orderedListCandidatesByNesting.get(0)).hasSize(3);
    assertThat(orderedListCandidatesByNesting.get(1)).hasSize(8);
  }

  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // Then nest the same in the top SLL with differing length (non abstractable for top)
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithPointerOffsetsNestedNoAbstractionTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            false,
            sllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.empty(),
            true);

    SMGState stateWoNested = currentState;
    for (int rolling = 0; rolling < listLength + 2; rolling++) {
      for (int all = 0; all < listLength + 2; all++) {
        // put in nested in all, but 1 (rolling) is not abstractable
        List<Value> nestedListPtrs =
            buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
                false,
                sllSize,
                rolling == all ? (listLength / 2) + 2 : listLength + 2,
                otherPtrOffset,
                internalListPtrOffset,
                Optional.empty(),
                false);

        // Now write the first pointer into the top elem
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
            currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
        // State does not change because of no mat
        currentState = derefedTopElem.orElseThrow().getSMGState();
        SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                topObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(nestedListPtrs.get(0))
                    .orElseThrow());
      }

      // Abstract
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      for (int all = 0; all < listLength + 2; all++) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
            currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
        // State does not change because of no mat
        currentState = derefedTopElem.orElseThrow().getSMGState();
        SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
        // Read the correct top ptr
        SMGValue ptrToNested =
            currentState.readSMGValue(topObj, hfo, pointerSizeInBits).getSMGValue();
        assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToNested)).isPresent();
        // assert that the first is concrete
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNested)
                    .orElseThrow()
                    .getOffset())
            .isEqualTo(BigInteger.ZERO);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNested)
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);
        SMGObject firstConcreteNested =
            currentState.getMemoryModel().getSmg().getPTEdge(ptrToNested).orElseThrow().pointsTo();
        assertThat(firstConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        SMGValue ptrToMaybeAbstrNested =
            currentState.readSMGValue(firstConcreteNested, nfo, pointerSizeInBits).getSMGValue();
        assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToMaybeAbstrNested))
            .isPresent();
        if (all == rolling) {
          // Not abstracted, still concrete, but shorter
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .getOffset())
              .isEqualTo(BigInteger.ZERO);
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .targetSpecifier())
              .isEqualTo(SMGTargetSpecifier.IS_REGION);
          SMGObject nextConcreteNested =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(ptrToMaybeAbstrNested)
                  .orElseThrow()
                  .pointsTo();
          assertThat(nextConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

          for (int i = 2; i < listLength / 2 + 2; i++) {
            SMGValue ptrToNotAbstrNested =
                currentState.readSMGValue(nextConcreteNested, nfo, pointerSizeInBits).getSMGValue();
            assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToNotAbstrNested))
                .isPresent();
            if (i == listLength / 2 + 1) {
              assertThat(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .getPTEdge(ptrToNotAbstrNested)
                          .orElseThrow()
                          .getOffset())
                  .isEqualTo(otherPtrOffset);
            } else {
              assertThat(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .getPTEdge(ptrToNotAbstrNested)
                          .orElseThrow()
                          .getOffset())
                  .isEqualTo(internalListPtrOffset);
            }
            assertThat(
                    currentState
                        .getMemoryModel()
                        .getSmg()
                        .getPTEdge(ptrToNotAbstrNested)
                        .orElseThrow()
                        .targetSpecifier())
                .isEqualTo(SMGTargetSpecifier.IS_REGION);
            nextConcreteNested =
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNotAbstrNested)
                    .orElseThrow()
                    .pointsTo();
            assertThat(nextConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          }
          SMGValue lastNext =
              currentState.readSMGValue(nextConcreteNested, nfo, pointerSizeInBits).getSMGValue();
          assertThat(lastNext.isZero()).isTrue();

        } else {
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .getOffset())
              .isEqualTo(BigInteger.ZERO);
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .targetSpecifier())
              .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
          SMGObject abstrNested =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(ptrToMaybeAbstrNested)
                  .orElseThrow()
                  .pointsTo();
          assertThat(abstrNested).isInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(abstrNested).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
          assertThat(((SMGSinglyLinkedListSegment) abstrNested).getMinLength())
              .isEqualTo(listLength);
          assertThat(((SMGSinglyLinkedListSegment) abstrNested).getNextPointerTargetOffset())
              .isEqualTo(internalListPtrOffset);
        }
      }
      // Top list is not abstracted as well for rolling != 0 and != listlength + 1
      if (rolling != 0 && rolling != listLength + 1) {
        checkConcreteSLLWithDiffPtrOffsetsFirstAndLast(
            false, listLength, topListPtrs, otherPtrOffset, internalListPtrOffset);
      } else {
        checkAbstractionOfLLWithConcreteFirstAndLast(
            false, listLength, topListPtrs, otherPtrOffset, internalListPtrOffset);
      }
      currentState = stateWoNested;
    }
  }

  // SLL with pointer target offset 32 in the middle, 2 elements left and right w offset 0
  // Then nest the same in the top SLL with differing length but list threshold is length -1,
  // so abstractable for top as long as it's at the edges
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void sllWithPointerOffsetsNestedPartialAbstractionTest()
      throws SMGException, SMGSolverException {
    int listLength = 8;
    BigInteger otherPtrOffset = BigInteger.ZERO;
    BigInteger internalListPtrOffset = BigInteger.valueOf(32);
    List<Value> topListPtrs =
        buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
            false,
            sllSize,
            listLength + 2,
            otherPtrOffset,
            internalListPtrOffset,
            Optional.empty(),
            true);

    SMGState stateWoNested = currentState;
    for (int rolling = 0; rolling < listLength + 2; rolling++) {
      for (int all = 0; all < listLength + 2; all++) {
        // put in nested in all, but 1 (rolling) is not abstractable
        List<Value> nestedListPtrs =
            buildConcreteListWithDifferentPtrTargetOffsetsInEndAndBeginning(
                false,
                sllSize,
                rolling == all ? (listLength / 2) + 2 : listLength + 2,
                otherPtrOffset,
                internalListPtrOffset,
                Optional.empty(),
                false);

        // Now write the first pointer into the top elem
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
            currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
        // State does not change because of no mat
        currentState = derefedTopElem.orElseThrow().getSMGState();
        SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
        currentState =
            currentState.writeValueWithoutChecks(
                topObj,
                BigInteger.ZERO,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(nestedListPtrs.get(0))
                    .orElseThrow());
      }

      // Abstract
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      for (int all = 0; all < listLength + 2; all++) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedTopElem =
            currentState.dereferencePointerWithoutMaterilization(topListPtrs.get(all));
        // State does not change because of no mat
        currentState = derefedTopElem.orElseThrow().getSMGState();
        SMGObject topObj = derefedTopElem.orElseThrow().getSMGObject();
        // Linked lists exist for rolling == 0,1,listLength + 2 - 1,listLength+2-2
        // For the later 2 it's a window
        if (rolling == 0 || rolling == listLength + 1) {
          // LL for all from 1 to listLength
          if (all > 0 && all < listLength + 1) {
            assertThat(topObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
          } else {
            assertThat(topObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          }
        } else if (rolling == 1) {
          if (all > rolling && all < listLength + 1) {
            assertThat(topObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
          } else {
            assertThat(topObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          }
        } else if (rolling == listLength) {
          if (all > 0 && all < rolling) {
            assertThat(topObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
          } else {
            assertThat(topObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          }
        } else {
          assertThat(topObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        }
        // Read the correct top ptr
        SMGValue ptrToNested =
            currentState.readSMGValue(topObj, hfo, pointerSizeInBits).getSMGValue();
        assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToNested)).isPresent();
        // assert that the first is concrete
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNested)
                    .orElseThrow()
                    .getOffset())
            .isEqualTo(BigInteger.ZERO);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNested)
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);
        SMGObject firstConcreteNested =
            currentState.getMemoryModel().getSmg().getPTEdge(ptrToNested).orElseThrow().pointsTo();
        assertThat(firstConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        SMGValue ptrToMaybeAbstrNested =
            currentState.readSMGValue(firstConcreteNested, nfo, pointerSizeInBits).getSMGValue();
        assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToMaybeAbstrNested))
            .isPresent();
        if (all == rolling) {
          // Not abstracted, still concrete, but shorter
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .getOffset())
              .isEqualTo(BigInteger.ZERO);
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .targetSpecifier())
              .isEqualTo(SMGTargetSpecifier.IS_REGION);
          SMGObject nextConcreteNested =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(ptrToMaybeAbstrNested)
                  .orElseThrow()
                  .pointsTo();
          assertThat(nextConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

          for (int i = 2; i < listLength / 2 + 2; i++) {
            SMGValue ptrToNotAbstrNested =
                currentState.readSMGValue(nextConcreteNested, nfo, pointerSizeInBits).getSMGValue();
            assertThat(currentState.getMemoryModel().getSmg().getPTEdge(ptrToNotAbstrNested))
                .isPresent();
            if (i == listLength / 2 + 1) {
              assertThat(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .getPTEdge(ptrToNotAbstrNested)
                          .orElseThrow()
                          .getOffset())
                  .isEqualTo(otherPtrOffset);
            } else {
              assertThat(
                      currentState
                          .getMemoryModel()
                          .getSmg()
                          .getPTEdge(ptrToNotAbstrNested)
                          .orElseThrow()
                          .getOffset())
                  .isEqualTo(internalListPtrOffset);
            }
            assertThat(
                    currentState
                        .getMemoryModel()
                        .getSmg()
                        .getPTEdge(ptrToNotAbstrNested)
                        .orElseThrow()
                        .targetSpecifier())
                .isEqualTo(SMGTargetSpecifier.IS_REGION);
            nextConcreteNested =
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(ptrToNotAbstrNested)
                    .orElseThrow()
                    .pointsTo();
            assertThat(nextConcreteNested).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          }
          SMGValue lastNext =
              currentState.readSMGValue(nextConcreteNested, nfo, pointerSizeInBits).getSMGValue();
          assertThat(lastNext.isZero()).isTrue();

        } else {
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .getOffset())
              .isEqualTo(BigInteger.ZERO);
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getSmg()
                      .getPTEdge(ptrToMaybeAbstrNested)
                      .orElseThrow()
                      .targetSpecifier())
              .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
          SMGObject abstrNested =
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(ptrToMaybeAbstrNested)
                  .orElseThrow()
                  .pointsTo();
          assertThat(abstrNested).isInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(abstrNested).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
          assertThat(((SMGSinglyLinkedListSegment) abstrNested).getMinLength())
              .isEqualTo(listLength);
          assertThat(((SMGSinglyLinkedListSegment) abstrNested).getNextPointerTargetOffset())
              .isEqualTo(internalListPtrOffset);
        }
      }
      // Top list is not abstracted as well for rolling != 0 and != listlength + 1
      if (rolling != 0 && rolling != 1 && rolling != listLength && rolling != listLength + 1) {
        checkConcreteSLLWithDiffPtrOffsetsFirstAndLast(
            false, listLength, topListPtrs, otherPtrOffset, internalListPtrOffset);
      }
      currentState = stateWoNested;
    }
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The lists are
   * then abstracted and checked. This works if we correctly check equality by shape and not pointer
   * identity.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void nestedListSLLTest() throws SMGException, SMGSolverException {
    // Smaller lengths are fine here, else this runs a while!
    // Increasing this is a good test for the overall performance of the SMGs!
    int listLength = 15;
    Value[] pointers = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointers, false);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // 1 null obj + 1 top list + listLength nested
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2 + listLength);
    // Now we have abstracted all lists in the state, including the nested ones
    SMGObject abstractedTopListSegment = null;
    for (Value pointer : pointers) {
      SMGStateAndOptionalSMGObjectAndOffset topListSegmentAndState =
          currentState.dereferencePointerWithoutMaterilization(pointer).orElseThrow();
      currentState = topListSegmentAndState.getSMGState();
      SMGObject currentTopListSegment = topListSegmentAndState.getSMGObject();
      // This is now always the same abstracted object
      if (abstractedTopListSegment != null) {
        assertThat(abstractedTopListSegment).isEqualTo(currentTopListSegment);
        abstractedTopListSegment = currentTopListSegment;
      } else {
        abstractedTopListSegment = currentTopListSegment;
      }
      assertThat(currentTopListSegment).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(currentTopListSegment).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) currentTopListSegment).getMinLength())
          .isEqualTo(listLength);
    }
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The lists are
   * then abstracted and materialized. This test then checks that the materialized list equals the
   * abstracted list in its shape and values.
   */
  @Ignore
  @Test
  public void nestedListMaterializationSLLTest() throws SMGException, SMGSolverException {
    // TODO: nested lists are somehow broken
    // Smaller lengths are fine here, else this runs a while!
    // Increasing this is a good test for the overall performance of the SMGs!
    int listLength = 15;
    Value[] pointers = buildConcreteList(false, sllSize, listLength);
    Value[][] nestedPointers = addSubListsToList(listLength, pointers, false);
    // TODO: we can't handle rebuilding from pointers towards nested lists yet, so this is a dummy
    assertThat(nestedPointers).isNotEmpty();

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    // 1 null obj + 1 top list + listLength nested
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2 + listLength);
    // Now we have abstracted all lists in the state, including the nested ones and materialize
    // again
    // <= because we materialize an additional segment to check it
    boolean checked0Plus = false;
    List<SMGStateAndOptionalSMGObjectAndOffset> topListSegmentAndState = null;
    for (int i = 0; i <= listLength; i++) {
      if (i < listLength) {
        Value originalPointer = pointers[i];
        // topListSegmentAndState is materialized
        topListSegmentAndState = currentState.dereferencePointer(originalPointer);
        assertThat(topListSegmentAndState).hasSize(1);

        // only concrete segments
        assertThat(topListSegmentAndState).hasSize(1);
        assertThat(topListSegmentAndState.get(0).hasSMGObjectAndOffset()).isTrue();

        currentState = topListSegmentAndState.get(0).getSMGState();
        SMGObject materializedTopListSegment = topListSegmentAndState.get(0).getSMGObject();
        assertThat(
                topListSegmentAndState
                    .get(0)
                    .getOffsetForObject()
                    .asNumericValue()
                    .bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        // deref nested list and check it
        // TODO: check materialization through read instead of deref
        // TODO: check materialization through initial pointer used
        List<ValueAndSMGState> readPointersAndStates =
            currentState.readValue(materializedTopListSegment, hfo, pointerSizeInBits, null);
        // Always size 1 as it is always a length+ list that is materialized
        assertThat(readPointersAndStates).hasSize(1);
        ValueAndSMGState readPointerAndState = readPointersAndStates.get(0);
        Value pointerToNestedList = readPointerAndState.getValue();
        currentState = readPointerAndState.getState();
        // Make sure that we have a valid pointer
        assertThat(currentState.getMemoryModel().isPointer(pointerToNestedList)).isTrue();
        List<SMGStateAndOptionalSMGObjectAndOffset> nestedObjectsAndStates =
            currentState.dereferencePointer(pointerToNestedList);

        checked0Plus = false;
        for (int j = 0; j <= listLength; j++) {
          assertThat(nestedObjectsAndStates).hasSize(1);
          SMGStateAndOptionalSMGObjectAndOffset nestedListObjectAndState =
              nestedObjectsAndStates.get(0);
          SMGObject nestedListObject = nestedListObjectAndState.getSMGObject();
          currentState = nestedListObjectAndState.getSMGState();
          assertThat(
                  nestedListObjectAndState.getOffsetForObject().asNumericValue().bigIntegerValue())
              .isEqualTo(BigInteger.ZERO);
          // Check that it's not an abstracted object, correct size and payload value + next ptr
          assertThat(nestedListObject).isNotSameInstanceAs(SMGSinglyLinkedListSegment.class);
          assertThat(nestedListObject.getSize()).isEqualTo(sllSize);
          List<ValueAndSMGState> readHeadsOfNestedList =
              currentState.readValue(nestedListObject, hfo, pointerSizeInBits, null);
          assertThat(readHeadsOfNestedList).hasSize(1);
          ValueAndSMGState readHeadOfNestedList = readHeadsOfNestedList.get(0);
          Value headValue = readHeadOfNestedList.getValue();
          currentState = readHeadOfNestedList.getState();
          assertThat(headValue.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
          // read and deref the next pointer
          List<ValueAndSMGState> readNfoOfNestedList =
              currentState.readValue(nestedListObject, nfo, pointerSizeInBits, null);
          if (readNfoOfNestedList.size() == 2) {
            if (checked0Plus) {
              break;
            }
            // Check that one is 0, the other is valid again once, then stop
            ValueAndSMGState readNfoNestedList = readNfoOfNestedList.get(0);
            Value nfoValue = readNfoNestedList.getValue();
            assertThat(nfoValue.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);

            readNfoNestedList = readNfoOfNestedList.get(1);
            nfoValue = readNfoNestedList.getValue();
            currentState = readNfoNestedList.getState();
            nestedObjectsAndStates = currentState.dereferencePointer(nfoValue);
            checked0Plus = true;
            continue;
          }
          assertThat(readNfoOfNestedList).hasSize(1);
          ValueAndSMGState readNfoNestedList = readNfoOfNestedList.get(0);
          Value nfoValue = readNfoNestedList.getValue();
          currentState = readNfoNestedList.getState();
          nestedObjectsAndStates = currentState.dereferencePointer(nfoValue);
          // repeat loop
        }
        // Make sure we got a 0+
        // assertThat(checked0Plus).isTrue();
      } else {
        // last concrete segment with a read at nfo that results in a split of an 0+
        if (topListSegmentAndState == null) {
          // This can never happen, but our CI complains....
          throw new RuntimeException();
        }
        assertThat(topListSegmentAndState).hasSize(1);

        assertThat(topListSegmentAndState.get(0).hasSMGObjectAndOffset()).isTrue();
        currentState = topListSegmentAndState.get(0).getSMGState();
        List<ValueAndSMGState> readNextValueAndStates =
            currentState.readValue(
                topListSegmentAndState.get(0).getSMGObject(), nfo, pointerSizeInBits, null);
        // One is again the end, the other 0+
        assertThat(readNextValueAndStates).hasSize(2);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(readNextValueAndStates.get(0).getValue())
                    .orElseThrow()
                    .isZero())
            .isTrue();
        assertThat(readNextValueAndStates.get(0).getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);

        currentState = readNextValueAndStates.get(1).getState();
        assertThat(
                currentState.getMemoryModel().isPointer(readNextValueAndStates.get(1).getValue()))
            .isTrue();
        topListSegmentAndState =
            currentState.dereferencePointer(readNextValueAndStates.get(1).getValue());
      }
    }
    // We materialized some additional segments
    // assertThat(stateBeforeAbstraction.getMemoryModel().getHeapObjects().size()).isEqualTo(currentState.getMemoryModel().getHeapObjects().size());
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The top list is
   * then abstracted and checked. This works if we correctly check equality by shape and not pointer
   * identity.
   */
  @Test
  public void nestedListDLLTest() {
    resetSMGStateAndVisitor();
    // TODO:
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The nested
   * lists are then abstracted and checked. Following this the top list is abstracted and checked.
   * This works if we correctly check equality by shape and not pointer identity.
   */
  @Test
  public void nestedListAbstractionSLLTest() {
    resetSMGStateAndVisitor();
  }

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The nested
   * lists are then abstracted and checked. Following this the top list is abstracted and checked.
   * This works if we correctly check equality by shape and not pointer identity.
   */
  @Test
  public void nestedListAbstractionDLLTest() {
    resetSMGStateAndVisitor();
    // TODO:
  }

  /**
   * Tests that pointers are correctly nested in SLL segments and dereferencing them correctly
   * materializes the list up to that memory, and all pointers are still valid, correctly nested and
   * point to the correct segments.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void correctPointerNestingSLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(false, sllSize, lengthOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, lengthOfList - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Now we have a 10+SLS
    // Deref a pointer not in the beginning or end, check that the list is consistent with the
    // pointers and the nesting level and materialization is correct afterwards
    // Deref at position 2, 3, 5, 9 and 10 and check pointers
    // We leave some space to check behaviour for automatic moving on for later pointers!!!
    derefPointersAtAndCheckListMaterialization(
        lengthOfList, pointers, new int[] {1, 2, 4, 8, 9}, false);
  }

  /**
   * Tests that pointers are correctly nested in SLL segments and dereferencing them correctly
   * materializes the list up to that memory, and all pointers are still valid, correctly nested and
   * point to the correct segments.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void correctPointerNestingDLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(true, dllSize, lengthOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, lengthOfList - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Now we have a 10+SLS
    // Deref a pointer not in the beginning or end, check that the list is consistent with the
    // pointers and the nesting level and materialization is correct afterwards
    // Deref at position 2, 3, 5, 9 and 10 and check pointers
    // We leave some space to check behaviour for automatic moving on for later pointers!!!
    derefPointersAtAndCheckListMaterialization(
        lengthOfList, pointers, new int[] {1, 2, 4, 8, 9}, true);
  }

  /**
   * Test that a list is correctly materialized to 0+ in the end and then correctly reabsorbed to
   * the original abstracted list with all pointers being correctly nested and no extra segments or
   * states added.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void correctZeroPlusAbsorptionSLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    nfo = BigInteger.ZERO;
    sllSize = pointerSizeInBits;
    // We start with no data and add int size space each iteration for data, moving the nfo
    for (int i = 0; i < 3; i++) {
      resetSMGStateAndVisitor();
      Value[] pointers = buildConcreteList(false, sllSize, lengthOfList);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, lengthOfList - 1, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      // Now we have a 10+SLS
      // Deref a pointer not in the beginning or end, check that the list is consistent with the
      // pointers and the nesting level and materialization is correct afterwards
      derefPointersAtAndCheckListMaterialization(
          lengthOfList, pointers, new int[] {lengthOfList - 2, lengthOfList - 1}, false);
      // Now only the 0+ trails, re-merge
      currentState = absFinder.findAndAbstractLists();
      // Now there should be only 1 non-zero valid object left that is a 10+ list (and 1 null obj)
      assertAbstractedList(pointers, lengthOfList, false);
      // Increase num of data and position of nfo
      nfo = nfo.add(pointerSizeInBits);
      sllSize = sllSize.add(pointerSizeInBits);
    }
  }

  /**
   * Test that a list is correctly materialized to 0+ in the end and then a 0+ is added to the left,
   * it is then correctly reabsorbed to the original abstracted list with all pointers being
   * correctly nested and no extra segments or states added.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void correctLeftZeroPlusAbsorptionDLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    nfo = BigInteger.ZERO;
    pfo = nfo.add(pointerSizeInBits);
    dllSize = pointerSizeInBits.multiply(BigInteger.TWO);
    dllSizeValue = new NumericValue(dllSize);
    // We start with no data and add int size space each iteration for data, moving the nfo
    for (int i = 0; i < 3; i++) {
      resetSMGStateAndVisitor();
      Value[] pointers = buildConcreteList(true, dllSize, lengthOfList);
      // Now add a 0+ left
      SMGStateAndOptionalSMGObjectAndOffset leftMostConcreteObjAndState =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = leftMostConcreteObjAndState.getSMGState();
      SMGObject leftMostConcreteObj = leftMostConcreteObjAndState.getSMGObject();
      ValueAndSMGState pointerToLeftmostConcreteAndState =
          currentState.searchOrCreateAddress(
              leftMostConcreteObj, CPointerType.POINTER_TO_VOID, BigInteger.ZERO);
      currentState = pointerToLeftmostConcreteAndState.getState();
      Value pointerToLeftMostConcrete = pointerToLeftmostConcreteAndState.getValue();
      SMGDoublyLinkedListSegment newDLLSegment =
          new SMGDoublyLinkedListSegment(
              0, dllSizeValue, BigInteger.ZERO, hfo, nfo, BigInteger.ZERO, pfo, BigInteger.ZERO, 0);
      ValueAndSMGState pointerToLeftmostZeroPlusAndState =
          currentState.searchOrCreateAddress(
              newDLLSegment,
              CPointerType.POINTER_TO_VOID,
              BigInteger.ZERO,
              0,
              SMGTargetSpecifier.IS_LAST_POINTER);
      currentState = pointerToLeftmostZeroPlusAndState.getState();
      Value pointerToLeftMostZeroPlus = pointerToLeftmostZeroPlusAndState.getValue();
      // Write the pointer to the leftmost concrete to the new 0+
      SymbolicProgramConfiguration newMemModel =
          currentState
              .getMemoryModel()
              .writeValue(
                  newDLLSegment,
                  nfo,
                  pointerSizeInBits,
                  currentState
                      .getMemoryModel()
                      .getSMGValueFromValue(pointerToLeftMostConcrete)
                      .orElseThrow());
      // Write the prev to 0 in the new 0+
      newMemModel =
          newMemModel.writeValue(newDLLSegment, pfo, pointerSizeInBits, SMGValue.zeroValue());
      // Write the prev of the concrete to the 0+
      newMemModel =
          newMemModel.writeValue(
              newDLLSegment,
              pfo,
              pointerSizeInBits,
              currentState
                  .getMemoryModel()
                  .getSMGValueFromValue(pointerToLeftMostZeroPlus)
                  .orElseThrow());
      currentState = currentState.copyAndReplaceMemoryModel(newMemModel);

      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, lengthOfList - 1, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      // Now we have a 10+DLS
      // Deref a pointer not in the beginning or end, check that the list is consistent with the
      // pointers and the nesting level and materialization is correct afterward
      derefPointersAtAndCheckListMaterialization(
          lengthOfList, pointers, new int[] {lengthOfList - 2, lengthOfList - 1}, true, true);
      // Now only the 0+ trails, re-merge
      currentState = absFinder.findAndAbstractLists();
      // Now there should be only 1 non-zero valid object left that is a 10+ list (and 1 null obj)
      assertAbstractedList(pointers, lengthOfList, true);
      // Data is intact
      checkListDataIntegrity(pointers, true);
      // Increase num of data and position of nfo
      nfo = nfo.add(pointerSizeInBits);
      dllSize = dllSize.add(pointerSizeInBits);
      dllSizeValue = new NumericValue(dllSize);
      pfo = nfo.add(pointerSizeInBits);
    }
  }

  /**
   * Test that a list is correctly materialized to 0+ in the end and then correctly reabsorbed to
   * the original abstracted list with all pointers being correctly nested and no extra segments or
   * states added.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void correctZeroPlusAbsorptionDLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    nfo = BigInteger.ZERO;
    pfo = nfo.add(pointerSizeInBits);
    dllSize = pointerSizeInBits.multiply(BigInteger.TWO);
    // We start with no data and add int size space each iteration for data, moving the nfo
    for (int i = 0; i < 3; i++) {
      resetSMGStateAndVisitor();
      Value[] pointers = buildConcreteList(true, dllSize, lengthOfList);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, lengthOfList - 1, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      // Now we have a 10+SLS
      // Deref a pointer not in the beginning or end, check that the list is consistent with the
      // pointers and the nesting level and materialization is correct afterwards
      derefPointersAtAndCheckListMaterialization(
          lengthOfList, pointers, new int[] {lengthOfList - 2}, true);
      // Now only the 0+ trails, re-merge
      currentState = absFinder.findAndAbstractLists();
      // Now there should be only 1 non-zero valid object left that is a 10+ list (and 1 null obj)
      assertAbstractedList(pointers, lengthOfList, true);
      // Data is intact
      checkListDataIntegrity(pointers, true);
      // Increase num of data and position of nfo
      nfo = nfo.add(pointerSizeInBits);
      dllSize = dllSize.add(pointerSizeInBits);
      pfo = nfo.add(pointerSizeInBits);
    }
  }

  /**
   * Asserts that the only valid existing object is an SLL or DLL equaling the length given and nfo
   * and pfo both being 0.
   *
   * @param pointers an array of all pointers to check that the properties hold.
   * @param lengthOfList length of the total list
   * @param isDll true if dll
   */
  private void assertAbstractedList(Value[] pointers, int lengthOfList, boolean isDll)
      throws SMGException {
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2);
    int numOfValidObjects = 0;
    for (SMGObject obj : currentState.getMemoryModel().getSmg().getObjects()) {
      // This includes invalid objects (null obj is invalid)!
      if (currentState.getMemoryModel().isObjectValid(obj)) {
        numOfValidObjects++;
        if (obj.getSize().asNumericValue().bigIntegerValue().equals(pointerSizeInBits)) {
          // Stack objects for ptrs
          // TODO: this also created pointerSize lists during the test
          // assertThat(currentState.getMemoryModel().isObjectValid(obj)).isTrue();
          continue;
        }
        assertThat(obj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(((SMGSinglyLinkedListSegment) obj).getMinLength()).isEqualTo(lengthOfList);
        // Assert that the next pointer has the correct value
        // next from last to 0+ is special as it is an added pointer
        // (We can deref any of the pointers here)
        SMGObject derefedAbstractedObj =
            currentState
                .dereferencePointerWithoutMaterilization(pointers[lengthOfList - 1])
                .orElseThrow()
                .getSMGObject();
        assertThat(obj).isEqualTo(derefedAbstractedObj);
        ValueAndSMGState readNfoWithoutMaterialization =
            currentState.readValueWithoutMaterialization(obj, nfo, pointerSizeInBits, null);
        currentState = readNfoWithoutMaterialization.getState();
        assertThat(readNfoWithoutMaterialization.getValue().isNumericValue()).isTrue();
        assertThat(readNfoWithoutMaterialization.getValue().asNumericValue().bigIntegerValue())
            .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
        if (isDll) {
          assertThat(obj).isInstanceOf(SMGDoublyLinkedListSegment.class);
          ValueAndSMGState readPfoWithoutMaterialization =
              currentState.readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null);
          currentState = readPfoWithoutMaterialization.getState();
          assertThat(readPfoWithoutMaterialization.getValue().isNumericValue()).isTrue();
          assertThat(readPfoWithoutMaterialization.getValue().asNumericValue().bigIntegerValue())
              .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
        }
      }
    }
    // 1 valid obj from the list + 10 stack objects
    assertThat(numOfValidObjects).isEqualTo(11);
  }

  /**
   * Tests behaviour for the read of a pointer pointing to a 0+ list segment for SLLs. Should split
   * into 2 states, one where the 0+ is deleted and the end of the list nfo value is read, one for
   * the extension of the list.
   *
   * @throws SMGException not thrown
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void zeroPlusRemovalSLLTest() throws SMGException, SMGSolverException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(false, sllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, sizeOfList, new SMGCPAStatistics());
    // Now we have a 3+SLS
    currentState = absFinder.findAndAbstractLists();
    assertThat(
            currentState.getMemoryModel().getSmg().getObjects().stream()
                .anyMatch(
                    o -> o instanceof SMGSinglyLinkedListSegment sll && sll.getMinLength() == 3))
        .isTrue();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    // Mat the second to last object, then use its next ptr for the last obj before 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndSecondToLastObject =
        currentState.dereferencePointer(pointers[sizeOfList - 2]);
    assertThat(statesAndSecondToLastObject).hasSize(1);
    currentState = statesAndSecondToLastObject.get(0).getSMGState();
    List<ValueAndSMGState> readNextPtrSToLast =
        currentState.readValue(
            statesAndSecondToLastObject.get(0).getSMGObject(), nfo, pointerSizeInBits, null);
    assertThat(readNextPtrSToLast).hasSize(1);
    currentState = readNextPtrSToLast.get(0).getState();
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(readNextPtrSToLast.get(0).getValue());
    assertThat(statesAndResultingObjects).hasSize(1);
    SMGStateAndOptionalSMGObjectAndOffset stateAndResultingObject =
        statesAndResultingObjects.get(0);
    currentState = stateAndResultingObject.getSMGState();
    SMGObject lastConcreteListObject = stateAndResultingObject.getSMGObject();
    assertThat(stateAndResultingObject.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
    // There may only be one 0+
    assertThat(
            currentState.getMemoryModel().getSmg().getObjects().stream()
                .filter(o -> o instanceof SMGSinglyLinkedListSegment sll && sll.getMinLength() == 0)
                .collect(ImmutableSet.toImmutableSet()))
        .hasSize(1);
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    checkNextPointsToZeroPlus(lastConcreteListObject, false);

    // Now read the next pointer to the 0+ object; this results in 2 states, one where the 0+
    // vanishes and one where we generate a new concrete list element and keep our 0+
    checkZeroPlusBehaviour(false, lastConcreteListObject, BigInteger.ZERO);
  }

  /**
   * Tests behaviour for the read of a pointer pointing to a 0+ list segment for DLLs. Should split
   * into 2 states, one where the 0+ is deleted and the end of the list nfo value is read, one for
   * the extension of the list.
   *
   * @throws SMGException not thrown
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void zeroPlusRemovalDLLTest() throws SMGException, SMGSolverException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(true, dllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, sizeOfList - 1, new SMGCPAStatistics());
    // Now we have a 3+DLS
    currentState = absFinder.findAndAbstractLists();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    // Mat the second to last object, then use its next ptr for the last obj before 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndSecondToLastObject =
        currentState.dereferencePointer(pointers[sizeOfList - 2]);
    assertThat(statesAndSecondToLastObject).hasSize(1);
    currentState = statesAndSecondToLastObject.get(0).getSMGState();
    List<ValueAndSMGState> readNextPtrSToLast =
        currentState.readValue(
            statesAndSecondToLastObject.get(0).getSMGObject(), nfo, pointerSizeInBits, null);
    assertThat(readNextPtrSToLast).hasSize(1);
    currentState = readNextPtrSToLast.get(0).getState();
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(readNextPtrSToLast.get(0).getValue());
    assertThat(statesAndResultingObjects).hasSize(1);
    SMGStateAndOptionalSMGObjectAndOffset stateAndResultingObject =
        statesAndResultingObjects.get(0);
    currentState = stateAndResultingObject.getSMGState();
    SMGObject lastConcreteListObject = stateAndResultingObject.getSMGObject();
    assertThat(stateAndResultingObject.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    checkNextPointsToZeroPlus(lastConcreteListObject, true);

    // Now read the next pointer to the 0+ object; this results in 2 states, one where the 0+
    // vanishes and one where we generate a new concrete list element and keep our 0+
    checkZeroPlusBehaviour(true, lastConcreteListObject, BigInteger.ZERO);
  }

  /**
   * Checks with a non dereferencing and not materializing read that nfo points to a 0+ segment. For
   * DLLs it confirms that the 0+ has a pfo that points to the concrete segment.
   *
   * @param lastConcreteListObject the last concrete segment who's nfo points to 0+.
   */
  private void checkNextPointsToZeroPlus(SMGObject lastConcreteListObject, boolean isDll)
      throws SMGException {
    SMGValueAndSMGState readValueAndState =
        currentState.readSMGValue(lastConcreteListObject, nfo, pointerSizeInBits);
    currentState = readValueAndState.getSMGState();
    SMGValue nextPointerValue = readValueAndState.getSMGValue();
    // Confirm that this is a pointer to a 0+ SLS
    assertThat(currentState.getMemoryModel().getSmg().isPointer(nextPointerValue)).isTrue();
    SMGPointsToEdge pointsToEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();
    assertThat(pointsToEdge.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(((SMGSinglyLinkedListSegment) pointsToEdge.pointsTo()).getMinLength()).isEqualTo(0);
    if (isDll) {
      SMGValueAndSMGState readPfoValueAndState =
          currentState.readSMGValue(pointsToEdge.pointsTo(), pfo, pointerSizeInBits);
      currentState = readPfoValueAndState.getSMGState();
      SMGValue prevPointerValue = readPfoValueAndState.getSMGValue();
      // Confirm that this is a pointer to a 0+ SLS
      assertThat(currentState.getMemoryModel().getSmg().isPointer(prevPointerValue)).isTrue();
      SMGPointsToEdge pointsToPrevConcreteEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
      assertThat(pointsToPrevConcreteEdge.pointsTo()).isEqualTo(lastConcreteListObject);
    }
  }

  /**
   * Checks that a proper read of the NFO pointing to 0+ generates 2 states. One with a value
   * expectedNfoValue, one with the extented list by 1.
   *
   * @param isDll true for DLLs. False else.
   * @param lastConcreteListObject the last concrete segment who's nfo points to 0+.
   * @param expectedNfoValue the value for the non extented read state.
   * @throws SMGException not thrown
   */
  private void checkZeroPlusBehaviour(
      boolean isDll, SMGObject lastConcreteListObject, BigInteger expectedNfoValue)
      throws SMGException {
    List<ValueAndSMGState> statesAndReadValueZeroPlus =
        currentState.readValue(lastConcreteListObject, nfo, pointerSizeInBits, null);
    assertThat(statesAndReadValueZeroPlus).hasSize(2);
    // The states are ordered, with the ending list being the first
    ValueAndSMGState firstReadValueAndState = statesAndReadValueZeroPlus.get(0);
    currentState = firstReadValueAndState.getState();
    assertThat(firstReadValueAndState.getValue().isNumericValue()).isTrue();
    assertThat(firstReadValueAndState.getValue().asNumericValue().bigIntegerValue())
        .isEquivalentAccordingToCompareTo(expectedNfoValue);

    // The second is the one with a new concrete segment and another 0+
    // Check that its a concrete region with a valid pointer to it
    ValueAndSMGState secondReadValueAndState = statesAndReadValueZeroPlus.get(1);
    currentState = secondReadValueAndState.getState();
    assertThat(secondReadValueAndState.getValue().isNumericValue()).isFalse();
    SMGValue pointerValueFromZeroPlus =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(secondReadValueAndState.getValue())
            .orElseThrow();
    assertThat(currentState.getMemoryModel().getSmg().isPointer(pointerValueFromZeroPlus)).isTrue();
    SMGPointsToEdge pointsToAdditionalSegmentEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(pointerValueFromZeroPlus).orElseThrow();
    assertThat(pointsToAdditionalSegmentEdge.pointsTo())
        .isNotInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(pointsToAdditionalSegmentEdge.targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_REGION);
    // Read the next pointer, check that it is a pointer to a 0+
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    SMGValueAndSMGState readNfoValueAndState =
        currentState.readSMGValue(pointsToAdditionalSegmentEdge.pointsTo(), nfo, pointerSizeInBits);
    currentState = readNfoValueAndState.getSMGState();
    SMGValue nextPointerValue = readNfoValueAndState.getSMGValue();
    // Confirm that this is a pointer to a 0+ SLS
    assertThat(currentState.getMemoryModel().getSmg().isPointer(nextPointerValue)).isTrue();
    SMGPointsToEdge pointsToNextConcreteEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();
    assertThat(pointsToNextConcreteEdge.pointsTo()).isInstanceOf(SMGSinglyLinkedListSegment.class);
    // assertThat(pointsToNextConcreteEdge.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
    assertThat(((SMGSinglyLinkedListSegment) pointsToNextConcreteEdge.pointsTo()).getMinLength())
        .isEqualTo(0);
    if (isDll) {
      SMGValueAndSMGState readPfoValueAndState =
          currentState.readSMGValue(
              pointsToAdditionalSegmentEdge.pointsTo(), pfo, pointerSizeInBits);
      currentState = readPfoValueAndState.getSMGState();
      SMGValue prevPointerValue = readPfoValueAndState.getSMGValue();
      // Confirm that this is a pointer to a 0+ SLS
      assertThat(currentState.getMemoryModel().getSmg().isPointer(prevPointerValue)).isTrue();
      SMGPointsToEdge pointsToPrevConcreteEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
      assertThat(pointsToPrevConcreteEdge.pointsTo()).isEqualTo(lastConcreteListObject);
    }
  }

  /*
   * Build a concrete list by hand and then use the abstraction algorithm on it and check the result.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicSLLFullAbstractionTest() throws SMGException, SMGSolverException {
    Value[] pointers = buildConcreteSLL();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            ((SMGSinglyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                .getMinLength())
        .isEqualTo(TEST_LIST_LENGTH);
    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            stateAndObjectAfterAbstraction.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    // There should be exactly TEST_LIST_LENGTH valid stack objects for the pointers
    // + 1 zero obj + 1 SLL obj
    assertThat(currentState.getMemoryModel().getSmg().getObjects()).hasSize(2 + TEST_LIST_LENGTH);
    int normalObjectCounter = 0;
    for (SMGObject object : currentState.getMemoryModel().getSmg().getObjects()) {
      if (object.getSize().asNumericValue().bigIntegerValue().equals(pointerSizeInBits)) {
        assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isTrue();
      } else if (object.isZero()) {
        continue;
      } else if (!(object instanceof SMGSinglyLinkedListSegment)) {
        normalObjectCounter++;
        assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
      } else {
        // We always start with at least element 2+
        if (((SMGSinglyLinkedListSegment) object).getMinLength() == TEST_LIST_LENGTH) {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isTrue();
        } else {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
        }
      }
    }
    // There should be 0 remnant objects
    assertThat(normalObjectCounter).isEqualTo(0);

    // Also only 2 heap objects known, the SLL and the 0 object
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2);
    for (SMGObject heapObj : currentState.getMemoryModel().getHeapObjects()) {
      if (!heapObj.isZero()) {
        assertThat(heapObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(((SMGSinglyLinkedListSegment) heapObj).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH);
      } else {
        assertThat(heapObj.isZero()).isTrue();
      }
    }
  }

  /*
   * Build a concrete list by hand and then use the abstraction algorithm on it and check the result.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicDLLFullAbstractionTest() throws SMGException, SMGSolverException {
    Value[] pointers =
        buildConcreteList(
            true, pointerSizeInBits.multiply(BigInteger.valueOf(3)), TEST_LIST_LENGTH);

    SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObject.getSMGState();
    assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObject.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject())
        .isInstanceOf(SMGDoublyLinkedListSegment.class);
    assertThat(
            ((SMGDoublyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                .getMinLength())
        .isEqualTo(TEST_LIST_LENGTH);

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(
            stateAndObjectAfterAbstraction.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits.add(pointerSizeInBits),
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicSLLFullAbstractionWithExternalPointerTest()
      throws SMGException, SMGSolverException {
    Value[] pointers =
        buildConcreteList(
            false, pointerSizeInBits.multiply(BigInteger.valueOf(2)), TEST_LIST_LENGTH);

    SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObject.getSMGState();
    assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObject.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            ((SMGSinglyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                .getMinLength())
        .isEqualTo(TEST_LIST_LENGTH);
    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            stateAndObjectAfterAbstraction.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);

    int level = TEST_LIST_LENGTH - 1;
    for (Value pointer : pointers) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeTarget =
          currentState.dereferencePointerWithoutMaterilization(pointer);
      assertThat(maybeTarget).isPresent();
      SMGStateAndOptionalSMGObjectAndOffset targetAndState = maybeTarget.orElseThrow();
      assertThat(targetAndState.getSMGObject())
          .isEqualTo(stateAndObjectAfterAbstraction.getSMGObject());
      SMGValue smgValue = currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
      assertThat(currentState.getMemoryModel().getSmg().getPTEdge(smgValue)).isPresent();

      for (SMGValue pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().keySet()) {
        if (pteMapping.equals(smgValue)) {
          assertThat(currentState.getMemoryModel().getNestingLevel(pteMapping)).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicDLLFullAbstractionWithExternalPointerTest()
      throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    Value[] pointers = buildConcreteList(true, dllSize, TEST_LIST_LENGTH);

    SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObject.getSMGState();
    assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObject.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, minAbstractionLength, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject())
        .isInstanceOf(SMGDoublyLinkedListSegment.class);
    assertThat(
            ((SMGDoublyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                .getMinLength())
        .isEqualTo(TEST_LIST_LENGTH);

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(
            stateAndObjectAfterAbstraction.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits.add(pointerSizeInBits),
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();

    int level = TEST_LIST_LENGTH - 1;
    for (Value pointer : pointers) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeTarget =
          currentState.dereferencePointerWithoutMaterilization(pointer);
      assertThat(maybeTarget).isPresent();
      SMGStateAndOptionalSMGObjectAndOffset targetAndState = maybeTarget.orElseThrow();
      assertThat(targetAndState.getSMGObject())
          .isEqualTo(stateAndObjectAfterAbstraction.getSMGObject());
      SMGValue smgValue = currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
      assertThat(currentState.getMemoryModel().getSmg().getPTEdge(smgValue)).isPresent();

      for (SMGValue pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().keySet()) {
        if (pteMapping.equals(smgValue)) {
          assertThat(currentState.getMemoryModel().getNestingLevel(pteMapping)).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicDLLFullAbstractionWithExternalPointerMaterializationTest()
      throws SMGException, SMGSolverException {
    Value[] pointers =
        buildConcreteList(
            true, pointerSizeInBits.multiply(BigInteger.valueOf(3)), TEST_LIST_LENGTH);

    SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObject.getSMGState();
    assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObject.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();

    SMGObject prevObj = null;
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState;
      if (i == TEST_LIST_LENGTH - 1) {
        Value correctPointer =
            currentState
                .readValueWithoutMaterialization(prevObj, nfo, pointerSizeInBits, null)
                .getValue();
        returnedObjAndState = currentState.dereferencePointer(correctPointer).get(0);
      } else {
        returnedObjAndState = currentState.dereferencePointer(pointers[i]).get(0);
      }
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      prevObj = newObj;
      assertThat(newObj).isNotSameInstanceAs(SMGSinglyLinkedListSegment.class);
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      // Check payload
      List<ValueAndSMGState> payloadAndState =
          currentState.readValue(newObj, hfo, pointerSizeInBits, null);
      // The hfo never materializes
      assertThat(payloadAndState).hasSize(1);
      currentState = payloadAndState.get(0).getState();
      Value payloadValue = payloadAndState.get(0).getValue();
      assertThat(payloadValue.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);

      // Read would normally materialize, we want to check the correct min length of the list first
      ValueAndSMGState nextPointerAndState =
          currentState.readValueWithoutMaterialization(newObj, nfo, pointerSizeInBits, null);

      SMGState nonMatCurrentState = nextPointerAndState.getState();
      Value nonMatNextPointer = nextPointerAndState.getValue();
      assertThat(nonMatCurrentState.getMemoryModel().isPointer(nonMatNextPointer)).isTrue();

      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextNonMatObj =
          nonMatCurrentState.dereferencePointerWithoutMaterilization(nonMatNextPointer);
      assertThat(maybeNextNonMatObj).isPresent();
      SMGStateAndOptionalSMGObjectAndOffset nextNonMatObjAndState =
          maybeNextNonMatObj.orElseThrow();
      nonMatCurrentState = nextNonMatObjAndState.getSMGState();
      SMGObject nextNonMatObj = nextNonMatObjAndState.getSMGObject();
      assertThat(nextNonMatObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(nonMatCurrentState.getMemoryModel().isObjectValid(nextNonMatObj)).isTrue();
      assertThat(((SMGSinglyLinkedListSegment) nextNonMatObj).getMinLength())
          .isEqualTo(TEST_LIST_LENGTH - i - 1);

      // Reading the next pointer materializes the next element
      // get(0) takes the list that is not extending for 0+
      nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null).get(0);
      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey())).isEqualTo(0);
        }
      }

      // Next pointer check equal pointers[i + 1]
      Value nextPointer = nextPointerAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nextPointer)).isTrue();
      if (i == TEST_LIST_LENGTH - 1) {
        assertThat(nextPointer.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(SMGObject.nullInstance())) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey())).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj).isPresent();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat(nextObj).isInstanceOf(SMGObject.class);
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                .isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }

        // Now get the next obj from the next pointer in the array WITHOUT deref.
        // It should be the same obj as above with the same length
        SMGStateAndOptionalSMGObjectAndOffset nextObjNonMatAndStateFromExternalPointer =
            nonMatCurrentState
                .dereferencePointerWithoutMaterilization(pointers[i + 1])
                .orElseThrow();
        nonMatCurrentState = nextObjNonMatAndStateFromExternalPointer.getSMGState();
        SMGObject newObjNonMatFromExternalPointer =
            nextObjNonMatAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjNonMatFromExternalPointer).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(
                nonMatCurrentState.getMemoryModel().isObjectValid(newObjNonMatFromExternalPointer))
            .isTrue();
        assertThat(((SMGSinglyLinkedListSegment) newObjNonMatFromExternalPointer).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - (i + 1));
        // Now from the materialized state
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjFromExternalPointer).isInstanceOf(SMGObject.class);
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        if (i >= TEST_LIST_LENGTH - 2) {
          assertThat(newObjFromExternalPointer).isInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(((SMGSinglyLinkedListSegment) newObjFromExternalPointer).getMinLength())
              .isEqualTo(0);
          assertThat(newObjFromExternalPointer).isNotEqualTo(nextObj);
        } else {
          assertThat(newObjFromExternalPointer).isEqualTo(nextObj);
        }
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                .isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }
      }

      // Back pointer equals pointers [i - 1]
      // TODO: back pointer
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void basicSLLFullAbstractionWithExternalPointerMaterializationTest()
      throws SMGException, SMGSolverException {
    Value[] pointers = buildConcreteSLL();

    SMGObject prevObj = null;
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState;
      if (i == TEST_LIST_LENGTH - 1) {
        Value correctPointer =
            currentState
                .readValueWithoutMaterialization(prevObj, nfo, pointerSizeInBits, null)
                .getValue();
        returnedObjAndState = currentState.dereferencePointer(correctPointer).get(0);
      } else {
        returnedObjAndState = currentState.dereferencePointer(pointers[i]).get(0);
      }
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      prevObj = newObj;
      assertThat(newObj).isNotSameInstanceAs(SMGSinglyLinkedListSegment.class);
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();

      List<ValueAndSMGState> payloadAndState =
          currentState.readValue(newObj, hfo, pointerSizeInBits, null);
      // The hfo never materializes
      assertThat(payloadAndState).hasSize(1);
      currentState = payloadAndState.get(0).getState();
      Value payloadValue = payloadAndState.get(0).getValue();
      assertThat(payloadValue.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);

      // Read would normally materialize, we want to check the correct min length of the list first
      ValueAndSMGState nextPointerAndState =
          currentState.readValueWithoutMaterialization(newObj, nfo, pointerSizeInBits, null);

      SMGState nonMatCurrentState = nextPointerAndState.getState();
      Value nonMatNextPointer = nextPointerAndState.getValue();
      assertThat(nonMatCurrentState.getMemoryModel().isPointer(nonMatNextPointer)).isTrue();

      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextNonMatObj =
          nonMatCurrentState.dereferencePointerWithoutMaterilization(nonMatNextPointer);
      assertThat(maybeNextNonMatObj).isPresent();
      SMGStateAndOptionalSMGObjectAndOffset nextNonMatObjAndState =
          maybeNextNonMatObj.orElseThrow();
      nonMatCurrentState = nextNonMatObjAndState.getSMGState();
      SMGObject nextNonMatObj = nextNonMatObjAndState.getSMGObject();
      assertThat(nextNonMatObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(nonMatCurrentState.getMemoryModel().isObjectValid(nextNonMatObj)).isTrue();
      assertThat(((SMGSinglyLinkedListSegment) nextNonMatObj).getMinLength())
          .isEqualTo(TEST_LIST_LENGTH - i - 1);

      // get(0) takes the list that is not extending for 0+
      nextPointerAndState = currentState.readValue(newObj, nfo, pointerSizeInBits, null).get(0);

      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey())).isEqualTo(0);
        }
      }

      // Next pointer check equal pointers[i + 1]
      Value nextPointer = nextPointerAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nextPointer)).isTrue();
      if (i == TEST_LIST_LENGTH - 1) {
        assertThat(nextPointer.asNumericValue().bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(SMGObject.nullInstance())) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey())).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj).isPresent();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat(nextObj).isInstanceOf(SMGObject.class);
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                .isLessThan(TEST_LIST_LENGTH - i);
          }
        }

        // Now get the next obj from the next pointer in the array WITHOUT deref.
        // It should be the same obj as above with the same length
        SMGStateAndOptionalSMGObjectAndOffset nextObjNonMatAndStateFromExternalPointer =
            nonMatCurrentState
                .dereferencePointerWithoutMaterilization(pointers[i + 1])
                .orElseThrow();
        nonMatCurrentState = nextObjNonMatAndStateFromExternalPointer.getSMGState();
        SMGObject newObjNonMatFromExternalPointer =
            nextObjNonMatAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjNonMatFromExternalPointer).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(
                nonMatCurrentState.getMemoryModel().isObjectValid(newObjNonMatFromExternalPointer))
            .isTrue();
        assertThat(((SMGSinglyLinkedListSegment) newObjNonMatFromExternalPointer).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - (i + 1));
        // Now from the materialized state
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjFromExternalPointer).isInstanceOf(SMGObject.class);
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        if (i >= TEST_LIST_LENGTH - 2) {
          assertThat(newObjFromExternalPointer).isInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(((SMGSinglyLinkedListSegment) newObjFromExternalPointer).getMinLength())
              .isEqualTo(0);
          assertThat(newObjFromExternalPointer).isNotEqualTo(nextObj);
        } else {
          assertThat(newObjFromExternalPointer).isEqualTo(nextObj);
        }
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                .isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }
      }

      // Back pointer equals pointers [i - 1]
      // TODO: back pointer
    }
  }

  /**
   * Builds a concrete SLL with size sllSize and length TEST_LIST_LENGTH, sanity checks and returns
   * all pointers in order.
   *
   * @return all pointers to the list in order.
   * @throws SMGException never thrown
   */
  private Value[] buildConcreteSLL() throws SMGException, SMGSolverException {
    Value[] pointers = buildConcreteList(false, sllSize, TEST_LIST_LENGTH);

    SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObject.getSMGState();
    assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObject.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    return pointers;
  }

  @Test
  public void basicDLLMaterializationTest() throws SMGException, SMGSolverException {
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(
            0,
            dllSizeValue,
            offset,
            hfo,
            nfo,
            BigInteger.ZERO,
            pfo,
            BigInteger.ZERO,
            TEST_LIST_LENGTH);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            numericPointerSizeInBits,
            new NumericValue(1),
            CNumericTypes.INT,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            numericPointerSizeInBits,
            new NumericValue(0),
            CPointerType.POINTER_TO_VOID,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(pfo),
            numericPointerSizeInBits,
            new NumericValue(0),
            CPointerType.POINTER_TO_VOID,
            dummyCDAEdge);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState =
        currentState.createAndAddPointer(
            pointer,
            currentAbstraction,
            LIST_POINTER_TYPE,
            BigInteger.ZERO,
            0,
            SMGTargetSpecifier.IS_FIRST_POINTER);

    // Save the pointer in a "stack" variable
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
    SMGObjectAndSMGState objAndState = currentState.copyAndAddStackObject(numericPointerSizeInBits);
    currentState = objAndState.getState();
    SMGObject stackObj = objAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj, BigInteger.ZERO, pointerSizeInBits, pointerToFirst);

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    TEST_LIST_LENGTH - 1));

    SMGObject[] previous = new SMGObject[TEST_LIST_LENGTH];
    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterialisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      assertThat(resultValuesAndStates).hasSize(1);
      resultValueAndState = resultValuesAndStates.get(0);

      currentState = resultValueAndState.getSMGState();
      // i + 3 because we always have i + 1 objects as a concrete list + zero object + abstract list
      // (0+ in the end)
      assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(i + 3);
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == TEST_LIST_LENGTH - 1) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object).isPresent();
        assertThat(object.orElseThrow().getSMGObject().getSize().asNumericValue().bigIntegerValue())
            .isEqualTo(dllSize);

        // get(0) takes the list that is not extending for 0+
        ValueAndSMGState nextPointer =
            currentState
                .readValue(object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null)
                .get(0);
        assertThat(nextPointer.getValue().isNumericValue()).isTrue();
        assertThat(nextPointer.getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        break;
      } else if (i == 0) {
        assertThat(currentPointer).isEqualTo(pointer);
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      assertThat(currentState.getMemoryModel().getValueFromSMGValue(headAndState.getSMGValue()))
          .isPresent();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .isNumericValue())
          .isTrue();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(BigInteger.ONE);
      SMGStateAndOptionalSMGObjectAndOffset prevObjAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      SMGValueAndSMGState prevPointerAndState =
          currentState.readSMGValue(newObj, pfo, pointerSizeInBits);
      currentState = prevPointerAndState.getSMGState();
      SMGValue prevPointer = prevPointerAndState.getSMGValue();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset prevObjFromPrevPointerAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(prevPointer).orElseThrow())
              .orElseThrow();
      if (i == 0) {
        assertThat(prevObjFromPrevPointerAndOffset.getSMGObject().isZero()).isTrue();
        previous[i] = prevObjAndOffset.getSMGObject();
      } else {
        previous[i] = prevObjAndOffset.getSMGObject();
        assertThat(previous[i - 1]).isEqualTo(prevObjFromPrevPointerAndOffset.getSMGObject());
      }

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      assertThat(targetToNextAndOffset.getSMGObject())
          .isInstanceOf(SMGSinglyLinkedListSegment.class);
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  @Test
  public void basicSLLMaterializationTest() throws SMGException, SMGSolverException {
    BigInteger offset = BigInteger.ZERO;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(
            0, sllSizeValue, offset, hfo, nfo, BigInteger.ZERO, TEST_LIST_LENGTH);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            numericPointerSizeInBits,
            new NumericValue(1),
            CNumericTypes.INT,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            numericPointerSizeInBits,
            new NumericValue(0),
            CNumericTypes.INT,
            dummyCDAEdge);
    // First pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState =
        currentState.createAndAddPointer(
            pointer,
            currentAbstraction,
            LIST_POINTER_TYPE,
            BigInteger.ZERO,
            0,
            SMGTargetSpecifier.IS_FIRST_POINTER);
    // Save the pointer in a stack variable
    SMGObjectAndSMGState objAndState = currentState.copyAndAddStackObject(sllSizeValue);
    currentState = objAndState.getState();
    SMGObject stackObj = objAndState.getSMGObject();
    currentState =
        currentState.writeValueWithoutChecks(
            stackObj,
            BigInteger.ZERO,
            pointerSizeInBits,
            currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow());

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    TEST_LIST_LENGTH - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterialisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;

      assertThat(resultValuesAndStates).hasSize(1);
      resultValueAndState = resultValuesAndStates.get(0);

      currentState = resultValueAndState.getSMGState();
      // i + 3 because we always have i + 1 objects as a concrete list + zero object + abstract list
      // (0+ in the end)
      assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(i + 3);
      // currentPointer == pointer to just materilized list segment
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == TEST_LIST_LENGTH - 1) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object).isPresent();
        assertThat(object.orElseThrow().getSMGObject().getSize().asNumericValue().bigIntegerValue())
            .isEqualTo(sllSize);

        // get(0) takes the list that is not extending for 0+
        ValueAndSMGState nextPointer =
            currentState
                .readValue(object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null)
                .get(0);
        assertThat(nextPointer.getValue().isNumericValue()).isTrue();
        assertThat(nextPointer.getValue().asNumericValue().bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        break;
      } else if (i == 0) {
        assertThat(currentPointer.equals(pointer)).isTrue();
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);

      SMGObject newObj = targetAndOffset.getSMGObject();
      // Read Head and check that its correct
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      assertThat(currentState.getMemoryModel().getValueFromSMGValue(headAndState.getSMGValue()))
          .isPresent();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .isNumericValue())
          .isTrue();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .asNumericValue()
                  .bigIntegerValue())
          .isEqualTo(BigInteger.ONE);

      // Read nxt pointer and check
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      assertThat(targetToNextAndOffset.getSMGObject())
          .isInstanceOf(SMGSinglyLinkedListSegment.class);
      currentAbstraction = (SMGSinglyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  // Build a list from behind and abstract (also materializes parts of the list in the process)
  @Test
  public void basicSLLReverseAbstractionTest() throws SMGException, SMGSolverException {
    // TODO: same with a pointer to a value and a pointer to a sublist/sublists in hfo
    // TODO: same with switched hfo and nfo
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    resetSMGStateAndVisitor();
    NumericValue zeroValue = new NumericValue(BigInteger.ZERO);
    NumericValue hfoValue = new NumericValue(hfo);
    NumericValue nfoValue = new NumericValue(nfo);
    // NumericValue pointerSizeValue = new NumericValue(pointerSizeInBits);
    NumericValue oneValue = new NumericValue(BigInteger.ONE);
    // Create a stack memory region for the head pointer
    SMGObjectAndSMGState stackObjForPointerAndState =
        currentState.copyAndAddStackObject(numericPointerSizeInBits);
    SMGObject plist = stackObjForPointerAndState.getSMGObject();
    currentState = stackObjForPointerAndState.getState();

    // "malloc" a list segment and create head pointer and save in stack
    SMGObjectAndSMGState initialListSegmentAndState =
        currentState.copyAndAddNewHeapObject(sllSizeValue);
    SMGObject initialListSegment = initialListSegmentAndState.getSMGObject();
    currentState = initialListSegmentAndState.getState();
    ValueAndSMGState ptrAndState =
        currentState.searchOrCreateAddress(initialListSegment, CPointerType.POINTER_TO_VOID, hfo);
    currentState = ptrAndState.getState();
    currentState =
        currentState.writeValueWithChecks(
            plist,
            hfoValue,
            numericPointerSizeInBits,
            ptrAndState.getValue(),
            CPointerType.POINTER_TO_VOID,
            null);

    // write the "next" pointer to 0 for the list segment
    currentState =
        currentState.writeValueWithChecks(
            initialListSegment,
            nfoValue,
            numericPointerSizeInBits,
            zeroValue,
            CPointerType.POINTER_TO_VOID,
            null);

    // Write some value in the payload (we write numeric 1)
    currentState =
        currentState.writeValueWithChecks(
            initialListSegment,
            hfoValue,
            numericPointerSizeInBits,
            oneValue,
            CPointerType.POINTER_TO_VOID,
            null);

    stackObjForPointerAndState = currentState.copyAndAddStackObject(numericPointerSizeInBits);
    SMGObject newHeadOnStack = stackObjForPointerAndState.getSMGObject();
    currentState = stackObjForPointerAndState.getState();

    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      // Now, in a loop, create/reuse a stack variable, create a new list segment,
      // create a pointer to it, save in stack variable
      SMGObjectAndSMGState newListSegmentAndState =
          currentState.copyAndAddNewHeapObject(sllSizeValue);
      SMGObject newListSegment = newListSegmentAndState.getSMGObject();
      currentState = newListSegmentAndState.getState();
      ValueAndSMGState newListPtrAndState =
          currentState.searchOrCreateAddress(
              newListSegment, CPointerType.POINTER_TO_VOID, BigInteger.ZERO);
      currentState = newListPtrAndState.getState();
      currentState =
          currentState.writeValueWithChecks(
              newHeadOnStack,
              hfoValue,
              numericPointerSizeInBits,
              newListPtrAndState.getValue(),
              CPointerType.POINTER_TO_VOID,
              null);
      // save pointer to previous list segment (read stack variable plist) in next field of just
      // created list segment
      // !! This might trigger a materialization if the non-head pointer is read!!
      List<ValueAndSMGState> listHeadPtrsAndStates =
          currentState.readValue(
              plist, BigInteger.ZERO, pointerSizeInBits, CPointerType.POINTER_TO_VOID);
      assertThat(listHeadPtrsAndStates).hasSize(1);
      currentState = listHeadPtrsAndStates.get(0).getState();
      Value listHeadPtr = listHeadPtrsAndStates.get(0).getValue();
      Optional<SMGStateAndOptionalSMGObjectAndOffset> derefOfListWOMat =
          currentState.dereferencePointerWithoutMaterilization(listHeadPtr);
      assertThat(derefOfListWOMat).isPresent();
      assertThat(derefOfListWOMat.orElseThrow().hasSMGObjectAndOffset()).isTrue();
      assertThat(currentState.getMemoryModel().pointsToZeroPlus(listHeadPtr)).isFalse();
      if (derefOfListWOMat.orElseThrow().getSMGObject().isSLL()) {
        // Might be off by 1
        assertThat(
                ((SMGSinglyLinkedListSegment) derefOfListWOMat.orElseThrow().getSMGObject())
                    .getMinLength())
            .isEqualTo(i);
      }

      currentState =
          currentState.writeValueWithChecks(
              newListSegment,
              nfoValue,
              numericPointerSizeInBits,
              listHeadPtr,
              CPointerType.POINTER_TO_VOID,
              null);

      // read data payload from now "next" list segment and save in new list segments data
      SMGObject derefedObj = derefOfListWOMat.orElseThrow().getSMGObject();
      int nestinglvl =
          currentState
              .getMemoryModel()
              .getNestingLevel(
                  currentState.getMemoryModel().getSMGValueFromValue(listHeadPtr).orElseThrow());
      ValueAndSMGState payloadPointerAndState;
      if (derefedObj instanceof SMGSinglyLinkedListSegment llObj) {
        assertThat(nestinglvl).isEqualTo(llObj.getMinLength() - 1);
        payloadPointerAndState =
            currentState.searchOrCreateAddress(
                derefedObj,
                CPointerType.POINTER_TO_VOID,
                hfo,
                nestinglvl,
                SMGTargetSpecifier.IS_FIRST_POINTER);
      } else {
        payloadPointerAndState =
            currentState.searchOrCreateAddress(
                derefedObj,
                CPointerType.POINTER_TO_VOID,
                hfo,
                nestinglvl,
                SMGTargetSpecifier.IS_REGION);
      }
      Value payloadPointer = payloadPointerAndState.getValue();
      currentState = payloadPointerAndState.getState();
      List<SMGStateAndOptionalSMGObjectAndOffset> listPayloadAndStates =
          currentState.dereferencePointer(payloadPointer);
      assertThat(listPayloadAndStates).hasSize(1);
      assertThat(listPayloadAndStates.get(0).hasSMGObjectAndOffset()).isTrue();
      assertThat(listPayloadAndStates.get(0).getOffsetForObject()).isEqualTo(hfoValue);
      currentState = listPayloadAndStates.get(0).getSMGState();
      SMGObject payloadObj = listPayloadAndStates.get(0).getSMGObject();
      List<ValueAndSMGState> valuesAndStates =
          currentState.readValue(payloadObj, hfo, pointerSizeInBits, null);
      assertThat(valuesAndStates).hasSize(1);
      Value payloadValue = valuesAndStates.get(0).getValue();
      assertThat(payloadValue).isEqualTo(oneValue);
      currentState = valuesAndStates.get(0).getState();
      currentState =
          currentState.writeValueWithChecks(
              newListSegment,
              hfoValue,
              numericPointerSizeInBits,
              payloadValue,
              CNumericTypes.INT,
              null);

      // Switch pointer of new segment to plist
      List<ValueAndSMGState> valuesAndStatesNewHead =
          currentState.readValue(
              newHeadOnStack, BigInteger.ZERO, pointerSizeInBits, CPointerType.POINTER_TO_VOID);
      assertThat(valuesAndStatesNewHead).hasSize(1);
      Value newHeadPtr = valuesAndStatesNewHead.get(0).getValue();
      assertThat(newHeadPtr).isEqualTo(newListPtrAndState.getValue());
      currentState = valuesAndStatesNewHead.get(0).getState();
      currentState =
          currentState.writeValueWithChecks(
              plist,
              zeroValue,
              numericPointerSizeInBits,
              newHeadPtr,
              CPointerType.POINTER_TO_VOID,
              null);

      // abstract
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, minAbstractionLength, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      // the pointer to the new element should point to minSize -1!
      // derefWOMat is the top of the list
      SMGStateAndOptionalSMGObjectAndOffset derefWOMat =
          currentState.dereferencePointerWithoutMaterilization(newHeadPtr).orElseThrow();
      if (derefWOMat.getSMGObject() instanceof SMGSinglyLinkedListSegment) {
        SMGValue smgPointerToHead =
            derefWOMat
                .getSMGState()
                .getMemoryModel()
                .getSMGValueFromValue(newHeadPtr)
                .orElseThrow();
        assertThat(derefWOMat.getSMGState().getMemoryModel().getNestingLevel(smgPointerToHead))
            .isEqualTo(0);
      }
    }
  }

  @Test
  public void basicDLLDetectionTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i, 1, 0, 0);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength, new SMGCPAStatistics());
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i > minAbstractionLength - 1) {
        assertThat(candidates).hasSize(1);
        SMGCandidate candidate = candidates.iterator().next();
        Optional<BigInteger> maybePfo = absFinder.isDLL(candidate, state.getMemoryModel().getSmg());
        assertThat(maybePfo).isPresent();
        // PFO is 64
        assertThat(maybePfo.orElseThrow()).isEqualTo(pointerSizeInBits.add(pointerSizeInBits));
      } else {
        assertThat(candidates).hasSize(0);
      }
    }
  }

  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void abstractReverseSLLTest() throws SMGException, SMGSolverException {
    // Minimum abstraction length before a list is abstracted
    int minAbstractionLength = 5;

    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i, 1, 0);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength, new SMGCPAStatistics());
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoSLL(firstObj.getObject(), nfo, BigInteger.ZERO, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGSinglyLinkedListSegment sll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGSinglyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(sll).isNull();
          sll = (SMGSinglyLinkedListSegment) object;
        } else {
          // The only valid non-SLL object is the stack variable
          if (object.getSize().asNumericValue().bigIntegerValue().equals(BigInteger.valueOf(32))) {
            assertThat(state.getMemoryModel().isObjectValid(object)).isTrue();
          } else {
            assertThat(state.getMemoryModel().isObjectValid(object)).isFalse();
          }
        }
      }
      assertThat(sll).isNotNull();
      if (sll != null) {
        assertThat(sll.getMinLength()).isEqualTo(i);
        assertThat(sll.getNextOffset()).isEqualTo(nfo);
        assertThat(sll.getSize().asNumericValue().bigIntegerValue()).isEqualTo(sllSize);
      }
    }
  }

  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @SuppressWarnings("null")
  @Test
  public void abstractSLLTest() throws SMGException, SMGSolverException {
    // Minimum abstraction length before a list is abstracted
    int minAbstractionLength = 3;

    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i, 1, 0);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength, new SMGCPAStatistics());
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoSLL(firstObj.getObject(), nfo, BigInteger.ZERO, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGSinglyLinkedListSegment sll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGSinglyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(sll).isNull();
          sll = (SMGSinglyLinkedListSegment) object;
        } else {
          // The only valid non-SLL object is the stack variable
          if (object.getSize().asNumericValue().bigIntegerValue().equals(BigInteger.valueOf(32))) {
            assertThat(state.getMemoryModel().isObjectValid(object)).isTrue();
          } else {
            assertThat(state.getMemoryModel().isObjectValid(object)).isFalse();
          }
        }
      }
      assertThat(sll.getMinLength()).isEqualTo(i);
      assertThat(sll.getNextOffset()).isEqualTo(nfo);
      assertThat(sll.getSize().asNumericValue().bigIntegerValue()).isEqualTo(sllSize);
    }
  }

  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @SuppressWarnings("null")
  @Test
  public void abstractDLLTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i, 1, 0, 0);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength, new SMGCPAStatistics());
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state =
          state.abstractIntoDLL(
              firstObj.getObject(), nfo, BigInteger.ZERO, pfo, BigInteger.ZERO, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGDoublyLinkedListSegment dll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGDoublyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(dll).isNull();
          dll = (SMGDoublyLinkedListSegment) object;
        } else {
          if (object.getSize().asNumericValue().bigIntegerValue().equals(pointerSizeInBits)) {
            // Only the stack variable memory is valid (original pointer)
            assertThat(state.getMemoryModel().isObjectValid(object)).isTrue();
          } else {
            assertThat(state.getMemoryModel().isObjectValid(object)).isFalse();
          }
        }
      }
      assertThat(dll.getMinLength()).isEqualTo(i);
      assertThat(dll.getNextOffset()).isEqualTo(nfo);
      assertThat(dll.getPrevOffset()).isEqualTo(pfo);
      assertThat(dll.getSize().asNumericValue().bigIntegerValue()).isEqualTo(dllSize);
      assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
    }
  }

  // Test the minimum length needed for abstraction
  @SuppressWarnings("null")
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void abstractDLLLimitTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;

    for (int minLength = minAbstractionLength;
        minLength < TEST_LIST_LENGTH;
        minLength = minLength + 10) {
      for (int i = 1; i < TEST_LIST_LENGTH; i++) {
        resetSMGStateAndVisitor();
        SMGState state = createXLongExplicitDLLOnHeap(i, 1, 0, 0);
        SMGCPAAbstractionManager absFinder =
            new SMGCPAAbstractionManager(state, minLength, new SMGCPAStatistics());
        ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
        if (i < minLength) {
          assertThat(candidates.isEmpty()).isTrue();
          continue;
        }
        SMGCandidate firstObj = candidates.iterator().next();
        assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
        state =
            state.abstractIntoDLL(
                firstObj.getObject(),
                nfo,
                BigInteger.ZERO,
                pfo,
                BigInteger.ZERO,
                ImmutableSet.of());

        Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
        // All should be invalid except our SLL here
        SMGDoublyLinkedListSegment dll = null;
        for (SMGObject object : objects) {
          if (object instanceof SMGDoublyLinkedListSegment
              && state.getMemoryModel().isObjectValid(object)) {
            assertThat(dll).isNull();
            dll = (SMGDoublyLinkedListSegment) object;
          } else if (object
              .getSize()
              .asNumericValue()
              .bigIntegerValue()
              .equals(pointerSizeInBits)) {
            assertThat(state.getMemoryModel().isObjectValid(object)).isTrue();
          } else {
            assertThat(state.getMemoryModel().isObjectValid(object)).isFalse();
          }
        }
        assertThat(dll.getMinLength()).isEqualTo(i);
        assertThat(dll.getNextOffset()).isEqualTo(nfo);
        assertThat(dll.getPrevOffset()).isEqualTo(pfo);
        assertThat(dll.getSize().asNumericValue().bigIntegerValue()).isEqualTo(dllSize);
        assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
        assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      }
    }
  }

  //

  /**
   * The next pointer at the end points to 0. nfo offset 32. Values are equal each list segment, but
   * increment based on list segment size (0,1,2... until nfo is reached).
   */
  private SMGState createXLongExplicitSLLOnHeap(int length, int value, int nextPtrTargetOffset)
      throws SMGException, SMGSolverException {
    buildConcreteListWithEqualValues(
        false,
        sllSize,
        length,
        value,
        BigInteger.valueOf(nextPtrTargetOffset),
        Optional.empty(),
        true);
    return currentState;
  }

  /**
   * The next pointer at the end points to 0. nfo offset 32. Values are equal each list segment, but
   * increment based on list segment size (0,1,2... until nfo is reached).
   */
  private SMGState createXLongExplicitDLLOnHeap(
      int length, int value, int nextPtrTargetOffset, int prevPtrTargetOffset)
      throws SMGException, SMGSolverException {
    buildConcreteListWithEqualValues(
        true,
        dllSize,
        length,
        value,
        BigInteger.valueOf(nextPtrTargetOffset),
        Optional.of(BigInteger.valueOf(prevPtrTargetOffset)),
        true);
    return currentState;
  }

  /**
   * Checks the integrity of list pointers and nesting levels.
   *
   * @param totalSizeOfList size of list.
   * @param pointers array of all the pointers to the (former) concrete list elements in order.
   *     Expected to be as large as totalSizeOfList.
   * @param derefPositions ordered array of deref positions, min: 0, max: totalSizeOfList - 1.
   * @param isDll true if dlls tested.
   * @throws SMGException indicates errors
   */
  private void derefPointersAtAndCheckListMaterialization(
      int totalSizeOfList, Value[] pointers, int[] derefPositions, boolean isDll)
      throws SMGException {
    derefPointersAtAndCheckListMaterialization(
        totalSizeOfList, pointers, derefPositions, isDll, false);
  }

  /**
   * Checks the integrity of list pointers and nesting levels.
   *
   * @param totalSizeOfList size of list.
   * @param pointers array of all the pointers to the (former) concrete list elements in order.
   *     Expected to be as large as totalSizeOfList.
   * @param derefPositions ordered array of deref positions, min: 0, max: totalSizeOfList - 1.
   * @param isDll true if dlls tested.
   * @throws SMGException indicates errors
   */
  private void derefPointersAtAndCheckListMaterialization(
      int totalSizeOfList,
      Value[] pointers,
      int[] derefPositions,
      boolean isDll,
      boolean extraPointer)
      throws SMGException {
    int tmp = 0;
    assertThat(derefPositions[0]).isAtLeast(0);
    for (int num : derefPositions) {
      assertThat(tmp).isAtMost(num);
      tmp = num;
    }
    assertThat(derefPositions[derefPositions.length - 1]).isLessThan(totalSizeOfList);
    List<SMGStateAndOptionalSMGObjectAndOffset> derefAtList = ImmutableList.of();
    int lastDeref = 0;
    for (int k : derefPositions) {
      if (k == totalSizeOfList - 1) {
        // Last pointer in pointers
        assertThat(lastDeref).isEqualTo(totalSizeOfList - 2);
        SMGStateAndOptionalSMGObjectAndOffset derefedList = derefAtList.get(0);
        ValueAndSMGState nextPtr =
            currentState.readValueWithoutMaterialization(
                derefedList.getSMGObject(), nfo, pointerSizeInBits, null);
        currentState = nextPtr.getState();
        derefAtList = currentState.dereferencePointer(nextPtr.getValue());
      } else {
        derefAtList = currentState.dereferencePointer(pointers[k]);
        lastDeref = k;
      }
      assertThat(derefAtList).hasSize(1);
      SMGStateAndOptionalSMGObjectAndOffset derefAt = derefAtList.get(0);
      currentState = derefAt.getSMGState();
      assertThat(derefAt.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      // pointers 0 ... k-1 are now pointing to non-abstracted segments w nesting level 0
      SMGObject prevObjForNxt = null;
      for (int i = 0; i <= k; i++) {
        Value derefPointer = pointers[i];
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefWConcreteTarget =
            currentState.dereferencePointerWithoutMaterilization(derefPointer);
        assertThat(derefWConcreteTarget).isPresent();
        SMGObject currentObj = derefWConcreteTarget.orElseThrow().getSMGObject();
        if (i == totalSizeOfList - 1) {
          // Last pointer
          assertThat(currentObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(((SMGSinglyLinkedListSegment) currentObj).getMinLength()).isEqualTo(0);
          Value nextPtrOfPrev =
              currentState
                  .readValueWithoutMaterialization(prevObjForNxt, nfo, pointerSizeInBits, null)
                  .getValue();
          derefWConcreteTarget =
              currentState.dereferencePointerWithoutMaterilization(nextPtrOfPrev);
          assertThat(derefWConcreteTarget).isPresent();
          currentObj = derefWConcreteTarget.orElseThrow().getSMGObject();
          assertThat(derefPointer).isNotEqualTo(nextPtrOfPrev);
          derefPointer = nextPtrOfPrev;
        }

        assertThat(currentObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

        prevObjForNxt = currentObj;
        ValueAndSMGState addressAndState =
            currentState.searchOrCreateAddress(
                derefWConcreteTarget.orElseThrow().getSMGObject(),
                CPointerType.POINTER_TO_VOID,
                BigInteger.ZERO);
        currentState = addressAndState.getState();
        Value address = addressAndState.getValue();
        assertThat(address).isEqualTo(derefPointer);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getNestingLevel(
                        currentState.getMemoryModel().getSMGValueFromValue(address).orElseThrow()))
            .isEqualTo(0);
        if (isDll && i > 0) {
          // has a back pointer w nesting level 0 that points to the prev object
          ValueAndSMGState backPointerRead =
              currentState.readValueWithoutMaterialization(
                  currentObj, pfo, pointerSizeInBits, null);
          currentState = backPointerRead.getState();
          Value backPointer = backPointerRead.getValue();
          // Get prev obj from this read back pointer
          Optional<SMGStateAndOptionalSMGObjectAndOffset> derefPrevFromRead =
              currentState.dereferencePointerWithoutMaterilization(backPointer);
          assertThat(derefPrevFromRead).isPresent();
          SMGObject prevObjFromRead = derefPrevFromRead.orElseThrow().getSMGObject();
          assertThat(prevObjFromRead).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

          // Get prev obj from pointers
          Optional<SMGStateAndOptionalSMGObjectAndOffset> derefPrevWConcreteTarget =
              currentState.dereferencePointerWithoutMaterilization(pointers[i - 1]);
          assertThat(derefPrevWConcreteTarget).isPresent();
          SMGObject prevObj = derefPrevWConcreteTarget.orElseThrow().getSMGObject();
          assertThat(prevObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

          // Should be the same obj and pointer
          assertThat(prevObjFromRead).isEqualTo(prevObj);
          assertThat(pointers[i - 1]).isEqualTo(backPointer);
          assertThat(
                  currentState.getMemoryModel().getSMGValueFromValue(pointers[i - 1]).orElseThrow())
              .isEqualTo(
                  currentState.getMemoryModel().getSMGValueFromValue(backPointer).orElseThrow());
          // Nesting level 0
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getNestingLevel(
                          currentState
                              .getMemoryModel()
                              .getSMGValueFromValue(address)
                              .orElseThrow()))
              .isEqualTo(0);
        }
      }
      // The others are pointing to the abstracted segment w nesting level accordingly
      for (int i = k + 1; i < totalSizeOfList; i++) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefWOConcreteTarget =
            currentState.dereferencePointerWithoutMaterilization(pointers[i]);
        assertThat(derefWOConcreteTarget).isPresent();
        assertThat(derefWOConcreteTarget.orElseThrow().getSMGObject())
            .isInstanceOf(SMGSinglyLinkedListSegment.class);
        if (isDll && i > 0) {
          // has a back pointer w nesting level 0 that points to the previous concrete object
          SMGObject currentObj = derefWOConcreteTarget.orElseThrow().getSMGObject();
          Optional<SMGStateAndOptionalSMGObjectAndOffset> derefWConcreteTarget =
              currentState.dereferencePointerWithoutMaterilization(pointers[k]);
          assertThat(derefWConcreteTarget).isPresent();
          SMGObject prevConcreteObj = derefWConcreteTarget.orElseThrow().getSMGObject();
          ValueAndSMGState backPointerRead =
              currentState.readValueWithoutMaterialization(
                  currentObj, pfo, pointerSizeInBits, null);
          currentState = backPointerRead.getState();
          Value backPointer = backPointerRead.getValue();
          // Get prev obj from this read back pointer
          Optional<SMGStateAndOptionalSMGObjectAndOffset> derefPrevFromRead =
              currentState.dereferencePointerWithoutMaterilization(backPointer);
          assertThat(derefPrevFromRead).isPresent();
          SMGObject prevObjFromRead = derefPrevFromRead.orElseThrow().getSMGObject();
          assertThat(prevObjFromRead).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
          assertThat(prevConcreteObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

          // Should be the same obj and pointer
          assertThat(prevObjFromRead).isEqualTo(prevConcreteObj);
          assertThat(pointers[k]).isEqualTo(backPointer);
          assertThat(currentState.getMemoryModel().getSMGValueFromValue(pointers[k]).orElseThrow())
              .isEqualTo(
                  currentState.getMemoryModel().getSMGValueFromValue(backPointer).orElseThrow());
          // Nesting level 0
          assertThat(
                  currentState
                      .getMemoryModel()
                      .getNestingLevel(
                          currentState
                              .getMemoryModel()
                              .getSMGValueFromValue(backPointer)
                              .orElseThrow()))
              .isEqualTo(0);
        }
      }

      // there are currently sizeOfList + 3 ZERO pointers total and more in special cases
      int expectedNumberOfPointers = totalSizeOfList + 3;
      if (k >= totalSizeOfList - 2) {
        // Additional pointer to last element
        expectedNumberOfPointers += 1;
      }
      if (k == totalSizeOfList - 1) {
        // Additional list segment materialized (list now 11+)
        expectedNumberOfPointers += 1;
      }
      if (extraPointer) {
        expectedNumberOfPointers += 1;
      }
      assertThat(currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet())
          .hasSize(expectedNumberOfPointers);

      int zeros = 0;
      int found0Plus = 0;
      int foundNum = 0;
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        // The pointers are not ordered, filter out the 3 zeros and confirm that each pointer from
        // pointers is present w correct nesting level
        if (entry.getKey().isZero()) {
          zeros++;
        } else {
          boolean found = false;
          for (int i = 0; i < pointers.length; i++) {
            Value pointer = pointers[i];
            SMGValue pointerSMGValue =
                currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
            if (pointerSMGValue.equals(entry.getKey())) {
              // check that no 2 pointers exist
              assertThat(found).isFalse();
              found = true;
              foundNum++;
              // check nesting level
              if (i < k + 1) {
                assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                    .isEqualTo(0);
              } else {
                assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                    .isEqualTo(totalSizeOfList - i - 1);
              }
            }
          }
          if (k + 1 == totalSizeOfList) {
            // Pointers from the prev object (that is materialized) have a first tag towards the
            // final object, but pointers from outside the list have a last tag!
            // So we get the next ptr towards the 0+ from the prev obj
            ValueAndSMGState nextPointerToZeroPlusWithoutMaterialization =
                currentState.readValueWithoutMaterialization(
                    currentState
                        .dereferencePointer(pointers[totalSizeOfList - 2])
                        .get(0)
                        .getSMGObject(),
                    nfo,
                    pointerSizeInBits,
                    null);
            currentState = nextPointerToZeroPlusWithoutMaterialization.getState();
            Value nextPointerToZeroPlus = nextPointerToZeroPlusWithoutMaterialization.getValue();
            SMGValue smgNextPointerToZeroPlus =
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(nextPointerToZeroPlus)
                    .orElseThrow();
            if (entry.getKey().equals(smgNextPointerToZeroPlus)) {
              // The lone next pointer from the last segment to 0+
              assertThat(currentState.getMemoryModel().getNestingLevel(entry.getKey()))
                  .isEqualTo(0);
              assertThat(found0Plus).isEqualTo(0);
              assertThat(found).isFalse();
              found0Plus++;
            }
          }
        }
      }
      if (k + 1 == totalSizeOfList) {
        assertThat(found0Plus).isEqualTo(1);
      }
      assertThat(foundNum).isEqualTo(totalSizeOfList);
      assertThat(zeros).isEqualTo(3);
    }
  }

  /**
   * Checks abstraction of middle list with ptr offsets internalListPtrOffset with length
   * listLength, except first and last ptrs in listPtrs with ptr target offsets otherPtrOffset.
   * listPtrs size is listLength+2
   */
  private void checkAbstractionOfLLWithConcreteFirstAndLast(
      boolean dll,
      int listLength,
      BigInteger listSegmentSize,
      Value pointerToFirstSegment,
      BigInteger otherPtrOffset,
      BigInteger internalListPtrOffset)
      throws SMGException {
    SMGObject listSegmentFront =
        currentState
            .dereferencePointerWithoutMaterilization(pointerToFirstSegment)
            .orElseThrow()
            .getSMGObject();

    assertThat(currentState.getMemoryModel().getSmg().isValid(listSegmentFront)).isTrue();
    assertThat(listSegmentFront.getSize().isNumericValue()).isTrue();
    assertThat(listSegmentFront.getSize().asNumericValue().bigIntegerValue())
        .isEqualTo(listSegmentSize);
    assertThat(listSegmentFront).isNotInstanceOf(SMGSinglyLinkedListSegment.class);

    ValueAndSMGState nextPtrAndState =
        currentState.readValueWithoutMaterialization(
            listSegmentFront, nfo, pointerSizeInBits, null);
    currentState = nextPtrAndState.getState();
    Value nextPtr = nextPtrAndState.getValue();

    // Check fst of abstracted
    Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedAbstrListFst =
        currentState.dereferencePointerWithoutMaterilization(nextPtr);

    assertThat(derefedAbstrListFst).isPresent();
    assertThat(derefedAbstrListFst.orElseThrow().hasSMGObjectAndOffset()).isTrue();
    SMGObject derefedFstObj = derefedAbstrListFst.orElseThrow().getSMGObject();
    assertThat(derefedFstObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
    if (dll) {
      assertThat(derefedFstObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getMinLength()).isEqualTo(listLength);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getPrevPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    } else {
      assertThat(derefedFstObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) derefedFstObj).getMinLength()).isEqualTo(listLength);
      assertThat(((SMGSinglyLinkedListSegment) derefedFstObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    }

    SMGValueAndSMGState readNextToAbstr =
        currentState.readSMGValue(listSegmentFront, nfo, pointerSizeInBits);
    Optional<SMGPointsToEdge> maybePTEToAbtrFst =
        currentState.getMemoryModel().getSmg().getPTEdge(readNextToAbstr.getSMGValue());
    assertThat(maybePTEToAbtrFst).isPresent();
    assertThat(maybePTEToAbtrFst.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
        .isEqualTo(otherPtrOffset);
    assertThat(maybePTEToAbtrFst.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

    Optional<SMGPointsToEdge> ptrFromBeginning =
        currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(currentState.getMemoryModel().getSMGValueFromValue(nextPtr).orElseThrow());
    assertThat(ptrFromBeginning.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
    assertThat(ptrFromBeginning.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
        .isEqualTo(otherPtrOffset);

    if (dll) {
      SMGValueAndSMGState readPrevOfFst =
          currentState.readSMGValue(derefedFstObj, pfo, pointerSizeInBits);
      Optional<SMGPointsToEdge> maybePTEPrevFromFstLst =
          currentState.getMemoryModel().getSmg().getPTEdge(readPrevOfFst.getSMGValue());
      assertThat(maybePTEPrevFromFstLst).isPresent();
      assertThat(maybePTEPrevFromFstLst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
      assertThat(maybePTEPrevFromFstLst.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(maybePTEPrevFromFstLst.orElseThrow().pointsTo()).isEqualTo(listSegmentFront);
    }

    SMGValueAndSMGState readNextOfLstAbstr =
        currentState.readSMGValue(derefedFstObj, nfo, pointerSizeInBits);
    Optional<SMGPointsToEdge> maybePTENextFromAbtrLst =
        currentState.getMemoryModel().getSmg().getPTEdge(readNextOfLstAbstr.getSMGValue());
    assertThat(maybePTENextFromAbtrLst).isPresent();
    assertThat(maybePTENextFromAbtrLst.orElseThrow().getOffset().asNumericValue().bigIntegerValue())
        .isEqualTo(otherPtrOffset);
    assertThat(maybePTENextFromAbtrLst.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_REGION);
    SMGObject listSegmentBack = maybePTENextFromAbtrLst.orElseThrow().pointsTo();
    assertThat(listSegmentBack).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(listSegmentBack.getSize().isNumericValue()).isTrue();
    assertThat(listSegmentBack.getSize().asNumericValue().bigIntegerValue())
        .isEqualTo(listSegmentSize);

    if (dll) {
      SMGValueAndSMGState readPrevOfLast =
          currentState.readSMGValue(listSegmentBack, pfo, pointerSizeInBits);
      Optional<SMGPointsToEdge> maybePTEPrevFromLastLst =
          currentState.getMemoryModel().getSmg().getPTEdge(readPrevOfLast.getSMGValue());
      assertThat(maybePTEPrevFromLastLst).isPresent();
      assertThat(maybePTEPrevFromLastLst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
      assertThat(maybePTEPrevFromLastLst.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      assertThat(maybePTEPrevFromLastLst.orElseThrow().pointsTo()).isEqualTo(derefedFstObj);
    }
  }

  /**
   * Checks abstraction of middle list with ptr offsets internalListPtrOffset with length
   * listLength, except first and last ptrs in listPtrs with ptr target offsets otherPtrOffset.
   * listPtrs size is listLength+2
   */
  private void checkAbstractionOfLLWithConcreteFirstAndLast(
      boolean dll,
      int listLength,
      List<Value> listPtrs,
      BigInteger otherPtrOffset,
      BigInteger internalListPtrOffset)
      throws SMGException {
    assertThat(listPtrs).hasSize(listLength + 2);
    SMGObject listSegmentFront =
        currentState
            .dereferencePointerWithoutMaterilization(listPtrs.get(0))
            .orElseThrow()
            .getSMGObject();
    SMGObject listSegmentBack =
        currentState
            .dereferencePointerWithoutMaterilization(listPtrs.get(listLength + 1))
            .orElseThrow()
            .getSMGObject();

    assertThat(currentState.getMemoryModel().getSmg().isValid(listSegmentFront)).isTrue();
    assertThat(currentState.getMemoryModel().getSmg().isValid(listSegmentBack)).isTrue();
    // Check fst of abstracted
    Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedAbstrListFst =
        currentState.dereferencePointerWithoutMaterilization(listPtrs.get(1));
    assertThat(derefedAbstrListFst).isPresent();
    assertThat(derefedAbstrListFst.orElseThrow().hasSMGObjectAndOffset()).isTrue();
    SMGObject derefedFstObj = derefedAbstrListFst.orElseThrow().getSMGObject();
    assertThat(derefedFstObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
    if (dll) {
      assertThat(derefedFstObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getMinLength()).isEqualTo(listLength);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
      assertThat(((SMGDoublyLinkedListSegment) derefedFstObj).getPrevPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    } else {
      assertThat(derefedFstObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) derefedFstObj).getMinLength()).isEqualTo(listLength);
      assertThat(((SMGSinglyLinkedListSegment) derefedFstObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    }

    SMGValueAndSMGState readNextToAbstr =
        currentState.readSMGValue(listSegmentFront, nfo, pointerSizeInBits);
    Optional<SMGPointsToEdge> maybePTEToAbtrFst =
        currentState.getMemoryModel().getSmg().getPTEdge(readNextToAbstr.getSMGValue());
    assertThat(maybePTEToAbtrFst).isPresent();
    assertThat(maybePTEToAbtrFst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
    assertThat(maybePTEToAbtrFst.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);

    Optional<SMGPointsToEdge> ptrFromBeginning =
        currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(
                currentState.getMemoryModel().getSMGValueFromValue(listPtrs.get(1)).orElseThrow());
    assertThat(ptrFromBeginning.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
    assertThat(ptrFromBeginning.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);

    if (dll) {
      SMGValueAndSMGState readPrevOfFst =
          currentState.readSMGValue(derefedFstObj, pfo, pointerSizeInBits);
      Optional<SMGPointsToEdge> maybePTEPrevFromFstLst =
          currentState.getMemoryModel().getSmg().getPTEdge(readPrevOfFst.getSMGValue());
      assertThat(maybePTEPrevFromFstLst).isPresent();
      assertThat(maybePTEPrevFromFstLst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
      assertThat(maybePTEPrevFromFstLst.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_REGION);
      assertThat(maybePTEPrevFromFstLst.orElseThrow().pointsTo()).isEqualTo(listSegmentFront);
    }

    // Check lst of abstracted
    Optional<SMGStateAndOptionalSMGObjectAndOffset> derefedAbstrListLst =
        currentState.dereferencePointerWithoutMaterilization(listPtrs.get(listLength - 2));
    assertThat(derefedAbstrListLst).isPresent();
    assertThat(derefedAbstrListLst.orElseThrow().hasSMGObjectAndOffset()).isTrue();
    SMGObject derefedLastObj = derefedAbstrListLst.orElseThrow().getSMGObject();
    assertThat(derefedLastObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
    if (dll) {
      assertThat(derefedLastObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGDoublyLinkedListSegment) derefedLastObj).getMinLength())
          .isEqualTo(listLength);
      assertThat(((SMGDoublyLinkedListSegment) derefedLastObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
      assertThat(((SMGDoublyLinkedListSegment) derefedLastObj).getPrevPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    } else {
      assertThat(derefedLastObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      assertThat(((SMGSinglyLinkedListSegment) derefedLastObj).getMinLength())
          .isEqualTo(listLength);
      assertThat(((SMGSinglyLinkedListSegment) derefedLastObj).getNextPointerTargetOffset())
          .isEqualTo(internalListPtrOffset);
    }

    SMGValueAndSMGState readNextOfLstAbstr =
        currentState.readSMGValue(derefedLastObj, nfo, pointerSizeInBits);
    Optional<SMGPointsToEdge> maybePTENextFromAbtrLst =
        currentState.getMemoryModel().getSmg().getPTEdge(readNextOfLstAbstr.getSMGValue());
    assertThat(maybePTENextFromAbtrLst).isPresent();
    assertThat(maybePTENextFromAbtrLst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
    assertThat(maybePTENextFromAbtrLst.orElseThrow().targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_REGION);
    assertThat(maybePTENextFromAbtrLst.orElseThrow().pointsTo()).isEqualTo(listSegmentBack);

    if (dll) {
      SMGValueAndSMGState readPrevOfLast =
          currentState.readSMGValue(listSegmentBack, pfo, pointerSizeInBits);
      Optional<SMGPointsToEdge> maybePTEPrevFromLastLst =
          currentState.getMemoryModel().getSmg().getPTEdge(readPrevOfLast.getSMGValue());
      assertThat(maybePTEPrevFromLastLst).isPresent();
      assertThat(maybePTEPrevFromLastLst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
      assertThat(maybePTEPrevFromLastLst.orElseThrow().targetSpecifier())
          .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      assertThat(maybePTEPrevFromLastLst.orElseThrow().pointsTo()).isEqualTo(derefedLastObj);
    }
  }

  private void checkConcreteSLLWithDiffPtrOffsetsFirstAndLast(
      boolean dll,
      int listLength,
      List<Value> listPtrs,
      BigInteger otherPtrOffset,
      BigInteger internalListPtrOffset)
      throws SMGException {
    assertThat(listPtrs).hasSize(listLength + 2);
    for (int i = 0; i < listLength + 2; i++) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> deref =
          currentState.dereferencePointerWithoutMaterilization(listPtrs.get(i));
      assertThat(deref).isPresent();
      assertThat(deref.orElseThrow().hasSMGObjectAndOffset()).isTrue();
      SMGObject listSegment = deref.orElseThrow().getSMGObject();

      assertThat(currentState.getMemoryModel().getSmg().isValid(listSegment)).isTrue();
      assertThat(listSegment).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      // Check ptrs of abstracted
      if (i == 0) {
        // Check next, but not prev
        SMGValueAndSMGState readNextToAbstr =
            currentState.readSMGValue(listSegment, nfo, pointerSizeInBits);
        Optional<SMGPointsToEdge> maybePTEToAbtrFst =
            currentState.getMemoryModel().getSmg().getPTEdge(readNextToAbstr.getSMGValue());
        assertThat(maybePTEToAbtrFst).isPresent();
        assertThat(maybePTEToAbtrFst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
        assertThat(maybePTEToAbtrFst.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);

        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefNext =
            currentState.dereferencePointerWithoutMaterilization(listPtrs.get(i + 1));
        assertThat(derefNext).isPresent();
        assertThat(derefNext.orElseThrow().hasSMGObjectAndOffset()).isTrue();
        SMGObject listSegmentNext = derefNext.orElseThrow().getSMGObject();
        assertThat(maybePTEToAbtrFst.orElseThrow().pointsTo()).isEqualTo(listSegmentNext);

      } else if (i == listPtrs.size() - 1) {
        // check prev, but not next
        if (dll) {
          throw new RuntimeException("Implement me");
        }

      } else {
        // Check next and prev
        SMGValueAndSMGState readNextToAbstr =
            currentState.readSMGValue(listSegment, nfo, pointerSizeInBits);
        Optional<SMGPointsToEdge> maybePTEToAbtrFst =
            currentState.getMemoryModel().getSmg().getPTEdge(readNextToAbstr.getSMGValue());
        assertThat(maybePTEToAbtrFst).isPresent();
        if (i == listPtrs.size() - 2) {
          assertThat(maybePTEToAbtrFst.orElseThrow().getOffset()).isEqualTo(otherPtrOffset);
        } else {
          assertThat(maybePTEToAbtrFst.orElseThrow().getOffset()).isEqualTo(internalListPtrOffset);
        }
        assertThat(maybePTEToAbtrFst.orElseThrow().targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);

        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefNext =
            currentState.dereferencePointerWithoutMaterilization(listPtrs.get(i + 1));
        assertThat(derefNext).isPresent();
        assertThat(derefNext.orElseThrow().hasSMGObjectAndOffset()).isTrue();
        SMGObject listSegmentNext = derefNext.orElseThrow().getSMGObject();
        assertThat(maybePTEToAbtrFst.orElseThrow().pointsTo()).isEqualTo(listSegmentNext);

        if (dll) {
          throw new RuntimeException("Implement me");
        }
      }
    }
  }
}

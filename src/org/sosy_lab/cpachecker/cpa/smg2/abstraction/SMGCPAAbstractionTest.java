// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPATest0;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager.SMGCandidate;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
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

  /**
   * Creates a concrete list, then saves a pointer to a nested list in EACH segment. The lists are
   * then abstracted and checked. This works if we correctly check equality by shape and not pointer
   * identity.
   */
  @Test
  public void nestedListSLLTest() throws SMGException, SMGSolverException {
    // Smaller lengths are fine here, else this runs a while!
    // Increasing this is a good test for the overall performance of the SMGs!
    int listLength = 15;
    Value[] pointers = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointers, false);

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
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
    // TODO:
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
  @Test
  public void correctPointerNestingSLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(false, sllSize, lengthOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, lengthOfList - 1);
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
  @Test
  public void correctPointerNestingDLLTest() throws SMGException, SMGSolverException {
    int lengthOfList = 10;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(true, dllSize, lengthOfList);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, lengthOfList - 1);
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
          new SMGCPAAbstractionManager(currentState, lengthOfList - 1);
      currentState = absFinder.findAndAbstractLists();
      // Now we have a 10+SLS
      // Deref a pointer not in the beginning or end, check that the list is consistent with the
      // pointers and the nesting level and materialization is correct afterwards
      derefPointersAtAndCheckListMaterialization(
          lengthOfList, pointers, new int[] {lengthOfList - 1}, false);
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
   * Test that a list is correctly materialized to 0+ in the end and then correctly reabsorbed to
   * the original abstracted list with all pointers being correctly nested and no extra segments or
   * states added.
   */
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
          new SMGCPAAbstractionManager(currentState, lengthOfList - 1);
      currentState = absFinder.findAndAbstractLists();
      // Now we have a 10+SLS
      // Deref a pointer not in the beginning or end, check that the list is consistent with the
      // pointers and the nesting level and materialization is correct afterwards
      derefPointersAtAndCheckListMaterialization(
          lengthOfList, pointers, new int[] {lengthOfList - 1}, true);
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
   * Asserts that the only valid existing object is a SLL or DLL equaling the length given and nfo
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
    assertThat(numOfValidObjects).isEqualTo(1);
  }

  /**
   * Tests behaviour for the read of a pointer pointing to a 0+ list segment for SLLs. Should split
   * into 2 states, one where the 0+ is deleted and the end of the list nfo value is read, one for
   * the extension of the list.
   *
   * @throws SMGException not thrown
   */
  @Test
  public void zeroPlusRemovalSLLTest() throws SMGException, SMGSolverException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(false, sllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, sizeOfList);
    // Now we have a 3+SLS
    currentState = absFinder.findAndAbstractLists();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(pointers[sizeOfList - 1]);
    assertThat(statesAndResultingObjects).hasSize(1);
    SMGStateAndOptionalSMGObjectAndOffset stateAndResultingObject =
        statesAndResultingObjects.get(0);
    currentState = stateAndResultingObject.getSMGState();
    SMGObject lastConcreteListObject = stateAndResultingObject.getSMGObject();
    assertThat(stateAndResultingObject.getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
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
  @Test
  public void zeroPlusRemovalDLLTest() throws SMGException, SMGSolverException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(true, dllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, sizeOfList);
    // Now we have a 3+DLS
    currentState = absFinder.findAndAbstractLists();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(pointers[sizeOfList - 1]);
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
    // There should be exactly listSize normal SMGObjects that are invalid (not zero objects)
    // + listSize - 1 SLL objects that are invalid and the 0 object invalid
    assertThat(currentState.getMemoryModel().getSmg().getObjects())
        .hasSize(1 + TEST_LIST_LENGTH + TEST_LIST_LENGTH - 1);
    int normalObjectCounter = 0;
    Boolean[] found = new Boolean[TEST_LIST_LENGTH - 1];
    for (SMGObject object : currentState.getMemoryModel().getSmg().getObjects()) {
      if (object.isZero()) {
        continue;
      } else if (!(object instanceof SMGSinglyLinkedListSegment)) {
        normalObjectCounter++;
        assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
      } else {
        assertThat(found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2]).isNull();
        // We always start with at least element 2+
        found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2] = true;
        if (((SMGSinglyLinkedListSegment) object).getMinLength() == TEST_LIST_LENGTH) {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isTrue();
        } else {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
        }
      }
    }
    assertThat(normalObjectCounter).isEqualTo(TEST_LIST_LENGTH);
    for (boolean f : found) {
      assertThat(f).isTrue();
    }

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

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
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

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
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
          assertThat(pteMapping.getNestingLevel()).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   */
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
        new SMGCPAAbstractionManager(currentState, minAbstractionLength);
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
          assertThat(pteMapping.getNestingLevel()).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
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

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState =
          currentState.dereferencePointer(pointers[i]).get(0);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(newObj).isNotSameInstanceAs(SMGSinglyLinkedListSegment.class);
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      // get(0) takes the list that is not extending for 0+
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null).get(0);
      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
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
            assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj).isPresent();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat(nextObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        assertThat(((SMGDoublyLinkedListSegment) nextObj).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - i - 1);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }

        // Now get the next obj from the next pointer in the array. It should be the same obj
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjFromExternalPointer).isInstanceOf(SMGDoublyLinkedListSegment.class);
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        assertThat(((SMGDoublyLinkedListSegment) newObjFromExternalPointer).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - (i + 1));
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }

        assertThat(nextObj).isEqualTo(newObjFromExternalPointer);
      }

      // Back pointer equals pointers [i - 1]
      // TODO: back pointer
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
  @Test
  public void basicSLLFullAbstractionWithExternalPointerMaterializationTest()
      throws SMGException, SMGSolverException {
    Value[] pointers = buildConcreteSLL();

    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState =
          currentState.dereferencePointer(pointers[i]).get(0);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(newObj).isNotSameInstanceAs(SMGSinglyLinkedListSegment.class);
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      // get(0) takes the list that is not extending for 0+
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null).get(0);
      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
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
            assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj).isPresent();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat(nextObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        assertThat(((SMGSinglyLinkedListSegment) nextObj).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - i - 1);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i);
          }
        }

        // Now get the next obj from the next pointer in the array. It should be the same obj
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat(newObjFromExternalPointer).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        assertThat(((SMGSinglyLinkedListSegment) newObjFromExternalPointer).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - (i + 1));
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }

        assertThat(nextObj).isEqualTo(newObjFromExternalPointer);
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

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();
    return pointers;
  }

  @Test
  public void basicDLLMaterializationTest() throws SMGException, SMGSolverException {
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, dllSize, offset, hfo, nfo, pfo, TEST_LIST_LENGTH);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            pointerSizeInBits,
            new NumericValue(1),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(pfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    TEST_LIST_LENGTH - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    SMGObject[] previous = new SMGObject[TEST_LIST_LENGTH];
    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
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
        assertThat(object.orElseThrow().getSMGObject().getSize()).isEqualTo(dllSize);

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
        new SMGSinglyLinkedListSegment(0, sllSize, offset, hfo, nfo, TEST_LIST_LENGTH);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            pointerSizeInBits,
            new NumericValue(1),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
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
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
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
        assertThat(object.orElseThrow().getSMGObject().getSize()).isEqualTo(sllSize);

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

  @Test
  public void basicSLLDetectionTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();

      if (i > minAbstractionLength - 1) {
        assertThat(candidates).hasSize(1);
        assertThat(absFinder.isDLL(candidates.iterator().next(), state.getMemoryModel().getSmg()))
            .isEmpty();
      } else {
        assertThat(candidates).hasSize(0);
      }
    }
  }

  @Test
  public void basicDLLDetectionTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
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

  @SuppressWarnings("null")
  @Test
  public void abstractSLLTest() throws SMGException, SMGSolverException {
    // Minimum abstraction length before a list is abstracted
    int minAbstractionLength = 3;

    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoSLL(firstObj.getObject(), nfo, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGSinglyLinkedListSegment sll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGSinglyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(sll).isNull();
          sll = (SMGSinglyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(sll.getMinLength()).isEqualTo(i);
      assertThat(sll.getNextOffset()).isEqualTo(nfo);
      assertThat(sll.getSize()).isEqualTo(sllSize);
    }
  }

  @SuppressWarnings("null")
  @Test
  public void abstractDLLTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGDoublyLinkedListSegment dll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGDoublyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(dll).isNull();
          dll = (SMGDoublyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(dll.getMinLength()).isEqualTo(i);
      assertThat(dll.getNextOffset()).isEqualTo(nfo);
      assertThat(dll.getPrevOffset()).isEqualTo(pfo);
      assertThat(dll.getSize()).isEqualTo(dllSize);
      assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
    }
  }

  // Test the minimum length needed for abstraction
  @SuppressWarnings("null")
  @Test
  public void abstractDLLLimitTest() throws SMGException, SMGSolverException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;

    for (int minLength = minAbstractionLength;
        minLength < TEST_LIST_LENGTH;
        minLength = minLength + 10) {
      for (int i = 1; i < TEST_LIST_LENGTH; i++) {
        resetSMGStateAndVisitor();
        SMGState state = createXLongExplicitDLLOnHeap(i);
        SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, minLength);
        ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
        if (i < minLength) {
          assertThat(candidates.isEmpty()).isTrue();
          continue;
        }
        SMGCandidate firstObj = candidates.iterator().next();
        assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
        state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo, ImmutableSet.of());

        Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
        // All should be invalid except our SLL here
        SMGDoublyLinkedListSegment dll = null;
        for (SMGObject object : objects) {
          if (object instanceof SMGDoublyLinkedListSegment
              && state.getMemoryModel().isObjectValid(object)) {
            assertThat(dll).isNull();
            dll = (SMGDoublyLinkedListSegment) object;
          } else {
            assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
          }
        }
        assertThat(dll.getMinLength()).isEqualTo(i);
        assertThat(dll.getNextOffset()).isEqualTo(nfo);
        assertThat(dll.getPrevOffset()).isEqualTo(pfo);
        assertThat(dll.getSize()).isEqualTo(dllSize);
        assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
        assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      }
    }
  }

  // The next and prev pointers at the end point to 0. The nfo is offset 32, pfo 64.
  // value at offset 0 is 1
  private SMGState createXLongExplicitDLLOnHeap(int length)
      throws SMGException, SMGSolverException {
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, dllSize, offset, hfo, nfo, pfo, length);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            pointerSizeInBits,
            new NumericValue(1),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(pfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    length - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      if (i + 1 == TEST_LIST_LENGTH) {
        assertThat(resultValuesAndStates).hasSize(2);
        // 2 states, one for a extended list with 0+ and one without 0+ that is not extended
        // We don't check that in this test, but assume that the second item is the non extended
        // version
        // TODO: make this assumption concrete somehow
        resultValueAndState = resultValuesAndStates.get(1);
      } else {
        assertThat(resultValuesAndStates).hasSize(1);
        resultValueAndState = resultValuesAndStates.get(0);
      }
      currentState = resultValueAndState.getSMGState();
      if (i + 1 == length) {
        break;
      }
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);

      SMGObject newObj = targetAndOffset.getSMGObject();
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
          .isInstanceOf(SMGDoublyLinkedListSegment.class);
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
    return currentState;
  }

  // The next pointer at the end points to 0. nfo offset 32. value at 0 is 1.
  private SMGState createXLongExplicitSLLOnHeap(int length)
      throws SMGException, SMGSolverException {
    BigInteger offset = BigInteger.ZERO;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(0, sllSize, offset, hfo, nfo, length);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(hfo),
            pointerSizeInBits,
            new NumericValue(1),
            null,
            dummyCDAEdge);
    currentState =
        currentState.writeValueWithChecks(
            currentAbstraction,
            new NumericValue(nfo),
            pointerSizeInBits,
            new NumericValue(0),
            null,
            dummyCDAEdge);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    length - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      if (i + 1 == TEST_LIST_LENGTH) {
        assertThat(resultValuesAndStates).hasSize(2);
        // 2 states, one for a extended list with 0+ and one without 0+ that is not extended
        // We don't check that in this test, but assume that the second item is the non extended
        // version
        // TODO: make this assumption concrete somehow
        resultValueAndState = resultValuesAndStates.get(1);
      } else {
        assertThat(resultValuesAndStates).hasSize(1);
        resultValueAndState = resultValuesAndStates.get(0);
      }
      currentState = resultValueAndState.getSMGState();
      if (i + 1 == length) {
        break;
      }
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);

      SMGObject newObj = targetAndOffset.getSMGObject();
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
    int tmp = 0;
    assertThat(derefPositions[0]).isAtLeast(0);
    for (int num : derefPositions) {
      assertThat(tmp).isAtMost(num);
      tmp = num;
    }
    assertThat(derefPositions[derefPositions.length - 1]).isLessThan(totalSizeOfList);
    for (int k : derefPositions) {
      List<SMGStateAndOptionalSMGObjectAndOffset> derefAtList =
          currentState.dereferencePointer(pointers[k]);
      assertThat(derefAtList).hasSize(1);
      SMGStateAndOptionalSMGObjectAndOffset derefAt = derefAtList.get(0);
      currentState = derefAt.getSMGState();
      assertThat(derefAt.getSMGObject()).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
      // pointers 0 ... k-1 are now pointing to non-abstracted segments w nesting level 0
      for (int i = 0; i <= k; i++) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> derefWConcreteTarget =
            currentState.dereferencePointerWithoutMaterilization(pointers[i]);
        assertThat(derefWConcreteTarget).isPresent();
        SMGObject currentObj = derefWConcreteTarget.orElseThrow().getSMGObject();
        assertThat(currentObj).isNotInstanceOf(SMGSinglyLinkedListSegment.class);
        ValueAndSMGState addressAndState =
            currentState.searchOrCreateAddress(
                derefWConcreteTarget.orElseThrow().getSMGObject(), BigInteger.ZERO);
        currentState = addressAndState.getState();
        Value address = addressAndState.getValue();
        assertThat(address).isEqualTo(pointers[i]);
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(address)
                    .orElseThrow()
                    .getNestingLevel())
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
                      .getSMGValueFromValue(backPointer)
                      .orElseThrow()
                      .getNestingLevel())
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
                      .getSMGValueFromValue(backPointer)
                      .orElseThrow()
                      .getNestingLevel())
              .isEqualTo(0);
        }
      }
      // there are currently sizeOfList + 3 ZERO pointers total and not more or less
      if (k + 1 < totalSizeOfList) {
        assertThat(currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet())
            .hasSize(totalSizeOfList + 3);
      } else {
        // The last one has an additional pointer to 0+
        assertThat(currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet())
            .hasSize(totalSizeOfList + 4);
      }
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
                assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
              } else {
                assertThat(entry.getKey().getNestingLevel()).isEqualTo(totalSizeOfList - i - 1);
              }
            }
          }
          if (k + 1 == totalSizeOfList) {
            // next from last to 0+ is special as it is an added pointer
            ValueAndSMGState nextPointerToZeroPlusWithoutMaterialization =
                currentState.readValueWithoutMaterialization(
                    currentState
                        .dereferencePointer(pointers[totalSizeOfList - 1])
                        .get(0)
                        .getSMGObject(),
                    nfo,
                    pointerSizeInBits,
                    null);
            currentState = nextPointerToZeroPlusWithoutMaterialization.getState();
            if (entry
                .getKey()
                .equals(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(
                            nextPointerToZeroPlusWithoutMaterialization.getValue())
                        .orElseThrow())) {
              // The lone next pointer from the last segment to 0+
              assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
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
}

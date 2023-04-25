// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;

/*
 * Test equality and lessOrEqual methods for SMGs.
 * This is not trivial as we need to compare memory by shape and abstraction.
 */
public class SMGCPAEqualityTest extends SMGCPATest0 {

  // 8 seems like a reasonable compromise that tests everything and is not too slow
  private int listLength = 8;

  /**
   * Compare 2 lists that are equal, but one is abstracted, the other is not.
   *
   * @throws SMGException never thrown
   */
  @Test
  public void concreteAndAbstractedListLessOrEqualTest() throws SMGException {
    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    SMGObject abstractedObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
            .orElseThrow()
            .getSMGObject();
    Value[] pointersConcreteList = buildConcreteList(false, sllSize, listLength);
    SMGObject concreteObjBeginning =
        currentState
            .dereferencePointerWithoutMaterilization(pointersConcreteList[0])
            .orElseThrow()
            .getSMGObject();

    // They are unequal (for the first object compared) with all offsets as the nfo is not equal
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjBeginning,
                abstractedObj,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();

    // If the nfo is restricted, they are equal
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableList.of(nfo),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjBeginning,
                abstractedObj,
                ImmutableList.of(nfo),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();

    SMGObject concreteObjEnd =
        currentState
            .dereferencePointerWithoutMaterilization(pointersConcreteList[listLength - 1])
            .orElseThrow()
            .getSMGObject();
    // The last concrete obj is equal to the abstracted obj as the nfo match
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjEnd,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjEnd,
                abstractedObj,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();
  }

  /**
   * Test lessOrEqual for 2 lists with sublists, one is abstracted, the other is not. We expect them
   * to be not-equal, for the current implementation. This might change in the future. (i.e. merges)
   * In any case, a 10 long concrete list can not subsume a 10+ abstracted list, but the abstracted
   * can subsume the concrete.
   *
   * @throws SMGException never thrown.
   */
  @Test
  public void concreteAndAbstractedListWSublistLessOrEqualTest() throws SMGException {
    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList, false);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    SMGObject abstractedObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
            .orElseThrow()
            .getSMGObject();
    Value[] pointersConcreteList = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersConcreteList, false);
    SMGObject concreteObjBeginning =
        currentState
            .dereferencePointerWithoutMaterilization(pointersConcreteList[0])
            .orElseThrow()
            .getSMGObject();

    // nfo would not be equal!
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableList.of(nfo),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjBeginning,
                abstractedObj,
                ImmutableList.of(nfo),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();
  }

  /**
   * Make 3 lists. Two idendical, with the same sublists (abstracted, i.e. 10+) and one smaller
   * (i.e. 9+). The 10 should always equal the other 10, while the 9 should only be equal for the
   * input 10, 9 (in that order. Because of the <= relation. And yes 10 <= 9!! Because 9+ also
   * covers 10+, but 10+ not 9+)
   *
   * @throws SMGException never thrown
   */
  @Test
  public void abstractedListWSublistLessOrEqualTest() throws SMGException {
    Value[] pointersSmallerAbstractedList = buildConcreteList(false, sllSize, listLength - 1);
    addSubListsToList(listLength, pointersSmallerAbstractedList, false);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    SMGObject smallerAbstractedListObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersSmallerAbstractedList[0])
            .orElseThrow()
            .getSMGObject();

    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList, false);
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    SMGObject abstractedListObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
            .orElseThrow()
            .getSMGObject();

    Value[] pointersAbstractedList2 = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList2, false);
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    SMGObject abstractedListObj2 =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList2[0])
            .orElseThrow()
            .getSMGObject();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj2,
                abstractedListObj,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj,
                abstractedListObj2,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    // Comparing the abstracted objects returns TRUE as they both have the same sublists/values
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj,
                smallerAbstractedListObj,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                smallerAbstractedListObj,
                abstractedListObj,
                ImmutableList.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    // Compare the length of the top lists by comparing the shape
    assertThat(
            currentState.areValuesEqual(
                currentState,
                pointersSmallerAbstractedList[0],
                currentState,
                pointersAbstractedList[0],
                EqualityCache.<Value>of()))
        .isFalse();
    assertThat(
            currentState.areValuesEqual(
                currentState,
                pointersAbstractedList[0],
                currentState,
                pointersSmallerAbstractedList[0],
                EqualityCache.<Value>of()))
        .isTrue();
  }

  /**
   * Compare 2 lists with nested lists. We make 1 nested list shorter, such that it does not
   * abstract, should therefore not be equal.
   *
   * @throws SMGException never thrown
   * @throws InvalidConfigurationException never thrown
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest()
      throws SMGException, InvalidConfigurationException {
    for (int i = 0; i < listLength; i++) {
      resetSMGStateAndVisitor();
      Value[] pointersAbstractedShortList = buildConcreteList(false, sllSize, listLength);
      int counter = 0;
      for (Value pointer : pointersAbstractedShortList) {
        // Generate the same list for each top list segment and save the first pointer as data
        Value[] pointersNested;
        if (i == counter) {
          // Make 1 list shorter
          pointersNested = buildConcreteList(false, sllSize, listLength / 2);
        } else {
          pointersNested = buildConcreteList(false, sllSize, listLength);
        }
        // We care only about the first pointer here
        SMGStateAndOptionalSMGObjectAndOffset topListSegmentAndState =
            currentState.dereferencePointerWithoutMaterilization(pointer).orElseThrow();
        currentState = topListSegmentAndState.getSMGState();
        SMGObject topListSegment = topListSegmentAndState.getSMGObject();
        currentState =
            currentState.writeValue(
                topListSegment,
                hfo,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(pointersNested[0])
                    .orElseThrow());
        counter++;
      }
      SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength);
      currentState = absFinder.findAndAbstractLists();
      SMGObject abstractedObjShort =
          currentState
              .dereferencePointerWithoutMaterilization(pointersAbstractedShortList[0])
              .orElseThrow()
              .getSMGObject();
      // This can't get abstracted with the current limits as the shape of the nested memory is not
      // equal
      assertThat(abstractedObjShort instanceof SMGSinglyLinkedListSegment).isFalse();

      // Abstracted complete list
      Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersAbstractedList, false);
      absFinder = new SMGCPAAbstractionManager(currentState, listLength);
      currentState = absFinder.findAndAbstractLists();
      SMGObject abstractedObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
              .orElseThrow()
              .getSMGObject();

      // Concrete complete list
      Value[] pointersOtherList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersOtherList, false);
      absFinder = new SMGCPAAbstractionManager(currentState, listLength);
      currentState = absFinder.findAndAbstractLists();
      SMGObject concreteObjBeginning =
          currentState
              .dereferencePointerWithoutMaterilization(pointersOtherList[0])
              .orElseThrow()
              .getSMGObject();

      // Check that the shortened list is not equal the abstracted or the concrete list
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  abstractedObjShort,
                  concreteObjBeginning,
                  ImmutableList.of(),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();

      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  abstractedObj,
                  abstractedObjShort,
                  ImmutableList.of(),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
    }
  }

  /**
   * Make 3 lists. All have the same length, all have sublists with the same length. 1 list get 1
   * changed value in the nested lists such that they are no longer abstractable. Then compare if
   * they are equal by shape with 2 lists, one concrete, one abstracted with all values equal expect
   * that one. None should be equal.
   *
   * @throws SMGException never thrown
   * @throws InvalidConfigurationException never thrown
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest2()
      throws SMGException, InvalidConfigurationException {
    for (int i = 0; i < listLength; i++) {
      resetSMGStateAndVisitor();
      Value[] pointersConcreteDifferentList = buildConcreteList(false, sllSize, listLength);
      Value[][] nestedDifferentLists =
          addSubListsToList(listLength, pointersConcreteDifferentList, false);
      SMGObject ithObj =
          currentState
              .dereferencePointerWithoutMaterilization(nestedDifferentLists[i][i])
              .orElseThrow()
              .getSMGObject();
      currentState =
          currentState.writeValueTo(
              ithObj, BigInteger.ZERO, pointerSizeInBits, new NumericValue(-1), null, dummyCDAEdge);

      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength - 1);
      currentState = absFinder.findAndAbstractLists();
      SMGObject notAbstractedListDifferentObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersConcreteDifferentList[i])
              .orElseThrow()
              .getSMGObject();
      // This can't get abstracted with the changed value as the shape of the nested memory is not
      // equal
      assertThat(notAbstractedListDifferentObj instanceof SMGSinglyLinkedListSegment).isFalse();

      // Abstracted complete list
      Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersAbstractedList, false);
      absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
      currentState = absFinder.findAndAbstractLists();
      SMGObject abstractedObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
              .orElseThrow()
              .getSMGObject();

      // Concrete complete list
      Value[] pointersConcreteList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersConcreteList, false);
      absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
      currentState = absFinder.findAndAbstractLists();
      SMGObject concreteObjBeginning =
          currentState
              .dereferencePointerWithoutMaterilization(pointersConcreteList[0])
              .orElseThrow()
              .getSMGObject();

      // Check that the shortened list is not equal the abstracted or the concrete list
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  notAbstractedListDifferentObj,
                  concreteObjBeginning,
                  ImmutableList.of(nfo),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  concreteObjBeginning,
                  notAbstractedListDifferentObj,
                  ImmutableList.of(nfo),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();

      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  abstractedObj,
                  notAbstractedListDifferentObj,
                  ImmutableList.of(nfo),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  notAbstractedListDifferentObj,
                  abstractedObj,
                  ImmutableList.of(nfo),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
    }
  }

  /**
   * We have a list, we check the next components' existence, then we move the current pointer to
   * the next and free the prev segment. The resulting list should be covered by the previous.
   */
  @Test
  public void testFreeLoopEquality()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    Value[] pointersConcreteDifferentList = buildConcreteList(true, dllSize, listLength);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1);
    currentState = absFinder.findAndAbstractLists();
    // "free" list except for last segment
    // We explicitly deref the current segment and read the next pointer beforehand
    SMGCPAExportOptions exportOptions = new SMGCPAExportOptions(null, SMGExportLevel.NEVER);
    SMGCPAExpressionEvaluator evaluator =
        new SMGCPAExpressionEvaluator(machineModel, logger, exportOptions, smgOptions);
    Value lastNextPointer = null;
    for (int i = 0; i < listLength; i++) {
      List<SMGStateAndOptionalSMGObjectAndOffset> deref =
          currentState.dereferencePointer(pointersConcreteDifferentList[i]);
      // Should only be 1 list element
      assertThat(deref).hasSize(1);
      currentState = deref.get(0).getSMGState();
      assertThat(deref.get(0).hasSMGObjectAndOffset()).isTrue();
      assertThat(deref.get(0).getOffsetForObject()).isEqualTo(BigInteger.ZERO);
      SMGObject curr = deref.get(0).getSMGObject();
      List<ValueAndSMGState> readNexts =
          evaluator.readValueWithPointerDereference(
              currentState, pointersConcreteDifferentList[i], nfo, pointerSizeInBits, null);
      ValueAndSMGState readNext;
      if (i < listLength - 1) {
        // Should only be 1 list element
        assertThat(readNexts).hasSize(1);
        readNext = readNexts.get(0);
      } else {
        assertThat(readNexts).hasSize(2);
        readNext = readNexts.get(1);
      }
      // We read the next pointer pointing to an abstracted list, hence this list was materialized

      currentState = readNext.getState();
      Value readPointer = readNext.getValue();
      if (i + 1 < listLength) {
        assertThat(readPointer).isEqualTo(pointersConcreteDifferentList[i + 1]);
      } else {
        lastNextPointer = readPointer;
      }
      SMGObject notAbstractedListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(notAbstractedListObj.isSLL()).isFalse();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isTrue();
      // Free current list segment
      List<SMGState> newStatesAfterFree =
          currentState.free(pointersConcreteDifferentList[i], null, null);
      assertThat(newStatesAfterFree).hasSize(1);
      currentState = newStatesAfterFree.get(0);
      notAbstractedListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isTrue();
      notAbstractedListObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersConcreteDifferentList[i])
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isFalse();
    }
    // Now we save the state for later
    SMGState stateW1Left = currentState;
    // Now read the next pointer (last), throw away the extra state, save pointer to new segment
    // and free current pointer
    // confirm that the last one is correct first
    List<SMGStateAndOptionalSMGObjectAndOffset> deref =
        currentState.dereferencePointer(lastNextPointer);
    // Should only be 1 list element
    assertThat(deref).hasSize(1);
    currentState = deref.get(0).getSMGState();
    assertThat(deref.get(0).hasSMGObjectAndOffset()).isTrue();
    assertThat(deref.get(0).getOffsetForObject()).isEqualTo(BigInteger.ZERO);
    List<ValueAndSMGState> readNextsInLast =
        evaluator.readValueWithPointerDereference(
            currentState, lastNextPointer, nfo, pointerSizeInBits, null);
    // Should only be 1 list element
    assertThat(readNextsInLast).hasSize(2);
    // When materializing, the first element is the minimal element, confirm that the value is 0
    assertThat(readNextsInLast.get(0).getValue().isNumericValue()).isTrue();
    assertThat(readNextsInLast.get(0).getValue().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    // Confirm that the other is materialized correctly
    currentState = readNextsInLast.get(1).getState();
    Value readNextPointer = readNextsInLast.get(1).getValue();
    SMGObject materializedList =
        currentState
            .dereferencePointerWithoutMaterilization(readNextPointer)
            .orElseThrow()
            .getSMGObject();
    assertThat(materializedList.isSLL()).isFalse();
    ValueAndSMGState pointerToZeroPlus =
        currentState.readValueWithoutMaterialization(
            materializedList, nfo, pointerSizeInBits, null);
    assertThat(currentState.getMemoryModel().pointsToZeroPlus(pointerToZeroPlus.getValue()))
        .isTrue();
    // Now we free the list element from before
    List<SMGState> freeList = currentState.free(lastNextPointer, null, null);
    assertThat(freeList).hasSize(1);
    currentState = freeList.get(0);
    // Compare the 2 states from before and now
    assertThat(currentState.isLessOrEqual(stateW1Left)).isTrue();
  }
}

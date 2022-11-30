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
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;

/*
 * Test equality and lessOrEqual methods for SMGs.
 * This is not trivial as we need to compare memory by shape and abstraction.
 */
public class SMGCPAEqualityTest extends SMGCPATest0 {

  @Test
  public void concreteAndAbstractedListLessOrEqualTest() throws SMG2Exception {
    int listLength = 15;
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
   * @throws SMG2Exception: never thrown.
   */
  @Test
  public void concreteAndAbstractedListWSublistLessOrEqualTest() throws SMG2Exception {
    int listLength = 10;
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

  /*
   * Make 3 lists. Two idendical, with the same sublists (abstracted, i.e. 10+) and one smaller
   * (i.e. 9+). The 10 should always equal the other 10, while the 9 should only be equal for the
   * input 10, 9 (in that order. Because of the <= relation. And yes 10 <= 9!!
   * Because 9+ also covers 10+, but 10+ not 9+)
   */
  @Test
  public void abstractedListWSublistLessOrEqualTest() throws SMG2Exception {
    int listLength = 10;
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

  /*
   * Compare 2 lists with nested lists. We make 1 nested list shorter, such that it does not abstract, should therefore not be equal.
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest()
      throws SMG2Exception, InvalidConfigurationException {
    int listLength = 6;
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

  /*
   * Make 3 lists. All have the same length, all have sublists with the same length.
   * 1 list get 1 changed value in the nested lists such that they are no longer abstractable.
   * Then compare if they are equal by shape with 2 lists, one concrete, one abstracted with all
   * values equal expect that one. None should be equal.
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest2()
      throws SMG2Exception, InvalidConfigurationException {
    int listLength = 6;
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
}

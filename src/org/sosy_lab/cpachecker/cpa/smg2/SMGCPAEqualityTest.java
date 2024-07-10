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
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/*
 * Test equality and lessOrEqual methods for SMGs.
 * This is not trivial as we need to compare memory by shape and abstraction.
 */
public class SMGCPAEqualityTest extends SMGCPATest0 {

  // 8 seems like a reasonable compromise that tests everything and is not too slow
  private static final int listLength = 8;

  /*
   * concrete list element = CLE
   * CLE -> X+ -> CLE -> 0+ -> 0
   * With a pointer towards both CLEs and X >= 0
   */
  @Test
  public void SLLWithConcreteElementsLessOrEqualTest() throws SMGException, SMGSolverException {
    // TODO: concrete list element = CLE
    //  CLE -> X+ -> CLE -> 0+ -> 0
    //  With a pointer towards both CLEs and X >= 0
    Value[] pointersToFstAndLst =
        buildConcreteListReturnFstAndLstPointer(false, sllSize, listLength);
    // Dummy assertion
    assertThat(pointersToFstAndLst).isNotEmpty();
  }

  /*
   * concrete list element = CLE
   * CLE -> X+ -> CLE -> 0+ -> 0
   * With a pointer towards both CLEs and X >= 0
   */
  @Test
  public void DLLWithConcreteElementsLessOrEqualTest() throws SMGException, SMGSolverException {
    // TODO: concrete list element = CLE
    //  CLE -> X+ -> CLE -> 0+ -> 0
    //  With a pointer towards both CLEs and X >= 0
    Value[] pointersToFstAndLst =
        buildConcreteListReturnFstAndLstPointer(true, dllSize, listLength);
    // Dummy assertion
    assertThat(pointersToFstAndLst).isNotEmpty();
  }

  /*
   * 2 lists with different lengths X, with a pointer somewhere,  -> X+ -> 0, should be equal only if the pointer is at a comparable location (examples are last and first)
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void SLLDifferentLengthSomePointerLessOrEqualTest()
      throws CPAException, InterruptedException {
    int maxListLen = 15;
    for (int i = 3; i < maxListLen; i = i + 3) {
      resetSMGStateAndVisitor();
      SMGState stateWithoutSmallerList = currentState;
      Value[] pointersToFstAndLstSmallerList =
          buildConcreteListReturnFstAndLstPointer(false, sllSize, i);
      currentState =
          new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
              .findAndAbstractLists();
      assertThatPointersPointToEqualAbstractedList(currentState, i, pointersToFstAndLstSmallerList);
      SMGState stateWithSmallerList = currentState;
      for (int j = i; j < maxListLen + 1; j++) {
        currentState = stateWithoutSmallerList;
        Value[] pointersToFstAndLstLargerList =
            buildConcreteListReturnFstAndLstPointer(false, sllSize, j);
        currentState =
            new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
                .findAndAbstractLists();
        assertThatPointersPointToEqualAbstractedList(
            currentState, j, pointersToFstAndLstLargerList);
        SMGState stateWithBiggerListWOLast = currentState.copyAndRemoveStackVariable("last");
        SMGState stateWithSmallerListWOLast =
            stateWithSmallerList.copyAndRemoveStackVariable("last");
        SMGState stateWithBiggerListWOFirst = currentState.copyAndRemoveStackVariable("first");
        SMGState stateWithSmallerListWOFirst =
            stateWithSmallerList.copyAndRemoveStackVariable("first");
        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOLast)).isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOLast)).isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(stateWithBiggerListWOLast.isLessOrEqual(stateWithSmallerListWOLast)).isTrue();

        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOFirst))
              .isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOFirst))
              .isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(stateWithBiggerListWOFirst.isLessOrEqual(stateWithSmallerListWOFirst)).isTrue();

        assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOFirst)).isFalse();
        assertThat(stateWithBiggerListWOFirst.isLessOrEqual(stateWithSmallerListWOLast)).isFalse();

        assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOLast)).isFalse();
        assertThat(stateWithBiggerListWOLast.isLessOrEqual(stateWithSmallerListWOFirst)).isFalse();
      }
    }
  }

  /*
   * 2 lists with different lengths X, with a pointer somewhere,  -> X+ -> 0, should be equal only if the pointer is at a comparable location (examples are last and first)
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void DLLDifferentLengthSomePointerLessOrEqualTest()
      throws CPAException, InterruptedException {
    int maxListLen = 15;
    for (int i = 3; i < maxListLen; i = i + 3) {
      resetSMGStateAndVisitor();
      SMGState stateWithoutSmallerList = currentState;
      Value[] pointersToFstAndLstSmallerList =
          buildConcreteListReturnFstAndLstPointer(true, dllSize, i);
      currentState =
          new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
              .findAndAbstractLists();
      assertThatPointersPointToEqualAbstractedList(currentState, i, pointersToFstAndLstSmallerList);
      SMGState stateWithSmallerList = currentState;
      for (int j = i; j < maxListLen + 1; j++) {
        currentState = stateWithoutSmallerList;
        Value[] pointersToFstAndLstLargerList =
            buildConcreteListReturnFstAndLstPointer(true, dllSize, j);
        currentState =
            new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
                .findAndAbstractLists();
        assertThatPointersPointToEqualAbstractedList(
            currentState, j, pointersToFstAndLstLargerList);
        SMGState stateWithBiggerListWOLast = currentState.copyAndRemoveStackVariable("last");
        SMGState stateWithSmallerListWOLast =
            stateWithSmallerList.copyAndRemoveStackVariable("last");
        SMGState stateWithBiggerListWOFirst = currentState.copyAndRemoveStackVariable("first");
        SMGState stateWithSmallerListWOFirst =
            stateWithSmallerList.copyAndRemoveStackVariable("first");
        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOLast)).isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOLast)).isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(stateWithBiggerListWOLast.isLessOrEqual(stateWithSmallerListWOLast)).isTrue();

        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOFirst))
              .isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOFirst))
              .isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(stateWithBiggerListWOFirst.isLessOrEqual(stateWithSmallerListWOFirst)).isTrue();

        assertThat(stateWithSmallerListWOLast.isLessOrEqual(stateWithBiggerListWOFirst)).isFalse();
        assertThat(stateWithBiggerListWOFirst.isLessOrEqual(stateWithSmallerListWOLast)).isFalse();

        assertThat(stateWithSmallerListWOFirst.isLessOrEqual(stateWithBiggerListWOLast)).isFalse();
        assertThat(stateWithBiggerListWOLast.isLessOrEqual(stateWithSmallerListWOFirst)).isFalse();
      }
    }
  }

  /*
   * 2 lists with different lengths X, with a first and last pointer,  -> X+ -> 0, should be equal
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void SLLDifferentLengthFstAndLstPointerLessOrEqualTest()
      throws CPAException, InterruptedException {
    int maxListLen = 15;
    for (int i = 3; i < maxListLen; i = i + 3) {
      resetSMGStateAndVisitor();
      SMGState stateWithoutSmallerList = currentState;
      Value[] pointersToFstAndLstSmallerList =
          buildConcreteListReturnFstAndLstPointer(false, sllSize, i);
      currentState =
          new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
              .findAndAbstractLists();
      assertThatPointersPointToEqualAbstractedList(currentState, i, pointersToFstAndLstSmallerList);
      SMGState stateWithSmallerList = currentState;
      for (int j = i; j < maxListLen + 1; j++) {
        currentState = stateWithoutSmallerList;
        Value[] pointersToFstAndLstLargerList =
            buildConcreteListReturnFstAndLstPointer(false, sllSize, j);
        currentState =
            new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
                .findAndAbstractLists();
        assertThatPointersPointToEqualAbstractedList(
            currentState, j, pointersToFstAndLstLargerList);
        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerList.isLessOrEqual(currentState)).isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerList.isLessOrEqual(currentState)).isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(currentState.isLessOrEqual(stateWithSmallerList)).isTrue();
      }
    }
  }

  /*
   * 2 lists with different lengths X, with a first and last pointer,  -> X+ -> 0, should be equal
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void DLLDifferentLengthFstAndLstPointerLessOrEqualTest()
      throws CPAException, InterruptedException {
    int maxListLen = 15;
    for (int i = 3; i < maxListLen; i = i + 3) {
      resetSMGStateAndVisitor();
      SMGState stateWithoutSmallerList = currentState;
      Value[] pointersToFstAndLstSmallerList =
          buildConcreteListReturnFstAndLstPointer(true, dllSize, i);
      currentState =
          new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
              .findAndAbstractLists();
      assertThatPointersPointToEqualAbstractedList(currentState, i, pointersToFstAndLstSmallerList);
      SMGState stateWithSmallerList = currentState;
      for (int j = i; j < maxListLen + 1; j++) {
        currentState = stateWithoutSmallerList;
        Value[] pointersToFstAndLstLargerList =
            buildConcreteListReturnFstAndLstPointer(true, dllSize, j);
        currentState =
            new SMGCPAAbstractionManager(currentState, 3, new SMGCPAStatistics())
                .findAndAbstractLists();
        assertThatPointersPointToEqualAbstractedList(
            currentState, j, pointersToFstAndLstLargerList);
        if (i != j) {
          // Bigger does not subsume the smaller
          assertThat(stateWithSmallerList.isLessOrEqual(currentState)).isFalse();
        } else {
          // Equal lists
          assertThat(stateWithSmallerList.isLessOrEqual(currentState)).isTrue();
        }
        // Bigger list (currentState) is subsumed by the smaller
        assertThat(currentState.isLessOrEqual(stateWithSmallerList)).isTrue();
      }
    }
  }

  /**
   * Compare 2 lists that are equal, but one is abstracted, the other is not.
   *
   * @throws SMGException never thrown
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void concreteAndAbstractedListLessOrEqualTest() throws SMGException, SMGSolverException {
    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
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
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjBeginning,
                abstractedObj,
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();

    // If the nfo is restricted, they are equal
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableMap.of(
                    abstractedObj,
                    ImmutableList.of(nfo),
                    concreteObjBeginning,
                    ImmutableList.of(nfo)),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableMap.of(
                    abstractedObj,
                    ImmutableList.of(nfo),
                    concreteObjBeginning,
                    ImmutableList.of(nfo)),
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
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.of()))
        .isTrue();
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                concreteObjEnd,
                abstractedObj,
                ImmutableMap.of(),
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
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void concreteAndAbstractedListWSublistLessOrEqualTest()
      throws SMGException, SMGSolverException {
    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList, false);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
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
                ImmutableMap.of(
                    abstractedObj,
                    ImmutableList.of(nfo),
                    concreteObjBeginning,
                    ImmutableList.of(nfo)),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedObj,
                concreteObjBeginning,
                ImmutableMap.of(
                    abstractedObj,
                    ImmutableList.of(nfo),
                    concreteObjBeginning,
                    ImmutableList.of(nfo)),
                currentState,
                currentState,
                EqualityCache.of()))
        .isFalse();
  }

  // Build some SLL elements that are connected but have (non nfo) pointers to
  // themselves w differing offsets. Tries NFO in different locations.
  @Test
  public void concreteSLLWithSelfPointerDifferingOffsetsTest()
      throws SMGException, SMGSolverException {
    BigInteger listSize = sllSize.add(pointerSizeInBits);
    for (List<BigInteger> offsetsForPointers :
        ImmutableList.of(
            ImmutableList.of(pointerSizeInBits, pointerSizeInBits.add(pointerSizeInBits)),
            ImmutableList.of(pointerSizeInBits.add(pointerSizeInBits), pointerSizeInBits))) {
      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        Value[] pointersConcreteList =
            buildConcreteList(false, sllSize.add(pointerSizeInBits), listLength, false);

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
                    .getSMGValueFromValue(pointersConcreteList[0])
                    .orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        for (Value ptr : pointersConcreteList) {
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

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(obj, offsetsForPointers.get(0));
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
              currentState.searchOrCreateAddress(obj, offsetsForPointers.get(1));
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

        // Check equality of succeeding list elements
        //   (Not equal for no excluded offsets, equal for excluded nfo)
        for (int i = 0; i < pointersConcreteList.length - 1; i++) {
          Value ptr1 = pointersConcreteList[i];
          Value ptr2 = pointersConcreteList[i + 1];
          SMGObject obj1 =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr1)
                  .orElseThrow()
                  .getSMGObject();
          SMGObject obj2 =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr2)
                  .orElseThrow()
                  .getSMGObject();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj1,
                      obj2,
                      ImmutableMap.of(),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj2,
                      obj1,
                      ImmutableMap.of(),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj1,
                      obj2,
                      ImmutableMap.of(
                          obj1, ImmutableList.of(changingNfo), obj2, ImmutableList.of(changingNfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isTrue();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj2,
                      obj1,
                      ImmutableMap.of(
                          obj1, ImmutableList.of(changingNfo), obj2, ImmutableList.of(changingNfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isTrue();
        }
      }
    }
  }

  // Build some DLL elements that are connected but have (non nfo/pfo) pointers to
  // themselves w differing offsets. Tries NFO/PFO in different locations.
  @Test
  public void concreteDLLWithSelfPointerDifferingOffsetsTest()
      throws SMGException, SMGSolverException {
    BigInteger listSize = dllSize.add(pointerSizeInBits);
    for (List<BigInteger> offsetsForPointers :
        ImmutableList.of(
            ImmutableList.of(pointerSizeInBits, pointerSizeInBits.add(pointerSizeInBits)),
            ImmutableList.of(pointerSizeInBits.add(pointerSizeInBits), pointerSizeInBits))) {
      // Respect that there is a PFO after the NFO!
      for (BigInteger changingNfo = BigInteger.ZERO;
          changingNfo.compareTo(listSize.subtract(pointerSizeInBits)) < 0;
          changingNfo = changingNfo.add(pointerSizeInBits)) {
        BigInteger changingPfo = changingNfo.add(pointerSizeInBits);
        Value[] pointersConcreteList = buildConcreteList(true, listSize, listLength, false);

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
                    .getSMGValueFromValue(pointersConcreteList[0])
                    .orElseThrow());

        BigInteger offsetFirstSelfPtr = BigInteger.ZERO;
        BigInteger offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);

        for (Value ptr : pointersConcreteList) {
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

            // Prev pointer
            Value prevPtr =
                currentState
                    .readValueWithoutMaterialization(obj, pfo, pointerSizeInBits, null)
                    .getValue();
            assertThat(currentState.getMemoryModel().isPointer(prevPtr)).isTrue();
            currentState =
                currentState.writeValueWithoutChecks(
                    obj,
                    changingNfo.add(pointerSizeInBits),
                    pointerSizeInBits,
                    currentState.getMemoryModel().getSMGValueFromValue(prevPtr).orElseThrow());
          }

          if (changingNfo.equals(BigInteger.ZERO)) {
            offsetFirstSelfPtr = pointerSizeInBits.add(pointerSizeInBits);
            offsetSecondSelfPtr = offsetFirstSelfPtr.add(pointerSizeInBits);
          } else if (changingNfo.equals(pointerSizeInBits)) {
            offsetSecondSelfPtr = changingNfo.add(pointerSizeInBits).add(pointerSizeInBits);
          }

          assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo);
          assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo);
          assertThat(offsetFirstSelfPtr).isNotEqualTo(changingNfo.add(pointerSizeInBits));
          assertThat(offsetSecondSelfPtr).isNotEqualTo(changingNfo.add(pointerSizeInBits));
          assertThat(offsetFirstSelfPtr).isNotEqualTo(offsetSecondSelfPtr);
          assertThat(offsetFirstSelfPtr.compareTo(listSize) < 0).isTrue();
          assertThat(offsetSecondSelfPtr.compareTo(listSize) < 0).isTrue();
          assertThat(changingNfo.compareTo(listSize) < 0).isTrue();
          assertThat(changingNfo.add(pointerSizeInBits).compareTo(listSize) < 0).isTrue();

          // Build self pointers w differing target offsets into the list
          ValueAndSMGState selfPtrAndState =
              currentState.searchOrCreateAddress(obj, offsetsForPointers.get(0));
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
              currentState.searchOrCreateAddress(obj, offsetsForPointers.get(1));
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

        // Check equality of succeeding list elements
        //   (Not equal for no excluded offsets, equal for excluded nfo)
        for (int i = 0; i < pointersConcreteList.length - 1; i++) {
          Value ptr1 = pointersConcreteList[i];
          Value ptr2 = pointersConcreteList[i + 1];
          SMGObject obj1 =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr1)
                  .orElseThrow()
                  .getSMGObject();
          SMGObject obj2 =
              currentState
                  .dereferencePointerWithoutMaterilization(ptr2)
                  .orElseThrow()
                  .getSMGObject();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj1,
                      obj2,
                      ImmutableMap.of(),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj2,
                      obj1,
                      ImmutableMap.of(),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj1,
                      obj2,
                      ImmutableMap.of(
                          obj1, ImmutableList.of(changingNfo), obj2, ImmutableList.of(changingNfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj2,
                      obj1,
                      ImmutableMap.of(
                          obj1, ImmutableList.of(changingNfo), obj2, ImmutableList.of(changingNfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isFalse();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj1,
                      obj2,
                      ImmutableMap.of(
                          obj1,
                          ImmutableList.of(changingNfo, changingPfo),
                          obj2,
                          ImmutableList.of(changingNfo, changingPfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isTrue();

          assertThat(
                  currentState.checkEqualValuesForTwoStatesWithExemptions(
                      obj2,
                      obj1,
                      ImmutableMap.of(
                          obj1,
                          ImmutableList.of(changingNfo, changingPfo),
                          obj2,
                          ImmutableList.of(changingNfo, changingPfo)),
                      currentState,
                      currentState,
                      EqualityCache.<Value>of()))
              .isTrue();
        }
      }
    }
  }

  /**
   * Make 3 lists. Two idendical, with the same sublists (abstracted, i.e. 10+) and one smaller
   * (i.e. 9+). The 10 should always equal the other 10, while the 9 should only be equal for the
   * input 10, 9 (in that order. Because of the <= relation. And yes 10 <= 9!! Because 9+ also
   * covers 10+, but 10+ not 9+)
   *
   * @throws SMGException never thrown
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void abstractedListWSublistLessOrEqualTest() throws SMGException, SMGSolverException {
    Value[] pointersSmallerAbstractedList = buildConcreteList(false, sllSize, listLength - 1);
    addSubListsToList(listLength, pointersSmallerAbstractedList, false);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Check that there is no more abstraction found
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    SMGState state2 = absFinder.findAndAbstractLists();
    assertThat(state2.getMemoryModel().getSmg().getObjects())
        .isEqualTo(currentState.getMemoryModel().getSmg().getObjects());
    SMGObject smallerAbstractedListObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersSmallerAbstractedList[0])
            .orElseThrow()
            .getSMGObject();

    Value[] pointersAbstractedList = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList, false);
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Check that there is no more abstraction found
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    state2 = absFinder.findAndAbstractLists();
    assertThat(state2.getMemoryModel().getSmg().getObjects())
        .isEqualTo(currentState.getMemoryModel().getSmg().getObjects());
    SMGObject abstractedListObj =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
            .orElseThrow()
            .getSMGObject();

    Value[] pointersAbstractedList2 = buildConcreteList(false, sllSize, listLength);
    addSubListsToList(listLength, pointersAbstractedList2, false);
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // Check that there is no more abstraction found
    absFinder = new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    state2 = absFinder.findAndAbstractLists();
    assertThat(state2.getMemoryModel().getSmg().getObjects())
        .isEqualTo(currentState.getMemoryModel().getSmg().getObjects());
    SMGObject abstractedListObj2 =
        currentState
            .dereferencePointerWithoutMaterilization(pointersAbstractedList2[0])
            .orElseThrow()
            .getSMGObject();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj2,
                abstractedListObj,
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj,
                abstractedListObj2,
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    // Comparing the abstracted objects returns TRUE as they both have the same sublists/values
    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                abstractedListObj,
                smallerAbstractedListObj,
                ImmutableMap.of(),
                currentState,
                currentState,
                EqualityCache.<Value>of()))
        .isTrue();

    assertThat(
            currentState.checkEqualValuesForTwoStatesWithExemptions(
                smallerAbstractedListObj,
                abstractedListObj,
                ImmutableMap.of(),
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
                EqualityCache.<Value>of(),
                EqualityCache.of()))
        .isFalse();
    assertThat(
            currentState.areValuesEqual(
                currentState,
                pointersAbstractedList[0],
                currentState,
                pointersSmallerAbstractedList[0],
                EqualityCache.<Value>of(),
                EqualityCache.of()))
        .isTrue();
  }

  /**
   * Compare 2 lists with nested lists. We make 1 nested list shorter, such that it does not
   * abstract, should therefore not be equal.
   *
   * @throws SMGException never thrown
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest() throws SMGException, SMGSolverException {
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
            currentState.writeValueWithoutChecks(
                topListSegment,
                hfo,
                pointerSizeInBits,
                currentState
                    .getMemoryModel()
                    .getSMGValueFromValue(pointersNested[0])
                    .orElseThrow());
        counter++;
      }
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
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
      absFinder = new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      SMGObject abstractedObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
              .orElseThrow()
              .getSMGObject();

      // Concrete complete list
      Value[] pointersOtherList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersOtherList, false);
      absFinder = new SMGCPAAbstractionManager(currentState, listLength, new SMGCPAStatistics());
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
                  ImmutableMap.of(),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();

      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  abstractedObj,
                  abstractedObjShort,
                  ImmutableMap.of(),
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
   */
  @Test
  public void abstractedListWSublistNotLessOrEqualTest2() throws SMGException, SMGSolverException {
    for (int i = 0; i < listLength; i++) {
      resetSMGStateAndVisitor();
      Value[] pointersConcreteDifferentList = buildConcreteList(false, sllSize, listLength);
      // Adds sublists equal sublists (0 value in all)
      Value[][] nestedDifferentLists =
          addSubListsToList(listLength, pointersConcreteDifferentList, false);
      SMGObject ithObj =
          currentState
              .dereferencePointerWithoutMaterilization(nestedDifferentLists[i][i])
              .orElseThrow()
              .getSMGObject();
      // Write -1 as value in ith element to prevent abstraction of this sublist and the toplist
      currentState =
          currentState.writeValueWithChecks(
              ithObj,
              new NumericValue(BigInteger.ZERO),
              new NumericValue(pointerSizeInBits),
              new NumericValue(-1),
              null,
              dummyCDAEdge);

      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
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
      absFinder =
          new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
      currentState = absFinder.findAndAbstractLists();
      SMGObject abstractedObj =
          currentState
              .dereferencePointerWithoutMaterilization(pointersAbstractedList[0])
              .orElseThrow()
              .getSMGObject();

      // Concrete complete list
      Value[] pointersConcreteList = buildConcreteList(false, sllSize, listLength);
      addSubListsToList(listLength, pointersConcreteList, false);
      absFinder =
          new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
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
                  ImmutableMap.of(
                      notAbstractedListDifferentObj,
                      ImmutableList.of(nfo),
                      concreteObjBeginning,
                      ImmutableList.of(nfo)),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  concreteObjBeginning,
                  notAbstractedListDifferentObj,
                  ImmutableMap.of(
                      concreteObjBeginning,
                      ImmutableList.of(nfo),
                      notAbstractedListDifferentObj,
                      ImmutableList.of(nfo)),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();

      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  abstractedObj,
                  notAbstractedListDifferentObj,
                  ImmutableMap.of(
                      abstractedObj,
                      ImmutableList.of(nfo),
                      notAbstractedListDifferentObj,
                      ImmutableList.of(nfo)),
                  currentState,
                  currentState,
                  EqualityCache.<Value>of()))
          .isFalse();
      assertThat(
              currentState.checkEqualValuesForTwoStatesWithExemptions(
                  notAbstractedListDifferentObj,
                  abstractedObj,
                  ImmutableMap.of(
                      notAbstractedListDifferentObj,
                      ImmutableList.of(nfo),
                      abstractedObj,
                      ImmutableList.of(nfo)),
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
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void testSLLNextPointerFreeLoopEquality() throws CPAException, InterruptedException {
    Value[] pointersConcreteDifferentList = buildConcreteList(false, sllSize, listLength);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // "free" list except for last segment
    // We explicitly deref the current segment and read the next pointer beforehand
    Value lastNextPointer = null;
    for (int i = 0; i < listLength; i++) {
      List<SMGStateAndOptionalSMGObjectAndOffset> deref;
      if (i == listLength - 1) {
        // Don't use the last ptr in the end of the array
        deref = currentState.dereferencePointer(lastNextPointer);
      } else {
        deref = currentState.dereferencePointer(pointersConcreteDifferentList[i]);
      }
      // Should only be 1 list element
      assertThat(deref).hasSize(1);
      currentState = deref.get(0).getSMGState();
      assertThat(deref.get(0).hasSMGObjectAndOffset()).isTrue();
      assertThat(deref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      List<ValueAndSMGState> readNexts;
      if (i == listLength - 1) {
        // Don't use the last ptr in the end of the array
        readNexts =
            evaluator.readValueWithPointerDereference(
                currentState, lastNextPointer, new NumericValue(nfo), pointerSizeInBits, null);
      } else {
        readNexts =
            evaluator.readValueWithPointerDereference(
                currentState,
                pointersConcreteDifferentList[i],
                new NumericValue(nfo),
                pointerSizeInBits,
                null);
      }
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
      Value prevPtr = null;
      if (i + 2 >= listLength) {
        // Read pointer now points to the last of the original concrete elements and the next in the
        // array is "last"
        // As a result, the last (pointersConcreteDifferentList[i + 1]) points to 0+
        // and the readPointer to the object before
        // read         last
        //   v           v
        // i -> i + i -> 0+
        assertThat(readPointer).isNotEqualTo(pointersConcreteDifferentList[listLength - 1]);
        // readPTE is the next pointer from the obj of pointersConcreteDifferentList[i]
        SMGPointsToEdge readPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(readPointer).orElseThrow())
                .orElseThrow();
        SMGPointsToEdge arrayPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(pointersConcreteDifferentList[listLength - 1])
                        .orElseThrow())
                .orElseThrow();
        assertThat(readPTE.pointsTo()).isNotEqualTo(arrayPTE.pointsTo());
        assertThat(readPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(arrayPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        ValueAndSMGState ptrToZeroPlusAndSt =
            currentState.readValueWithoutMaterialization(
                readPTE.pointsTo(), nfo, pointerSizeInBits, null);
        currentState = ptrToZeroPlusAndSt.getState();
        Value ptrToZeroPlus = ptrToZeroPlusAndSt.getValue();
        SMGPointsToEdge ptrToZeroPlusPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(ptrToZeroPlus).orElseThrow())
                .orElseThrow();
        assertThat(ptrToZeroPlusPTE.pointsTo()).isEqualTo(arrayPTE.pointsTo());
        assertThat(ptrToZeroPlusPTE.targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      } else if (i + 1 < listLength) {
        assertThat(readPointer).isEqualTo(pointersConcreteDifferentList[i + 1]);
      }
      if (i + 1 == listLength) {
        prevPtr = lastNextPointer;
      } else {
        prevPtr = pointersConcreteDifferentList[i];
      }

      SMGObject notAbstractedListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(notAbstractedListObj.isSLL()).isFalse();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isTrue();
      // Save the ptr to the next segment in a stack var to avoid cleanup of the ptr
      SMGObjectAndSMGState newStackObjAndState =
          currentState.copyAndAddStackObject(new NumericValue(pointerSizeInBits));
      currentState = newStackObjAndState.getState();
      currentState =
          currentState.writeValueWithoutChecks(
              newStackObjAndState.getSMGObject(),
              BigInteger.ZERO,
              pointerSizeInBits,
              currentState.getMemoryModel().getSMGValueFromValue(readPointer).orElseThrow());
      // Free current list segment
      List<SMGState> newStatesAfterFree = currentState.free(prevPtr, null, null);
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
              .dereferencePointerWithoutMaterilization(prevPtr)
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isFalse();
      lastNextPointer = readPointer;
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
    assertThat(deref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    List<ValueAndSMGState> readNextsInLast =
        evaluator.readValueWithPointerDereference(
            currentState, lastNextPointer, new NumericValue(nfo), pointerSizeInBits, null);
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

  /**
   * We have a list, we check the next components' existence, then we move the current pointer to
   * the next and free the prev segment. The resulting list should be covered by the previous.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void testDLLNextPointerFreeLoopEquality() throws CPAException, InterruptedException {
    Value[] pointersConcreteDifferentList = buildConcreteList(true, dllSize, listLength);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // "free" list except for last segment
    // We explicitly deref the current segment and read the next pointer beforehand
    Value lastNextPointer = null;
    for (int i = 0; i < listLength; i++) {
      List<SMGStateAndOptionalSMGObjectAndOffset> deref;
      if (i == listLength - 1) {
        // Don't use the last ptr in the end of the array
        deref = currentState.dereferencePointer(lastNextPointer);
      } else {
        deref = currentState.dereferencePointer(pointersConcreteDifferentList[i]);
      }
      // Should only be 1 list element
      assertThat(deref).hasSize(1);
      currentState = deref.get(0).getSMGState();
      assertThat(deref.get(0).hasSMGObjectAndOffset()).isTrue();
      assertThat(deref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      List<ValueAndSMGState> readNexts;
      if (i == listLength - 1) {
        // Don't use the last ptr in the end of the array
        readNexts =
            evaluator.readValueWithPointerDereference(
                currentState, lastNextPointer, new NumericValue(nfo), pointerSizeInBits, null);
      } else {
        readNexts =
            evaluator.readValueWithPointerDereference(
                currentState,
                pointersConcreteDifferentList[i],
                new NumericValue(nfo),
                pointerSizeInBits,
                null);
      }
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
      Value prevPtr = null;
      if (i + 2 >= listLength) {
        // Read pointer now points to the last of the original concrete elements and the next in the
        // array is "last"
        // As a result, the last (pointersConcreteDifferentList[i + 1]) points to 0+
        // and the readPointer to the object before
        // read         last
        //   v           v
        // i -> i + i -> 0+
        assertThat(readPointer).isNotEqualTo(pointersConcreteDifferentList[listLength - 1]);
        // readPTE is the next pointer from the obj of pointersConcreteDifferentList[i]
        SMGPointsToEdge readPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(readPointer).orElseThrow())
                .orElseThrow();
        SMGPointsToEdge arrayPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(pointersConcreteDifferentList[listLength - 1])
                        .orElseThrow())
                .orElseThrow();
        assertThat(readPTE.pointsTo()).isNotEqualTo(arrayPTE.pointsTo());
        assertThat(readPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(arrayPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
        ValueAndSMGState ptrToZeroPlusAndSt =
            currentState.readValueWithoutMaterialization(
                readPTE.pointsTo(), nfo, pointerSizeInBits, null);
        currentState = ptrToZeroPlusAndSt.getState();
        Value ptrToZeroPlus = ptrToZeroPlusAndSt.getValue();
        SMGPointsToEdge ptrToZeroPlusPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState.getMemoryModel().getSMGValueFromValue(ptrToZeroPlus).orElseThrow())
                .orElseThrow();
        assertThat(ptrToZeroPlusPTE.pointsTo()).isEqualTo(arrayPTE.pointsTo());
        assertThat(ptrToZeroPlusPTE.targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
      } else if (i + 1 < listLength) {
        assertThat(readPointer).isEqualTo(pointersConcreteDifferentList[i + 1]);
      }
      if (i + 1 == listLength) {
        prevPtr = lastNextPointer;
      } else {
        prevPtr = pointersConcreteDifferentList[i];
      }

      SMGObject notAbstractedListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(notAbstractedListObj.isSLL()).isFalse();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isTrue();
      // Free current list segment
      List<SMGState> newStatesAfterFree = currentState.free(prevPtr, null, null);
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
              .dereferencePointerWithoutMaterilization(prevPtr)
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedListObj)).isFalse();
      lastNextPointer = readPointer;
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
    assertThat(deref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    List<ValueAndSMGState> readNextsInLast =
        evaluator.readValueWithPointerDereference(
            currentState, lastNextPointer, new NumericValue(nfo), pointerSizeInBits, null);
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

  /**
   * We have a list, we check the next components' existence, then we move the current pointer to
   * the next and free the prev segment. The resulting list should be covered by the previous.
   */
  // TODO: fix test for non-abstraction of regions w pointers from outside of the list
  @Ignore
  @Test
  public void testDLLPrevPointerFreeLoopEquality() throws CPAException, InterruptedException {
    Value[] pointersConcreteDifferentList = buildConcreteList(true, dllSize, listLength);
    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, listLength - 1, new SMGCPAStatistics());
    currentState = absFinder.findAndAbstractLists();
    // "free" list except for last segment
    // We explicitly deref the current segment and read the next pointer beforehand
    // runningListElementPointer == pointer to current list element
    Value runningListElementPointer = pointersConcreteDifferentList[listLength - 1];
    // previousListElementPtr == pointer to the list element pointing to runningListElementPointer
    // via prev pointer. This pointer / list segment is to be freed.
    Value previousListElementPtr;
    for (int i = listLength - 1; i >= 0; i--) {
      List<SMGStateAndOptionalSMGObjectAndOffset> currentDeref =
          currentState.dereferencePointer(runningListElementPointer);

      assertThat(currentState.getMemoryModel().getNestingLevel(pointersConcreteDifferentList[i]))
          .isEqualTo(0);
      assertThat(currentState.getMemoryModel().getNestingLevel(runningListElementPointer))
          .isEqualTo(0);
      // Should only be 1 list element
      assertThat(currentDeref).hasSize(1);
      currentState = currentDeref.get(0).getSMGState();
      assertThat(currentDeref.get(0).hasSMGObjectAndOffset()).isTrue();
      assertThat(currentDeref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
          .isEqualTo(BigInteger.ZERO);
      // currentDeref is based on prev pointers, check that its equal to a deref of the external
      // pointer at that location (except the first case)
      if (i > 0) {
        assertThat(
                currentState.getMemoryModel().getNestingLevel(pointersConcreteDifferentList[i - 1]))
            .isEqualTo(0);
        List<SMGStateAndOptionalSMGObjectAndOffset> currentDerefFromExternal =
            currentState.dereferencePointer(pointersConcreteDifferentList[i]);
        SMGValue currentSMGPointerExternal =
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(pointersConcreteDifferentList[i])
                .orElseThrow();
        assertThat(currentState.getMemoryModel().getNestingLevel(pointersConcreteDifferentList[i]))
            .isEqualTo(0);
        assertThat(currentDerefFromExternal).hasSize(1);
        currentState = currentDerefFromExternal.get(0).getSMGState();
        assertThat(currentDerefFromExternal.get(0).hasSMGObjectAndOffset()).isTrue();
        assertThat(
                currentDerefFromExternal
                    .get(0)
                    .getOffsetForObject()
                    .asNumericValue()
                    .bigIntegerValue())
            .isEqualTo(BigInteger.ZERO);
        assertThat(currentDerefFromExternal.get(0).getSMGObject())
            .isEqualTo(currentDeref.get(0).getSMGObject());
        assertThat(
                currentState
                    .getMemoryModel()
                    .getSmg()
                    .getPTEdge(currentSMGPointerExternal)
                    .orElseThrow()
                    .targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_REGION);
      }

      List<ValueAndSMGState> readPrevsOfCurrent =
          evaluator.readValueWithPointerDereference(
              currentState,
              runningListElementPointer,
              new NumericValue(pfo),
              pointerSizeInBits,
              null);

      ValueAndSMGState readPrevPointerAndState;
      if (i != 0) {
        // Should only be 1 list element
        assertThat(readPrevsOfCurrent).hasSize(1);
        readPrevPointerAndState = readPrevsOfCurrent.get(0);
      } else {
        assertThat(readPrevsOfCurrent).hasSize(2);
        readPrevPointerAndState = readPrevsOfCurrent.get(1);
        // We only want to take a look at the extended list
      }
      // We read the prev pointer pointing to an abstracted list, hence this list is materialized
      currentState = readPrevPointerAndState.getState();
      Value readPrevPointer = readPrevPointerAndState.getValue();
      if (1 >= i) {
        // Read pointer now points to the last of the original concrete elements
        // As a result, the first pointer (pointersConcreteDifferentList[0]) points to 0+
        // and the readPrevPointer to the object before
        // first   currentDeref
        //  v         v
        // 0+   ->   CE -> CE
        assertThat(readPrevPointer).isNotEqualTo(pointersConcreteDifferentList[0]);
        SMGPointsToEdge readPrevPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(readPrevPointer)
                        .orElseThrow())
                .orElseThrow();
        SMGPointsToEdge firstPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(pointersConcreteDifferentList[0])
                        .orElseThrow())
                .orElseThrow();
        assertThat(readPrevPTE.pointsTo()).isNotEqualTo(firstPTE.pointsTo());
        assertThat(readPrevPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_REGION);
        assertThat(firstPTE.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
        ValueAndSMGState readPtrToZeroPlusAndSt =
            currentState.readValueWithoutMaterialization(
                readPrevPTE.pointsTo(), pfo, pointerSizeInBits, null);
        currentState = readPtrToZeroPlusAndSt.getState();
        Value readPtrToZeroPlus = readPtrToZeroPlusAndSt.getValue();
        SMGPointsToEdge readPtrToZeroPlusPTE =
            currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(
                    currentState
                        .getMemoryModel()
                        .getSMGValueFromValue(readPtrToZeroPlus)
                        .orElseThrow())
                .orElseThrow();
        assertThat(readPtrToZeroPlusPTE.pointsTo()).isEqualTo(firstPTE.pointsTo());
        assertThat(readPtrToZeroPlusPTE.targetSpecifier())
            .isEqualTo(SMGTargetSpecifier.IS_LAST_POINTER);
      } else {
        assertThat(readPrevPointer).isEqualTo(pointersConcreteDifferentList[i - 1]);
      }

      previousListElementPtr = runningListElementPointer;
      runningListElementPointer = readPrevPointer;

      // Check that the new running list element is valid (the object behind prev of the element
      // about to be freed)
      SMGObject notAbstractedPrevListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPrevPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(notAbstractedPrevListObj.isSLL()).isFalse();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedPrevListObj)).isTrue();
      // Free current list segment
      List<SMGState> newStatesAfterFree = currentState.free(previousListElementPtr, null, null);
      assertThat(newStatesAfterFree).hasSize(1);
      currentState = newStatesAfterFree.get(0);
      // check that the prev from the just freed is still valid
      notAbstractedPrevListObj =
          currentState
              .dereferencePointerWithoutMaterilization(readPrevPointer)
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedPrevListObj)).isTrue();
      // Check that freed object is invalid
      notAbstractedPrevListObj =
          currentState
              .dereferencePointerWithoutMaterilization(previousListElementPtr)
              .orElseThrow()
              .getSMGObject();
      assertThat(currentState.getMemoryModel().isObjectValid(notAbstractedPrevListObj)).isFalse();
    }
    // Now we save the state for later
    SMGState stateW1Left = currentState;
    // confirm that the first pointer is correct first
    List<SMGStateAndOptionalSMGObjectAndOffset> deref =
        currentState.dereferencePointer(runningListElementPointer);
    // Should only be 1 list element
    assertThat(deref).hasSize(1);
    currentState = deref.get(0).getSMGState();
    assertThat(deref.get(0).hasSMGObjectAndOffset()).isTrue();
    assertThat(deref.get(0).getOffsetForObject().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    // Read prev of current list segment (reads into 0+, causes materialization)
    List<ValueAndSMGState> readPrevsInLast =
        evaluator.readValueWithPointerDereference(
            currentState,
            runningListElementPointer,
            new NumericValue(pfo),
            pointerSizeInBits,
            null);
    // Should only be 1 list element
    assertThat(readPrevsInLast).hasSize(2);
    // When materializing, the first element is the minimal element, confirm that the value is 0
    assertThat(readPrevsInLast.get(0).getValue().isNumericValue()).isTrue();
    assertThat(readPrevsInLast.get(0).getValue().asNumericValue().bigIntegerValue())
        .isEqualTo(BigInteger.ZERO);
    // Confirm that the other is materialized correctly
    currentState = readPrevsInLast.get(1).getState();
    Value readPrevPointer = readPrevsInLast.get(1).getValue();
    SMGObject newlyMaterializedListSegment =
        currentState
            .dereferencePointerWithoutMaterilization(readPrevPointer)
            .orElseThrow()
            .getSMGObject();
    assertThat(newlyMaterializedListSegment.isSLL()).isFalse();
    ValueAndSMGState pointerToZeroPlus =
        currentState.readValueWithoutMaterialization(
            newlyMaterializedListSegment, pfo, pointerSizeInBits, null);
    assertThat(currentState.getMemoryModel().pointsToZeroPlus(pointerToZeroPlus.getValue()))
        .isTrue();
    // Now we free the list element from before
    List<SMGState> freeList = currentState.free(runningListElementPointer, null, null);
    assertThat(freeList).hasSize(1);
    currentState = freeList.get(0);
    // Compare the 2 states from before and now
    assertThat(currentState.isLessOrEqual(stateW1Left)).isTrue();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState.EqualityCache;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGNode;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;

public class SMGCPAMaterializer {

  private final LogManager logger;

  // For the 0+ segments
  private static final int MINIMUM_LIST_LENGTH = 0;

  private final SMGCPAStatistics statistics;

  public SMGCPAMaterializer(LogManager pLogger, SMGCPAStatistics pStatistics) {
    logger = pLogger;
    statistics = pStatistics;
  }

  /**
   * Materializes lists from abstract lists into concrete lists. See the paper for more info. Note:
   * 0+ behave differently to the others in that it generates 2 states. We order those! The first in
   * the list is the minimal state where the 0+ is deleted, the second keeps a 0+ and grows.
   *
   * @param valueToPointerToAbstractObject pointer to an {@link SMGSinglyLinkedListSegment}.
   * @param pAbstractObject the target of valueToPointerToAbstractObject ({@link
   *     SMGSinglyLinkedListSegment} or {@link SMGDoublyLinkedListSegment})
   * @param state current {@link SMGState}.
   * @return list of returned {@link SMGValueAndSMGState} with the value being the updated pointers.
   *     (In the context of the new state valueToPointerToAbstractObject behaves the same!)
   * @throws SMGException in case of critical errors.
   */
  public List<SMGValueAndSMGState> handleMaterialisation(
      SMGValue valueToPointerToAbstractObject,
      SMGSinglyLinkedListSegment pAbstractObject,
      SMGState state)
      throws SMGException {
    assert state.getMemoryModel().getSmg().checkPointerNestingLevelConsistency();
    // Materialize from the left ( CE -> 3+ -> 0 => CE -> CE -> 2+ -> 0) for first ptrs and all next
    // ptrs. Materialize from the right for all last ptrs and prevs.
    List<SMGValueAndSMGState> materializedResult;
    if (pAbstractObject.getMinLength() == MINIMUM_LIST_LENGTH) {
      // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
      // removed
      materializedResult =
          handleZeroPlusMaterialization(pAbstractObject, valueToPointerToAbstractObject, state);
    } else {
      materializedResult =
          ImmutableList.of(
              materialiseAbstractedList(pAbstractObject, valueToPointerToAbstractObject, state));
    }
    assertSMGSanity(materializedResult);
    return materializedResult;
  }

  private void assertSMGSanity(List<SMGValueAndSMGState> materializedResult) {
    for (SMGValueAndSMGState newState : materializedResult) {
      assert newState.getSMGState().getMemoryModel().checkSMGSanity();
    }
  }

  /*
   * This generates 2 states.
   * One where we materialize the list once more
   * (we decide which way in the end), and one where the 0+ is deleted.
   * For the remove state, the last pointer now points to the prev obj (to the left),
   * while the next pointer points to the next of the 0+ (to the right).
   */
  private List<SMGValueAndSMGState> handleZeroPlusMaterialization(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pointerValueTowardsThisSegment, SMGState state)
      throws SMGException {

    statistics.incrementZeroPlusMaterializations();
    statistics.startTotalZeroPlusMaterializationTime();

    logger.log(
        Level.ALL,
        "Split into 2 states because of 0+ "
            + pListSeg.getClass().getSimpleName()
            + " materialization.",
        pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pfo = null;
    if (pListSeg instanceof SMGDoublyLinkedListSegment dll) {
      pfo = dll.getPrevOffset();
    }
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();

    SMGValue prevPointerValue;
    if (pListSeg.isSLL()) {
      prevPointerValue = getSLLPrevObjPointer(currentState, pListSeg, nfo, pointerSize);
    } else {
      SMGValueAndSMGState prevPointerAndState =
          currentState.readSMGValue(pListSeg, pfo, pointerSize);
      currentState = prevPointerAndState.getSMGState();
      prevPointerValue = prevPointerAndState.getSMGValue();
    }

    // Replace the value pointerValueTowardsThisSegment with the next value read in the entire SMG
    // FIRST pointer needs to point to the next value
    // Important: first pointer specifier is depending on the next ptr for the non-extended case
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replacePointerValuesWithExistingOrNew(
                    pListSeg,
                    nextPointerValue,
                    ImmutableSet.of(
                        SMGTargetSpecifier.IS_FIRST_POINTER, SMGTargetSpecifier.IS_ALL_POINTER)));

    // Last ptr to the current
    // Important: last pointer specifier need to be region for the non-extended case if it points
    // towards a region, else last
    assert !(currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(prevPointerValue)
                .orElseThrow()
                .pointsTo()
            instanceof SMGSinglyLinkedListSegment)
        || currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(prevPointerValue)
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_LAST_POINTER);

    assert currentState
                .getMemoryModel()
                .getSmg()
                .getPTEdge(prevPointerValue)
                .orElseThrow()
                .pointsTo()
            instanceof SMGSinglyLinkedListSegment
        || currentState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(prevPointerValue)
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_REGION);

    assert currentState
            .getMemoryModel()
            .getSmg()
            .getObjectsPointingToZeroPlusAbstraction(pListSeg)
            .isEmpty()
        || currentState.getMemoryModel().getSmg().getPTEdgeMapping().values().stream()
            .anyMatch(
                pte ->
                    pte.pointsTo().equals(pListSeg)
                        && !pte.targetSpecifier().equals(SMGTargetSpecifier.IS_ALL_POINTER));

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replacePointerValuesWithExistingOrNew(
                    pListSeg,
                    prevPointerValue,
                    ImmutableSet.of(SMGTargetSpecifier.IS_LAST_POINTER)));

    assert currentState.getMemoryModel().getSmg().getPointerValuesForTarget(pListSeg).isEmpty();

    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    currentState =
        currentState.writeValueWithoutChecks(
            pListSeg, nfo, currentState.getMemoryModel().getSizeOfPointer(), SMGValue.zeroValue());
    if (pListSeg instanceof SMGDoublyLinkedListSegment dll) {
      currentState =
          currentState.writeValueWithoutChecks(
              pListSeg,
              dll.getPrevOffset(),
              currentState.getMemoryModel().getSizeOfPointer(),
              SMGValue.zeroValue());
    }
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .copyAndRemoveObjectAndAssociatedSubSMG(pListSeg)
                .getSPC());

    SMGPointsToEdge pte =
        state.getMemoryModel().getSmg().getPTEdge(pointerValueTowardsThisSegment).orElseThrow();

    if (pte.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
      assert pte.getOffset()
          .equals(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(nextPointerValue)
                  .orElseThrow()
                  .getOffset());
      returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    } else if (pte.targetSpecifier().equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      assert pte.getOffset()
          .equals(
              currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(prevPointerValue)
                  .orElseThrow()
                  .getOffset());
      returnStates.add(SMGValueAndSMGState.of(currentState, prevPointerValue));
    } else {
      // It's not really unknown, but most likely wrong!
      // While it is theoretically possible that we traverse some ALL memory and end up here,
      // most likely it's just a wrongly labeled ALL pointer that should be FST or LST.
      throw new SMGException("Unknown pointer specifier towards 0+ list segment.");
    }

    statistics.stopTotalZeroPlusMaterializationTime();
    assert state.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    return returnStates
        .add(materialiseAbstractedList(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
  }

  /**
   * Materialize from the left for FIRST pointers. (e.g. REGION -> 3+ -> 0 => REGION -> REGION -> 2+
   * -> 0, assuming that the next pointer points to the right hand side 0.) Materialize from the
   * right for all LAST pointers. (e.g. REGION -> 3+ -> REGION => REGION -> 2+ -> REGION -> REGION,
   * assuming that the next pointer points to the right hand side region.)
   *
   * <p>The nesting level depicts where other abstracted memory is located in relation to the
   * currently materialized abstract list. Each time a list segment is materialized, nested sub-SMGs
   * are copied and the nesting level of the new sub-SMG (values and pointers) are decremented by 1.
   * We return the pointer to the segment just materialized. Note: the initial input pointer does
   * not guarantee that it points to the new concrete region!
   */
  private SMGValueAndSMGState materialiseAbstractedList(
      SMGSinglyLinkedListSegment pListSeg,
      SMGValue pPointerTowardsListSegToMaterializeFrom,
      SMGState pState)
      throws SMGException {
    SMGTargetSpecifier pointerSpecifier =
        pState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(pPointerTowardsListSegToMaterializeFrom)
            .orElseThrow()
            .targetSpecifier();

    if (!pState.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMGException(
          "Error when materializing a "
              + pListSeg.getClass().getSimpleName()
              + ": trying to materialize out of invalid memory.");
    }

    assert pState.getMemoryModel().checkSMGSanity();
    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    Preconditions.checkArgument(
        pointerSpecifier.equals(SMGTargetSpecifier.IS_LAST_POINTER)
            || pointerSpecifier.equals(SMGTargetSpecifier.IS_FIRST_POINTER));

    logger.log(
        Level.FINE,
        "Materialise " + pListSeg.getClass().getSimpleName() + ": ",
        pListSeg + " with a pointer with specifier " + pointerSpecifier);
    assert pState.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    statistics.startTotalMaterializationTime();
    statistics.incrementListMaterializations();

    SMGValueAndSMGState materializedPointerAndState;
    if (pointerSpecifier.equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      materializedPointerAndState =
          materialiseAbstractListFromLast(
              pListSeg, pPointerTowardsListSegToMaterializeFrom, pState);
    } else {
      materializedPointerAndState =
          materialiseAbstractListFromFirst(
              pListSeg, pPointerTowardsListSegToMaterializeFrom, pState);
    }

    statistics.stopTotalMaterializationTime();
    assert materializedPointerAndState.getSMGState().getMemoryModel().checkSMGSanity();
    return materializedPointerAndState;
  }

  private SMGValueAndSMGState materialiseAbstractListFromLast(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {
    BigInteger nfo = pListSeg.getNextOffset();
    SMGPointsToEdge initialPTE =
        state.getMemoryModel().getSmg().getPTEdge(pInitialPointer).orElseThrow();
    BigInteger initialPointerTargetOffset =
        initialPTE.getOffset().asNumericValue().bigIntegerValue();
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers
    // (LAST or FIRST only)
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndSubSMGForMaterialization(
            pListSeg, state, ImmutableSet.of(SMGTargetSpecifier.IS_LAST_POINTER));
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Get the new version of the initial pointer to the new concrete region
    CType initialPointerType = currentState.getMemoryModel().getTypeForValue(pInitialPointer);
    ValueAndSMGState pointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(
            newConcreteRegion, initialPointerType, initialPointerTargetOffset);
    currentState = pointerToNewConcreteAndState.getState();
    SMGValue valueOfPointerToConcreteObject =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(pointerToNewConcreteAndState.getValue())
            .orElseThrow();

    // Assert that the new pointer is correct
    Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
        currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
    Preconditions.checkState(maybePointsToEdgeToConcreteRegion.isPresent());
    Preconditions.checkState(
        maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
    Preconditions.checkState(
        maybePointsToEdgeToConcreteRegion
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_REGION));

    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstractListAndCopyValuesAndSwitchPointers(pListSeg, currentState);

    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create a new last pointer to the new abstract list segment and save in prev of new concrete
    if (pListSeg instanceof SMGDoublyLinkedListSegment oldDll) {
      ValueAndSMGState newLastPointerToNewAbstractedList =
          currentState.searchOrCreateAddress(
              newAbsListSeg,
              initialPointerType,
              oldDll.getPrevPointerTargetOffset(),
              newAbsListSeg.getNestingLevel(),
              SMGTargetSpecifier.IS_LAST_POINTER);
      currentState = newLastPointerToNewAbstractedList.getState();
      Value newLastPointerToAbstrValue = newLastPointerToNewAbstractedList.getValue();

      SMGValue smgPtrToAbstr =
          currentState
              .getMemoryModel()
              .getSMGValueFromValue(newLastPointerToAbstrValue)
              .orElseThrow();
      Preconditions.checkArgument(
          currentState
                  .getMemoryModel()
                  .getSmg()
                  .getPTEdge(smgPtrToAbstr)
                  .orElseThrow()
                  .targetSpecifier()
              == SMGTargetSpecifier.IS_LAST_POINTER);

      // Write the new value w pointer towards the new abstract region to new concrete region as
      // prev pointer
      currentState =
          currentState.writeValueWithoutChecks(
              newConcreteRegion, oldDll.getPrevOffset(), pointerSize, smgPtrToAbstr);
    }

    // Set the next pointer of the new abstract segment to the new concrete segment
    // (The target offset of the initial pointer and a next pointer might not match!)
    ValueAndSMGState nextPointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(
            newConcreteRegion, initialPointerType, newAbsListSeg.getNextPointerTargetOffset());
    currentState = nextPointerToNewConcreteAndState.getState();
    SMGValue nextPointerToNewConcrete =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(nextPointerToNewConcreteAndState.getValue())
            .orElseThrow();
    currentState =
        currentState.writeValueWithoutChecks(
            newAbsListSeg, nfo, pointerSize, nextPointerToNewConcrete);

    // Remove the old abstract list segment
    Preconditions.checkState(
        currentState
            .getMemoryModel()
            .getSmg()
            .getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(pListSeg)
            .isEmpty());
    currentState =
        currentState.writeValueWithoutChecks(
            pListSeg, nfo, currentState.getMemoryModel().getSizeOfPointer(), SMGValue.zeroValue());
    if (pListSeg instanceof SMGDoublyLinkedListSegment dll) {
      currentState =
          currentState.writeValueWithoutChecks(
              pListSeg,
              dll.getPrevOffset(),
              currentState.getMemoryModel().getSizeOfPointer(),
              SMGValue.zeroValue());
    }
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .copyAndRemoveObjectAndAssociatedSubSMG(pListSeg)
                .getSPC());

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfRightHandSideMaterializedList(
        newConcreteRegion, newAbsListSeg, currentState);
    return SMGValueAndSMGState.of(currentState, valueOfPointerToConcreteObject);
  }

  private SMGValueAndSMGState materialiseAbstractListFromFirst(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {
    BigInteger nfo = pListSeg.getNextOffset();
    SMGPointsToEdge initialPTE =
        state.getMemoryModel().getSmg().getPTEdge(pInitialPointer).orElseThrow();
    BigInteger initialPointerTargetOffset =
        initialPTE.getOffset().asNumericValue().bigIntegerValue();
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers (LAST
    // or FIRST)
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndSubSMGForMaterialization(
            pListSeg, state, ImmutableSet.of(SMGTargetSpecifier.IS_FIRST_POINTER));
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Get the new version of the initial pointer to the new concrete region
    CType initialPointerType = currentState.getMemoryModel().getTypeForValue(pInitialPointer);
    ValueAndSMGState pointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(
            newConcreteRegion, initialPointerType, initialPointerTargetOffset);
    currentState = pointerToNewConcreteAndState.getState();
    SMGValue valueOfPointerToConcreteObject =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(pointerToNewConcreteAndState.getValue())
            .orElseThrow();

    // Assert that the new pointer is correct
    Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
        currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
    Preconditions.checkState(maybePointsToEdgeToConcreteRegion.isPresent());
    Preconditions.checkState(
        maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
    Preconditions.checkState(
        maybePointsToEdgeToConcreteRegion
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_REGION));

    // TODO: problem, on 1+ we might have first and last ptrs (and all), but never want to switch
    // the last and all pointer to an concrete element for the extended list (this case), but
    // switch it to the 0+

    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstractListAndCopyValuesAndSwitchPointers(pListSeg, currentState);

    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create a new first pointer to the new abstract list segment and save in next of new concrete
    ValueAndSMGState nextPointerToNewAbstractedListAndState =
        currentState.searchOrCreateAddress(
            newAbsListSeg,
            initialPointerType,
            newAbsListSeg.getNextPointerTargetOffset(),
            newAbsListSeg.getNestingLevel(),
            SMGTargetSpecifier.IS_FIRST_POINTER);
    currentState = nextPointerToNewAbstractedListAndState.getState();
    SMGValue nextPointerToNewAbstractedList =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(nextPointerToNewAbstractedListAndState.getValue())
            .orElseThrow();
    currentState =
        currentState.writeValueWithoutChecks(
            newConcreteRegion, nfo, pointerSize, nextPointerToNewAbstractedList);

    // Set the prev pointer of the new abstract segment to the new concrete segment for DLLs
    if (pListSeg instanceof SMGDoublyLinkedListSegment oldDll) {
      ValueAndSMGState prevPointerValueAndState =
          currentState.searchOrCreateAddress(
              newConcreteRegion, initialPointerType, oldDll.getPrevPointerTargetOffset());
      currentState = prevPointerValueAndState.getState();
      SMGValue prevPointerValue =
          currentState
              .getMemoryModel()
              .getSMGValueFromValue(prevPointerValueAndState.getValue())
              .orElseThrow();
      currentState =
          currentState.writeValueWithoutChecks(
              newAbsListSeg, oldDll.getPrevOffset(), pointerSize, prevPointerValue);
    }

    // Remove the old abstract list segment
    Preconditions.checkState(
        currentState
            .getMemoryModel()
            .getSmg()
            .getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(pListSeg)
            .isEmpty());
    currentState =
        currentState.writeValueWithoutChecks(
            pListSeg, nfo, currentState.getMemoryModel().getSizeOfPointer(), SMGValue.zeroValue());
    if (pListSeg instanceof SMGDoublyLinkedListSegment dll) {
      currentState =
          currentState.writeValueWithoutChecks(
              pListSeg,
              dll.getPrevOffset(),
              currentState.getMemoryModel().getSizeOfPointer(),
              SMGValue.zeroValue());
    }
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .copyAndRemoveObjectAndAssociatedSubSMG(pListSeg)
                .getSPC());

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfLeftHandSideMaterializedList(
        newConcreteRegion, newAbsListSeg, currentState);
    return SMGValueAndSMGState.of(currentState, pInitialPointer);
  }

  /**
   * Decrements the Abstracted list segment by creating a new abstracted list segment with length -
   * 1, then copies all values from the old to the new, then replaces all pointers towards the old
   * segment with the new one as the new target.
   *
   * @param pListSeg the old {@link SMGSinglyLinkedListSegment} or {@link
   *     SMGDoublyLinkedListSegment}.
   * @param pState the current {@link SMGState}
   * @return the new {@link SMGState} and the new abstract list segment.
   */
  private SMGObjectAndSMGState decrementAbstractListAndCopyValuesAndSwitchPointers(
      SMGSinglyLinkedListSegment pListSeg, SMGState pState) {
    // Create the now smaller abstracted list
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    SMGState currentState = pState.copyAndAddObjectToHeap(newAbsListSeg);
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);
    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);

    // Switch all remaining pointers from the old abstract object to the new    currentState =
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().replaceAllPointersTowardsWith(pListSeg, newAbsListSeg));
    return SMGObjectAndSMGState.of(newAbsListSeg, currentState);
  }

  /**
   * Creates a new concrete copy of the given abstract list. The new object is a region that is an
   * exact copy of the given abstracted list, adds it as heap object, copies all values of the
   * abstracted list segment to it, and switches all pointers with the given spec to the new object.
   * (i.e. FIRST or LAST should be used to be switched to the new region) This method also copies
   * the nested sub-SMG of the abstracted list and decrements its nesting level by 1. (nested ==
   * nesting level higher than the level of the abstracted/new concrete list element. Those need to
   * be copied. Otherwise, (level(pListSeg) == level(sub-SMG)) pointers point to the same memory, no
   * copy necessary) Also, ALL pointers towards the abstracted list (from either the abstracted list
   * itself or the nested sub-SMG) also get a pointer towards the new region.
   *
   * @param pListSeg the abstracted list segment being materialized, either {@link
   *     SMGSinglyLinkedListSegment} or {@link SMGDoublyLinkedListSegment}.
   * @param pState current {@link SMGState}.
   * @return the new concrete region and the current state.
   */
  private SMGObjectAndSMGState createNewConcreteRegionAndSubSMGForMaterialization(
      SMGSinglyLinkedListSegment pListSeg,
      SMGState pState,
      Set<SMGTargetSpecifier> specifierToSwitch)
      throws SMGException {
    // Add new concrete memory region
    SMGObjectAndSMGState newConcreteRegionAndState = pState.copyAndAddNewHeapRegion(pListSeg);
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Careful when copying memory. Only nested memory needs a copy, while other memory needs a copy
    // (Copy == new, but equal memory/values. Nested == nesting level higher than the current)
    currentState = copySubSMGRootedAt(pListSeg, newConcreteRegion, currentState);

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSpecificPointersTowardsWith(
                    pListSeg, newConcreteRegion, specifierToSwitch));
    return SMGObjectAndSMGState.of(newConcreteRegion, currentState);
  }

  /**
   * Copies all values from sourceObj to newMemory. Memory of pointers copied is then also copied,
   * depending on the nesting, except for the pointers at the next/prev offsets of root. If a havok
   * generating nested value is copied, a new symbolic value with the correct constraints in the
   * state is added instead.
   *
   * <p>This method does NOT handle pointers towards the memory copied or the original memory of the
   * copy!!!!
   *
   * @param root source of the values/memory copied. SLL or DLL.
   * @param newMemory target of the values/memory copied.
   * @param pState current {@link SMGState}
   * @return a new {@link SMGState} with all values copied from sourceObj to newMemory and all
   *     pointers and havoc generators copied into new memory/values.
   */
  private SMGState copySubSMGRootedAt(
      SMGSinglyLinkedListSegment root, SMGObject newMemory, SMGState pState) throws SMGException {
    Preconditions.checkState(newMemory.getNestingLevel() >= 0);
    // Add all values. next/prev pointer is wrong here, depending on left/right sided
    // materialization! We write this later in the materialization.
    // If one of those is a pointer, we copy the pointer and memory structure
    Set<BigInteger> excludedOffsets = ImmutableSet.of(root.getNextOffset());
    if (root instanceof SMGDoublyLinkedListSegment dllListSeg) {
      excludedOffsets = ImmutableSet.of(dllListSeg.getNextOffset(), dllListSeg.getPrevOffset());
    }
    return copyMemoryOfTo(
        root,
        newMemory,
        pState,
        excludedOffsets,
        root.getRelevantEqualities(),
        new HashMap<>(),
        newMemory);
  }

  /**
   * Copies all values from sourceObj to newMemory. Memory of pointers copied is then also copied,
   * except for the pointers at the offsets given in excludedOffsets. If a havok generating value is
   * copied, a new symbolic value with the correct constraints in the state is added instead.
   * (Copied pointers point to the same memory as before and are not nested, while replicated
   * pointers are nested and are new pointers to new memory that is itself a copy/replication etc.)
   *
   * <p>This method does NOT handle pointers towards the memory copied or the original memory of the
   * copy currently!!!!
   *
   * @param sourceObj source of the values/memory copied.
   * @param newMemory target of the values/memory copied.
   * @param pState current {@link SMGState}
   * @param excludedOffsets offsets excluded from being copied, for example nfo, pfo.
   * @param replicationCache values that are present in this cache need replication. All others need
   *     to be copied.
   * @param copiedMemoryCache a map from old memory to be copied to the new copy. Needed because of
   *     e.g. DLLs, or looping memory, so that we connect the memory correctly.
   * @return a new {@link SMGState} with all values copied from sourceObj to newMemory and all
   *     pointers and havoc generators copied into new memory/values.
   */
  private SMGState copyMemoryOfTo(
      SMGObject sourceObj,
      SMGObject newMemory,
      SMGState pState,
      Set<BigInteger> excludedOffsets,
      EqualityCache<Value> replicationCache,
      Map<SMGNode, SMGNode> copiedMemoryCache,
      SMGObject parentMaterialized)
      throws SMGException {
    // Initial copy to the newly created memory
    SMGState currentState = pState.copyAllValuesFromObjToObj(sourceObj, newMemory);
    // Remember what was already processed
    copiedMemoryCache.put(sourceObj, newMemory);
    // All HVEs except for unwanted (next/prev)
    Set<SMGHasValueEdge> valuesWONextAndPrev =
        currentState
            .getMemoryModel()
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(sourceObj, PersistentSet.of())
            .stream()
            .filter(hve -> !excludedOffsets.contains(hve.getOffset()))
            .collect(ImmutableSet.toImmutableSet());

    // Now we have all values whose memory we might need to replicate
    for (SMGHasValueEdge hve : valuesWONextAndPrev) {
      BigInteger offset = hve.getOffset();
      BigInteger sizeInBits = hve.getSizeInBits();
      SMGValue smgValue = hve.hasValue();
      Optional<Value> maybeValue = currentState.getMemoryModel().getValueFromSMGValue(smgValue);
      Preconditions.checkState(maybeValue.isPresent());

      if (maybeValue.orElseThrow().isNumericValue()) {
        continue;
      } else if (currentState.getMemoryModel().getNestingLevel(smgValue)
          == parentMaterialized.getNestingLevel()) {
        // TODO: isCopyValue() is legacy, remove once safely refactored using only the nesting lvl!
        assert isCopyValue(maybeValue.orElseThrow(), replicationCache);
        // Copy case; we already copied the value, do nothing.
        continue;
      }
      Preconditions.checkState(
          currentState.getMemoryModel().getNestingLevel(smgValue)
              > parentMaterialized.getNestingLevel());

      // Replication cases (Nested memory or values. Need nesting level decrement.)
      if (currentState.getMemoryModel().getSmg().isPointer(smgValue)) {
        currentState =
            replicatePointerWithSubSMG(
                newMemory,
                copiedMemoryCache,
                replicationCache,
                currentState,
                smgValue,
                offset,
                sizeInBits,
                parentMaterialized);

      } else if (maybeValue.isPresent()
          && maybeValue.orElseThrow() instanceof SymbolicExpression
          && maybeValue.orElseThrow() instanceof ConstantSymbolicExpression constExpr
          && constExpr.getValue() instanceof SymbolicIdentifier) {

        currentState =
            replicateNestedNonPointerValue(
                newMemory,
                smgValue,
                maybeValue.orElseThrow(),
                currentState,
                offset,
                sizeInBits,
                copiedMemoryCache,
                parentMaterialized);
      }
    }
    return currentState;
  }

  private SMGState replicatePointerWithSubSMG(
      SMGObject currentMemory,
      Map<SMGNode, SMGNode> alreadyCopiedMemory,
      EqualityCache<Value> replicationCache,
      SMGState currentState,
      SMGValue smgValue,
      BigInteger offsetOfSMGValueInCurrentMemory,
      BigInteger sizeOfSMGValueInBits,
      SMGObject parentMaterialized)
      throws SMGException {
    Value value = currentState.getMemoryModel().getValueFromSMGValue(smgValue).orElseThrow();
    // Copy memory and insert new pointer
    SMGStateAndOptionalSMGObjectAndOffset targetMemoryAndState =
        currentState.dereferencePointerWithoutMaterilization(value).orElseThrow();
    currentState = targetMemoryAndState.getSMGState();
    SMGObject oldTargetMemory = targetMemoryAndState.getSMGObject();
    // Copy targetMemory
    SMGValue newSMGValueToWrite;
    if (oldTargetMemory.isZero()) {
      newSMGValueToWrite = SMGValue.zeroValue();
    } else {
      SMGObject newTarget;
      if (alreadyCopiedMemory.containsKey(oldTargetMemory)) {
        newTarget = (SMGObject) alreadyCopiedMemory.get(oldTargetMemory);
      } else {
        int oldTargetMemNestingLvl = oldTargetMemory.getNestingLevel();
        int parentNestingLevel = parentMaterialized.getNestingLevel();
        Preconditions.checkState(parentNestingLevel < oldTargetMemNestingLvl);

        SMGObjectAndSMGState copiedTargetMemoryAndState =
            currentState.copyAndAddNewHeapObject(oldTargetMemory, oldTargetMemNestingLvl - 1);
        newTarget = copiedTargetMemoryAndState.getSMGObject();
        currentState = copiedTargetMemoryAndState.getState();
        // Now copy all values and copy all memory for pointers again recursively
        // (copyMemoryOfTo adds the new memory to the cache)
        currentState =
            copyMemoryOfTo(
                oldTargetMemory,
                newTarget,
                currentState,
                ImmutableSet.of(),
                replicationCache,
                alreadyCopiedMemory,
                parentMaterialized);
      }
      // Create a new pointer to the new memory that is equal to the old and save in newConcrete
      CType ptrType = currentState.getMemoryModel().getTypeForValue(smgValue);
      SMGPointsToEdge oldPTE =
          currentState.getMemoryModel().getSmg().getPTEdge(smgValue).orElseThrow();
      Value oldOffset = oldPTE.getOffset();
      // Nesting level of pointers always refers to the nesting level of the target, except for ALL
      // pointers, which are +1
      int nestingLevel = newTarget.getNestingLevel();
      SMGTargetSpecifier specifier = oldPTE.targetSpecifier();

      if (specifier == SMGTargetSpecifier.IS_ALL_POINTER) {
        Preconditions.checkState(oldPTE.pointsTo() instanceof SMGSinglyLinkedListSegment);
        if (newTarget.equals(parentMaterialized)) {
          Preconditions.checkState(!(parentMaterialized instanceof SMGSinglyLinkedListSegment));
          // The pointer is still in all mode, reset it to region
          specifier = SMGTargetSpecifier.IS_REGION;
        } else {
          Preconditions.checkState(newTarget instanceof SMGSinglyLinkedListSegment);
          nestingLevel++;
        }
      }

      ValueAndSMGState newPtrAndState =
          currentState.searchOrCreateAddress(
              newTarget, ptrType, oldOffset, nestingLevel, specifier);

      currentState = newPtrAndState.getState();
      Value newPtr = newPtrAndState.getValue();
      newSMGValueToWrite = currentState.getMemoryModel().getSMGValueFromValue(newPtr).orElseThrow();
    }
    return currentState.writeValueWithoutChecks(
        currentMemory, offsetOfSMGValueInCurrentMemory, sizeOfSMGValueInBits, newSMGValueToWrite);
  }

  private SMGState replicateNestedNonPointerValue(
      SMGObject newMemory,
      SMGValue oldSMGValue,
      Value oldValue,
      SMGState currentState,
      BigInteger offset,
      BigInteger sizeInBits,
      Map<SMGNode, SMGNode> copiedMemoryCache,
      SMGObject parentMaterialized)
      throws SMGException {
    CType valueType = currentState.getMemoryModel().getTypeForValue(oldValue);

    // TODO: replace with getting the constraints and copying them for a new sym expr
    if (currentState.valueContainedInConstraints(oldValue)) {
      throw new SMGException(
          "Could not copy constraints on symbolic value when materializing a list.");
    }

    SMGValue smgValueOfNewSym;
    if (copiedMemoryCache.containsKey(oldSMGValue)) {
      smgValueOfNewSym = (SMGValue) copiedMemoryCache.get(oldSMGValue);
    } else {
      // Create symbolic value with the same type as the one above and save in the SMG
      int oldValueNestingLevel = currentState.getMemoryModel().getNestingLevel(oldValue);
      Preconditions.checkState(parentMaterialized.getNestingLevel() < oldValueNestingLevel);
      Value newSymbolicValue = currentState.getNewSymbolicValue(oldValue);
      SMGValueAndSMGState valueAndState =
          currentState.copyAndAddValue(newSymbolicValue, valueType, oldValueNestingLevel - 1);
      smgValueOfNewSym = valueAndState.getSMGValue();
      currentState = valueAndState.getSMGState();
      copiedMemoryCache.put(oldSMGValue, smgValueOfNewSym);
    }
    return currentState.writeValueWithoutChecks(newMemory, offset, sizeInBits, smgValueOfNewSym);
  }

  /**
   * Returns true if the {@link Value} given is to be copied. Returns false if it is to be
   * replicated (either a new symbolic value with the same constraints is to be written to the
   * location of the value entered here or the same memory with a new pointer is to be added instead
   * of the pointer value given here).
   *
   * @param pValue a {@link Value} that may be a pointer or a symbolic value (or any other).
   * @param pReplicationCache the equality cache of the creation of the original abstracted list
   *     section that knows if this value is to be copied or replicated.
   * @return true for copy, false for replication.
   */
  private boolean isCopyValue(Value pValue, EqualityCache<Value> pReplicationCache) {
    // TODO: fix self references in the cache
    return pReplicationCache.isEqualityKnown(pValue, pValue);
  }

  /**
   * Returns the prev list object of an 0+ SLL or ends with an exception.
   *
   * @param pState current state.
   * @param currZeroPlus the 0+.
   * @param nfo the nfo of the 0+.
   * @param pointerSize the pointer size.
   * @return The SMGValue of an PTE pointing towards the previous element (leftsided) of an 0+ SLL.
   * @throws SMGException never thrown.
   */
  private SMGValue getSLLPrevObjPointer(
      SMGState pState,
      SMGSinglyLinkedListSegment currZeroPlus,
      BigInteger nfo,
      BigInteger pointerSize)
      throws SMGException {
    List<SMGObject> prevObjects =
        pState.getMemoryModel().getSmg().getObjectsPointingToZeroPlusAbstraction(currZeroPlus);
    // We expect at most 1 ALL, FIRST and LAST pointer pointing towards any 0+
    assert prevObjects.size() <= 3;
    Optional<SMGObject> prevObj = Optional.empty();
    CType type = null;
    for (SMGObject maybePrevObj : prevObjects) {
      if (maybePrevObj.getSize().equals(currZeroPlus.getSize())) {
        // Do not change the state!
        SMGValueAndSMGState nextPointerOfPrevAndState =
            pState.readSMGValue(maybePrevObj, nfo, pointerSize);
        SMGValue nextPointerOfPrev = nextPointerOfPrevAndState.getSMGValue();
        if (pState.getMemoryModel().getSmg().isPointer(nextPointerOfPrev)) {
          SMGPointsToEdge pte =
              pState.getMemoryModel().getSmg().getPTEdge(nextPointerOfPrev).orElseThrow();
          if (pte.pointsTo().equals(currZeroPlus)
              && pte.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
            // This loop should always be very small (1-3 objects)
            Preconditions.checkArgument(prevObj.isEmpty());
            type = pState.getMemoryModel().getTypeForValue(nextPointerOfPrev);
            // correct object
            prevObj = Optional.of(maybePrevObj);
          }
        }
      }
    }
    Preconditions.checkArgument(prevObj.isPresent());
    ValueAndSMGState addressToPrev =
        pState.searchOrCreateAddress(prevObj.orElseThrow(), type, BigInteger.ZERO);
    SMGState currentState = addressToPrev.getState();
    return currentState
        .getMemoryModel()
        .getSMGValueFromValue(addressToPrev.getValue())
        .orElseThrow();
  }

  private boolean checkPointersOfLeftHandSideMaterializedList(
      SMGObject pNewConcreteRegion,
      SMGSinglyLinkedListSegment newAbsListSeg,
      SMGState pCurrentState)
      throws SMGException {
    for (SMGValue valuesPointingTo :
        pCurrentState.getMemoryModel().getSmg().getPointerValuesForTarget(newAbsListSeg)) {
      int ptrNstLvl = pCurrentState.getMemoryModel().getSmg().getNestingLevel(valuesPointingTo);
      if (newAbsListSeg.getMinLength() != 0 && ptrNstLvl >= newAbsListSeg.getMinLength()) {
        return false;
      }
    }
    if (newAbsListSeg instanceof SMGDoublyLinkedListSegment dll) {
      return checkPointersOfMaterializedDLL(
          pNewConcreteRegion, newAbsListSeg.getNextOffset(), dll.getPrevOffset(), pCurrentState);
    } else {
      return checkPointersOfMaterializedSLL(
          pNewConcreteRegion, newAbsListSeg.getNextOffset(), pCurrentState);
    }
  }

  private boolean checkPointersOfRightHandSideMaterializedList(
      SMGObject pNewConcreteRegion,
      SMGSinglyLinkedListSegment pNewAbsListSeg,
      SMGState pCurrentState)
      throws SMGException {
    for (SMGValue valuesPointingTo :
        pCurrentState.getMemoryModel().getSmg().getPointerValuesForTarget(pNewAbsListSeg)) {
      if (pNewAbsListSeg.getMinLength() != 0
          && pCurrentState.getMemoryModel().getSmg().getNestingLevel(valuesPointingTo)
              >= pNewAbsListSeg.getMinLength()) {
        return false;
      }
    }
    if (pNewAbsListSeg instanceof SMGDoublyLinkedListSegment newDll) {
      return checkPointersOfRightHandSideMaterializedDLL(
          pNewConcreteRegion, newDll.getNextOffset(), newDll.getPrevOffset(), pCurrentState);
    } else {
      return checkPointersOfRightHandSideMaterializedSLL(
          pNewConcreteRegion, pNewAbsListSeg, pNewAbsListSeg.getNextOffset(), pCurrentState);
    }
  }

  // Check that the pointers of a list are correct
  private boolean checkPointersOfMaterializedDLL(
      SMGObject newConcreteRegion, BigInteger nfo, BigInteger pfo, SMGState state)
      throws SMGException {
    // We can only check the connection of new concrete elements to new abstract,
    // the direction of the mat is specified via the matFromRight flag
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();
    SMGValueAndSMGState nextPointerAndState =
        state.readSMGValue(newConcreteRegion, nfo, pointerSize);
    SMGState currentState = nextPointerAndState.getSMGState();
    SMGValueAndSMGState prevPointerAndState =
        currentState.readSMGValue(newConcreteRegion, pfo, pointerSize);
    currentState = prevPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    SMGValue prevPointerValue = prevPointerAndState.getSMGValue();

    Optional<SMGPointsToEdge> prevPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
    SMGObject start = newConcreteRegion;
    List<SMGObject> listOfObjects = new ArrayList<>();
    if (prevPointer.isPresent()) {
      // There is at least 1 object before the new materialized,
      // if it is a valid list, we start from there
      // Note: there might be objects before that one! Or the prev object might look like a list
      // segment, without being one, i.e. the nfo does not point back to the start object.
      SMGObject maybeStart = prevPointer.orElseThrow().pointsTo();
      if (state.getMemoryModel().isObjectValid(maybeStart)
          && maybeStart.isSizeEqual(start)
          && !start.equals(maybeStart)) {
        SMGValueAndSMGState nextPointerAndStateOfPrev =
            state.readSMGValue(maybeStart, nfo, pointerSize);
        SMGValue nextPointerValueOfPrev = nextPointerAndStateOfPrev.getSMGValue();
        Optional<SMGPointsToEdge> nextPointerOfPrev =
            currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValueOfPrev);
        if (nextPointerOfPrev.isPresent()
            && nextPointerOfPrev.orElseThrow().pointsTo().equals(start)) {
          start = maybeStart;
          listOfObjects.add(start);
        }
      }
    }

    listOfObjects.add(newConcreteRegion);
    Optional<SMGPointsToEdge> nextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    // There is always a next obj if mat from left
    Preconditions.checkArgument(nextPointer.isPresent());
    SMGObject abstractObjectFollowingNewConcrete = nextPointer.orElseThrow().pointsTo();
    listOfObjects.add(abstractObjectFollowingNewConcrete);
    SMGValueAndSMGState nextNextPointerAndState =
        state.readSMGValue(abstractObjectFollowingNewConcrete, nfo, pointerSize);
    currentState = nextNextPointerAndState.getSMGState();
    SMGValue nextNextPointerValue = nextNextPointerAndState.getSMGValue();
    Optional<SMGPointsToEdge> nextNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextNextPointerValue);
    // This might not exist
    if (nextNextPointer.isPresent()) {
      listOfObjects.add(nextNextPointer.orElseThrow().pointsTo());
    }

    if (!checkList(start, nfo, listOfObjects, currentState)) {
      return false;
    }
    Collections.reverse(listOfObjects);
    return checkList(listOfObjects.get(0), pfo, listOfObjects, currentState);
  }

  // Check that the pointers of a list are correct for last pointer materialization
  private boolean checkPointersOfRightHandSideMaterializedDLL(
      SMGObject newConcreteRegion, BigInteger nfo, BigInteger pfo, SMGState state)
      throws SMGException {
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();
    SMGState currentState = state;
    SMGValueAndSMGState prevPointerAndState =
        currentState.readSMGValue(newConcreteRegion, pfo, pointerSize);
    currentState = prevPointerAndState.getSMGState();
    SMGValue prevPointerValue = prevPointerAndState.getSMGValue();

    // TODO: check that all previous last pointers now point to the concrete

    Optional<SMGPointsToEdge> prevPointerEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
    SMGObject currentObj = newConcreteRegion;
    // Since we materialized to the right, there needs to be a left list segment
    if (prevPointerEdge.isEmpty()) {
      return false;
    }

    SMGObject prevObj = prevPointerEdge.orElseThrow().pointsTo();
    // The prev needs to be an abstracted segment with a last specifier
    if (!(prevObj instanceof SMGSinglyLinkedListSegment)) {
      return false;
    } else if (!prevPointerEdge
        .orElseThrow()
        .targetSpecifier()
        .equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      return false;
    }

    // There is at least 1 object before the new materialized if it is a valid list.
    // Note: there might be objects before that one! Or the prev object might look like a list
    // segment, without being one, i.e. the nfo does not point back to the start object.
    if (!state.getMemoryModel().isObjectValid(prevObj)
        || !prevObj.isSizeEqual(currentObj)
        || currentObj.equals(prevObj)) {
      return false;
    }
    SMGValueAndSMGState nextOfPrevPointerAndState = state.readSMGValue(prevObj, nfo, pointerSize);
    SMGValue nextOfPrevPointerValue = nextOfPrevPointerAndState.getSMGValue();
    Optional<SMGPointsToEdge> nextOfPrevPointerEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextOfPrevPointerValue);
    // currentObj == new concrete obj
    if (nextOfPrevPointerEdge.isEmpty()
        || !nextOfPrevPointerEdge.orElseThrow().pointsTo().equals(currentObj)
        || !nextOfPrevPointerEdge
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_REGION)
        || currentState.getMemoryModel().getNestingLevel(nextOfPrevPointerValue) != 0) {
      return false;
    }
    return true;
  }

  private boolean checkPointersOfRightHandSideMaterializedSLL(
      SMGObject newConcreteRegion,
      SMGSinglyLinkedListSegment pNewAbsListSeg,
      BigInteger nfo,
      SMGState state) {
    Preconditions.checkArgument(!(pNewAbsListSeg instanceof SMGDoublyLinkedListSegment));
    SMG smg = state.getMemoryModel().getSmg();
    // check that the next of the abstracted SLL now points to the new concrete region
    SMGAndHasValueEdges readValue =
        smg.readValue(pNewAbsListSeg, nfo, smg.getSizeOfPointer(), false);
    if (readValue.getHvEdges().size() != 1
        || !smg.isPointer(readValue.getHvEdges().get(0).hasValue())
        || !smg.getPTEdge(readValue.getHvEdges().get(0).hasValue())
            .orElseThrow()
            .pointsTo()
            .equals(newConcreteRegion)) {
      return false;
    }
    // Check that the new concrete region has only region pointers towards it
    Set<SMGValue> ptrValuesTowardsConcrete = smg.getPointerValuesForTarget(newConcreteRegion);
    for (SMGValue ptrTowardsConcrete : ptrValuesTowardsConcrete) {
      SMGPointsToEdge pte = smg.getPTEdge(ptrTowardsConcrete).orElseThrow();
      if (!pte.targetSpecifier().equals(SMGTargetSpecifier.IS_REGION)) {
        return false;
      }
    }
    return true;
  }

  private boolean checkPointersOfMaterializedSLL(
      SMGObject newConcreteRegion, BigInteger nfo, SMGState state) throws SMGException {
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();
    SMGValueAndSMGState nextPointerAndState =
        state.readSMGValue(newConcreteRegion, nfo, pointerSize);
    SMGState currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();

    SMGObject start = newConcreteRegion;
    List<SMGObject> listOfObjects = new ArrayList<>();
    listOfObjects.add(newConcreteRegion);
    Optional<SMGPointsToEdge> nextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    // There is always a next obj
    Preconditions.checkArgument(nextPointer.isPresent());
    SMGObject abstractObjectFollowingNewConcrete = nextPointer.orElseThrow().pointsTo();
    listOfObjects.add(abstractObjectFollowingNewConcrete);
    SMGValueAndSMGState nextNextPointerAndState =
        state.readSMGValue(abstractObjectFollowingNewConcrete, nfo, pointerSize);
    currentState = nextNextPointerAndState.getSMGState();
    SMGValue nextNextPointerValue = nextNextPointerAndState.getSMGValue();
    Optional<SMGPointsToEdge> nextNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextNextPointerValue);
    // This might not exist
    if (nextNextPointer.isPresent()) {
      listOfObjects.add(nextNextPointer.orElseThrow().pointsTo());
    }
    return checkList(start, nfo, listOfObjects, currentState);
  }

  // Expects the list of expected objects in listOfObjects in the correct order for the offset
  private boolean checkList(
      SMGObject start, BigInteger pointerOffset, List<SMGObject> listOfObjects, SMGState state)
      throws SMGException {
    SMGObject currentObj = start;
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    for (int i = 0; i < listOfObjects.size(); i++) {
      SMGObject toCheckObj = listOfObjects.get(i);
      if (!currentObj.equals(toCheckObj)) {
        return false;
      }
      if (i == listOfObjects.size() - 1 || !state.getMemoryModel().isObjectValid(currentObj)) {
        break;
      }

      SMGValueAndSMGState nextPointerAndState =
          state.readSMGValue(currentObj, pointerOffset, pointerSize);
      SMGState currentState = nextPointerAndState.getSMGState();
      SMGValue nextPointerValue = nextPointerAndState.getSMGValue();

      Optional<SMGPointsToEdge> prevPointer =
          currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);

      if (prevPointer.isEmpty()) {
        // There might be no prev in start, this might happen for list elements that are not
        // abstractable
        return start.equals(toCheckObj);
      } else {
        currentObj = prevPointer.orElseThrow().pointsTo();
      }
    }
    return true;
  }
}

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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

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
   *     (In the context of the new state valueTopointerToAbstractObject behaves the same!)
   * @throws SMGException in case of critical errors.
   */
  public List<SMGValueAndSMGState> handleMaterialisation(
      SMGValue valueToPointerToAbstractObject,
      SMGSinglyLinkedListSegment pAbstractObject,
      SMGState state)
      throws SMGException {
    assert state.getMemoryModel().getSmg().checkFirstPointerNestingLevelConsistency();
    // Materialize from the left ( CE -> 3+ -> 0 => CE -> CE -> 2+ -> 0) for first ptrs and all next
    // ptrs. Materialize from the right for all last ptrs and prevs.
    if (pAbstractObject.getMinLength() == MINIMUM_LIST_LENGTH) {
      // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
      // removed
      return handleZeroPlusLLS(pAbstractObject, valueToPointerToAbstractObject, state);
    } else {
      return ImmutableList.of(
          materialiseLLS(pAbstractObject, valueToPointerToAbstractObject, state));
    }
  }

  /*
   * This generates 2 states.
   * One where we materialize the list once more
   * (we decide which way in the end), and one where the 0+ is deleted.
   * For the remove state, the last pointer now points to the prev obj (to the left),
   * while the next pointer points to the next of the 0+ (to the right).
   */
  private List<SMGValueAndSMGState> handleZeroPlusLLS(
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
                .replacePointersWithSMGValue(
                    pListSeg,
                    nextPointerValue,
                    0,
                    ImmutableSet.of(SMGTargetSpecifier.IS_FIRST_POINTER)));

    // Last ptr to the current
    // Important: last pointer specifier need to be region for the non-extended case
    assert currentState
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
                .replacePointersWithSMGValue(
                    pListSeg,
                    prevPointerValue,
                    0,
                    ImmutableSet.of(SMGTargetSpecifier.IS_LAST_POINTER)));

    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    if (state
        .getMemoryModel()
        .getSmg()
        .getPTEdge(pointerValueTowardsThisSegment)
        .orElseThrow()
        .targetSpecifier()
        .equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
      returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    } else if (state
        .getMemoryModel()
        .getSmg()
        .getPTEdge(pointerValueTowardsThisSegment)
        .orElseThrow()
        .targetSpecifier()
        .equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      returnStates.add(SMGValueAndSMGState.of(currentState, prevPointerValue));
    } else {
      throw new SMGException("Unknown pointer specifier towards 0+ list segment.");
    }

    statistics.stopTotalZeroPlusMaterializationTime();
    assert state.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    return returnStates
        .add(materialiseLLS(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
  }

  private SMGValueAndSMGState materialiseLLS(
      SMGSinglyLinkedListSegment pListSeg,
      SMGValue pPointerValueTowardsThisSegment,
      SMGState pState)
      throws SMGException {
    // Materialize from the left ( CE -> 3+ -> 0 => CE -> CE -> 2+ -> 0) for first ptrs and all next
    // ptrs.
    // Materialize from the right for all last ptrs and prevs.
    // ( CE -> 3+ -> CE => CE -> 2+ -> CE -> CE)
    SMGTargetSpecifier pointerSpecifier =
        pState
            .getMemoryModel()
            .getSmg()
            .getPTEdge(pPointerValueTowardsThisSegment)
            .orElseThrow()
            .targetSpecifier();
    if (pointerSpecifier.equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
      return materialiseLLSFromTheRight(pListSeg, pPointerValueTowardsThisSegment, pState);
    } else {
      return materialiseLLSFromTheLeft(pListSeg, pPointerValueTowardsThisSegment, pState);
    }
  }

  /**
   * Materializes an abstract list from the right. Example: 3+SLL -> 0 => 2+SLL -> new concrete -> 0
   *
   * @param pListSeg current abstracted list
   * @param pInitialPointer pointer to the abstracted list
   * @param state current {@link SMGState}
   * @return the initial pointer + the state with the materialized list.
   * @throws SMGException only for critical internal errors. Should NEVER be thrown.
   */
  private SMGValueAndSMGState materialiseLLSFromTheRight(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {
    statistics.startTotalMaterializationTime();
    statistics.incrementListMaterializations();
    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMGException(
          "Error when materializing a "
              + pListSeg.getClass().getSimpleName()
              + ": trying to materialize out of invalid memory.");
    }

    assert state.getMemoryModel().getSmg().checkSMGSanity();
    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);

    logger.log(Level.FINE, "Materialise " + pListSeg.getClass().getSimpleName() + ": ", pListSeg);

    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger nextPointerTargetOffset = pListSeg.getNextPointerTargetOffset();
    BigInteger pfo = null;
    BigInteger prevPointerTargetOffset = null;
    if (pListSeg instanceof SMGDoublyLinkedListSegment doublyLLS) {
      pfo = doublyLLS.getPrevOffset();
      prevPointerTargetOffset = doublyLLS.getPrevPointerTargetOffset();
    }
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers
    // We switch all and last pointers only.
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndCopyValuesAndSwitchPointers(
            pListSeg,
            state,
            MINIMUM_LIST_LENGTH,
            ImmutableSet.of(SMGTargetSpecifier.IS_ALL_POINTER, SMGTargetSpecifier.IS_LAST_POINTER));
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Get the pointer to the new concrete region
    ValueAndSMGState pointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(newConcreteRegion, nextPointerTargetOffset);
    currentState = pointerToNewConcreteAndState.getState();
    SMGValue valueOfPointerToConcreteObject =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(pointerToNewConcreteAndState.getValue())
            .orElseThrow();

    { // Some assertions
      Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
          currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
      Preconditions.checkArgument(maybePointsToEdgeToConcreteRegion.isPresent());
      Preconditions.checkArgument(
          maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
      Preconditions.checkArgument(
          maybePointsToEdgeToConcreteRegion
              .orElseThrow()
              .targetSpecifier()
              .equals(SMGTargetSpecifier.IS_REGION));
    }

    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstrLSAndPointersAndCopyValuesAndSwitchPointers(pListSeg, currentState);

    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create the new pointer to the new abstract list segment with the correct nesting level
    // Only needed by DLLs
    if (pListSeg instanceof SMGDoublyLinkedListSegment) {
      // TODO: Is it correct that we switch the all ptrs to last? Don't we need to copy the all
      // memory beforehand?
      ValueAndSMGState lastPointerToAbstrAndState =
          currentState.searchOrCreateAddress(
              newAbsListSeg,
              prevPointerTargetOffset,
              MINIMUM_LIST_LENGTH,
              SMGTargetSpecifier.IS_LAST_POINTER,
              ImmutableSet.of(SMGTargetSpecifier.IS_ALL_POINTER));
      currentState = lastPointerToAbstrAndState.getState();
      Value newLastPointerToAbstrValue = lastPointerToAbstrAndState.getValue();
      Preconditions.checkArgument(
          currentState.getMemoryModel().getNestingLevel(newLastPointerToAbstrValue)
              == MINIMUM_LIST_LENGTH);

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
      // prev
      // pointer
      currentState =
          currentState.writeValueWithoutChecks(newConcreteRegion, pfo, pointerSize, smgPtrToAbstr);
    }

    // Set the next pointer of the new abstract segment to the new concrete segment
    currentState =
        currentState.writeValueWithoutChecks(
            newAbsListSeg, nfo, pointerSize, valueOfPointerToConcreteObject);

    // Remove the old abstract list segment
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfRightHandSideMaterializedList(newConcreteRegion, nfo, pfo, currentState);
    assert currentState.getMemoryModel().getSmg().checkSMGSanity();
    // pInitialPointer might now point to the materialized object!
    if (pInitialPointer.equals(valueOfPointerToConcreteObject)) {
      // The nesting level of the initial pointer should be 0
      assert currentState.getMemoryModel().getNestingLevel(pInitialPointer) == 0;
    }
    statistics.stopTotalMaterializationTime();
    return SMGValueAndSMGState.of(currentState, pInitialPointer);
  }

  /*
   * The nesting level depicts where the rest of the memory is located in
   * relation to the abstract list. Each time a list segment is materialized, the sub-SMG of the
   * DLL is copied and the nesting level of the new sub-SMG (values and pointers) is
   * decremented by 1. (according to the paper, see comment in the code for how we do it currently)
   * We return the pointer to the segment just materialized.
   * Note: pValueOfPointerToAbstractObject does not guarantee that it points to the new concrete region!!!
   * Example: 3+SLL -> 0 => new concrete -> 2+SLL -> 0
   */
  private SMGValueAndSMGState materialiseLLSFromTheLeft(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {
    statistics.startTotalMaterializationTime();
    statistics.incrementListMaterializations();
    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMGException(
          "Error when materializing a "
              + pListSeg.getClass().getSimpleName()
              + ": trying to materialize out of invalid memory.");
    }

    assert state.getMemoryModel().getSmg().checkSMGSanity();
    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);

    logger.log(Level.FINE, "Materialise " + pListSeg.getClass().getSimpleName() + ": ", pListSeg);
    assert state.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger nextPointerTargetOffset = pListSeg.getNextPointerTargetOffset();
    BigInteger pfo = null;
    if (pListSeg instanceof SMGDoublyLinkedListSegment doublyLLS) {
      pfo = doublyLLS.getPrevOffset();
    }
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers
    // Don't switch last pointers (might happen for 1+ and 0+ as their nesting level is 0). They
    // need to remain on the 0+.
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndCopyValuesAndSwitchPointers(
            pListSeg,
            state,
            Integer.max(pListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH),
            ImmutableSet.of(
                SMGTargetSpecifier.IS_ALL_POINTER, SMGTargetSpecifier.IS_FIRST_POINTER));
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();
    assert currentState.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    // Get the pointer to the new concrete region (DLLs need that later, SLLs can have some
    // assertions)
    // Theoretically this might create a pointer/value that might not be used in SLLs
    ValueAndSMGState pointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(newConcreteRegion, BigInteger.ZERO);
    currentState = pointerToNewConcreteAndState.getState();
    SMGValue valueOfPointerToConcreteObject =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(pointerToNewConcreteAndState.getValue())
            .orElseThrow();

    { // Some assertions
      assert currentState
          .getMemoryModel()
          .getSmg()
          .getPTEdge(valueOfPointerToConcreteObject)
          .orElseThrow()
          .targetSpecifier()
          .equals(SMGTargetSpecifier.IS_REGION);
      Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
          currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
      Preconditions.checkArgument(maybePointsToEdgeToConcreteRegion.isPresent());
      Preconditions.checkArgument(
          maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
    }

    // TODO: problem, on 1+ we might have first and last ptrs (and all), but never want to switch
    // the last pointer to an concrete element for the extended list (this case), but switch it to
    // the 0+
    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstrLSAndCopyValuesAndSwitchPointers(pListSeg, currentState);
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create or find the new pointer to the new abstract list segment with the correct nesting
    // level and specifier
    int newNestingLevel = Integer.max(newAbsListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH);
    // There might be an existing other ptr already (ALL specifier) that needs to be replaced by
    // this new first pointer
    // TODO: is this correct?
    ValueAndSMGState pointerAndState =
        currentState.searchOrCreateAddress(
            newAbsListSeg,
            nextPointerTargetOffset,
            newNestingLevel,
            SMGTargetSpecifier.IS_FIRST_POINTER,
            ImmutableSet.of(SMGTargetSpecifier.IS_ALL_POINTER));
    currentState = pointerAndState.getState();
    Value newPointerValue = pointerAndState.getValue();
    Preconditions.checkArgument(
        currentState.getMemoryModel().getNestingLevel(newPointerValue) == newNestingLevel);

    // Create a new value and map the old pointer towards the abstract region on it
    // Create a Value mapping for the new Value representing a pointer
    SMGValueAndSMGState newValuePointingToWardsAbstractListAndState =
        currentState.copyAndAddValue(newPointerValue, newNestingLevel);

    SMGValue newValuePointingToWardsAbstractList =
        newValuePointingToWardsAbstractListAndState.getSMGValue();
    currentState = newValuePointingToWardsAbstractListAndState.getSMGState();

    // Write the new value w pointer towards the new abstract region to new concrete region as next
    // pointer
    currentState =
        currentState.writeValueWithoutChecks(
            newConcreteRegion, nfo, pointerSize, newValuePointingToWardsAbstractList);

    if (pListSeg instanceof SMGDoublyLinkedListSegment dllListSeg) {
      // Set the prev pointer of the new abstract segment to the new concrete segment
      SMGValue prevPointerValue = valueOfPointerToConcreteObject;
      if (!dllListSeg.getPrevPointerTargetOffset().equals(BigInteger.ZERO)) {
        ValueAndSMGState prevPointerValueAndState =
            currentState.searchOrCreateAddress(
                newConcreteRegion, dllListSeg.getPrevPointerTargetOffset());
        currentState = prevPointerValueAndState.getState();
        prevPointerValue =
            currentState
                .getMemoryModel()
                .getSMGValueFromValue(prevPointerValueAndState.getValue())
                .orElseThrow();
      }
      currentState =
          currentState.writeValueWithoutChecks(newAbsListSeg, pfo, pointerSize, prevPointerValue);

      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(pListSeg, nfo, pointerSize);
      currentState = nextPointerAndState.getSMGState();
      SMGValue nextPointerValue = nextPointerAndState.getSMGValue();

      Optional<SMGPointsToEdge> maybeNextPointer =
          currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
      if (maybeNextPointer.isPresent()
          && currentState
              .getMemoryModel()
              .isObjectValid(maybeNextPointer.orElseThrow().pointsTo())) {
        // Write the prev pointer of the next object to the prev object
        // We expect that all valid objects nfo points to are list segments
        currentState =
            currentState.writeValueWithoutChecks(
                maybeNextPointer.orElseThrow().pointsTo(),
                pfo,
                pointerSize,
                newValuePointingToWardsAbstractList);
      }
    }

    // Remove the old abstract list segment
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfMaterializedList(newConcreteRegion, nfo, pfo, currentState);
    assert currentState.getMemoryModel().getSmg().checkSMGSanity();
    // pInitialPointer might now point to the materialized object!
    if (pInitialPointer.equals(valueOfPointerToConcreteObject)) {
      // The nesting level of the initial pointer should be 0
      assert currentState.getMemoryModel().getSmg().getNestingLevel(pInitialPointer) == 0;
    }
    statistics.stopTotalMaterializationTime();
    assert currentState.getMemoryModel().getSmg().checkNotAbstractedNestingLevelConsistency();

    return SMGValueAndSMGState.of(currentState, pInitialPointer);
  }

  /**
   * Decrements the Abstracted list segment by creating a new abstracted list segment with min
   * length - 1, then copies all values from the old to the new, then replaces all pointers towards
   * the old segment with the new one as the new target and decrements their nesting level by 1.
   *
   * @param pListSeg the old {@link SMGSinglyLinkedListSegment} or {@link
   *     SMGDoublyLinkedListSegment}.
   * @param pState the current {@link SMGState}
   * @return the new {@link SMGState} and the new abstract list segment.
   */
  private SMGObjectAndSMGState decrementAbstrLSAndPointersAndCopyValuesAndSwitchPointers(
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
            currentState
                .getMemoryModel()
                .replaceAllPointersTowardsWithAndDecrementNestingLevel(pListSeg, newAbsListSeg));
    return SMGObjectAndSMGState.of(newAbsListSeg, currentState);
  }

  /**
   * Decrements the Abstracted list segment by creating a new abstracted list segment with min
   * length - 1, then copies all values from the old to the new, then replaces all pointers towards
   * the old segment with the new one as the new target.
   *
   * @param pListSeg the old {@link SMGSinglyLinkedListSegment} or {@link
   *     SMGDoublyLinkedListSegment}.
   * @param pState the current {@link SMGState}
   * @return the new {@link SMGState} and the new abstract list segment.
   */
  private SMGObjectAndSMGState decrementAbstrLSAndCopyValuesAndSwitchPointers(
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
   * Creates a new concrete region, adds it as heap object, copies all values of the abstracted list
   * segment to it, and switches the pointers with the nesting level given to the new object. This
   * also copies memory associated with ALL pointers and constraints of values copied.
   *
   * @param pListSeg the abstracted list segment, either {@link SMGSinglyLinkedListSegment} or
   *     {@link SMGDoublyLinkedListSegment}.
   * @param pState current {@link SMGState}
   * @param nestingLevelToSwitch the nesting level whose pointers are supposed to switch to the new
   *     segment. Typically, pListSeg.getMinLength() - 1 for the leftmost element, or 0 for the
   *     rightmost.
   * @return the new concrete region and the current state
   */
  private SMGObjectAndSMGState createNewConcreteRegionAndCopyValuesAndSwitchPointers(
      SMGSinglyLinkedListSegment pListSeg,
      SMGState pState,
      int nestingLevelToSwitch,
      Set<SMGTargetSpecifier> specifierToSwitch)
      throws SMGException {
    // Add new concrete memory region
    SMGObjectAndSMGState newConcreteRegionAndState =
        pState.copyAndAddNewHeapObject(pListSeg.getSize());
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Add all values. next/prev pointer is wrong here, depending on left/right sided
    // materialization! We write this later in the materialization.
    // If one of those is a pointer, we copy the pointer and memory structure
    Set<BigInteger> excludedOffsets = ImmutableSet.of(pListSeg.getNextOffset());
    if (pListSeg instanceof SMGDoublyLinkedListSegment dllListSeg) {
      excludedOffsets = ImmutableSet.of(pListSeg.getNextOffset(), dllListSeg.getPrevOffset());
    }
    // Careful when copying memory. Some memory needs a copy and some needs replication!
    // (copy == the same memory/values, replication == new, but equal memory/values)
    currentState =
        copyMemoryOfTo(
            pListSeg,
            newConcreteRegion,
            currentState,
            excludedOffsets,
            pListSeg.getRelevantEqualities(),
            ImmutableMap.of());

    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
                    pListSeg,
                    newConcreteRegion,
                    Integer.max(nestingLevelToSwitch, MINIMUM_LIST_LENGTH),
                    specifierToSwitch));
    return SMGObjectAndSMGState.of(newConcreteRegion, currentState);
  }

  /**
   * Copies all values from sourceObj to newMemory. Memory of pointers copied is then also copied,
   * except for the pointers at the offsets given in excludedOffsets. If a havok generating value is
   * copied, a new symbolic value with the correct constraints in the state is added instead.
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
   * @param copiedMemory a map from old memory to be copied to the new copy. Needed because of e.g.
   *     DLLs, or looping memory, so that we connect the memory correctly.
   * @return a new {@link SMGState} with all values copied from sourceObj to newMemory and all
   *     pointers and havoc generators copied into new memory/values.
   */
  private SMGState copyMemoryOfTo(
      SMGObject sourceObj,
      SMGObject newMemory,
      SMGState pState,
      Set<BigInteger> excludedOffsets,
      EqualityCache<Value> replicationCache,
      Map<SMGObject, SMGObject> copiedMemory)
      throws SMGException {
    SMGState currentState = pState.copyAllValuesFromObjToObj(sourceObj, newMemory);
    copiedMemory =
        ImmutableMap.<SMGObject, SMGObject>builder()
            .putAll(copiedMemory)
            .put(sourceObj, newMemory)
            .buildOrThrow();
    // All HVEs copied
    PersistentSet<SMGHasValueEdge> setOfValues =
        currentState
            .getMemoryModel()
            .getSmg()
            .getSMGObjectsWithSMGHasValueEdges()
            .getOrDefault(sourceObj, PersistentSet.of());
    // Filter out offsets that are unwanted, for example nfo or pfo
    Set<SMGHasValueEdge> valuesWONextAndPrev =
        setOfValues.stream()
            .filter(hve -> !excludedOffsets.contains(hve.getOffset()))
            .collect(ImmutableSet.toImmutableSet());

    // Now we have all values whose memory we might need to copy
    for (SMGHasValueEdge hve : valuesWONextAndPrev) {
      BigInteger offset = hve.getOffset();
      BigInteger sizeInBits = hve.getSizeInBits();
      SMGValue smgValue = hve.hasValue();
      Optional<Value> maybeValue = currentState.getMemoryModel().getValueFromSMGValue(smgValue);
      if (maybeValue.isPresent() && isCopyValue(maybeValue.orElseThrow(), replicationCache)) {
        // Copy case; we already copied the value, do nothing.
        continue;
      }

      // Replication cases
      if (currentState.getMemoryModel().getSmg().isPointer(smgValue)) {
        Value value = currentState.getMemoryModel().getValueFromSMGValue(smgValue).orElseThrow();
        // Copy memory and insert new pointer
        SMGStateAndOptionalSMGObjectAndOffset targetMemoryAndState =
            currentState.dereferencePointerWithoutMaterilization(value).orElseThrow();
        currentState = targetMemoryAndState.getSMGState();
        SMGObject oldTargetMemory = targetMemoryAndState.getSMGObject();
        Preconditions.checkArgument(
            targetMemoryAndState
                .getOffsetForObject()
                .asNumericValue()
                .bigIntegerValue()
                .equals(BigInteger.ZERO));
        // Copy targetMemory
        SMGValue newSMGValueToWrite;
        if (oldTargetMemory.isZero()) {
          newSMGValueToWrite = SMGValue.zeroValue();
        } else {
          SMGObject newTarget;
          if (copiedMemory.containsKey(oldTargetMemory)) {
            newTarget = copiedMemory.get(oldTargetMemory);
          } else {
            SMGObjectAndSMGState copiedTargetMemoryAndState =
                currentState.copyAndAddNewHeapObject(oldTargetMemory);
            newTarget = copiedTargetMemoryAndState.getSMGObject();
            currentState = copiedTargetMemoryAndState.getState();
            // Now copy all values and copy all memory for pointers again recursively
            currentState =
                copyMemoryOfTo(
                    oldTargetMemory,
                    newTarget,
                    currentState,
                    ImmutableSet.of(),
                    replicationCache,
                    copiedMemory);
          }
          // Create a new pointer to the new memory that is equal to the old and save in newConcrete
          SMGPointsToEdge oldPTE =
              currentState.getMemoryModel().getSmg().getPTEdge(smgValue).orElseThrow();
          BigInteger oldOffset = oldPTE.getOffset();
          int oldPtrNestingLvl = currentState.getMemoryModel().getNestingLevel(value);
          SMGTargetSpecifier oldPtrTargetSpec = oldPTE.targetSpecifier();
          ValueAndSMGState newPtrAndState =
              currentState.searchOrCreateAddress(
                  newTarget, oldOffset, oldPtrNestingLvl, oldPtrTargetSpec);
          currentState = newPtrAndState.getState();
          Value newPtr = newPtrAndState.getValue();
          newSMGValueToWrite =
              currentState.getMemoryModel().getSMGValueFromValue(newPtr).orElseThrow();
        }
        currentState =
            currentState.writeValueWithoutChecks(newMemory, offset, sizeInBits, newSMGValueToWrite);

      } else if (maybeValue.isPresent()
          && maybeValue.orElseThrow() instanceof SymbolicExpression
          && maybeValue.orElseThrow() instanceof ConstantSymbolicExpression constExpr
          && constExpr.getValue() instanceof SymbolicIdentifier) {
        Value value = maybeValue.orElseThrow();
        boolean valueHasConstraints = currentState.valueContainedInConstraints(value);
        // TODO: replace with getting the constraints and copying them for a new sym expr
        if (valueHasConstraints) {
          throw new SMGException(
              "Could not copy constraints on symbolic value when materializing a list.");
        }

        // Create symbolic value with the same type as the one above and save in the SMG
        Value newSymbolicValue = currentState.getNewSymbolicValue(value);
        SMGValueAndSMGState valueAndState = currentState.copyAndAddValue(newSymbolicValue);
        SMGValue smgValueOfNewSym = valueAndState.getSMGValue();
        currentState = valueAndState.getSMGState();
        currentState =
            currentState.writeValueWithoutChecks(newMemory, offset, sizeInBits, smgValueOfNewSym);
      }
    }
    return currentState;
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
            // correct object
            prevObj = Optional.of(maybePrevObj);
          }
        }
      }
    }
    Preconditions.checkArgument(prevObj.isPresent());
    ValueAndSMGState addressToPrev =
        pState.searchOrCreateAddress(prevObj.orElseThrow(), BigInteger.ZERO);
    SMGState currentState = addressToPrev.getState();
    return currentState
        .getMemoryModel()
        .getSMGValueFromValue(addressToPrev.getValue())
        .orElseThrow();
  }

  private boolean checkPointersOfMaterializedList(
      SMGObject pNewConcreteRegion, BigInteger pNfo, BigInteger pPfo, SMGState pCurrentState)
      throws SMGException {
    if (pPfo == null) {
      return checkPointersOfMaterializedSLL(pNewConcreteRegion, pNfo, pCurrentState);
    } else {
      return checkPointersOfMaterializedDLL(pNewConcreteRegion, pNfo, pPfo, pCurrentState);
    }
  }

  private boolean checkPointersOfRightHandSideMaterializedList(
      SMGObject pNewConcreteRegion, BigInteger pNfo, BigInteger pPfo, SMGState pCurrentState)
      throws SMGException {
    if (pPfo != null) {
      return checkPointersOfRightHandSideMaterializedDLL(
          pNewConcreteRegion, pNfo, pPfo, pCurrentState);
    } else {
      return checkPointersOfRightHandSideMaterializedSLL(pNewConcreteRegion, pNfo, pCurrentState);
    }
  }

  // Check that the pointers of a list are correct
  private boolean checkPointersOfMaterializedDLL(
      SMGObject newConcreteRegion, BigInteger nfo, BigInteger pfo, SMGState state)
      throws SMGException {
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
    SMGValueAndSMGState nextPointerAndState =
        state.readSMGValue(newConcreteRegion, nfo, pointerSize);
    SMGState currentState = nextPointerAndState.getSMGState();
    SMGValueAndSMGState prevPointerAndState =
        currentState.readSMGValue(newConcreteRegion, pfo, pointerSize);
    currentState = prevPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    SMGValue prevPointerValue = prevPointerAndState.getSMGValue();

    Optional<SMGPointsToEdge> prevPointerEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
    SMGObject currentObj = newConcreteRegion;
    List<SMGObject> listOfObjectsVisited = new ArrayList<>();
    if (prevPointerEdge.isEmpty()) {
      return false;
    }

    SMGObject prevObj = prevPointerEdge.orElseThrow().pointsTo();
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
    while (state.getMemoryModel().isObjectValid(prevObj)
        && prevObj.isSizeEqual(currentObj)
        && !currentObj.equals(prevObj)
        && !listOfObjectsVisited.contains(currentObj)) {
      SMGValueAndSMGState nextOfPrevPointerAndState = state.readSMGValue(prevObj, nfo, pointerSize);
      SMGValue nextOfPrevPointerValue = nextOfPrevPointerAndState.getSMGValue();
      Optional<SMGPointsToEdge> nextOfPrevPointerEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(nextOfPrevPointerValue);
      if (nextOfPrevPointerEdge.isEmpty()
          || !nextOfPrevPointerEdge.orElseThrow().pointsTo().equals(currentObj)) {
        return false;
      }

      if (currentObj instanceof SMGSinglyLinkedListSegment) {
        if (!nextOfPrevPointerEdge
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
          return false;
        }
      } else {
        if (!nextOfPrevPointerEdge
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_REGION)) {
          return false;
        }
      }
      // prevPointerEdge points to prevObj
      if (prevObj instanceof SMGSinglyLinkedListSegment) {
        if (!prevPointerEdge
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_LAST_POINTER)) {
          return false;
        }
      } else {
        if (!prevPointerEdge.orElseThrow().targetSpecifier().equals(SMGTargetSpecifier.IS_REGION)) {
          return false;
        }
      }
      listOfObjectsVisited.add(currentObj);
      currentObj = prevObj;

      prevPointerAndState = state.readSMGValue(currentObj, pfo, pointerSize);
      prevPointerValue = prevPointerAndState.getSMGValue();
      prevPointerEdge = currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
      if (prevPointerEdge.isEmpty()) {
        // No more list segments to the left
        break;
      }
      prevObj = prevPointerEdge.orElseThrow().pointsTo();
    }

    nextPointerAndState = state.readSMGValue(newConcreteRegion, nfo, pointerSize);
    nextPointerValue = nextPointerAndState.getSMGValue();
    Optional<SMGPointsToEdge> nextPointerEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);

    if (nextPointerEdge.isPresent() && !nextPointerValue.isZero()) {
      SMGObject nextObj = nextPointerEdge.orElseThrow().pointsTo();
      // the next obj might have a prev pointer that is region to the new concrete
      prevPointerAndState = state.readSMGValue(nextObj, pfo, pointerSize);
      prevPointerValue = prevPointerAndState.getSMGValue();
      prevPointerEdge = currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
      if (prevPointerEdge.isPresent()
          && prevPointerEdge.orElseThrow().pointsTo().equals(currentObj)
          && !prevPointerEdge
              .orElseThrow()
              .targetSpecifier()
              .equals(SMGTargetSpecifier.IS_REGION)) {
        return false;
      }
      if (nextObj instanceof SMGSinglyLinkedListSegment) {
        // the new concrete has a next ptr that is first
        if (!nextPointerEdge
            .orElseThrow()
            .targetSpecifier()
            .equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
          return false;
        }
      } else {
        // the new concrete has a next ptr that is region
        if (!nextPointerEdge.orElseThrow().targetSpecifier().equals(SMGTargetSpecifier.IS_REGION)) {
          return false;
        }
      }
    }

    return true;
  }

  @SuppressWarnings("unused")
  private boolean checkPointersOfRightHandSideMaterializedSLL(
      SMGObject newConcreteRegion, BigInteger nfo, SMGState state) {
    // TODO:
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
        return false;
      }

      currentObj = prevPointer.orElseThrow().pointsTo();
    }
    return true;
  }
}

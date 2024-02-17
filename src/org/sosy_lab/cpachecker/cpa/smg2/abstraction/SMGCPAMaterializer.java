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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPAMaterializer {

  private final LogManager logger;

  // For the 0+ segments
  private static final int MINIMUM_LIST_LENGTH = 0;

  public SMGCPAMaterializer(LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * Materializes lists from abstract lists into concrete lists. See the paper for more info. Note:
   * 0+ behave differently to the others in that it generates 2 states. We order those! The first in
   * the list is the minimal state where the 0+ is deleted, the second keeps a 0+ and grows.
   *
   * @param valueTopointerToAbstractObject pointer to an {@link SMGSinglyLinkedListSegment}.
   * @param pAbstractObject the target of valueTopointerToAbstractObject.
   * @param state current {@link SMGState}.
   * @return list of returned {@link SMGValueAndSMGState} with the value being the updated pointers.
   *     (In the context of the new state valueTopointerToAbstractObject behaves the same!)
   * @throws SMGException in case of critical errors.
   */
  public List<SMGValueAndSMGState> handleMaterialisation(
      SMGValue valueTopointerToAbstractObject, SMGObject pAbstractObject, SMGState state)
      throws SMGException {

    if (pAbstractObject instanceof SMGDoublyLinkedListSegment dllListSeg) {
      if (dllListSeg.getMinLength() == MINIMUM_LIST_LENGTH) {
        // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
        // gone
        return handleZeroPlusDLS(dllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return ImmutableList.of(materialiseLLS(dllListSeg, valueTopointerToAbstractObject, state));
      }
    } else if (pAbstractObject instanceof SMGSinglyLinkedListSegment sllListSeg) {
      if (sllListSeg.getMinLength() == MINIMUM_LIST_LENGTH) {
        // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
        // gone
        return handleZeroPlusSLS(sllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return ImmutableList.of(materialiseLLS(sllListSeg, valueTopointerToAbstractObject, state));
      }
    }
    throw new SMGException(
        "The SMG failed to materialize a abstract list as the memory object handled was not"
            + " abstracted to begin with.");
  }

  /*
   * This generates 2 states. One where we materialize the list once more and add the 0+ back and one where the 0+ is deleted.
   * When removing SLSs, we read the next pointer, then we remove the SLL segment and write the next
   * pointer to the previous memory as the new next pointer.
   * We are not allowed to change the pointer in this case as it might be value 0.
   * We know the previous segment needs to be a list, so the nfo is always correct.
   * The first state in the list is the state without 0+ in it, the second is the one where it grows.
   */
  private List<SMGValueAndSMGState> handleZeroPlusSLS(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pointerValueTowardsThisSegment, SMGState state)
      throws SMGException {

    logger.log(Level.ALL, "Split into 2 states because of 0+ SLS materialization.", pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();
    List<SMGObject> prevObjects =
        currentState.getMemoryModel().getSmg().getObjectsPointingToZeroPlusAbstraction(pListSeg);
    assert prevObjects.size() <= 1; // We expect at most 1 list segment pointing towards the 0+
    assert currentState
        .readSMGValue(prevObjects.get(0), nfo, pointerSize)
        .getSMGValue()
        .equals(pointerValueTowardsThisSegment);

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    // Write the value to the nfo of the previous object
    currentState =
        currentState.writeValueWithoutChecks(
            prevObjects.get(0), nfo, pointerSize, nextPointerValue);
    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    // currentState = currentState.prunePointerValueTargets(pListSeg, ImmutableSet.of(nfo));
    // Also remove the object
    // TODO: merge prunePointerValueTargets into copyAndRemoveObjectAndAssociatedSubSMG
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    return returnStates
        .add(materialiseLLS(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
  }

  /*
   * This generates 2 states. One where we materialize the list once more and add the 0+ back and one where the 0+ is deleted.
   */
  private List<SMGValueAndSMGState> handleZeroPlusDLS(
      SMGDoublyLinkedListSegment pListSeg, SMGValue pointerValueTowardsThisSegment, SMGState state)
      throws SMGException {

    logger.log(Level.ALL, "Split into 2 states because of 0+ DLS materialization.", pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pfo = pListSeg.getPrevOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();
    int pointersFixed = 0;

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValueAndSMGState prevPointerAndState = currentState.readSMGValue(pListSeg, pfo, pointerSize);
    currentState = prevPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    SMGValue prevPointerValue = prevPointerAndState.getSMGValue();

    Optional<SMGPointsToEdge> maybePrevPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue);
    if (maybePrevPointer.isPresent() && !maybePrevPointer.orElseThrow().pointsTo().isZero()) {
      SMGObject prevObj = maybePrevPointer.orElseThrow().pointsTo();
      assert prevObj.getSize().equals(pListSeg.getSize());
      assert currentState
          .readSMGValue(prevObj, nfo, pointerSize)
          .getSMGValue()
          .equals(pointerValueTowardsThisSegment);
      // Override the next pointer of the prev object with the next object of the 0+
      currentState =
          currentState.writeValueWithoutChecks(prevObj, nfo, pointerSize, nextPointerValue);
      pointersFixed++;
    }

    Optional<SMGPointsToEdge> maybeNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    if (maybeNextPointer.isPresent() && !maybeNextPointer.orElseThrow().pointsTo().isZero()) {
      SMGObject nextObj = maybeNextPointer.orElseThrow().pointsTo();
      assert nextObj.getSize().equals(pListSeg.getSize());
      assert currentState
          .readSMGValue(nextObj, pfo, pointerSize)
          .getSMGValue()
          .equals(pointerValueTowardsThisSegment);
      // Override the prev pointer of the next object with the prev object of the 0+
      currentState =
          currentState.writeValueWithoutChecks(nextObj, pfo, pointerSize, prevPointerValue);
      pointersFixed++;
    }

    assert state
            .getMemoryModel()
            .getSmg()
            .getObjectsPointingToZeroPlusAbstraction(pListSeg)
            .size()
        == pointersFixed;

    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    // currentState = currentState.prunePointerValueTargets(pListSeg, ImmutableSet.of(nfo));
    // Also remove the object
    // TODO: merge prunePointerValueTargets into copyAndRemoveObjectAndAssociatedSubSMG
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    return returnStates
        .add(materialiseLLS(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
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
   * segment to it, and switches the pointers with the nesting level given to the new object.
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
      SMGSinglyLinkedListSegment pListSeg, SMGState pState, int nestingLevelToSwitch) {
    // Add new concrete memory region
    SMGObjectAndSMGState newConcreteRegionAndState =
        pState.copyAndAddHeapObject(pListSeg.getSize());
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Add all values. next pointer is wrong here!
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newConcreteRegion);
    // TODO: Check all not nfo values if they are pointers, if they are, we need to copy their
    // targets and
    // insert a new pointer to the copy
    // -2 in the nesting lvl as we have not decremented the sll yet
    /*
    currentState =
        currentState.copyMemoryNotOriginatingFrom(
            newConcreteRegion,
            ImmutableSet.of(nfo),
            Integer.max(pListSeg.getMinLength() - 2, MINIMUM_LIST_LENGTH));
            */

    // --------------------------------------------------- 1

    // Replace the pointer behind the value pointing to the abstract region with a pointer to the
    // new object.
    // We don't change the nesting level of the pointers! We switch only those with new nesting
    // level == current minLength to the new concrete region and set that one to 0.
    // This saves us a lookup compared to the SMG paper!
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
                    pListSeg,
                    newConcreteRegion,
                    Integer.max(nestingLevelToSwitch, MINIMUM_LIST_LENGTH)));
    return SMGObjectAndSMGState.of(newConcreteRegion, currentState);
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
  @SuppressWarnings("unused")
  private SMGValueAndSMGState materialiseLLSFromTheRight(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {

    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMGException(
          "Error when materializing a "
              + pListSeg.getClass().getSimpleName()
              + ": trying to materialize out of invalid memory.");
    }

    assert state.getMemoryModel().getSmg().checkSMGSanity();
    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);

    logger.log(Level.FINE, "Materialise " + pListSeg.getClass().getSimpleName() + ": ", pListSeg);
    // TODO:
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pfo = null;
    if (pListSeg instanceof SMGDoublyLinkedListSegment doublyLLS) {
      pfo = doublyLLS.getPrevOffset();
    }
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndCopyValuesAndSwitchPointers(pListSeg, state, MINIMUM_LIST_LENGTH);
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    // Get the pointer to the new concrete region
    ValueAndSMGState pointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(newConcreteRegion, BigInteger.ZERO);
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
    }

    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstrLSAndCopyValuesAndSwitchPointers(pListSeg, currentState);
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create the new pointer to the new abstract list segment with the correct nesting level
    // Only needed by DLLs
    if (pListSeg instanceof SMGDoublyLinkedListSegment) {
      int newNestingLevel = MINIMUM_LIST_LENGTH;
      ValueAndSMGState pointerAndState =
          currentState.createAndAddPointerWithNestingLevel(
              newAbsListSeg, BigInteger.ZERO, newNestingLevel);
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

      // Write the new value w pointer towards the new abstract region to new concrete region as
      // prev
      // pointer
      currentState =
          currentState.writeValueWithoutChecks(
              newConcreteRegion, pfo, pointerSize, newValuePointingToWardsAbstractList);
    }

    // Set the next pointer of the new abstract segment to the new concrete segment
    currentState =
        currentState.writeValueWithoutChecks(
            newAbsListSeg, nfo, pointerSize, valueOfPointerToConcreteObject);

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveAbstractedObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfMaterializedList(newConcreteRegion, nfo, pfo, currentState);
    assert currentState.getMemoryModel().getSmg().checkSMGSanity();
    // pInitialPointer might now point to the materialized object!
    if (pInitialPointer.equals(valueOfPointerToConcreteObject)) {
      // The nesting level of the initial pointer should be 0
      assert currentState.getMemoryModel().getNestingLevel(pInitialPointer) == 0;
    }
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
  private SMGValueAndSMGState materialiseLLS(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pInitialPointer, SMGState state)
      throws SMGException {

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
    BigInteger pfo = null;
    if (pListSeg instanceof SMGDoublyLinkedListSegment doublyLLS) {
      pfo = doublyLLS.getPrevOffset();
    }
    BigInteger pointerSize = state.getMemoryModel().getSizeOfPointer();

    // Add new concrete memory region, copy all values from the abstracted and switch pointers
    SMGObjectAndSMGState newConcreteRegionAndState =
        createNewConcreteRegionAndCopyValuesAndSwitchPointers(
            pListSeg, state, Integer.max(pListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH));
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

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
      Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
          currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
      Preconditions.checkArgument(maybePointsToEdgeToConcreteRegion.isPresent());
      Preconditions.checkArgument(
          maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
    }

    // Create the now smaller abstracted list
    SMGObjectAndSMGState newAbsListSegAndState =
        decrementAbstrLSAndCopyValuesAndSwitchPointers(pListSeg, currentState);
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) newAbsListSegAndState.getSMGObject();
    currentState = newAbsListSegAndState.getState();

    // Create the new pointer to the new abstract list segment with the correct nesting level
    int newNestingLevel = Integer.max(newAbsListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH);
    ValueAndSMGState pointerAndState =
        currentState.createAndAddPointerWithNestingLevel(
            newAbsListSeg, BigInteger.ZERO, newNestingLevel);
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

    if (pListSeg instanceof SMGDoublyLinkedListSegment) {
      // Set the prev pointer of the new abstract segment to the new concrete segment
      currentState =
          currentState.writeValueWithoutChecks(
              newAbsListSeg, pfo, pointerSize, valueOfPointerToConcreteObject);

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
    currentState = currentState.copyAndRemoveAbstractedObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfMaterializedList(newConcreteRegion, nfo, pfo, currentState);
    assert currentState.getMemoryModel().getSmg().checkSMGSanity();
    // pInitialPointer might now point to the materialized object!
    if (pInitialPointer.equals(valueOfPointerToConcreteObject)) {
      // The nesting level of the initial pointer should be 0
      assert currentState.getMemoryModel().getSmg().getNestingLevel(pInitialPointer) == 0;
    }
    return SMGValueAndSMGState.of(currentState, pInitialPointer);
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
          && maybeStart.getSize().compareTo(start.getSize()) == 0
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

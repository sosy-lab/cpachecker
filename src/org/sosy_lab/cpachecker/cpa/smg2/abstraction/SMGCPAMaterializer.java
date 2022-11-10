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
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
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
   * @throws SMG2Exception in case of critical errors.
   */
  public List<SMGValueAndSMGState> handleMaterilisation(
      SMGValue valueTopointerToAbstractObject, SMGObject pAbstractObject, SMGState state)
      throws SMG2Exception {

    if (pAbstractObject instanceof SMGDoublyLinkedListSegment) {
      SMGDoublyLinkedListSegment sllListSeg = (SMGDoublyLinkedListSegment) pAbstractObject;
      if (sllListSeg.getMinLength() == MINIMUM_LIST_LENGTH) {
        // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
        // gone
        return handleZeroPlusDLS(sllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return ImmutableList.of(materialiseDLLS(sllListSeg, valueTopointerToAbstractObject, state));
      }
    } else if (pAbstractObject instanceof SMGSinglyLinkedListSegment) {
      SMGSinglyLinkedListSegment dllListSeg = (SMGSinglyLinkedListSegment) pAbstractObject;
      if (dllListSeg.getMinLength() == MINIMUM_LIST_LENGTH) {
        // handles 0+ and splits into 2 states. One with a longer list and 0+ again, one where its
        // gone
        return handleZeroPlusSLS(dllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return ImmutableList.of(materialiseSLLS(dllListSeg, valueTopointerToAbstractObject, state));
      }
    }
    throw new SMG2Exception("The SMG failed to materialize a abstract list.");
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
      throws SMG2Exception {

    logger.log(Level.ALL, "Split into 2 states because of 0+ SLS materialization.", pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();
    SMGObject prevObj =
        currentState
            .getMemoryModel()
            .getSmg()
            .getPreviousObjectOfZeroPlusAbstraction(pointerValueTowardsThisSegment);

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    // Write the value to the nfo of the previous object
    currentState = currentState.writeValue(prevObj, nfo, pointerSize, nextPointerValue);
    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    // Also remove the object
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    return returnStates
        .add(materialiseSLLS(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
  }

  /*
   * This generates 2 states. One where we materialize the list once more and add the 0+ back and one where the 0+ is deleted.
   */
  private List<SMGValueAndSMGState> handleZeroPlusDLS(
      SMGDoublyLinkedListSegment pListSeg, SMGValue pointerValueTowardsThisSegment, SMGState state)
      throws SMG2Exception {

    logger.log(Level.ALL, "Split into 2 states because of 0+ DLS materialization.", pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pfo = pListSeg.getPrevOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValueAndSMGState prevPointerAndState = currentState.readSMGValue(pListSeg, pfo, pointerSize);
    currentState = prevPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    SMGValue prevPointerValue = prevPointerAndState.getSMGValue();

    SMGPointsToEdge prevPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
    SMGObject prevObj = prevPointer.pointsTo();

    Optional<SMGPointsToEdge> maybeNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    if (maybeNextPointer.isPresent() && !maybeNextPointer.orElseThrow().pointsTo().isZero()) {
      // Write the prev pointer of the next object to the prev object
      currentState =
          currentState.writeValue(
              maybeNextPointer.orElseThrow().pointsTo(), pfo, pointerSize, prevPointerValue);
    }

    // Write the value to the nfo of the previous object
    currentState = currentState.writeValue(prevObj, nfo, pointerSize, nextPointerValue);
    // We can assume that a 0+ does not have other valid pointers to it!
    // Remove all other pointers/subgraphs associated with the 0+ object
    // Also remove the object
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().copyAndRemoveObjectAndAssociatedSubSMG(pListSeg));

    returnStates.add(SMGValueAndSMGState.of(currentState, nextPointerValue));
    return returnStates
        .add(materialiseDLLS(pListSeg, pointerValueTowardsThisSegment, state))
        .build();
  }

  /*
   * Materialize the SLL that pValueOfPointerToAbstractObject points towards.
   * This ALWAYS materializes the list once, including 0+.
   * It always adds a new abstracted list after the materialized list segment. This might be 0+.
   * We return the pointer to the segment just materialized.
   */
  private SMGValueAndSMGState materialiseSLLS(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pValueOfPointerToAbstractObject, SMGState state)
      throws SMG2Exception {

    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMG2Exception("Error when materializing a SLL.");
    }
    SMGValue initialPointer = pValueOfPointerToAbstractObject;

    logger.log(Level.ALL, "Materialise SLL ", pListSeg);

    SMGObjectAndSMGState newConcreteRegionAndState = state.copyAndAddHeapObject(pListSeg.getSize());
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    // Add all values. next pointer is wrong here!
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newConcreteRegion);

    // Create the now smaller abstracted list
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    // Now replace the abstract list element with a new abstract list element with length - 1
    currentState = currentState.copyAndAddObjectToHeap(newAbsListSeg);
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);
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
                    Integer.max(pListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH)));

    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    // Now we can safely switch all remaining pointers to the new abstract segment
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().replaceAllPointersTowardsWith(pListSeg, newAbsListSeg));

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);

    ValueAndSMGState newAddressAndState =
        currentState.createAndAddPointerWithNestingLevel(
            newAbsListSeg,
            BigInteger.ZERO,
            Integer.max(newAbsListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH));
    currentState = newAddressAndState.getState();
    Value newPointerValue = newAddressAndState.getValue();
    // Create a new value and map the old pointer towards the abstract region on it
    // Create a Value mapping for the new Value representing a pointer
    SMGValueAndSMGState newValuePointingToWardsAbstractListAndState =
        currentState.copyAndAddValue(newPointerValue);
    SMGValue newValuePointingToWardsAbstractList =
        newValuePointingToWardsAbstractListAndState
            .getSMGValue()
            .withNestingLevelAndCopy(newAbsListSeg.getMinLength() - 1);
    currentState = newValuePointingToWardsAbstractListAndState.getSMGState();

    // Write the new value w pointer towards abstract region to new region
    currentState =
        currentState.writeValue(
            newConcreteRegion, nfo, pointerSize, newValuePointingToWardsAbstractList);

    assert checkPointersOfMaterializedSLL(newConcreteRegion, nfo, currentState);
    // Note: valueOfPointerToAbstractObject is now pointing to the materialized object!
    return SMGValueAndSMGState.of(currentState, initialPointer);
  }

  /*
   * The nesting level depicts where the rest of the memory is located in
   * relation to the abstract list. Each time a list segment is materialized, the sub-SMG of the
   * DLL is copied and the nesting level of the new sub-SMG (values and pointers) is
   * decremented by 1. (according to the paper, see comment in the code for how we do it currently)
   * We return the pointer to the segment just materialized.
   * Note: pValueOfPointerToAbstractObject does not guarantee that it points to the new concrete region!!!
   */
  private SMGValueAndSMGState materialiseDLLS(
      SMGDoublyLinkedListSegment pListSeg, SMGValue pValueOfPointerToAbstractObject, SMGState state)
      throws SMG2Exception {

    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMG2Exception("Error when materializing a DLL.");
    }

    SMGValue initialPointer = pValueOfPointerToAbstractObject;

    logger.log(Level.ALL, "Materialise DLL ", pListSeg);

    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pfo = pListSeg.getPrevOffset();

    SMGObjectAndSMGState newConcreteRegionAndState = state.copyAndAddHeapObject(pListSeg.getSize());
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    // Add all values. next pointer is wrong here!
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newConcreteRegion);
    // Replace the pointer behind the value pointing to the abstract region with a pointer to the
    // new object
    // We don't change the nesting level of the pointers! We switch only those with new nesting
    // level == new minLength to the new concrete region and set that one to 0.
    // This saves us a lookup compared to the SMG paper!
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
                    pListSeg,
                    newConcreteRegion,
                    Integer.max(pListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH)));

    Preconditions.checkArgument(pListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    ValueAndSMGState correctPointerToNewConcreteAndState =
        currentState.searchOrCreateAddress(newConcreteRegion, BigInteger.ZERO);
    currentState = correctPointerToNewConcreteAndState.getState();
    Optional<SMGValue> maybeValueOfPointerToConcreteObject =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(correctPointerToNewConcreteAndState.getValue());
    Preconditions.checkArgument(maybeValueOfPointerToConcreteObject.isPresent());
    SMGValue valueOfPointerToConcreteObject = maybeValueOfPointerToConcreteObject.orElseThrow();
    {
      Optional<SMGPointsToEdge> maybePointsToEdgeToConcreteRegion =
          currentState.getMemoryModel().getSmg().getPTEdge(valueOfPointerToConcreteObject);
      Preconditions.checkArgument(maybePointsToEdgeToConcreteRegion.isPresent());
      Preconditions.checkArgument(
          maybePointsToEdgeToConcreteRegion.orElseThrow().pointsTo().equals(newConcreteRegion));
    }

    SMGDoublyLinkedListSegment newAbsListSeg =
        (SMGDoublyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    currentState = currentState.copyAndAddObjectToHeap(newAbsListSeg);
    // Switch all remaining pointers from the old abstract object to the new
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().replaceAllPointersTowardsWith(pListSeg, newAbsListSeg));
    // Copy the values from the old abstract list element to the new abstract list segment with
    // length - 1
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);
    // Create the new pointer to the new abstract list segment with the correct nesting level
    ValueAndSMGState pointerAndState =
        currentState.createAndAddPointerWithNestingLevel(
            newAbsListSeg,
            BigInteger.ZERO,
            Integer.max(newAbsListSeg.getMinLength() - 1, MINIMUM_LIST_LENGTH));
    currentState = pointerAndState.getState();
    Value newPointerValue = pointerAndState.getValue();

    // Create a new value and map the old pointer towards the abstract region on it
    // Create a Value mapping for the new Value representing a pointer
    SMGValueAndSMGState newValuePointingToWardsAbstractListAndState =
        currentState.copyAndAddValue(newPointerValue);
    // We receive the SMGValue for the new pointer here
    SMGValue newValuePointingToWardsAbstractList =
        newValuePointingToWardsAbstractListAndState
            .getSMGValue()
            .withNestingLevelAndCopy(newAbsListSeg.getMinLength() - 1);
    currentState = newValuePointingToWardsAbstractListAndState.getSMGState();

    // Write the new value w pointer towards abstract region to new concrete region as next pointer
    currentState =
        currentState.writeValue(
            newConcreteRegion, nfo, pointerSize, newValuePointingToWardsAbstractList);

    // Set the prev pointer of the new abstract segment to the new concrete segment
    currentState =
        currentState.writeValue(newAbsListSeg, pfo, pointerSize, valueOfPointerToConcreteObject);

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();

    Optional<SMGPointsToEdge> maybeNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    if (maybeNextPointer.isPresent()
        && currentState.getMemoryModel().isObjectValid(maybeNextPointer.orElseThrow().pointsTo())) {
      // Write the prev pointer of the next object to the prev object
      // We expect that all valid objects nfo points to are list segments
      currentState =
          currentState.writeValue(
              maybeNextPointer.orElseThrow().pointsTo(),
              pfo,
              pointerSize,
              newValuePointingToWardsAbstractList);
    }

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= MINIMUM_LIST_LENGTH);
    assert checkPointersOfMaterializedDLL(newConcreteRegion, nfo, pfo, currentState);
    // pValueOfPointerToAbstractObject might now point to the materialized object!
    if (initialPointer.equals(valueOfPointerToConcreteObject)) {
      // Reset nesting level
      initialPointer = initialPointer.withNestingLevelAndCopy(0);
    }
    return SMGValueAndSMGState.of(currentState, initialPointer);
  }

  // Check that the pointers of a list are correct
  private boolean checkPointersOfMaterializedDLL(
      SMGObject newConcreteRegion, BigInteger nfo, BigInteger pfo, SMGState state) {
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
      SMGObject maybeStart = prevPointer.orElseThrow().pointsTo();
      if (state.getMemoryModel().isObjectValid(maybeStart)
          && maybeStart.getSize().compareTo(start.getSize()) == 0
          && !start.equals(maybeStart)) {
        start = maybeStart;
        listOfObjects.add(start);
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
      SMGObject newConcreteRegion, BigInteger nfo, SMGState state) {
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
      SMGObject start, BigInteger pointerOffset, List<SMGObject> listOfObjects, SMGState state) {
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

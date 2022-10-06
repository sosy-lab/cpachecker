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
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPAMaterializer {

  private final LogManager logger;

  public SMGCPAMaterializer(LogManager pLogger) {
    logger = pLogger;
  }

  public SMGValueAndSMGState handleMaterilisation(
      SMGValue valueTopointerToAbstractObject, SMGObject pAbstractObject, SMGState state)
      throws SMG2Exception {

    if (pAbstractObject instanceof SMGDoublyLinkedListSegment) {
      SMGDoublyLinkedListSegment sllListSeg = (SMGDoublyLinkedListSegment) pAbstractObject;
      if (sllListSeg.getMinLength() == 0) {
        return removeDLLS(sllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return materialiseDLLS(sllListSeg, valueTopointerToAbstractObject, state);
      }
    } else if (pAbstractObject instanceof SMGSinglyLinkedListSegment) {
      SMGSinglyLinkedListSegment dllListSeg = (SMGSinglyLinkedListSegment) pAbstractObject;
      if (dllListSeg.getMinLength() == 0) {
        return handleZeroPlusSLS(dllListSeg, valueTopointerToAbstractObject, state);
      } else {
        return materialiseSLLS(dllListSeg, valueTopointerToAbstractObject, state);
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
   */
  private SMGValueAndSMGState handleZeroPlusSLS(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pointerValueTowardsThisSegment, SMGState state)
      throws SMG2Exception {

    logger.log(Level.ALL, "Split into 2 states because of 0+ SLS materialization.", pListSeg);
    ImmutableList.Builder<SMGValueAndSMGState> returnStates = ImmutableList.builder();
    returnStates.add(materialiseSLLS(pListSeg, pointerValueTowardsThisSegment, state));


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
    Optional<SMGPointsToEdge> maybeNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    if (maybeNextPointer.isEmpty()) {
      // There is no valid next pointer, so write the value to the nfo of the previous object
      // We can assume that a 0+ does not have other valid pointers to it!
      currentState = currentState.writeValue(prevObj, nfo, pointerSize, nextPointerValue);
      currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);
      return SMGValueAndSMGState.of(currentState, nextPointerValue);
    }
    SMGPointsToEdge nextPointer = maybeNextPointer.orElseThrow();

    // Set the pointer of the value in the previous segment to the next pointer of the list
    // Also set ALL pointers pointing towards the list segment to the previous object
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .copyAndRemoveSLLObjectAndReplacePointers(
                    pListSeg, pointerValueTowardsThisSegment, nextPointer, prevObj));

    return SMGValueAndSMGState.of(currentState, nextPointerValue);
  }

  /*
   * This generates 2 states. One where we materialize the list once more and add the 0+ back and one where the 0+ is deleted.
   */
  private SMGValueAndSMGState removeDLLS(
      SMGDoublyLinkedListSegment pListSeg,
      SMGValue pointerValueTowardsThisSegment,
      SMGState state) {

    logger.log(Level.ALL, "Remove 0+ SLL ", pListSeg);

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

    Optional<SMGPointsToEdge> maybeNextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue);
    if (maybeNextPointer.isEmpty()) {
      // There is no valid next pointer, so write the value to the nfo of the previous object
      // We can assume that a 0+ does not have other valid pointers to it!
      // Since this is a DLL we get the prev obj from the prev pointer
      SMGPointsToEdge prevPointer =
          currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
      currentState =
          currentState.writeValue(prevPointer.pointsTo(), nfo, pointerSize, nextPointerValue);
      currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);
      return SMGValueAndSMGState.of(currentState, nextPointerValue);
    }
    SMGPointsToEdge nextPointer = maybeNextPointer.orElseThrow();
    SMGPointsToEdge prevPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();

    // Remove the DLL object and replace the pointers.
    // If a -> b -> c and a <- b <- c then after this a -> c and a <- c
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .copyAndRemoveDLLObjectAndReplacePointers(
                    pListSeg, pointerValueTowardsThisSegment, nextPointer, prevPointer));

    return SMGValueAndSMGState.of(currentState, nextPointerValue);
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

    SMGValue valueOfPointerToAbstractObject =
        pValueOfPointerToAbstractObject.withNestingLevelAndCopy(0);
    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMG2Exception("Error when materializing a SLL.");
    }

    logger.log(Level.ALL, "Materialise SLL ", pListSeg);

    SMGObjectAndSMGState newConcreteRegionAndState = state.copyAndAddHeapObject(pListSeg.getSize());
    SMGState currentState = newConcreteRegionAndState.getState();
    SMGObject newConcreteRegion = newConcreteRegionAndState.getSMGObject();

    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    // Add all values. next pointer is wrong here!
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newConcreteRegion);
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
                    pListSeg, newConcreteRegion, pListSeg.getMinLength() - 1));

    /*
    if (pListSeg.getMinLength() == 1) {
      // Remove the old abstract list segment
      currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);
      return SMGValueAndSMGState.of(currentState, valueOfPointerToAbstractObject);
    }
    */
    Preconditions.checkArgument(pListSeg.getMinLength() >= 0);

    // Make the new value a pointer to the correct location and object
    Value newPointerValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    int newMinLength = Integer.max(newAbsListSeg.getMinLength() - 1, 0);
    currentState =
        currentState.createAndAddPointerWithNestingLevel(
            newPointerValue, newAbsListSeg, BigInteger.ZERO, newMinLength);
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

    // Now replace the abstract list element with a new abstract list element with length - 1
    currentState = currentState.copyAndAddObjectToHeap(newAbsListSeg);
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().replaceAllPointersTowardsWith(pListSeg, newAbsListSeg));

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() >= 0);
    // Note: valueOfPointerToAbstractObject is now pointing to the materialized object!
    return SMGValueAndSMGState.of(currentState, valueOfPointerToAbstractObject);
  }

  /*
   * TODO: nesting level. The nesting level depicts where the rest of the memory is located in
   * relation to the abstract list. Each time a list segment is materialized, the sub-SMG of the
   * SLL is copied and the nesting level of the new sub-SMG (values and pointers) is
   * decremented by 1.
   * We return the pointer to the segment just materialized.
   */
  private SMGValueAndSMGState materialiseDLLS(
      SMGDoublyLinkedListSegment pListSeg, SMGValue pValueOfPointerToAbstractObject, SMGState state)
      throws SMG2Exception {

    SMGValue valueOfPointerToAbstractObject =
        pValueOfPointerToAbstractObject.withNestingLevelAndCopy(0);
    if (!state.getMemoryModel().isObjectValid(pListSeg)) {
      throw new SMG2Exception("Error when materializing a DLL.");
    }

    logger.log(Level.ALL, "Materialise DLL ", pListSeg);

    // SMGPointsToEdge pointerToAbstractObject =
    //    state.getMemoryModel().getSmg().getPTEdge(pValueOfPointerToAbstractObject).orElseThrow();

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
                    pListSeg, newConcreteRegion, pListSeg.getMinLength() - 1));

    if (pListSeg.getMinLength() == 1) {
      // Remove the old abstract list segment
      currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);
      return SMGValueAndSMGState.of(currentState, valueOfPointerToAbstractObject);
    }
    Preconditions.checkArgument(pListSeg.getMinLength() > 1);
    // Make the new value a pointer to the correct location and object
    Value newPointerValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGDoublyLinkedListSegment newAbsListSeg =
        (SMGDoublyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    currentState =
        currentState.createAndAddPointerWithNestingLevel(
            newPointerValue, newAbsListSeg, BigInteger.ZERO, newAbsListSeg.getMinLength() - 1);

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

    // Now replace the abstract list element with a new abstract list element with length - 1
    currentState = currentState.copyAndAddObjectToHeap(newAbsListSeg);
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);
    // Write the value pointing to the new concrete region to the abstracted list
    currentState =
        currentState.writeValue(newAbsListSeg, pfo, pointerSize, valueOfPointerToAbstractObject);
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState.getMemoryModel().replaceAllPointersTowardsWith(pListSeg, newAbsListSeg));

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    Preconditions.checkArgument(newAbsListSeg.getMinLength() > 0);
    // Note: pValueOfPointerToAbstractObject is now pointing to the materialized object!
    return SMGValueAndSMGState.of(currentState, valueOfPointerToAbstractObject);
  }
}

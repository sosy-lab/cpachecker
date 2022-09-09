// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
        return removeSLLS(dllListSeg, valueTopointerToAbstractObject, null, state);
      } else {
        return materialiseSLLS(dllListSeg, valueTopointerToAbstractObject, state);
      }
    }
    throw new SMG2Exception("The SMG failed to materialize a abstract list.");
  }

  /*
   * When removing SLLs, we read the next pointer, then we remove the SLL segment and write the next
   * pointer to the previous memory as the new next pointer.
   * We know the previous segment needs to be a list, so the nfo is always correct.
   */
  private SMGValueAndSMGState removeSLLS(
      SMGSinglyLinkedListSegment pListSeg,
      SMGValue pointerValueTowardsThisSegment,
      @Nullable SMGObject prevObj,
      SMGState state) {

    logger.log(Level.ALL, "Remove 0+ SLL ", pListSeg);

    SMGState currentState = state;
    BigInteger nfo = pListSeg.getNextOffset();
    BigInteger pointerSize = currentState.getMemoryModel().getSizeOfPointer();

    SMGValueAndSMGState nextPointerAndState = currentState.readSMGValue(pListSeg, nfo, pointerSize);
    currentState = nextPointerAndState.getSMGState();
    SMGValue nextPointerValue = nextPointerAndState.getSMGValue();
    SMGPointsToEdge nextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();

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

    SMGPointsToEdge nextPointer =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();
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
   * TODO: nesting level. The nesting level depicts where the rest of the memory is located in
   * relation to the abstract list. Each time a list segment is materialized, the sub-SMG of the
   * SLL is copied and the nesting level of the new sub-SMG (values and pointers) is
   * decremented by 1.
   */
  private SMGValueAndSMGState materialiseSLLS(
      SMGSinglyLinkedListSegment pListSeg, SMGValue pValueOfPointerToAbstractObject, SMGState state)
      throws SMG2Exception {

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
    // new object
    // TODO: this is not according to the paper! We should copy all connected parts (subsmg) and
    // connect the new subsmg to the new part while the old staýs with the abstraction
    currentState =
        currentState.replaceAllPointersTowardsWith(
            pValueOfPointerToAbstractObject, newConcreteRegion);

    // Make the new value a pointer to the correct location and object
    Value newPointerValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGSinglyLinkedListSegment newAbsListSeg =
        (SMGSinglyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    currentState =
        currentState.createAndAddPointer(newPointerValue, newAbsListSeg, BigInteger.ZERO);
    // Create a new value and map the old pointer towards the abstract region on it
    // Create a Value mapping for the new Value representing a pointer
    SMGValueAndSMGState newValuePointingToWardsAbstractListAndState =
        currentState.copyAndAddValue(newPointerValue);
    SMGValue newValuePointingToWardsAbstractList =
        newValuePointingToWardsAbstractListAndState.getSMGValue();
    currentState = newValuePointingToWardsAbstractListAndState.getSMGState();

    // Write the new value w pointer towards abstract region to new region
    currentState =
        currentState.writeValue(
            newConcreteRegion, nfo, pointerSize, newValuePointingToWardsAbstractList);

    // Now replace the abstract list element with a new abstract list element with length - 1

    currentState = currentState.copyAndAddObjectToHeap(newAbsListSeg);
    currentState = currentState.copyAllValuesFromObjToObj(pListSeg, newAbsListSeg);

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    if (newAbsListSeg.getMinLength() == 0) {
      return removeSLLS(
          newAbsListSeg, newValuePointingToWardsAbstractList, newConcreteRegion, currentState);
    }

    Preconditions.checkArgument(newAbsListSeg.getSize().compareTo(BigInteger.ZERO) > 0);
    // Note: pValueOfPointerToAbstractObject is now pointing to the materialized object!
    return SMGValueAndSMGState.of(currentState, pValueOfPointerToAbstractObject);
  }

  /*
   * TODO: nesting level. The nesting level depicts where the rest of the memory is located in
   * relation to the abstract list. Each time a list segment is materialized, the sub-SMG of the
   * SLL is copied and the nesting level of the new sub-SMG (values and pointers) is
   * decremented by 1.
   */
  private SMGValueAndSMGState materialiseDLLS(
      SMGDoublyLinkedListSegment pListSeg, SMGValue pValueOfPointerToAbstractObject, SMGState state)
      throws SMG2Exception {

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
    // This is a design choice! The SMG paper does not state this.
    // My reasoning is this: if i have a pointer to a abstract list that is NOT the pointer used to
    // materialize is, i still would most likely want the first element after.
    currentState =
        currentState.replaceAllPointersTowardsWith(
            pValueOfPointerToAbstractObject, newConcreteRegion);

    // Make the new value a pointer to the correct location and object
    Value newPointerValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    SMGDoublyLinkedListSegment newAbsListSeg =
        (SMGDoublyLinkedListSegment) pListSeg.decrementLengthAndCopy();
    currentState =
        currentState.createAndAddPointer(newPointerValue, newAbsListSeg, BigInteger.ZERO);
    // Create a new value and map the old pointer towards the abstract region on it
    // Create a Value mapping for the new Value representing a pointer
    SMGValueAndSMGState newValuePointingToWardsAbstractListAndState =
        currentState.copyAndAddValue(newPointerValue);
    SMGValue newValuePointingToWardsAbstractList =
        newValuePointingToWardsAbstractListAndState.getSMGValue();
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
        currentState.writeValue(newAbsListSeg, pfo, pointerSize, pValueOfPointerToAbstractObject);

    // Remove the old abstract list segment
    currentState = currentState.copyAndRemoveObjectFromHeap(pListSeg);

    if (newAbsListSeg.getMinLength() == 0) {
      return removeDLLS(newAbsListSeg, newValuePointingToWardsAbstractList, currentState);
    }

    Preconditions.checkArgument(newAbsListSeg.getSize().compareTo(BigInteger.ZERO) > 0);
    // Note: pValueOfPointerToAbstractObject is now pointing to the materialized object!
    return SMGValueAndSMGState.of(currentState, pValueOfPointerToAbstractObject);
  }
}

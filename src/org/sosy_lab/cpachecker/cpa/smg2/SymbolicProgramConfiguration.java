// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.CValue;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.SMGProveNequality;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGandValue;

public class SymbolicProgramConfiguration {

  private final SMG smg;
  private final PersistentMap<String, SMGObject> globalVariableMapping;
  private final PersistentStack<StackFrame> stackVariableMapping;
  private final PersistentSet<SMGObject> heapObjects;
  private PersistentSet<SMGObject> externalObjectAllocation;

  private final PersistentBiMap<CValue, SMGValue> valueMapping;

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentSet<SMGObject> pExternalObjectAllocation,
      PersistentBiMap<CValue, SMGValue> pValueMapping) {
    globalVariableMapping = pGlobalVariableMapping;
    stackVariableMapping = pStackVariableMapping;
    smg = pSmg;
    externalObjectAllocation = pExternalObjectAllocation;
    heapObjects = pHeapObjects;
    valueMapping = pValueMapping;
  }

  public static SymbolicProgramConfiguration of(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentSet<SMGObject> pExternalObjectAllocation,
      PersistentBiMap<CValue, SMGValue> pValueMapping) {
    return new SymbolicProgramConfiguration(
        pSmg,
        pGlobalVariableMapping,
        pStackVariableMapping,
        pHeapObjects,
        pExternalObjectAllocation,
        pValueMapping);
  }

  public static SymbolicProgramConfiguration of(BigInteger sizeOfPtr) {
    PersistentBiMap<CValue, SMGValue> emptyMap = PersistentBiMap.of();
    return new SymbolicProgramConfiguration(
        new SMG(sizeOfPtr),
        PathCopyingPersistentTreeMap.of(),
        PersistentStack.of(),
        PersistentSet.of(),
        PersistentSet.of(),
        emptyMap.putAndCopy(CValue.zero(), SMGValue.zeroValue()));
  }

  public PersistentMap<String, SMGObject> getGolbalVariableToSmgObjectMap() {
    return globalVariableMapping;
  }

  public SMG getSmg() {
    return smg;
  }

  public BigInteger getSizeOfPointer() {
    return smg.getSizeOfPointer();
  }

  public boolean proveInequality(SMGValue pValue1, SMGValue pValue2) {
    SMGProveNequality nequality = new SMGProveNequality(smg);
    return nequality.proveInequality(pValue1, pValue2);
  }

  public SymbolicProgramConfiguration copyAndAddGlobalObject(
      SMGObject pNewObject, String pVarName) {
    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping.putAndCopy(pVarName, pNewObject),
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public PersistentStack<StackFrame> getStackFrames() {
    return stackVariableMapping;
  }

  public SymbolicProgramConfiguration copyAndAddStackObject(SMGObject pNewObject, String pVarName) {
    StackFrame currentFrame = stackVariableMapping.peek();
    PersistentStack<StackFrame> tmpStack = stackVariableMapping.popAndCopy();
    currentFrame = currentFrame.copyAndAddStackVariable(pVarName, pNewObject);
    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping,
        tmpStack.pushAndCopy(currentFrame),
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public SymbolicProgramConfiguration copyAndAddStackFrame(
      CFunctionDeclaration pFunctionDefinition, MachineModel model) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping.pushAndCopy(new StackFrame(pFunctionDefinition, model)),
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public SymbolicProgramConfiguration copyAndRemoveGlobalVariable(String pIdentifier) {
    Optional<SMGObject> objToRemove = Optional.ofNullable(globalVariableMapping.get(pIdentifier));
    if (objToRemove.isEmpty()) {
      return this;
    }
    PersistentMap<String, SMGObject> newGlobalsMap =
        globalVariableMapping.removeAndCopy(pIdentifier);
    SMG newSmg = smg.copyAndInvalidateObject(objToRemove.orElseThrow());
    return of(
        newSmg,
        newGlobalsMap,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public SymbolicProgramConfiguration copyAndRemoveStackVariable(String pIdentifier) {
    // If a stack variable becomes out of scope, there are not more than one frames which could
    // contain the variable
    Optional<StackFrame> frameOptional =
        Streams.stream(stackVariableMapping)
            .filter(frame -> frame.containsVariable(pIdentifier))
            .findAny();
    if (frameOptional.isEmpty()) {
      return this;
    }
    // ensured by frameOptional.isPresent()
    SMGObject objToRemove = frameOptional.orElseThrow().getVariable(pIdentifier);
    StackFrame newFrame = frameOptional.orElseThrow().copyAndRemoveVariable(pIdentifier);
    PersistentStack<StackFrame> newStack =
        stackVariableMapping.replace(frame -> frame == frameOptional.orElseThrow(), newFrame);
    SMG newSmg = smg.copyAndInvalidateObject(objToRemove);
    return of(
        newSmg,
        globalVariableMapping,
        newStack,
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  /**
   * Copy SPC and add a object to the heap. * With checks: throws {@link IllegalArgumentException}
   * when asked to add an object already present.
   *
   * @param pObject Object to add.
   */
  public SymbolicProgramConfiguration copyAndAddHeapObject(SMGObject pObject) {
    if (heapObjects.contains(pObject)) {
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    return of(
        smg.copyAndAddObject(pObject),
        globalVariableMapping,
        stackVariableMapping,
        heapObjects.addAndCopy(pObject),
        externalObjectAllocation,
        valueMapping);
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and any edges leading
   * from/to it.
   *
   * <p>TODO: A test case with (invalid) passing of an address of a dropped frame object outside,
   * and working with them. For that, we should probably keep those as invalid, so we can spot such
   * bug.
   */
  public SymbolicProgramConfiguration copyAndDropStackFrame() {
    StackFrame frame = stackVariableMapping.peek();
    PersistentStack<StackFrame> newStack = stackVariableMapping.popAndCopy();
    SMG newSmg = smg;
    for (SMGObject object : frame.getAllObjects()) {
      newSmg = smg.copyAndInvalidateObject(object);
    }
    return of(
        newSmg,
        globalVariableMapping,
        newStack,
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public SymbolicProgramConfiguration copyAndPruneUnreachable(
      Collection<SMGObject> pUnreachableObjects) {
    Collection<SMGObject> visibleObjects =
        FluentIterable.concat(
                getGolbalVariableToSmgObjectMap().values(),
                FluentIterable.from(stackVariableMapping)
                    .transformAndConcat(stackFrame -> stackFrame.getAllObjects()))
            .toSet();
    SMGObjectsAndValues reachable = smg.collectReachableObjectsAndValues(visibleObjects);
    Set<SMGObject> unreachableObjects =
        new HashSet<>(Sets.difference(smg.getObjects(), reachable.getObjects()));
    Set<SMGValue> unreachableValues =
        new HashSet<>(Sets.difference(smg.getValues(), reachable.getValues()));
    SMG newSmg =
        smg.copyAndRemoveObjects(unreachableObjects).copyAndRemoveValues(unreachableValues);
    // copy into return collection
    pUnreachableObjects.addAll(unreachableObjects);
    PersistentSet<SMGObject> newHeapObjects = heapObjects;
    for (SMGObject smgObject : unreachableObjects) {
      newHeapObjects = newHeapObjects.removeAndCopy(smgObject);
    }
    return of(
        newSmg,
        globalVariableMapping,
        stackVariableMapping,
        newHeapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  public Optional<SMGObject> getReturnObjectForCurrentStackFrame() {
    return stackVariableMapping.peek().getReturnObject();
  }

  public SymbolicProgramConfiguration copyAndPutValue(CValue cValue, SMGValue smgValue) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping.putAndCopy(cValue, smgValue));
  }

  public Optional<SMGValue> getValue(CValue cValue) {
    return Optional.ofNullable(valueMapping.get(cValue));
  }

  public SymbolicProgramConfiguration copyAndCreateValue(CValue cValue) {
    return copyAndPutValue(cValue, SMGValue.of());
  }

  public SymbolicProgramConfiguration copyAndAddExternalObject(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.addAndCopy(pObject),
        valueMapping);
  }

  public Optional<SMGObject> getObjectForVisibleVariable(String pName) {

    // First look in stack frame
    for (StackFrame frame : stackVariableMapping) {
      if (frame.containsVariable(pName)) {
        return Optional.of(frame.getVariable(pName));
      }
    }
    // Second check global
    if (globalVariableMapping.containsKey(pName)) {
      return Optional.of(globalVariableMapping.get(pName));
    }
    // no variable found
    return Optional.empty();
  }

  public boolean isObjectExternallyAllocated(SMGObject pObject) {
    return externalObjectAllocation.contains(pObject);
  }

  /**
   * Checks if a smg object is valid in the current context.
   *
   * @param pObject - the object to be checked
   * @return smg.isValid(pObject)
   */
  public boolean isObjectValid(SMGObject pObject) {
    return smg.isValid(pObject);
  }

  /**
   * Reads the explicit value written in a memory chunk represented as SMGRegion object at a certain
   * offset with a given size. This memory chunk can ever be a covered by exactly one
   * SMGHasValueEdge or multiple of them. If the memory chunk to be read does not fit a single edge,
   * the resulting value is computed bitwise.
   *
   * @param pObject - the SMGRegion
   * @param pFieldOffset - the offset
   * @param pSizeofInBits - the size of the chunk
   * @return the explicit value written in the defined memory chunk
   */
  public CValue readValue(SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    if (!isObjectValid(pObject) && !isObjectExternallyAllocated(pObject)) {
      return CValue.getUnknownValue();
    }
    // This call to readValue might create a new SMGValue, but here we are only interested in
    // existing values, that exactly matches offset and size.
    // ReadValue already covers the check for nullified blocks.
    SMGandValue newSMGAndValue = smg.readValue(pObject, pFieldOffset, pSizeofInBits);
    SMGValue smgValue = newSMGAndValue.getValue();
    if (valueMapping.containsValue(smgValue)) {
      return valueMapping.inverse().get(smgValue);
    }
    // A memory chunk might by covered by one or more has value edges
    Collection<SMGHasValueEdge> overlappingEdges =
        smg.getOverlappingEdges(pObject, pFieldOffset, pSizeofInBits);
    CValue returnValue = CValue.zero();

    // smg.getOverlappingEdges is already sorted by offset
    for (SMGHasValueEdge edge : overlappingEdges) {
      smgValue = edge.hasValue();
      // if one block is unknown explicit value computations fails
      if (!valueMapping.containsValue(smgValue)) {
        return CValue.getUnknownValue();
      }
      // bitwise computation of the return value
      returnValue =
          bitwiseReadValue(
              pFieldOffset,
              edge.getOffset(),
              pSizeofInBits,
              edge.getSizeInBits(),
              valueMapping.inverse().get(smgValue),
              returnValue);
      // reached block end
      if (reachedBlockEnd(edge, pFieldOffset, pSizeofInBits)) {
        return returnValue;
      }
    }
    // if there are no overlapping edges or if the chunk is not fully covered
    return CValue.getUnknownValue();
  }

  /**
   * Checks if a edge covers the end of a memory chunk given by an offset and size tuple.
   *
   * @param pEdge - the edge to be checked
   * @param pFieldOffset - the chunks offset
   * @param pSizeofInBits - the chunks size
   * @return edgeOffset + edgeSize >= pFieldOffset + pSizeofInBits
   */
  private boolean reachedBlockEnd(
      SMGHasValueEdge pEdge, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    return pEdge.getOffset().add(pEdge.getSizeInBits()).compareTo(pSizeofInBits.add(pFieldOffset))
        >= 0;
  }

  /**
   * Utility function to read a certain bit range of a value and bitwise concatenate the return
   * values.
   *
   * @param fieldOffset - the offset of the memory chunk to be read
   * @param edgeOffset - the offset of the edge covering the chunk
   * @param fieldSize - the size of the memory chunk to be read
   * @param edgeSize - the size of the edge covering the chunk
   * @param readValue - the value which is represented by the edge
   * @param returnCValue - the CValue bits which were already read with previous edges
   * @return the concatenation of returnCValue with the bits of readValue which are in the memory
   *     chunk
   */
  private CValue bitwiseReadValue(
      BigInteger fieldOffset,
      BigInteger edgeOffset,
      BigInteger fieldSize,
      BigInteger edgeSize,
      CValue readValue,
      CValue returnCValue) {
    // TODO handle little and big endian here

    // Edge may start "before" the chunk to be read. Cut off the bits to left of the chunk.
    int leftBitsToBeCutOff = fieldOffset.subtract(edgeOffset).intValue();
    readValue = readValue.shiftRight(leftBitsToBeCutOff);

    // Edge may end "after" the chunk to be read. Cut off the bits to right of the chunk.
    int cutOffCounter = fieldOffset.add(fieldSize).subtract(edgeSize.add(edgeOffset)).intValue();

    while (cutOffCounter > 0) {
      readValue = readValue.clearBit(edgeSize.intValue() - cutOffCounter--);
    }

    // concatenate the resulting bits with the already computed bit value
    return returnCValue.concat(readValue);
  }
}

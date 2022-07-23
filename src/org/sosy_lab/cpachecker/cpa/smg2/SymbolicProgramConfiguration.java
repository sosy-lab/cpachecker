// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSPC;
import org.sosy_lab.cpachecker.cpa.smg2.util.SPCAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.CValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.SMGProveNequality;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGandValue;

/**
 * This class models the memory with its global/heap/stack variables. Its idea is that we handle
 * only SMG specific stuff here, and have already transformed all CPA and C (or other) specific
 * stuff. This class splits the values by types, the values in themselfs are value ranges or
 * addresses. Also, we handle (in)equality for the value analysis here, as this is more delicate
 * than just ==, <, > etc. see proveInequality() for more info. Variable ranges are needed for the
 * abstraction (join of SMGs) to succeed more often. The abstraction might however merge SMG objects
 * and value ranges making read and equality non trivial.
 */
public class SymbolicProgramConfiguration {

  /** The SMG modelling this memory image. */
  private final SMG smg;

  /** Mapping of all global variables to their SMGObjects. Use the value mapping to get values. */
  private final PersistentMap<String, SMGObject> globalVariableMapping;

  /* The stack of stackFrames.
   * (Each function call creates a new one that has to be popd once the function is returned/ends)
   */
  private final PersistentStack<StackFrame> stackVariableMapping;

  /* (SMG)Objects on the heap. */
  private final PersistentSet<SMGObject> heapObjects;

  /* Map of (SMG)Objects externaly allocated. The bool denotes validity, true = valid, false = invalid i.e. after free() */
  private PersistentMap<SMGObject, Boolean> externalObjectAllocation;

  /**
   * Maps the symbolic value ranges to their abstract SMG counterparts. (SMGs use only abstract, but
   * unique values. Such that a SMGValue with id 1 is always equal only with a SMGValue with id 1.
   * We need symbolic value ranges for the values analysis. Concrete values would ruin abstraction
   * capabilities. The only exception are addresses, hence why they are seperate) . IMportant: this
   * mapping is only part of the total mapping! You NEED to map the SMGValue using the mapping of
   * the SPC! TODO: use SymbolicRegionManager or smth like it in a changed implementation of the
   * value. TODO: map the SMGValues using the SPC mapping or decide on a new mapping idea
   */
  private final ImmutableBiMap<Value, SMGValue> valueMapping;

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentMap<SMGObject, Boolean> pExternalObjectAllocation,
      ImmutableBiMap<Value, SMGValue> pValueMapping) {
    globalVariableMapping = pGlobalVariableMapping;
    stackVariableMapping = pStackVariableMapping;
    smg = pSmg;
    externalObjectAllocation = pExternalObjectAllocation;
    heapObjects = pHeapObjects;
    valueMapping = pValueMapping;
  }

  /**
   * Creates a new {@link SymbolicProgramConfiguration} out of the elements given.
   *
   * @param pSmg the {@link SMG} of this SPC.
   * @param pGlobalVariableMapping the global variable map as {@link PersistentMap} mapping {@link
   *     String} to {@link SMGObject}.
   * @param pStackVariableMapping the stack variable mappings as a {@link PersistentStack} of {@link
   *     StackFrame}s.
   * @param pHeapObjects the heap {@link SMGObject}s on a {@link PersistentStack}.
   * @param pExternalObjectAllocation externally allocated {@link SMGObject}s on a map that saves
   *     the validity of the object. False is invalid.
   * @param pValueMapping {@link BiMap} mapping the {@link Value}s and {@link SMGValue}s.
   * @return the newly created {@link SymbolicProgramConfiguration}.
   */
  public static SymbolicProgramConfiguration of(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects,
      PersistentMap<SMGObject, Boolean> pExternalObjectAllocation,
      ImmutableBiMap<Value, SMGValue> pValueMapping) {
    return new SymbolicProgramConfiguration(
        pSmg,
        pGlobalVariableMapping,
        pStackVariableMapping,
        pHeapObjects,
        pExternalObjectAllocation,
        pValueMapping);
  }

  /**
   * Creates a new, empty {@link SymbolicProgramConfiguration} and returns it.
   *
   * @param sizeOfPtr the size of the pointers in this new SPC in bits as {@link BigInteger}.
   * @return The newly created {@link SymbolicProgramConfiguration}.
   */
  public static SymbolicProgramConfiguration of(BigInteger sizeOfPtr) {
    return new SymbolicProgramConfiguration(
        new SMG(sizeOfPtr),
        PathCopyingPersistentTreeMap.of(),
        PersistentStack.of(),
        PersistentSet.of(SMGObject.nullInstance()),
        PathCopyingPersistentTreeMap.of(),
        ImmutableBiMap.of(new NumericValue(0), SMGValue.zeroValue()));
  }

  /**
   * @return The global variable mapping in a {@link PersistentMap} from String to the {@link
   *     SMGObject}.
   */
  public PersistentMap<String, SMGObject> getGlobalVariableToSmgObjectMap() {
    return globalVariableMapping;
  }

  /** Returns the SMG that models the memory used in this {@link SymbolicProgramConfiguration}. */
  public SMG getSmg() {
    return smg;
  }

  /**
   * Returns the size of the pointer used for the SMG of this {@link SymbolicProgramConfiguration}.
   */
  public BigInteger getSizeOfPointer() {
    return smg.getSizeOfPointer();
  }

  /**
   * Tries to check for inequality of 2 {@link SMGValue}s used in the SMG of this {@link
   * SymbolicProgramConfiguration}. This does NOT check the (concrete) CValues of the entered
   * values, but only if they refer to the same memory location in the SMG or not! TODO: remove
   * CValues and replace by symbolic value ranges.
   *
   * @param pValue1 A {@link SMGValue} to be check for inequality with pValue2.
   * @param pValue2 A {@link SMGValue} to be check for inequality with pValue1.
   * @return True if the 2 {@link SMGValue}s are not equal, false if they are equal.
   */
  public boolean proveInequality(SMGValue pValue1, SMGValue pValue2) {
    // Can this be solved without creating a new SMGProveNequality every time?
    // TODO: Since we need to rework the values anyway, make a new class for this.
    SMGProveNequality nequality = new SMGProveNequality(smg);
    return nequality.proveInequality(pValue1, pValue2);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the mapping from the variable
   * identifier pVarName to the {@link SMGObject} pNewObject.
   *
   * @param pNewObject - The {@link SMGObject} that the variable is mapped to.
   * @param pVarName - The variable identifier that the {@link SMGObject} is mapped to.
   * @return The copied SPC with the global object added.
   */
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

  /**
   * @return The stack of {@link StackFrame}s modeling the function stacks of this {@link
   *     SymbolicProgramConfiguration}.
   */
  public PersistentStack<StackFrame> getStackFrames() {
    return stackVariableMapping;
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the stack object given with the
   * variable name given to it.
   *
   * @param pNewObject the new stack object.
   * @param pVarName the name of the added stack object.
   * @return Copy of this SPC with the stack variable mapping added.
   */
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

  /**
   * Replaces the Value mapping of the oldValue <-> SMGValue to the newValueToBeAssigned for the
   * same SMGValue. (Represents a change of Value without a write operation)
   *
   * @param oldValue The {@link Value} currently mapped to a {@link SMGValue} that should be
   *     replaced.
   * @param newValueToBeAssigned the new {@link Value} that should replace the old Value.
   * @return a SPC with the mapping of the Value replaced with the new one.
   */
  public SymbolicProgramConfiguration copyAndReplaceValueMapping(
      Value oldValue, Value newValueToBeAssigned) {
    ImmutableBiMap.Builder<Value, SMGValue> builder = ImmutableBiMap.builder();
    for (Entry<Value, SMGValue> entry : valueMapping.entrySet()) {
      if (entry.getKey() == oldValue) {
        builder.put(newValueToBeAssigned, entry.getValue());
      } else {
        builder.put(entry);
      }
    }
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        builder.buildOrThrow());
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds a {@link StackFrame} based on the
   * entered model and function definition. More information on StackFrames can be found in the
   * Stackframe class.
   *
   * @param pFunctionDefinition - The {@link CFunctionDeclaration} that the {@link StackFrame} will
   *     be based upon.
   * @param model - The {@link MachineModel} the new {@link StackFrame} be based upon.
   * @return The SPC copy with the new {@link StackFrame}.
   */
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

  /**
   * Copies this {@link SymbolicProgramConfiguration} and removes the global variable given.
   *
   * @param pIdentifier - String identifier of the global variable to remove.
   * @return Copy of the SPC with the global variable removed.
   */
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

  /**
   * Copies this {@link SymbolicProgramConfiguration} and removes the stack variable given.
   *
   * @param pIdentifier - String identifier for the variable to be removed.
   * @return Copy of the SPC with the variable removed.
   */
  public SymbolicProgramConfiguration copyAndRemoveStackVariable(String pIdentifier) {
    // If a stack variable becomes out of scope, there are not more than one frames which could
    // contain the variable
    Optional<StackFrame> frameOptional = Optional.empty();
    for (StackFrame frame : stackVariableMapping) {
      if (frame.containsVariable(pIdentifier)) {
        frameOptional = Optional.of(frame);
      }
    }
    if (frameOptional.isEmpty()) {
      return this;
    }
    StackFrame oldFrame = frameOptional.orElseThrow();
    // ensured by frameOptional.isPresent()
    SMGObject objToRemove = frameOptional.orElseThrow().getVariable(pIdentifier);
    StackFrame newFrame = frameOptional.orElseThrow().copyAndRemoveVariable(pIdentifier);
    PersistentStack<StackFrame> newStack =
        stackVariableMapping.replace(frame -> frame == oldFrame, newFrame);
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
   * Checks if the entered {@link SMGObject} is part of the heap.
   *
   * @param pObject {@link SMGObject} to be checked.
   * @return true if the object is in the heap.
   */
  public boolean isHeapObject(SMGObject pObject) {
    return heapObjects.contains(pObject);
  }

  public PersistentSet<SMGObject> getHeapObjects() {
    return heapObjects;
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and any edges leading
   * from/to it.
   *
   * <p>TODO: A test case with (invalid) passing of an address of a dropped frame object outside,
   * and working with them. For that, we should probably keep those as invalid, so we can spot such
   * bugs.
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

  /**
   * Copies this {@link SymbolicProgramConfiguration} and returns a new SPC with all entered
   * unreachable SMGObjects pruned + a collection of the unreachable {@link SMGObject}s.
   *
   * @return The new SPC without the entered SMGObjects and the unreachable {@link SMGObject}s.
   */
  public SPCAndSMGObjects copyAndPruneUnreachable() {
    Collection<SMGObject> visibleObjects =
        FluentIterable.concat(
                globalVariableMapping.values(),
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
    PersistentSet<SMGObject> newHeapObjects = heapObjects;
    for (SMGObject smgObject : unreachableObjects) {
      newHeapObjects = newHeapObjects.removeAndCopy(smgObject);
    }
    return SPCAndSMGObjects.of(
        of(
            newSmg,
            globalVariableMapping,
            stackVariableMapping,
            newHeapObjects,
            externalObjectAllocation,
            valueMapping),
        unreachableObjects);
  }

  /**
   * @return {@link SMGObject} reserved for the return value of the current StackFrame.
   */
  public Optional<SMGObject> getReturnObjectForCurrentStackFrame() {
    return stackVariableMapping.peek().getReturnObject();
  }

  /**
   * @return true if there is a return object for the current stack frame.
   */
  public boolean hasReturnObjectForCurrentStackFrame() {
    return stackVariableMapping.peek().getReturnObject().isPresent();
  }

  /**
   * Copies the {@link SymbolicProgramConfiguration} and puts the mapping for the cValue to the
   * smgValue (and vice versa) into the returned copy. Note: the value is not yet added to the SMG!
   * And if there is a mapping already present for a Value or SMGValue this will fail!
   *
   * @param cValue {@link CValue} that is mapped to the entered smgValue.
   * @param smgValue {@link SMGValue} that is mapped to the entered cValue.
   * @return A copy of this SPC with the value mapping added.
   */
  public SymbolicProgramConfiguration copyAndPutValue(Value cValue, SMGValue smgValue) {
    ImmutableBiMap.Builder<Value, SMGValue> builder = ImmutableBiMap.builder();
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        builder.putAll(valueMapping).put(cValue, smgValue).buildOrThrow());
  }

  /**
   * Copies the {@link SymbolicProgramConfiguration} and sets the externaly allocated {@link
   * SMGObject} validity to false. If the entered {@link SMGObject} is not yet in the external
   * allocation map this will enter it!
   *
   * @param pObject the {@link SMGObject} that is externally allocated to be set to invalid.
   * @return A copy of this SPC with the validity of the external object changed.
   */
  public SymbolicProgramConfiguration copyAndInvalidateExternalAllocation(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.putAndCopy(pObject, false),
        valueMapping);
  }

  /**
   * Returns an {@link Optional} that may be empty if no {@link SMGValue} for the entered {@link
   * Value} exists in the value mapping. If a mapping exists, it returns the {@link SMGValue} for
   * the entered {@link Value} inside the Optional.
   *
   * @param cValue The {@link Value} you want the potential {@link SMGValue} for.
   * @return {@link Optional} that contains the {@link SMGValue} for the entered {@link Value} if it
   *     exists, empty else.
   */
  public Optional<SMGValue> getSMGValueFromValue(Value cValue) {
    // TODO: map the returned value using the SPC mapping!
    return Optional.ofNullable(valueMapping.get(cValue));
  }

  /**
   * Returns an {@link Optional} that may be empty if no {@link Value} for the entered {@link
   * SMGValue} exists in the value mapping. If a mapping exists, it returns the {@link Value} for
   * the entered {@link SMGValue} inside the Optional. Reverse method of getSMGValue();
   *
   * @param smgValue The {@link SMGValue} you want the potential {@link Value} for.
   * @return {@link Optional} that contains the {@link Value} for the entered {@link SMGValue} if it
   *     exists, empty else.
   */
  public Optional<Value> getValueFromSMGValue(SMGValue smgValue) {
    return Optional.ofNullable(valueMapping.inverse().get(smgValue));
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and creates a mapping of a {@link Value} to a
   * newly created {@link SMGValue}. This checks if there is a mapping already, and if there exists
   * a mapping the unchanged SPC will be returned.
   *
   * @param cValue The {@link Value} you want to create a new, symbolic {@link SMGValue} for and map
   *     them to each other.
   * @return The new SPC with the new {@link SMGValue} and the value mapping from the entered {@link
   *     Value} to the new {@link SMGValue}.
   */
  public SymbolicProgramConfiguration copyAndCreateValue(Value cValue) {
    if (valueMapping.containsKey(cValue)) {
      return this;
    }
    return copyAndPutValue(cValue, SMGValue.of());
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and adds the {@link SMGObject} pObject to it
   * before returning the new SPC.
   *
   * @param pObject The {@link SMGObject} you want to add to the SPC.
   * @return The new SPC with the {@link SMGObject} added.
   */
  public SymbolicProgramConfiguration copyAndAddExternalObject(SMGObject pObject) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation.putAndCopy(pObject, true),
        valueMapping);
  }

  /**
   * Copies this {@link SymbolicProgramConfiguration} and replaces the SMG with a new one. Meant for
   * read/write operations. The SMG has to be a successor of the old one.
   *
   * @param pSMG the new {@link SMG} that replaces the old one but is an successor of the old one.
   * @return The new SPC with the new {@link SMG}.
   */
  private SymbolicProgramConfiguration copyAndReplaceSMG(SMG pSMG) {
    return of(
        pSMG,
        globalVariableMapping,
        stackVariableMapping,
        heapObjects,
        externalObjectAllocation,
        valueMapping);
  }

  /**
   * Tries to search for a variable that is currently visible in the current {@link StackFrame} and
   * in the global variables and returns the variable if found. If it is not found, the {@link
   * Optional} will be empty. Note: this returns the SMGObject in which the value for the variable
   * is written. Read with the correct type!
   *
   * @param pName Name of the variable you want to search for as a {@link String}.
   * @return {@link Optional} that contains the variable if found, but is empty if not found.
   */
  public Optional<SMGObject> getObjectForVisibleVariable(String pName) {

    // Only look in the current stack frame
    StackFrame currentFrame = stackVariableMapping.peek();
    if (currentFrame.containsVariable(pName)) {
      return Optional.of(currentFrame.getVariable(pName));
    }

    // Second check global
    if (globalVariableMapping.containsKey(pName)) {
      return Optional.of(globalVariableMapping.get(pName));
    }
    // no variable found
    return Optional.empty();
  }

  /**
   * Returns <code>true</code> if the entered {@link SMGObject} is externally allocated. This does
   * not check the validity of the external object.
   *
   * @param pObject The {@link SMGObject} you want to know if its externally allocated or not.
   * @return <code>true</code> if the entered pObject is externally allocated, <code>false</code> if
   *     not.
   */
  public boolean isObjectExternallyAllocated(SMGObject pObject) {
    return externalObjectAllocation.containsKey(pObject);
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
   * The exact read method as specified by the original SMG paper! My guess is that a lot of other
   * forms of read (more exact reads) are not possible once we use join/abstration.
   *
   * @param pObject the {@link SMGObject} read.
   * @param pFieldOffset {@link BigInteger} offset.
   * @param pSizeofInBits {@link BigInteger} sizeInBits.
   * @return {@link SMGValueAndSPC} tuple for the copy of the SPC with the value read and the {@link
   *     SMGValue} read from it.
   */
  public SMGValueAndSPC readValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    SMGandValue newSMGAndValue = smg.readValue(pObject, pFieldOffset, pSizeofInBits);
    return SMGValueAndSPC.of(newSMGAndValue.getValue(), copyAndReplaceSMG(newSMGAndValue.getSMG()));
  }

  /**
   * Reads the explicit value written in a memory chunk represented as SMGRegion object at a certain
   * offset with a given size. This memory chunk can ever be a covered by exactly one
   * SMGHasValueEdge or multiple of them. If the memory chunk to be read does not fit a single edge,
   * the resulting value is computed bitwise. TODO: this most likely will fail the second we use
   * abstraction and SMGs are joined.
   *
   * @param pObject - the SMGRegion
   * @param pFieldOffset - the offset
   * @param pSizeofInBits - the size of the chunk
   * @return the explicit value written in the defined memory chunk
   */
  public Value readValuePrecise(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    if (!isObjectValid(pObject) && !isObjectExternallyAllocated(pObject)) {
      return new UnknownValue();
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
    Value returnValue = new NumericValue(0);

    // smg.getOverlappingEdges is already sorted by offset
    for (SMGHasValueEdge edge : overlappingEdges) {
      smgValue = edge.hasValue();
      // if one block is unknown explicit value computations fails
      if (!valueMapping.containsValue(smgValue)) {
        return new UnknownValue();
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
    return new UnknownValue();
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
   * @param returnValue - the Value bits which were already read with previous edges
   * @return the concatenation of returnValue with the bits of readValue which are in the memory
   *     chunk
   */
  @SuppressWarnings("unused")
  private Value bitwiseReadValue(
      BigInteger fieldOffset,
      BigInteger edgeOffset,
      BigInteger fieldSize,
      BigInteger edgeSize,
      Value readValue,
      Value returnValue) {
    // TODO handle little and big endian here

    return null;
    // TODO: decide if i want to repair this or throw it out. I doubt this works after joins
    /*
    // Edge may start "before" the chunk to be read. Cut off the bits to left of the chunk.
    int leftBitsToBeCutOff = fieldOffset.subtract(edgeOffset).intValue();
    readValue = readValue.shiftRight(leftBitsToBeCutOff);

    // Edge may end "after" the chunk to be read. Cut off the bits to right of the chunk.
    int cutOffCounter =
        fieldOffset.add(fieldSize).subtract(edgeSize.add(edgeOffset)).intValue();

    while (cutOffCounter > 0) {
      readValue = readValue.clearBit(edgeSize.intValue() - cutOffCounter--);
    }

    // concatenate the resulting bits with the already computed bit value
    // return returnCValue.concat(readValue);
    */
  }

  /**
   * Copy SPC and add a pointer to an object at a specified offset. The target needs to be a region
   * (not a LIST)! If the mapping Value <-> SMGValue does not exists it is created, else the old
   * SMGValue is used. If there was a pointer from this SMGValue to an SMGObject it is replaced with
   * the one given.
   *
   * @param address the {@link Value} representing the address to the {@link SMGObject} at the
   *     specified offset.
   * @param target the {@link SMGObject} the {@link Value} points to.
   * @param offsetInBits the offset in the {@link SMGObject} in bits as {@link BigInteger}.
   * @return a copy of the SPC with the pointer to the {@link SMGObject} and the specified offset
   *     added.
   */
  public SymbolicProgramConfiguration copyAndAddPointerFromAddressToRegion(
      Value address, SMGObject target, BigInteger offsetInBits) {
    // If there is no SMGValue for this address we create it, else we use the existing
    SymbolicProgramConfiguration spc = this.copyAndCreateValue(address);
    SMGValue smgAddress = spc.getSMGValueFromValue(address).orElseThrow();
    // Now we create a points-to-edge from this value to the target object at the
    // specified offset, overriding any existing from this value
    SMGPointsToEdge pointsToEdge =
        new SMGPointsToEdge(target, offsetInBits, SMGTargetSpecifier.IS_REGION);
    return spc.copyAndReplaceSMG(spc.getSmg().copyAndAddPTEdge(pointsToEdge, smgAddress));
  }

  /**
   * Checks if a {@link SMGPointsToEdge} exists for the entered target object and offset and returns
   * a {@link Optional} that is filled with the SMGValue leading to the points-to-edge, empty if
   * there is none. (This always assumes SMGTargetSpecifier.IS_REGION)
   *
   * @param target {@link SMGObject} that is the target of the points-to-edge.
   * @param offset {@link BigInteger} offset in bits in the target.
   * @return either an empty {@link Optional} if there is no such edge, but the {@link SMGValue}
   *     within if there is such a points-to-edge.
   */
  public Optional<SMGValue> getAddressValueForPointsToTarget(SMGObject target, BigInteger offset) {
    BiMap<SMGValue, SMGPointsToEdge> pteMapping = getSmg().getPTEdgeMapping();
    SMGPointsToEdge searchedForEdge =
        new SMGPointsToEdge(target, offset, SMGTargetSpecifier.IS_REGION);

    for (Entry<SMGValue, SMGPointsToEdge> entry : pteMapping.entrySet()) {
      if (entry.getValue().compareTo(searchedForEdge) == 0) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  /**
   * Tries to dereference the pointer given by the argument {@link Value}. Returns a empty Optional
   * if the dereference fails because the entered {@link Value} is not known as a pointer. This does
   * not check validity of the Value!
   *
   * @param pointer the {@link Value} to dereference.
   * @return Optional filled with the {@link SMGObjectAndOffset} of the target of the pointer. Empty
   *     if its not a pointer in the current {@link SymbolicProgramConfiguration}.
   */
  public Optional<SMGObjectAndOffset> dereferencePointer(Value pointer) {
    if (!isPointer(pointer)) {
      // Not known or not known as a pointer, return nothing
      return Optional.empty();
    }
    SMGPointsToEdge ptEdge = smg.getPTEdge(valueMapping.get(pointer)).orElseThrow();
    return Optional.of(SMGObjectAndOffset.of(ptEdge.pointsTo(), ptEdge.getOffset()));
  }

  /**
   * Returns true if the value entered is a pointer in the current SPC. This checks for the
   * existence of a known mapping from Value to SMGValue to a SMGPointsToEdge.
   *
   * @param maybePointer {@link Value} that you want to check.
   * @return true is the entered {@link Value} is a address that points to a memory location. False
   *     else.
   */
  public boolean isPointer(Value maybePointer) {
    if (valueMapping.containsKey(maybePointer)) {
      return smg.isPointer(valueMapping.get(maybePointer));
    }
    return false;
  }

  /**
   * Write value into the SMG at the specified offset in bits with the size given in bits. This
   * assumes that the SMGValue is already correctly mapped, but will insert it into the SMG.
   */
  public SymbolicProgramConfiguration writeValue(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits, SMGValue pValue) {
    // Adding the value should be save here, if its already added no harm is done
    SMG newSMG = smg.copyAndAddValue(pValue);
    return copyAndReplaceSMG(newSMG.writeValue(pObject, pFieldOffset, pSizeofInBits, pValue));
  }

  /**
   * This assumes that the entered {@link SMGObject} is part of the SPC!
   *
   * @param pObject the {@link SMGObject} to invalidate.
   * @return a new SPC with the entered object invalidated.
   */
  public SymbolicProgramConfiguration invalidateSMGObject(SMGObject pObject) {
    Preconditions.checkArgument(smg.getObjects().contains(pObject));
    SymbolicProgramConfiguration newSPC = this;
    if (isObjectExternallyAllocated(pObject)) {
      newSPC = copyAndInvalidateExternalAllocation(pObject);
    }
    SMG newSMG = newSPC.getSmg().copyAndInvalidateObject(pObject);
    return newSPC.copyAndReplaceSMG(newSMG);
  }
}

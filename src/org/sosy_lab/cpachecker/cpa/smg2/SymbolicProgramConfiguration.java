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
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.SMGProveNequality;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SymbolicProgramConfiguration {

  private final SMG smg;
  private final PersistentMap<String, SMGObject> globalVariableMapping;
  private final PersistentStack<StackFrame> stackVariableMapping;
  private final PersistentSet<SMGObject> heapObjects;

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<String, SMGObject> pGlobalVariableMapping,
      PersistentStack<StackFrame> pStackVariableMapping,
      PersistentSet<SMGObject> pHeapObjects) {
    globalVariableMapping = pGlobalVariableMapping;
    stackVariableMapping = pStackVariableMapping;
    smg = pSmg;
    heapObjects = pHeapObjects;
  }

  public static SymbolicProgramConfiguration
      of(
          SMG pSmg,
          PersistentMap<String, SMGObject> pGlobalVariableMapping,
          PersistentStack<StackFrame> pStackVariableMapping,
          PersistentSet<SMGObject> pHeapObjects) {
    return new SymbolicProgramConfiguration(
        pSmg,
        pGlobalVariableMapping,
        pStackVariableMapping,
        pHeapObjects);
  }

  public static SymbolicProgramConfiguration of(BigInteger sizeOfPtr) {
    return new SymbolicProgramConfiguration(
        new SMG(sizeOfPtr),
        PathCopyingPersistentTreeMap.of(),
        PersistentStack.of(),
        PersistentSet.of());
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

  public SymbolicProgramConfiguration
      copyAndAddGlobalObject(SMGObject pNewObject, String pVarName) {
    return of(
        smg.copyAndAddObject(pNewObject),
        globalVariableMapping.putAndCopy(pVarName, pNewObject),
        stackVariableMapping,
        heapObjects);
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
        heapObjects);
  }

  public SymbolicProgramConfiguration
      copyAndAddStackFrame(CFunctionDeclaration pFunctionDefinition, MachineModel model) {
    return of(
        smg,
        globalVariableMapping,
        stackVariableMapping.pushAndCopy(new StackFrame(pFunctionDefinition, model)),
        heapObjects);
  }

  public SymbolicProgramConfiguration copyAndRemoveGlobalVariable(String pIdentifier) {
    Optional<SMGObject> objToRemove =
        Optional.ofNullable(globalVariableMapping.get(pIdentifier));
    if (objToRemove.isEmpty()) {
      return this;
    }
    PersistentMap<String, SMGObject> newGlobalsMap =
        globalVariableMapping.removeAndCopy(pIdentifier);
    SMG newSmg = smg.copyAndInvalidateObject(objToRemove.orElseThrow());
    return of(newSmg, newGlobalsMap, stackVariableMapping, heapObjects);
  }

  public SymbolicProgramConfiguration copyAndRemoveStackVariable(String pIdentifier) {
    // If a stack variable becomes out of scope, there are not more than one frames which could contain the variable
    Optional<StackFrame> frameOptional =
        StreamSupport.stream(stackVariableMapping.spliterator(), false)
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
    return of(newSmg, globalVariableMapping, newStack, heapObjects);
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
        heapObjects.addAndCopy(pObject));
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and any edges leading
   * from/to it.
   *
   * TODO: A test case with (invalid) passing of an address of a dropped frame object outside, and
   * working with them. For that, we should probably keep those as invalid, so we can spot such bug.
   *
   */
  public SymbolicProgramConfiguration copyAndDropStackFrame() {
    StackFrame frame = stackVariableMapping.peek();
    PersistentStack<StackFrame> newStack = stackVariableMapping.popAndCopy();
    SMG newSmg = smg;
    for (SMGObject object : frame.getAllObjects()) {
      newSmg = smg.copyAndInvalidateObject(object);
    }
    return of(newSmg, globalVariableMapping, newStack, heapObjects);
  }

  public SymbolicProgramConfiguration
      copyAndPruneUnreachable(Collection<SMGObject> pUnreachableObjects) {
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
    return of(newSmg, globalVariableMapping, stackVariableMapping, newHeapObjects);
  }
}

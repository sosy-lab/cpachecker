/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Extending SMG with notions specific for programs in C language:
 *  - separation of global, heap and stack objects
 *  - null object and value
 */
class CLangSMG extends SMG implements WritableSMG {
  /**
   * A container for object found on the stack:
   *  - local variables
   *  - parameters
   *
   * TODO: [STACK-FRAME-STRUCTURE] Perhaps it could be wrapped in a class?
   */
  final private ArrayDeque<CLangStackFrame> stack_objects = new ArrayDeque<>();

  /**
   * A container for objects allocated on heap
   */
  final private HashSet<SMGObject> heap_objects = new HashSet<>();

  /**
   * A container for global objects
   */
  final private HashMap<String, SMGRegion> global_objects = new HashMap<>();

  /**
   * A flag signifying the edge leading to this state caused memory to be leaked
   * TODO: Seems pretty arbitrary: perhaps we should have a more general solution,
   *       like a container with (type, message) error witness kind of thing?
   */
  private boolean has_leaks = false;

  static private LogManager logger = null;

  /**
   * A flag setting if the class should perform additional consistency checks.
   * It should be useful only during debugging, when is should find bad
   * external calls closer to their origin. We probably do not want t
   * run the checks in the production build.
   */
  static private boolean perform_checks = false;

  static public void setPerformChecks(boolean pSetting, LogManager logger) {
    CLangSMG.perform_checks = pSetting;
    CLangSMG.logger = logger;
  }

  /**
   * Based on the current setting of runtime check level, it either performs
   * a full consistency check or not. If the check is performed and the
   * state is deemed inconsistent, a {@link SMGInconsistentException} is thrown.
   *
   * Constant.
   *
   * @param pLevel A level of the check request. When e.g. HALF is passed, it
   * means "perform the check if the setting is HALF or finer.
   * @throws SMGInconsistentException
   */
  final public void performConsistencyCheck() throws SMGInconsistentException {
    if (performChecks()) {
      if ( ! CLangSMGConsistencyVerifier.verifyCLangSMG(logger, this) ) {
        throw new SMGInconsistentException("SMG was found inconsistent during a check");
      }
    }
  }

  static public boolean performChecks() {
    return CLangSMG.perform_checks;
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * Newly constructed CLangSMG contains a single nullObject with an address
   * pointing to it, and is empty otherwise.
   */
  public CLangSMG(MachineModel pMachineModel) {
    super(pMachineModel);
    heap_objects.add(getNullObject());
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pHeap The original CLangSMG
   */
  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);

    for (CLangStackFrame stack_frame : pHeap.stack_objects) {
      CLangStackFrame new_frame = new CLangStackFrame(stack_frame);
      stack_objects.add(new_frame);
    }

    heap_objects.addAll(pHeap.heap_objects);
    global_objects.putAll(pHeap.global_objects);
    has_leaks = pHeap.has_leaks;
  }

  @Override
  public void removeHeapObject(final SMGObject pObj) {
    super.removeObject(pObj);
    if (isHeapObject(pObj)) {
      heap_objects.remove(pObj);
    } else {
      throw new IllegalArgumentException("Cannot directly remove non-heap objects");
    }
  }

  /**
   * Add a object to the heap.
   *
   * Keeps consistency: no
   *
   * With checks: throws {@link IllegalArgumentException} when asked to add
   * an object already present.
   *
   * @param pObject Object to add.
   */
  @Override
  public void addHeapObject(SMGObject pObject) {
    if (CLangSMG.performChecks() && heap_objects.contains(pObject)) {
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    heap_objects.add(pObject);
    addObject(pObject);
  }

  /**
   * Add a global object to the SMG
   *
   * Keeps consistency: no
   *
   * With checks: throws {@link IllegalArgumentException} when asked to add
   * an object already present, or an global object with a label identifying
   * different object

   * @param pObject Object to add
   */
  @Override
  public void addGlobalObject(SMGRegion pObject) {
    if (CLangSMG.performChecks() && global_objects.values().contains(pObject)) {
      throw new IllegalArgumentException("Global object already in the SMG: [" + pObject + "]");
    }

    if (CLangSMG.performChecks() && global_objects.containsKey(pObject.getLabel())) {
      throw new IllegalArgumentException("Global object with label [" + pObject.getLabel() + "] already in the SMG");
    }

    global_objects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  /**
   * Adds an object to the current stack frame
   *
   * Keeps consistency: no
   *
   * @param pObject Object to add
   *
   * TODO: [SCOPES] Scope visibility vs. stack frame issues: handle cases where a variable is visible
   * but is is allowed to override (inner blocks)
   * TODO: Consistency check (allow): different objects with same label inside a frame, but in different block
   * TODO: Test for this consistency check
   *
   * TODO: Shall we need an extension for putting objects to upper frames?
   */
  @Override
  public void addStackObject(SMGRegion pObject) {
    super.addObject(pObject);
    stack_objects.peek().addStackVariable(pObject.getLabel(), pObject);
  }

  /**
   * Add a new stack frame for the passed function.
   *
   * Keeps consistency: yes
   *
   * @param pFunctionDeclaration A function for which to create a new stack frame
   */
  @Override
  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) {
    CLangStackFrame newFrame = new CLangStackFrame(pFunctionDeclaration, getMachineModel());

    // Return object is NULL for void functions
    SMGObject returnObject = newFrame.getReturnObject();
    if (returnObject != null) {
      super.addObject(newFrame.getReturnObject());
    }
    stack_objects.push(newFrame);
  }

  /**
   * Sets a flag indicating this SMG is a successor over the edge causing a
   * memory leak.
   *
   * Keeps consistency: yes
   */
  @Override
  public void setMemoryLeak() {
    has_leaks = true;
  }

  /**
   * Remove a top stack frame from the SMG, along with all objects in it, and
   * any edges leading from/to it.
   *
   * TODO: A testcase with (invalid) passing of an address of a dropped frame object
   * outside, and working with them. For that, we should probably keep those as invalid, so
   * we can spot such bug.
   *
   * Keeps consistency: yes
   */
  @Override
  public void dropStackFrame() {
    CLangStackFrame frame = stack_objects.pop();
    for (SMGObject object : frame.getAllObjects()) {
      removeObjectAndEdges(object);
    }

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  /**
   * Prune the SMG: remove all unreachable objects (heap ones: global and stack
   * are always reachable) and values.
   *
   * TODO: Too large. Refactor into fewer pieces
   *
   * Keeps consistency: yes
   */
  @Override
  public void pruneUnreachable() {
    Set<SMGObject> seen = new HashSet<>();
    Set<Integer> seen_values = new HashSet<>();
    Queue<SMGObject> workqueue = new ArrayDeque<>();

    // TODO: wrap to getStackObjects(), perhaps just internally?
    for (CLangStackFrame frame : getStackFrames()) {
      for (SMGObject stack_object : frame.getAllObjects()) {
        workqueue.add(stack_object);
      }
    }

    workqueue.addAll(getGlobalObjects().values());

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    /*
     * TODO: Refactor into generic methods for obtaining reachable/unreachable
     * subSMGs
     *
     * TODO: Perhaps introduce a SubSMG class which would be a SMG tied
     * to a certain (Clang)SMG and guaranteed to be a subset of it?
     */

    while ( ! workqueue.isEmpty()) {
      SMGObject processed = workqueue.remove();
      if ( ! seen.contains(processed)) {
        seen.add(processed);
        filter.filterByObject(processed);
        for (SMGEdgeHasValue outbound : getHVEdges(filter)) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if ( pointedObject != null && ! seen.contains(pointedObject)) {
            workqueue.add(pointedObject);
          }
          if ( ! seen_values.contains(Integer.valueOf(outbound.getValue()))) {
            seen_values.add(Integer.valueOf(outbound.getValue()));
          }
        }
      }
    }

    /*
     * TODO: Refactor into generic methods for substracting SubSMGs (see above)
     */
    Set<SMGObject> stray_objects = new HashSet<>(Sets.difference(getObjects(), seen));
    for (SMGObject stray_object : stray_objects) {
      if (stray_object.notNull()) {
        if (isObjectValid(stray_object)) {
          setMemoryLeak();
        }
        removeObjectAndEdges(stray_object);
        heap_objects.remove(stray_object);

      }
    }

    Set<Integer> stray_values = new HashSet<>(Sets.difference(getValues(), seen_values));
    for (Integer stray_value : stray_values) {
      if (stray_value != getNullValue()) {
        // Here, we can't just remove stray value, we also have to remove the points-to edge
        if(isPointer(stray_value)) {
          removePointsToEdge(stray_value);
        }

        removeValue(stray_value);
      }
    }

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Getter for obtaining a string representation of the CLangSMG. Constant.
   *
   * @return String representation of the CLangSMG
   */
  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects + "\n global_objects="
        + global_objects + "\n " + valuesToString() + "\n " + ptToString() + "\n " + hvToString() + "\n]";
  }

  /**
   * Returns an SMGObject tied to the variable name. The name must be visible in
   * the current scope: it needs to be visible either in the current frame, or it
   * is a global variable. Constant.
   *
   * @param pVariableName A name of the variable
   * @return An object tied to the name, if such exists in the visible scope. Null otherwise.
   *
   * TODO: [SCOPES] Test for getting visible local object hiding other local object
   */
  @Override
  public SMGRegion getObjectForVisibleVariable(String pVariableName) {
    // Look in the local frame
    if (stack_objects.size() != 0) {
      if (stack_objects.peek().containsVariable(pVariableName)) {
        return stack_objects.peek().getVariable(pVariableName);
      }
    }

    // Look in the global scope
    if (global_objects.containsKey(pVariableName)) {
      return global_objects.get(pVariableName);
    }
    return null;
  }

  /**
   * Returns the stack of frames containing objects. Constant.
   *
   * @return Stack of frames
   */
  @Override
  public ArrayDeque<CLangStackFrame> getStackFrames() {
    return stack_objects;
  }

  /**
   * Constant.
   *
   * @return Unmodifiable view of the set of the heap objects
   */
  @Override
  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects);
  }

  /**
   * Constant.
   *
   * Checks whether given object is on the heap.
   *
   * @param object SMGObject to be checked.
   * @return True, if the given object is referenced in the set of heap objects, false otherwise.
   *
   */
  @Override
  public boolean isHeapObject(SMGObject object) {
    return heap_objects.contains(object);
  }

  /**
   * Constant.
   *
   * @return Unmodifiable map from variable names to global objects.
   */
  @Override
  public Map<String, SMGRegion> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  /**
   * Constant.
   *
   * @return True if the SMG is a successor over the edge causing some memory
   * to be leaked. Returns false otherwise.
   */
  @Override
  public boolean hasMemoryLeaks() {
    return has_leaks;
  }

  /**
   * Constant.
   *
   * @return a {@link SMGObject} for current function return value
   */
  @Override
  public SMGRegion getStackReturnObject(int pUp) {
    return stack_objects.peek().getReturnObject();
  }

  public String getFunctionName(SMGRegion pObject) {
    for (CLangStackFrame cLangStack : stack_objects) {
      if (cLangStack.getAllObjects().contains(pObject)) {
        return cLangStack.getFunctionDeclaration().getName();
      }
    }

    throw new IllegalArgumentException("No function name for non-stack object");
  }

  @Override
  public void mergeValues(int v1, int v2) throws SMGInconsistentException {
    super.mergeValues(v1, v2);

    performConsistencyCheck();
  }

  final public void removeHeapObjectAndEdges(SMGObject pObject) {
    heap_objects.remove(pObject);
    removeObjectAndEdges(pObject);
  }

  @Override
  public boolean containsValue(Integer pValue) {
    return getValues().contains(pValue);
  }

  /**
   * Determine, whether the two given symbolic values are not equal.
   * If this method does not return true, the relation of these
   * symbolic values is unknown.
   *
   * @param value1 first symbolic value to be checked
   * @param value2 second symbolic value to be checked
   * @return true, if the symbolic values are known to be not equal, false, if it is unknown.
   * @throws SMGInconsistentException
   */
  @Override
  public boolean isUnequal(int value1, int value2) throws SMGInconsistentException {

    if (isPointer(value1) && isPointer(value2)) {

      if (value1 != value2) {
        /* This is just a safety check,
        equal pointers should have equal symbolic values.*/
        SMGEdgePointsTo edge1;
        SMGEdgePointsTo edge2;
        edge1 = getPointer(value1);
        edge2 = getPointer(value2);

        return edge1.getObject() != edge2.getObject() || edge1.getOffset() != edge2.getOffset();
      }
    }
    return false;
  }

  /**
   * Get the symbolic value, that represents the address
   * pointing to the given memory with the given offset, if it exists.
   *
   * @param memory
   *          get address belonging to this memory.
   * @param offset
   *          get address with this offset relative to the beginning of the
   *          memory.
   * @return Address of the given field, or null, if such an address does not
   *         yet exist in the SMG.
   */
  @Override
  @Nullable
  public Integer getAddress(SMGObject pMemory, Integer pOffset) {

    // TODO A better way of getting those edges, maybe with a filter
    // like the Has-Value-Edges

    Set<SMGEdgePointsTo> pointsToEdges = getPTEdges();

    for (SMGEdgePointsTo edge : pointsToEdges) {
      if (edge.getObject().equals(pMemory) && edge.getOffset() == pOffset) {
        return edge.getValue();
      }
    }

    return null;
  }

  /**
   * Read Value in field (object, type) of an Object.
   *
   * This method does not modify the state being read,
   * and is therefore safe to call outside of a
   * transfer relation context.
   *
   * @param pObject SMGObject representing the memory the field belongs to.
   * @param pOffset offset of field being read.
   * @param pType type of field
   * @return A Symbolic value, if found, otherwise null.
   */
  @Override
  public Integer readValue(SMGObject pObject, int pOffset, CType pType) {
    if (! isObjectValid(pObject)) {
      return null;
    }

    SMGEdgeHasValue edge = new SMGEdgeHasValue(pType, pOffset, pObject, 0);

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject).filterAtOffset(pOffset);

    for (SMGEdgeHasValue object_edge : getHVEdges(filter)) {
      if (edge.isCompatibleFieldOnSameObject(object_edge, getMachineModel())) {
        return object_edge.getValue();
      }
    }

    if (isCoveredByNullifiedBlocks(edge)) { return 0; }

    return null;
  }

  @Override
  public SMGEdgePointsTo addNewHeapAllocation(int pSize, String pLabel) throws SMGInconsistentException {
    SMGRegion new_object = new SMGRegion(pSize, pLabel);
    int new_value = SMGValueFactory.getNewValue();
    SMGEdgePointsTo points_to = new SMGEdgePointsTo(new_value, new_object, 0);
    addHeapObject(new_object);
    addValue(new_value);
    addPointsToEdge(points_to);

    performConsistencyCheck();
    return points_to;
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   * Additionally, this method writes a points-to edge into the
   * SMG, if the given symbolic value points to an address, and
   *
   *
   * @param object SMGObject representing the memory the field belongs to.
   * @param offset offset of field written into.
   * @param type type of field written into.
   * @param value value to be written into field.
   * @param machineModel Currently used Machine Model
   * @throws SMGInconsistentException
   */
  @Override
  public SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset,
      CType pType, SMGSymbolicValue pValue) throws SMGInconsistentException {

    int value;

    // If the value is not yet known by the SMG
    // create a unconstrained new symbolic value
    if (pValue.isUnknown()) {
      value = SMGValueFactory.getNewValue();
    } else {
      value = pValue.getAsInt();
    }

    // If the value represents an address, and the address is known,
    // add the necessary points-To edge.
    if (pValue instanceof SMGAddressValue) {
      if (! containsValue(value)) {
        SMGAddress address = ((SMGAddressValue) pValue).getAddress();

        if (!address.isUnknown()) {
          if(!containsValue(value)) {
            addValue(value);
          }
          SMGEdgePointsTo pointsToEdge = new SMGEdgePointsTo(value, pObject, pOffset);
          addPointsToEdge(pointsToEdge);
        }
      }
    }

    return writeValue(pObject, pOffset, pType, value);
  }

  /**
   * This method simulates a free invocation. It checks,
   * whether the call is valid, and invalidates the
   * Memory the given address points to.
   * The address (address, offset, smgObject) is the argument
   * of the free invocation. It does not need to be part of the SMG.
   *
   * @param pAddress The symbolic Value of the address.
   * @param pOffset The offset of the address relative to the beginning of smgObject.
   * @param pRegion The memory the given Address belongs to.
   * @throws SMGInconsistentException
   */
  @Override
  public void free(Integer pAddress, Integer pOffset, SMGRegion pRegion) throws SMGInconsistentException {

    if (! isHeapObject(pRegion)) {
      // You may not free any objects not on the heap.
      //setInvalidFree();
      return;
    }

    if(!(pOffset == 0)) {
      // you may not invoke free on any address that you
      // didn't get through a malloc invocation.
      //setInvalidFree();
      return;
    }

    if (! isObjectValid(pRegion)) {
      // you may not invoke free multiple times on
      // the same object
      //setInvalidFree();
      return;
    }

    setValidity(pRegion, false);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pRegion);

    List<SMGEdgeHasValue> to_remove = new ArrayList<>();
    for (SMGEdgeHasValue edge : getHVEdges(filter)) {
      to_remove.add(edge);
    }

    for (SMGEdgeHasValue edge : to_remove) {
      removeHasValueEdge(edge);
    }

    performConsistencyCheck();
  }

  /**
   * Write a value into a field (offset, type) of an Object.
   *
   *
   * @param object SMGObject representing the memory the field belongs to.
   * @param offset offset of field written into.
   * @param type type of field written into.
   * @param value value to be written into field.
   * @param machineModel Currently used Machine Model
   * @throws SMGInconsistentException
   */
  private SMGEdgeHasValue writeValue(SMGObject pObject, int pOffset, CType pType, Integer pValue) throws SMGInconsistentException {
    // vgl Algorithm 1 Byte-Precise Verification of Low-Level List Manipulation FIT-TR-2012-04

    if (! isObjectValid(pObject)) {
      //Attempt to write to invalid object
      return null;
    }

    SMGEdgeHasValue new_edge = new SMGEdgeHasValue(pType, pOffset, pObject, pValue);

    // Check if the edge is  not present already
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pObject);

    Iterable<SMGEdgeHasValue> edges = getHVEdges(filter);
    if (Iterables.contains(edges, new_edge)) {
      performConsistencyCheck();
      return new_edge;
    }

    // If the value is not in the SMG, we need to add it
    if ( ! getValues().contains(pValue) ) {
      addValue(pValue);
    }

    HashSet<SMGEdgeHasValue> overlappingZeroEdges = new HashSet<>();
    HashSet<SMGEdgeHasValue> toRemove = new HashSet<>();

    /* We need to remove all non-zero overlapping edges
     * and remember all overlapping zero edges to shrink them later
     */
    for (SMGEdgeHasValue hv : edges) {

      boolean hvEdgeOverlaps = new_edge.overlapsWith(hv, getMachineModel());
      boolean hvEdgeIsZero = hv.getValue() == getNullValue();

      if (hvEdgeOverlaps) {
        if (hvEdgeIsZero) {
          overlappingZeroEdges.add(hv);
        } else {
          toRemove.add(hv);
        }


        //TODO This method of shrinking did not work for my benchmarks, investigate
        /*
        if (hv.getValue() == heap.getNullValue()) {
          if (hv.getOffset() < new_edge.getOffset()) {
            int prefixNullSize = new_edge.getOffset() - hv.getOffset();
            SMGEdgeHasValue prefixNull = new SMGEdgeHasValue(prefixNullSize, hv.getOffset(), pObject, heap.getNullValue());
            heap.addHasValueEdge(prefixNull);
          }

          int hvEnd = hv.getOffset() + hv.getSizeInBytes(heap.getMachineModel());
          int neEnd = new_edge.getOffset() + new_edge.getSizeInBytes(heap.getMachineModel());
          if (hvEnd > neEnd) {
            int postfixNullSize = hvEnd - neEnd;
            SMGEdgeHasValue postfixNull = new SMGEdgeHasValue(postfixNullSize, neEnd, pObject, heap.getNullValue());
            heap.addHasValueEdge(postfixNull);
          }
        }
        */
      }
    }

    for (SMGEdgeHasValue hv : toRemove) {
      removeHasValueEdge(hv);
    }
    shrinkOverlappingZeroEdges(new_edge, overlappingZeroEdges);

    addHasValueEdge(new_edge);
    performConsistencyCheck();

    return new_edge;
  }

  private void shrinkOverlappingZeroEdges(SMGEdgeHasValue pNew_edge,
      Set<SMGEdgeHasValue> pOverlappingZeroEdges) {

    SMGObject object = pNew_edge.getObject();
    int offset = pNew_edge.getOffset();

    boolean newEdgePointsToZero = pNew_edge.getValue() == 0;
    MachineModel maModel = getMachineModel();
    int sizeOfType = pNew_edge.getSizeInBytes(maModel);

    // Shrink overlapping zero edges
    for (SMGEdgeHasValue zeroEdge : pOverlappingZeroEdges) {
      // If the new_edge points to zero, we can just remove them
      removeHasValueEdge(zeroEdge);

      if (!newEdgePointsToZero) {

        int zeroEdgeOffset = zeroEdge.getOffset();

        int offset2 = offset + sizeOfType;
        int zeroEdgeOffset2 = zeroEdgeOffset + zeroEdge.getSizeInBytes(maModel);

        if (zeroEdgeOffset < offset) {
          SMGEdgeHasValue newZeroEdge = new SMGEdgeHasValue(offset - zeroEdgeOffset, zeroEdgeOffset, object, 0);
          addHasValueEdge(newZeroEdge);
        }

        if (offset2 < zeroEdgeOffset2) {
          SMGEdgeHasValue newZeroEdge = new SMGEdgeHasValue(zeroEdgeOffset2 - offset2, offset2, object, 0);
          addHasValueEdge(newZeroEdge);
        }
      }
    }
  }

  /**
   * Copys (shallow) the hv-edges of source in the given source range
   * to the target at the given target offset. Note that the source
   * range (pSourceRangeSize - pSourceRangeOffset) has to fit into
   * the target range ( size of pTarget - pTargetRangeOffset).
   * Also, pSourceRangeOffset has to be less or equal to the size
   * of the source Object.
   *
   * This method is mainly used to assign struct variables.
   *
   * @param pSource the SMGObject providing the hv-edges
   * @param pTarget the target of the copy process
   * @param pTargetRangeOffset begin the copy of source at this offset
   * @param pSourceRangeSize the size of the copy of source
   * @param pSourceRangeOffset insert the copy of source into target at this offset
   * @throws SMGInconsistentException thrown if the copying leads to an inconsistent SMG.
   */
  @Override
  public void copy(SMGObject pSource, SMGObject pTarget, int pSourceRangeOffset, int pSourceRangeSize, int pTargetRangeOffset) throws SMGInconsistentException {

    int copyRange = pSourceRangeSize - pSourceRangeOffset;

    assert pSource.getSize() >= pSourceRangeSize;
    assert pSourceRangeOffset >= 0;
    assert pTargetRangeOffset >= 0;
    assert copyRange >= 0;
    assert copyRange <= pTarget.getSize();

    // If copy range is 0, do nothing
    if(copyRange == 0) {
      return;
    }

    int targetRangeSize = pTargetRangeOffset + copyRange;

    SMGEdgeHasValueFilter filterSource = SMGEdgeHasValueFilter.objectFilter(pSource);
    SMGEdgeHasValueFilter filterTarget = SMGEdgeHasValueFilter.objectFilter(pTarget);

    //Remove all Target edges in range
    Iterable<SMGEdgeHasValue> targetEdges = getHVEdges(filterTarget);

    List<SMGEdgeHasValue> toBeErased = new ArrayList<>();

    for (SMGEdgeHasValue edge : targetEdges) {
      if (edge.overlapsWith(pTargetRangeOffset, targetRangeSize, getMachineModel())) {
        toBeErased.add(edge);
      }
    }

    for (SMGEdgeHasValue edge : toBeErased) {
      // Be wary of concurrent modification while writing values
      removeHasValueEdge(edge);
    }

    // Shift the source edge offset depending on the target range offset
    int copyShift = pTargetRangeOffset - pSourceRangeOffset;

    List<SMGEdgeHasValue> toBeWritten = new ArrayList<>();

    for (SMGEdgeHasValue edge : getHVEdges(filterSource)) {
      if (edge.overlapsWith(pSourceRangeOffset, pSourceRangeSize, getMachineModel())) {
        // Be wary of concurrent modification while writing values
        toBeWritten.add(edge);
      }
    }

    for(SMGEdgeHasValue edge : toBeWritten) {
      int offset = edge.getOffset() + copyShift;
      writeValue(pTarget, offset, edge.getType(), edge.getValue());
    }

    performConsistencyCheck();
    //TODO Why do I do this here?
    pruneUnreachable();
    performConsistencyCheck();
  }

  @Override
  public boolean isGlobalObject(SMGObject pObject) {
    if (pObject.isAbstract()) {
      return false;
    }
    return getGlobalObjects().containsValue(pObject);
  }

  /**
   * Makes SMGState create a new object and put it into the current stack
   * frame.
   *
   * Keeps consistency: yes
   *
   * @param pType Type of the new object
   * @param pVarName Name of the local variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  @Override
  public SMGObject addLocalVariable(CType pType, String pVarName) throws SMGInconsistentException {
    int size = getMachineModel().getSizeof(pType);
    SMGRegion new_object = new SMGRegion(size, pVarName);

    addStackObject(new_object);
    performConsistencyCheck();
    return new_object;
  }

  /**
   * Makes SMGState create a new object and put it into the global namespace
   *
   * Keeps consistency: yes
   *
   * @param pType Type of the new object
   * @param pVarName Name of the global variable
   * @return Newly created object
   *
   * @throws SMGInconsistentException when resulting SMGState is inconsistent
   * and the checks are enabled
   */
  @Override
  public SMGObject addGlobalVariable(CType pType, String pVarName) throws SMGInconsistentException {
    int size = getMachineModel().getSizeof(pType);
    SMGRegion new_object = new SMGRegion(size, pVarName);

    addGlobalObject(new_object);
    performConsistencyCheck();
    return new_object;
  }
}

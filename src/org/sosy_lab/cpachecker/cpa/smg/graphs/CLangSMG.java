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

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Extending SMG with notions specific for programs in C language:
 *  - separation of global, heap and stack objects
 *  - null object and value
 */
public class CLangSMG extends SMG {
  /**
   * A container for object found on the stack:
   *  - local variables
   *  - parameters
   *
   * TODO: [STACK-FRAME-STRUCTURE] Perhaps it could be wrapped in a class?
   */
  final private Deque<CLangStackFrame> stack_objects = new ArrayDeque<>();

  /**
   * A container for objects allocated on heap
   */
  final private Set<SMGObject> heap_objects = new HashSet<>();

  /**
   * A container for global objects
   */
  final private Map<String, SMGRegion> global_objects = new HashMap<>();

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
  public void addHeapObject(SMGObject pObject) {
    if (CLangSMG.performChecks() && heap_objects.contains(pObject)) {
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    heap_objects.add(pObject);
    addObject(pObject);
  }

  public Set<SMGEdgePointsTo> getPointerToObject(SMGObject obj) {
    return getPtEdges(SMGEdgePointsToFilter.targetObjectFilter(obj));
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

    // Mark all reachable from ExternallyAllocated objects as safe for remove
    workqueue.addAll(stray_objects);
    while ( ! workqueue.isEmpty()) {
      SMGObject processed = workqueue.remove();
      if (isObjectExternallyAllocated(processed)) {
        filter.filterByObject(processed);
        for (SMGEdgeHasValue outbound : getHVEdges(filter)) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if (stray_objects.contains(pointedObject) && !isObjectExternallyAllocated(pointedObject)) {
            setExternallyAllocatedFlag(pointedObject, true);
            workqueue.add(pointedObject);
          }
        }
      }
    }

    for (SMGObject stray_object : stray_objects) {
      if (stray_object.notNull()) {
        if (isObjectValid(stray_object) && !isObjectExternallyAllocated(stray_object)) {
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
        if (isPointer(stray_value)) {
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
        + global_objects + "\n " + valuesToString() + "\n " + ptToString() + "\n " + hvToString()
        + "\n" + getMapOfMemoryLocationsWithValue().toString() + "\n]";
  }

  private Map<MemoryLocation, Integer> getMapOfMemoryLocationsWithValue() {

    Set<MemoryLocation> memlocs = getMemoryLocations();
    Map<MemoryLocation, Integer> result = new HashMap<>();

    for (MemoryLocation memloc : memlocs) {
      Set<SMGEdgeHasValue> edge = getHVEdgeFromMemoryLocation(memloc);

      if (!edge.isEmpty()) {
        result.put(memloc, edge.iterator().next().getValue());
      }
    }

    return result;
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
   * Returns the (modifiable) stack of frames containing objects. Constant.
   *
   * @return Stack of frames
   */
  public Deque<CLangStackFrame> getStackFrames() {
    //TODO: [FRAMES-STACK-STRUCTURE] This still allows modification, as queues
    // do not have the appropriate unmodifiable method. There is probably some good
    // way how to provide a read-only view for iteration, but I do not know it
    return stack_objects;
  }

  /**
   * Constant.
   *
   * @return Unmodifiable view of the set of the heap objects
   */
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
  public boolean isHeapObject(SMGObject object) {
    return heap_objects.contains(object);
  }

  /**
   * Constant.
   *
   * @return Unmodifiable map from variable names to global objects.
   */
  public Map<String, SMGRegion> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  /**
   * Constant.
   *
   * @return True if the SMG is a successor over the edge causing some memory
   * to be leaked. Returns false otherwise.
   */
  public boolean hasMemoryLeaks() {
    // TODO: [MEMLEAK DETECTION] There needs to be a proper graph algorithm
    //       in the future. Right now, we can discover memory leaks only
    //       after unassigned malloc call result, so we know that immediately.
    return has_leaks;
  }

  /**
   * Constant.
   *
   * @return a {@link SMGObject} for current function return value
   */
  public SMGObject getFunctionReturnObject() {
    return stack_objects.peek().getReturnObject();
  }

  @Nullable
  public String getFunctionName(SMGObject pObject) {
    for (CLangStackFrame cLangStack : stack_objects) {
      if (cLangStack.getAllObjects().contains(pObject)) {
        return cLangStack.getFunctionDeclaration().getName();
      }
    }

    return null;
  }

  @Override
  public void mergeValues(int v1, int v2) {

    super.mergeValues(v1, v2);

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  final public void removeHeapObjectAndEdges(SMGObject pObject) {
    heap_objects.remove(pObject);
    removeObjectAndEdges(pObject);
  }

  public IDExpression createIDExpression(SMGObject pObject) {

    if (global_objects.containsValue(pObject)) {
      // TODO Breaks if label is changed
      return new IDExpression(pObject.getLabel());
    }

    for (CLangStackFrame frame : stack_objects) {
      if (frame.getVariables().containsValue(pObject)) {
        // TODO Breaks if label is changed

        return new IDExpression(pObject.getLabel(), frame.getFunctionDeclaration().getName());
      }
    }

    return null;
  }

  private Set<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(MemoryLocation pLocation) {

    SMGObject objectAtLocation = getObjectFromMemoryLocation(pLocation);

    if(objectAtLocation == null) {
      return Collections.emptySet();
    }

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(objectAtLocation);

    if (pLocation.isReference()) {
      filter.filterAtOffset((int) pLocation.getOffset());
    }

    // Remember, edges may overlap with different types
    Set<SMGEdgeHasValue> edgesToForget = getHVEdges(filter);

    return edgesToForget;
  }

  @Nullable
  private SMGObject getObjectFromMemoryLocation(MemoryLocation pLocation) {

    String locId = pLocation.getIdentifier();

    if (pLocation.isOnFunctionStack()) {

      if (!hasStackFrame(pLocation.getFunctionName())) {
        return null;
      }

      CLangStackFrame frame = getStackFrame(pLocation.getFunctionName());

      if(locId.equals("___cpa_temp_result_var_")) {
        return frame.getReturnObject();
      }

      if (!frame.hasVariable(locId)) {
        return null;
      }

      return frame.getVariable(locId);
    } else if (global_objects.containsKey(locId)) {

      return global_objects.get(locId);
    } else if (hasHeapObjectWithId(locId)) {

      return getHeapObjectWithId(locId);
    } else {
      return null;
    }
  }

  public Optional<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(SMGMemoryPath pLocation) {

    Optional<SMGObject> initialRegion = getInitialRegion(pLocation);

    if (!initialRegion.isPresent()) {
      return Optional.empty();
    }

    SMGObject object = initialRegion.get();
    List<Integer> offsets = pLocation.getPathOffset();
    SMGEdgeHasValue hve;
    Iterator<Integer> it = offsets.iterator();

    while (it.hasNext()) {

      int offset = it.next();
      Set<SMGEdgeHasValue> hves =
          getHVEdges(SMGEdgeHasValueFilter.objectFilter(object).filterAtOffset(offset));

      if (hves.isEmpty()) {
        return Optional.empty();
      }

      hve = Iterables.getOnlyElement(hves);

      int value = hve.getValue();

      if (!it.hasNext()) {
        return Optional.of(hve);
      }

      if (!isPointer(value)) {
        return Optional.empty();
      }

      SMGEdgePointsTo ptE = getPointer(value);
      object = ptE.getObject();
    }

    throw new AssertionError();
  }

  private Optional<SMGObject> getInitialRegion(SMGMemoryPath pLocation) {

    String initalVarName = pLocation.getVariableName();

    if (pLocation.startsWithGlobalVariable()) {
      if (global_objects.containsKey(initalVarName)) {
        SMGObject initialRegion = global_objects.get(initalVarName);
        return Optional.of(initialRegion);
      } else {
        return Optional.empty();
      }
    } else {

      String functionName = pLocation.getFunctionName();
      int locationOnStack = pLocation.getLocationOnStack();

      if (stack_objects.size() <= locationOnStack) {
        return Optional.empty();
      }
      CLangStackFrame frame = Iterables.get(stack_objects, locationOnStack);

      if (!frame.getFunctionDeclaration().getName()
          .equals(functionName)) {
        return Optional.empty();
      }

      if (frame.containsVariable(initalVarName)) {
        SMGObject initialObject = frame.getVariable(initalVarName);
        return Optional.of(initialObject);
      } else {
        return Optional.empty();
      }
    }
  }

  private SMGObject getHeapObjectWithId(String pLocId) {

    for (SMGObject object : heap_objects) {
      if (object.getLabel().equals(pLocId)) {
        return object;
      }
    }

    throw new AssertionError("Heap has no such object");
  }

  private boolean hasHeapObjectWithId(String pLocId) {

    for (SMGObject object : heap_objects) {
      if (object.getLabel().equals(pLocId)) {
        return true;
      }
    }

    return false;
  }

  /*
   * Returns stack frame of given function name
   */
  private boolean hasStackFrame(String pFunctionName) {

    for (CLangStackFrame frame : stack_objects) {
      String frameName = frame.getFunctionDeclaration().getName();
      if (frameName.equals(pFunctionName)) {
        return true;
      }
    }

    return false;
  }

  /*
   * Returns stack frame of given function name
   */
  private CLangStackFrame getStackFrame(String pFunctionName) {

    for (CLangStackFrame frame : stack_objects) {
      String frameName = frame.getFunctionDeclaration().getName();
      if (frameName.equals(pFunctionName)) {
        return frame;
      }
    }

    throw new AssertionError("No stack frame " + pFunctionName + " exists.");
  }

  public Set<MemoryLocation> getMemoryLocations() {

    Set<MemoryLocation> result = new HashSet<>();

    for (SMGEdgeHasValue hvedge : getHVEdges()) {
      result.add(resolveMemLoc(hvedge));
    }

    return result;
  }

  private MemoryLocation resolveMemLoc(SMGEdgeHasValue hvEdge) {

    SMGObject object = hvEdge.getObject();
    long offset = hvEdge.getOffset();

    if (global_objects.containsValue(object) || isHeapObject(object)) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {

      CLangStackFrame frame = getStackFrameOfObject(object);

      String functionName = frame.getFunctionDeclaration().getName();

      return MemoryLocation.valueOf(functionName, object.getLabel(), offset);
    }
  }

  private CLangStackFrame getStackFrameOfObject(SMGObject pObject) {

    String regionLabel = pObject.getLabel();

    for (CLangStackFrame frame : stack_objects) {
      if ((frame.containsVariable(regionLabel)
            && frame.getVariable(regionLabel) == pObject)
          || pObject == frame.getReturnObject()) {

        return frame;
      }
    }

    throw new AssertionError("object " + pObject.getLabel() + " is not on a function stack");
  }

  @Override
  public boolean equals(Object pObj) {
    /*
     * A Clang Smg is equal to a CLang smg
     * iff their super classes are equal to another.
     */

    return super.equals(pObj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public Set<SMGMemoryPath> getMemoryPaths() {

    Set<SMGMemoryPath> result = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    getMemoryPathsFromGlobalVariables(result, reached);

    getMemoryPathsFromStack(result, reached);

    return result;
  }

  private void getMemoryPathsFromStack(Set<SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : stack_objects) {
      List<String> stackVariables = new ArrayList<>(frame.getVariables().keySet());
      Collections.sort(stackVariables);
      String functionName = frame.getFunctionDeclaration().getName();

      for (String variable : stackVariables) {
        SMGObject smgObject = frame.getVariable(variable);
        getMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.STACK, null,
            functionName, pLocationOnStack, variable);
      }

      if (frame.getReturnObject() != null) {
        getMemoryPathsFromObject(frame.getReturnObject(), pResult, pReached,
            SMGObjectPosition.STACK,
            null, functionName, pLocationOnStack, frame.getReturnObject().getLabel());
      }

      pLocationOnStack = pLocationOnStack + 1;
    }
  }

  private void getMemoryPathsFromGlobalVariables(Set<SMGMemoryPath> pResult,
      Set<SMGObject> pReached) {
    List<String> globalVariables = new ArrayList<>(global_objects.keySet());
    Collections.sort(globalVariables);

    for (String variable : globalVariables) {
      SMGRegion varObject = global_objects.get(variable);
      getMemoryPathsFromObject(varObject, pResult, pReached, SMGObjectPosition.GLOBAL, null, null, null, variable);
    }
  }

  private void getMemoryPathsFromObject(SMGObject pSmgObject, Set<SMGMemoryPath> pResult,
      Set<SMGObject> pReached, SMGObjectPosition pPos, SMGMemoryPath pParent, String pFunctionName,
      Integer pLocationOnStack, String pVariableName) {

    Set<SMGEdgeHasValue> objectHves = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject));
    List<Integer> offsets = new ArrayList<>();
    Map<Integer, SMGObject> offsetToRegion = new HashMap<>();
    Map<Integer, SMGMemoryPath> offsetToParent = new HashMap<>();


    for (SMGEdgeHasValue objectHve : objectHves) {
      Integer value = objectHve.getValue();
      Integer offset = objectHve.getOffset();

      SMGMemoryPath path =
          getSMGMemoryPath(pVariableName, offset, pPos, pFunctionName, pLocationOnStack, pParent);
      pResult.add(path);

      if (isPointer(value)) {
        SMGObject rObject = getObjectPointedBy(value);

        if (isHeapObject(rObject) && !pReached.contains(rObject)) {
          pReached.add(rObject);
          offsets.add(offset);
          offsetToRegion.put(offset, rObject);
          offsetToParent.put(offset, path);
        }
      }
    }

    Collections.sort(offsets);

    for (Integer offset : offsets) {

      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath,
          null, null, null);
    }
  }

  private SMGMemoryPath getSMGMemoryPath(String pVariableName, Integer pOffset,
      SMGObjectPosition pPos, String pFunctionName, Integer pLocationOnStack,
      SMGMemoryPath pParent) {

    switch (pPos) {
      case GLOBAL:
        return SMGMemoryPath.valueOf(pVariableName, pOffset);
      case STACK:
        return SMGMemoryPath.valueOf(pVariableName, pFunctionName, pOffset, pLocationOnStack);
      case HEAP:
        return SMGMemoryPath.valueOf(pParent, pOffset);
      default:
        throw new AssertionError();
    }
  }

  private static enum SMGObjectPosition {
    STACK,
    HEAP,
    GLOBAL;
  }

  /**
   * Remove all values and every edge from the smg.
   */
  public void clearValues() {
    clearValuesHvePte();
  }

  @Override
  public void clearObjects() {
    global_objects.clear();
    heap_objects.clear();
    super.clearObjects();

    for (CLangStackFrame frame : stack_objects) {
      frame.clearStackVariables();

      if(frame.getReturnObject() != null) {
        addObject(frame.getReturnObject());
      }
    }

    /*May not remove null object.*/
    heap_objects.add(getNullObject());
  }

  public Map<SMGObject, SMGMemoryPath> getHeapObjectMemoryPaths() {

    Map<SMGObject, SMGMemoryPath> result = new HashMap<>();
    Set<SMGObject> reached = new HashSet<>();

    getHeapObjectMemoryPathsFromGlobalVariables(result, reached);

    getHeapObjectMemoryPathsFromStack(result, reached);

    return result;
  }

  private void getHeapObjectMemoryPathsFromGlobalVariables(Map<SMGObject, SMGMemoryPath> pResult,
      Set<SMGObject> pReached) {
    List<String> globalVariables = new ArrayList<>(global_objects.keySet());
    Collections.sort(globalVariables);

    for (String variable : globalVariables) {
      SMGRegion varObject = global_objects.get(variable);
      getHeapObjectMemoryPathsFromObject(varObject, pResult, pReached, SMGObjectPosition.GLOBAL, null, null, null, variable);
    }
  }

  private void getHeapObjectMemoryPathsFromStack(Map<SMGObject, SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : stack_objects) {
      List<String> stackVariables = new ArrayList<>(frame.getVariables().keySet());
      Collections.sort(stackVariables);
      String functionName = frame.getFunctionDeclaration().getName();

      for (String variable : stackVariables) {
        SMGObject smgObject = frame.getVariable(variable);
        getHeapObjectMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.STACK, null,
            functionName, pLocationOnStack, variable);
      }

      if (frame.getReturnObject() == null) {
        continue;
      }

      getHeapObjectMemoryPathsFromObject(frame.getReturnObject(), pResult, pReached, SMGObjectPosition.STACK,
          null, functionName, pLocationOnStack, frame.getReturnObject().getLabel());
      pLocationOnStack = pLocationOnStack + 1;
    }
  }

  private void getHeapObjectMemoryPathsFromObject(SMGObject pSmgObject, Map<SMGObject, SMGMemoryPath> pResult,
      Set<SMGObject> pReached, SMGObjectPosition pPos, SMGMemoryPath pParent, String pFunctionName,
      Integer pLocationOnStack, String pVariableName) {

    Set<SMGEdgeHasValue> objectHves = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject));
    List<Integer> offsets = new ArrayList<>();
    Map<Integer, SMGObject> offsetToRegion = new HashMap<>();
    Map<Integer, SMGMemoryPath> offsetToParent = new HashMap<>();


    for (SMGEdgeHasValue objectHve : objectHves) {
      Integer value = objectHve.getValue();

      if (!isPointer(value)) {
        continue;
      }

      SMGObject rObject = getObjectPointedBy(value);
      Integer offset = objectHve.getOffset();

      if (!isHeapObject(rObject) || pReached.contains(rObject)) {
        continue;
      }

      pReached.add(rObject);
      offsets.add(offset);
      offsetToRegion.put(offset, rObject);

      SMGMemoryPath path =
          getSMGMemoryPath(pVariableName, offset, pPos, pFunctionName, pLocationOnStack, pParent);

      offsetToParent.put(offset, path);
      pResult.put(rObject, path);
    }

    Collections.sort(offsets);

    for (Integer offset : offsets) {

      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getHeapObjectMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath,
          null, null, null);
    }
  }

  public void removeGlobalVariableAndEdges(String pVariable) {

    if (!global_objects.containsKey(pVariable)) {
      return;
    }

    SMGObject obj = global_objects.get(pVariable);
    global_objects.remove(pVariable);

    removeObjectAndEdges(obj);
  }

  public void removeStackVariableAndEdges(String pVariable, CLangStackFrame frame) {

    if (!frame.containsVariable(pVariable)) {
      return;
    }

    SMGObject obj = frame.getVariable(pVariable);
    frame.removeVariable(pVariable);

    removeObjectAndEdges(obj);
  }

  public Optional<SMGEdgeHasValue> forget(SMGMemoryPath pLocation) {

    Optional<SMGEdgeHasValue> edgeToForget = getHVEdgeFromMemoryLocation(pLocation);

    if (!edgeToForget.isPresent()) {
      return Optional.empty();
    }

    removeHasValueEdge(edgeToForget.get());

    return edgeToForget;
  }

  public SMGStateInformation forgetStackVariable(MemoryLocation pMemoryLocation) {

    if (pMemoryLocation.isOnFunctionStack()) {
      return forgetFunctionStackVariable(pMemoryLocation);
    } else {
      return forgetGlobalVariable(pMemoryLocation);
    }
  }

  private SMGStateInformation forgetGlobalVariable(MemoryLocation pMemoryLocation) {

    String varName = pMemoryLocation.getIdentifier();

    if (!global_objects.containsKey(varName)) {
      return SMGStateInformation.of();
    }

    SMGObject globalObject = global_objects.get(varName);

    SMGStateInformation info = createStateInfo(globalObject);

    removeGlobalVariableAndEdges(varName);
    return info;
  }

  private SMGStateInformation createStateInfo(SMGObject pObj) {

    Set<SMGEdgeHasValue> hves = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj));
    Set<SMGEdgePointsTo> ptes = getPtEdges(SMGEdgePointsToFilter.targetObjectFilter(pObj));
    Set<SMGEdgePointsTo> resultPtes = new HashSet<>();
    resultPtes.addAll(ptes);

    for (SMGEdgeHasValue edge : hves) {
      if (isPointer(edge.getValue())) {
        resultPtes.add(getPointer(edge.getValue()));
      }
    }

    return SMGStateInformation.of(hves, resultPtes, isObjectValid(pObj),
        isObjectExternallyAllocated(pObj));
  }

  private SMGStateInformation forgetFunctionStackVariable(MemoryLocation pMemoryLocation) {

    CLangStackFrame frame = getFrame(pMemoryLocation);
    String variableName = pMemoryLocation.getIdentifier();

    if (!frame.containsVariable(variableName)) {
      return SMGStateInformation.of();
    }

    SMGObject reg = frame.getVariable(variableName);

    SMGStateInformation info = createStateInfo(reg);
    removeStackVariableAndEdges(variableName, frame);

    return info;
  }

  private CLangStackFrame getFrame(final MemoryLocation pMemoryLocation) {

    Collection<CLangStackFrame> result =
        Collections2.filter(stack_objects, (CLangStackFrame frame) -> {
          return frame.getFunctionDeclaration().getName().equals(pMemoryLocation.getFunctionName());
        });
    return Iterables.getOnlyElement(result);
  }

  public void remember(MemoryLocation pMemoryLocation, SMGRegion pRegion,
      SMGStateInformation pInfo) {

    rememberRegion(pMemoryLocation, pRegion, pInfo);
    rememberEdges(pInfo);
  }

  public void rememberEdges(SMGStateInformation pForgottenInformation) {
    for(SMGEdgeHasValue edge : pForgottenInformation.getHvEdges()) {
      if(!getHVEdges().contains(edge)) {
        addHasValueEdge(edge);
      }
    }

    for (SMGEdgePointsTo pte : pForgottenInformation.getPtEdges()) {
      if (!isPointer(pte.getValue())) {
        addPointsToEdge(pte);
      }
    }
  }

  private void rememberRegion(MemoryLocation pMemoryLocation, SMGRegion pRegion,
      SMGStateInformation pInfo) {

    if (pMemoryLocation.isOnFunctionStack()) {
      CLangStackFrame frame = getFrame(pMemoryLocation);
      frame.addStackVariable(pMemoryLocation.getIdentifier(), pRegion);
    } else {
      global_objects.put(pRegion.getLabel(), pRegion);
    }

    addObject(pRegion, pInfo.isValid(), pInfo.isExternal());
  }

  public void unknownWrite() {
    clearValues();
  }
}
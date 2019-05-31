/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGStateInformation;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Extending SMG with notions specific for programs in C language: - separation of global, heap and
 * stack objects - null object and value
 */
public class CLangSMG extends SMG implements UnmodifiableCLangSMG {

  /**
   * A container for object found on the stack:
   *  - local variables
   *  - parameters
   */
  private PersistentStack<CLangStackFrame> stack_objects = PersistentStack.of();

  /**
   * A container for objects allocated on heap
   */
  private PersistentSet<SMGObject> heap_objects;

  /**
   * A container for global objects
   */
  private PersistentMap<String, SMGRegion> global_objects;

  /** logger is always NULL, except for JUnit-tests */
  private static LogManager logger = null;

  /**
   * A flag setting if the class should perform additional consistency checks.
   * It should be useful only during debugging, when is should find bad
   * external calls closer to their origin. We probably do not want t
   * run the checks in the production build.
   */
  static private boolean perform_checks = false;

  @Override
  public boolean containsInvalidElement(Object elem) {
    if (elem instanceof SMGObject) {
      SMGObject smgObject = (SMGObject) elem;
      return isHeapObject(smgObject) || isGlobal(smgObject) || isStackObject(smgObject);
    } else if (elem instanceof SMGEdgeHasValue) {
      SMGEdgeHasValue edgeHasValue = (SMGEdgeHasValue) elem;
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(edgeHasValue.getObject())
              .filterAtOffset(edgeHasValue.getOffset())
              .filterHavingValue(edgeHasValue.getValue());
      SMGHasValueEdges edges = getHVEdges(filter);
      return edges.size() != 0;
    } else if (elem instanceof SMGEdgePointsTo) {
      SMGEdgePointsTo edgePointsTo = (SMGEdgePointsTo) elem;
      SMGEdgePointsToFilter filter =
          SMGEdgePointsToFilter.targetObjectFilter(edgePointsTo.getObject())
              .filterAtTargetOffset(edgePointsTo.getOffset())
              .filterHavingValue(edgePointsTo.getValue());
      Set<SMGEdgePointsTo> edges = getPtEdges(filter);
      return !edges.isEmpty();
    } else if (elem instanceof SMGValue) {
      SMGValue smgValue = (SMGValue) elem;
      return getValues().contains(smgValue);
    }
    return false;
  }

  @Override
  public String getNoteMessageOnElement(Object elem) {
    if (elem instanceof SMGEdge) {
      return "Assign edge";
    } else if (elem instanceof Integer || elem instanceof SMGValue) {
      return "Assign value";
    } else if (elem instanceof SMGObject) {
      SMGObject smgObject = (SMGObject) elem;
      if (isFunctionParameter(smgObject)) {
        return "Function parameter";
      }
      return "Object creation";
    }
    return null;
  }

  public static void setPerformChecks(boolean pSetting, LogManager pLogger) {
    CLangSMG.perform_checks = pSetting;
    CLangSMG.logger = pLogger;
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
    global_objects = PathCopyingPersistentTreeMap.of();
    heap_objects = PersistentSet.of();
    heap_objects = heap_objects.addAndCopy(SMGNullObject.INSTANCE);
  }

  /**
   * Copy constructor.
   *
   * <p>Keeps consistency: yes
   *
   * @param pHeap The original CLangSMG
   */
  private CLangSMG(CLangSMG pHeap) {
    super(pHeap);

    stack_objects = pHeap.stack_objects;
    heap_objects = pHeap.heap_objects;
    global_objects = pHeap.global_objects;
  }

  @Override
  public CLangSMG copyOf() {
    return new CLangSMG(this);
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
    heap_objects = heap_objects.addAndCopy(pObject);
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
  public void addGlobalObject(SMGRegion pObject) {
    if (CLangSMG.performChecks() && global_objects.values().contains(pObject)) {
      throw new IllegalArgumentException("Global object already in the SMG: [" + pObject + "]");
    }

    if (CLangSMG.performChecks() && global_objects.containsKey(pObject.getLabel())) {
      throw new IllegalArgumentException("Global object with label [" + pObject.getLabel() + "] already in the SMG");
    }

    global_objects = global_objects.putAndCopy(pObject.getLabel(), pObject);
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
    CLangStackFrame top = stack_objects.peek();
    Preconditions.checkArgument(!top.hasVariable(pObject.getLabel()), "object with same label cannot be added twice");
    stack_objects = stack_objects.popAndCopy().pushAndCopy(top.addStackVariable(pObject.getLabel(), pObject));
  }

  @Override
  public boolean isStackEmpty() {
    return stack_objects.isEmpty();
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
    stack_objects = stack_objects.pushAndCopy(newFrame);
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
    CLangStackFrame frame = stack_objects.peek();
    stack_objects = stack_objects.popAndCopy();
    for (SMGObject object : frame.getAllObjects()) {
      removeObjectAndEdges(object);
    }

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  /**
   * Prune the SMG: remove all unreachable objects (heap ones: global and stack are always
   * reachable) and values.
   *
   * <p>Keeps consistency: yes
   *
   * @return all unreachable objects, e.g. all the objects that represent a memory leak.
   */
  public Set<SMGObject> pruneUnreachable() {
    Set<SMGObject> seen = new HashSet<>();
    Set<SMGValue> seen_values = new HashSet<>();
    collectReachableObjectsAndValues(seen, seen_values);

    /*
     * TODO: Refactor into generic methods for obtaining reachable/unreachable
     * subSMGs
     *
     * TODO: Perhaps introduce a SubSMG class which would be a SMG tied
     * to a certain (Clang)SMG and guaranteed to be a subset of it?
     */
    Set<SMGObject> stray_objects = new HashSet<>(Sets.difference(getObjects().asSet(), seen));

    // Mark all reachable from ExternallyAllocated objects as safe for remove
    markExternallyAllocatedObjects(stray_objects);

    Set<SMGObject> unreachableObjects = removeObjects(stray_objects);
    removeValues(Sets.difference(getValues().asSet(), seen_values));

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }

    return unreachableObjects;
  }

  /**
   * remove the given valid non-external objects from the heap.
   *
   * @return all removed valid objects, that are not externally allocated and might leak memory.
   */
  private Set<SMGObject> removeObjects(Collection<SMGObject> objects) {
    Set<SMGObject> unreachableObjects = new LinkedHashSet<>();
    for (SMGObject object : objects) {
      if (object != SMGNullObject.INSTANCE) {
        if (isObjectValid(object) && !isObjectExternallyAllocated(object)) {
          unreachableObjects.add(object);
        }
        removeHeapObjectAndEdges(object);
      }
    }
    return unreachableObjects;
  }

  /** remove the given valid values and also pointers from those values. */
  private void removeValues(Collection<SMGValue> values) {
    for (SMGValue value : values) {
      if (!value.isZero()) {
        // Here, we can't just remove stray value, we also have to remove the points-to edge
        if (isPointer(value)) {
          removePointsToEdge(value);
        }
        removeValue(value);
      }
    }
  }

  /** mark all children reachable from externally allocated objects as externally allocated. */
  private void markExternallyAllocatedObjects(Collection<SMGObject> objects) {
    Queue<SMGObject> workqueue = new ArrayDeque<>(objects);
    while (!workqueue.isEmpty()) {
      SMGObject processed = workqueue.remove();
      if (isObjectExternallyAllocated(processed)) {
        for (SMGEdgeHasValue outbound : getHVEdges(SMGEdgeHasValueFilter.objectFilter(processed))) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if (objects.contains(pointedObject) && !isObjectExternallyAllocated(pointedObject)) {
            setExternallyAllocatedFlag(pointedObject, true);
            workqueue.add(pointedObject);
          }
        }
      }
    }
  }

  private void collectReachableObjectsAndValues(
      Set<SMGObject> seenObjects, Set<SMGValue> seenValues) {

    // basis: get all direct reachable objects
    Deque<SMGObject> workqueue = new ArrayDeque<>(getGlobalObjects().values());
    for (CLangStackFrame frame : getStackFrames()) {
      workqueue.addAll(frame.getAllObjects());
    }

    // search all indirect reachable objects
    while (!workqueue.isEmpty()) {
      SMGObject obj = workqueue.pop();
      if (seenObjects.add(obj)) {
        for (SMGEdgeHasValue outbound : getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj))) {
          SMGObject pointedObject = getObjectPointedBy(outbound.getValue());
          if (pointedObject != null) {
            workqueue.add(pointedObject);
          }
          seenValues.add(outbound.getValue());
        }
      }
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
    return "CLangSMG [\n stack_objects=" + stack_objects
        + "\n heap_objects=" + heap_objects
        + "\n global_objects=" + global_objects
        + "\n values=" + getValues()
        + "\n pointsTo=" + getPTEdges()
        + "\n hasValue=" + getHVEdges()
        + "\n" + getMapOfMemoryLocationsWithValue() + "\n]";
  }

  private Map<MemoryLocation, SMGValue> getMapOfMemoryLocationsWithValue() {
    Map<MemoryLocation, SMGValue> result = new HashMap<>();

    for (SMGEdgeHasValue hvedge : getHVEdges()) {
      MemoryLocation memloc = resolveMemLoc(hvedge);
      SMGHasValueEdges edge = getHVEdgeFromMemoryLocation(memloc);

      if (edge.size() != 0) {
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
   * Returns the (unmodifiable) stack of frames containing objects. Constant.
   *
   * @return Stack of frames
   */
  @Override
  public PersistentStack<CLangStackFrame> getStackFrames() {
    return stack_objects;
  }

  private boolean isStackObject(SMGObject pObject) {

    String regionLabel = pObject.getLabel();

    for (CLangStackFrame frame : stack_objects) {
      if ((frame.containsVariable(regionLabel) && frame.getVariable(regionLabel) == pObject)
          || pObject == frame.getReturnObject()) {

        return true;
      }
    }

    return false;
  }

  private boolean isFunctionParameter(SMGObject pObject) {
    String regionLabel = pObject.getLabel();
    for (CLangStackFrame frame : stack_objects) {
      for (CParameterDeclaration parameter : frame.getFunctionDeclaration().getParameters()) {
        if (parameter.getName().equals(regionLabel) && frame.getVariable(regionLabel) == pObject) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Constant.
   *
   * @return Unmodifiable view of the set of the heap objects
   */
  @Override
  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects.asSet());
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
  public PersistentMap<String, SMGRegion> getGlobalObjects() {
    return global_objects;
  }

  /**
   * Constant.
   *
   * <p>Checks whether given object is global.
   *
   * @param object SMGObject to be checked.
   * @return True, if the given object is referenced in the set of global objects, false otherwise.
   */
  @Override
  public boolean isGlobal(SMGObject object) {
    return global_objects.containsValue(object);
  }

  /**
   * Constant.
   *
   * @return a {@link SMGObject} for current function return value
   */
  @Override
  public SMGObject getFunctionReturnObject() {
    return stack_objects.peek().getReturnObject();
  }

  @Override
  public void replaceValue(SMGValue fresh, SMGValue old) {

    super.replaceValue(fresh, old);

    if (CLangSMG.performChecks()) {
      CLangSMGConsistencyVerifier.verifyCLangSMG(CLangSMG.logger, this);
    }
  }

  final public void removeHeapObjectAndEdges(SMGObject pObject) {
    heap_objects = heap_objects.removeAndCopy(pObject);
    removeObjectAndEdges(pObject);
  }

  @Override
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

  private SMGHasValueEdges getHVEdgeFromMemoryLocation(MemoryLocation pLocation) {
    SMGObject objectAtLocation = getObjectFromMemoryLocation(pLocation);
    if (objectAtLocation == null) {
      return new SMGHasValueEdgeSet();
    }

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(objectAtLocation);
    if (pLocation.isReference()) {
      filter.filterAtOffset(pLocation.getOffset());
    }

    // Remember, edges may overlap with different types
    return getHVEdges(filter);
  }

  @Nullable
  private SMGObject getObjectFromMemoryLocation(MemoryLocation pLocation) {

    String locId = pLocation.getIdentifier();

    if (pLocation.isOnFunctionStack()) {

      CLangStackFrame frame =
          Iterables.find(
              stack_objects,
              f -> f.getFunctionDeclaration().getName().equals(pLocation.getFunctionName()),
              null);

      if (frame == null) {
        return null;
      }

      if (locId.equals(CLangStackFrame.RETVAL_LABEL)) {
        return frame.getReturnObject();
      }

      if (!frame.hasVariable(locId)) {
        return null;
      }

      return frame.getVariable(locId);
    } else if (global_objects.containsKey(locId)) {

      return global_objects.get(locId);
    } else {

      return Iterables.tryFind(heap_objects, object -> object.getLabel().equals(locId)).orNull();
    }
  }

  @Override
  public Optional<SMGEdgeHasValue> getHVEdgeFromMemoryLocation(SMGMemoryPath pLocation) {

    Optional<SMGObject> initialRegion = getInitialRegion(pLocation);
    if (!initialRegion.isPresent()) {
      return Optional.empty();
    }

    SMGObject object = initialRegion.get();
    List<Long> offsets = pLocation.getPathOffset();
    Iterator<Long> it = offsets.iterator();

    while (it.hasNext()) {
      long offset = it.next();

      SMGHasValueEdges hves =
          getHVEdges(SMGEdgeHasValueFilter.objectFilter(object).filterAtOffset(offset));
      if (hves.size() == 0) {
        return Optional.empty();
      }

      SMGEdgeHasValue hve = Iterables.getOnlyElement(hves);
      if (!it.hasNext()) {
        return Optional.of(hve);
      }

      SMGValue value = hve.getValue();
      if (!isPointer(value)) {
        return Optional.empty();
      }

      object = getPointer(value).getObject();
    }

    throw new AssertionError();
  }

  /**
   * return the object (function or global scope) for a memory-path, i.e., a variable with matching
   * the given name, function name, and stack depth.
   */
  private Optional<SMGObject> getInitialRegion(SMGMemoryPath pLocation) {
    String initalVarName = pLocation.getVariableName();
    if (pLocation.startsWithGlobalVariable()) {
      return Optional.ofNullable(global_objects.get(initalVarName));
    } else {
      String functionName = pLocation.getFunctionName();
      int locationOnStack = pLocation.getLocationOnStack();
      if (stack_objects.size() <= locationOnStack) {
        return Optional.empty();
      }
      CLangStackFrame frame = Iterables.get(stack_objects, locationOnStack);
      if (!frame.getFunctionDeclaration().getName().equals(functionName)) {
        return Optional.empty();
      }
      return Optional.ofNullable(frame.getVariable(initalVarName));
    }
  }

  private MemoryLocation resolveMemLoc(SMGEdgeHasValue hvEdge) {

    SMGObject object = hvEdge.getObject();
    long offset = hvEdge.getOffset();

    if (global_objects.containsValue(object) || isHeapObject(object)) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {

      String regionLabel = object.getLabel();
      CLangStackFrame frame =
          Iterables.find(
              stack_objects,
              f ->
                  (f.containsVariable(regionLabel) && f.getVariable(regionLabel) == object)
                      || object == f.getReturnObject());

      String functionName = frame.getFunctionDeclaration().getName();

      return MemoryLocation.valueOf(functionName, object.getLabel(), offset);
    }
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

  @Override
  public Set<SMGMemoryPath> getMemoryPaths() {

    Set<SMGMemoryPath> result = new HashSet<>();
    Set<SMGObject> reached = new HashSet<>();

    getMemoryPathsFromGlobalVariables(result, reached);
    getMemoryPathsFromStack(result, reached);

    return Collections.unmodifiableSet(result);
  }

  private void getMemoryPathsFromStack(Set<SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : stack_objects) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> entry : frame.getVariables().entrySet()) {
        getMemoryPathsFromObject(
            entry.getValue(),
            pResult,
            pReached,
            SMGObjectPosition.STACK,
            null,
            functionName,
            pLocationOnStack,
            entry.getKey());
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
    for (Entry<String, SMGRegion> entry : global_objects.entrySet()) {
      getMemoryPathsFromObject(
          entry.getValue(),
          pResult,
          pReached,
          SMGObjectPosition.GLOBAL,
          null,
          null,
          null,
          entry.getKey());
    }
  }

  private void getMemoryPathsFromObject(SMGObject pSmgObject, Set<SMGMemoryPath> pResult,
      Set<SMGObject> pReached, SMGObjectPosition pPos, SMGMemoryPath pParent, String pFunctionName,
      Integer pLocationOnStack, String pVariableName) {

    List<Long> offsets = new ArrayList<>();
    Map<Long, SMGObject> offsetToRegion = new HashMap<>();
    Map<Long, SMGMemoryPath> offsetToParent = new HashMap<>();

    for (SMGEdgeHasValue objectHve : getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject))) {
      SMGValue value = objectHve.getValue();
      long offset = objectHve.getOffset();

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

    for (long offset : offsets) {

      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath,
          null, null, null);
    }
  }

  private SMGMemoryPath getSMGMemoryPath(String pVariableName, long pOffset,
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

  private enum SMGObjectPosition {
    STACK,
    HEAP,
    GLOBAL
  }

  /**
   * Remove all values and every edge from the smg.
   */
  public void clearValues() {
    clearValuesHvePte();
  }

  @Override
  public void clearObjects() {
    global_objects = PathCopyingPersistentTreeMap.of();
    heap_objects = PersistentSet.of();
    super.clearObjects();

    // clear objects, but keep functions on the stack
    PersistentStack<CLangStackFrame> newStack = PersistentStack.of();
    for (CLangStackFrame frame : stack_objects) {
      newStack =
          newStack.pushAndCopy(
              new CLangStackFrame(frame.getFunctionDeclaration(), getMachineModel()));

      if(frame.getReturnObject() != null) {
        addObject(frame.getReturnObject());
      }
    }
    stack_objects = newStack;

    /*May not remove null object.*/
    heap_objects = heap_objects.addAndCopy(SMGNullObject.INSTANCE);
  }

  @Override
  public Map<SMGObject, SMGMemoryPath> getHeapObjectMemoryPaths() {

    Map<SMGObject, SMGMemoryPath> result = new HashMap<>();
    Set<SMGObject> reached = new HashSet<>();

    getHeapObjectMemoryPathsFromGlobalVariables(result, reached);

    getHeapObjectMemoryPathsFromStack(result, reached);

    return Collections.unmodifiableMap(result);
  }

  private void getHeapObjectMemoryPathsFromGlobalVariables(Map<SMGObject, SMGMemoryPath> pResult,
      Set<SMGObject> pReached) {
    for (Entry<String, SMGRegion> entry : global_objects.entrySet()) {
      getHeapObjectMemoryPathsFromObject(
          entry.getValue(),
          pResult,
          pReached,
          SMGObjectPosition.GLOBAL,
          null,
          null,
          null,
          entry.getKey());
    }
  }

  private void getHeapObjectMemoryPathsFromStack(Map<SMGObject, SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : stack_objects) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> entry : frame.getVariables().entrySet()) {
        getHeapObjectMemoryPathsFromObject(
            entry.getValue(),
            pResult,
            pReached,
            SMGObjectPosition.STACK,
            null,
            functionName,
            pLocationOnStack,
            entry.getKey());
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

    List<Long> offsets = new ArrayList<>();
    Map<Long, SMGObject> offsetToRegion = new HashMap<>();
    Map<Long, SMGMemoryPath> offsetToParent = new HashMap<>();


    for (SMGEdgeHasValue objectHve : getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject))) {
      SMGValue value = objectHve.getValue();

      if (!isPointer(value)) {
        continue;
      }

      SMGObject rObject = getObjectPointedBy(value);
      long offset = objectHve.getOffset();

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

    for (long offset : offsets) {

      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getHeapObjectMemoryPathsFromObject(smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath,
          null, null, null);
    }
  }

  public void removeGlobalVariableAndEdges(String pVariable) {
    SMGObject obj = global_objects.get(pVariable);
    if (obj != null) {
      global_objects = global_objects.removeAndCopy(pVariable);
      removeObjectAndEdges(obj);
    }
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
      return forgetFunctionStackVariable(pMemoryLocation, true);
    } else {
      return forgetGlobalVariable(pMemoryLocation);
    }
  }

  private SMGStateInformation forgetGlobalVariable(MemoryLocation pMemoryLocation) {
    String varName = pMemoryLocation.getIdentifier();
    if (!global_objects.containsKey(varName)) {
      return SMGStateInformation.of();
    }
    SMGStateInformation info = createStateInfo(global_objects.get(varName));
    removeGlobalVariableAndEdges(varName);
    return info;
  }

  private SMGStateInformation createStateInfo(SMGObject pObj) {

    SMGHasValueEdges hves = getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj));
    Set<SMGEdgePointsTo> ptes = getPtEdges(SMGEdgePointsToFilter.targetObjectFilter(pObj));
    Set<SMGEdgePointsTo> resultPtes = new HashSet<>(ptes);

    for (SMGEdgeHasValue edge : hves) {
      if (isPointer(edge.getValue())) {
        resultPtes.add(getPointer(edge.getValue()));
      }
    }

    return SMGStateInformation.of(hves, resultPtes, isObjectValid(pObj),
        isObjectExternallyAllocated(pObj));
  }

  /** returns information about the removed variable if 'createInfo' is set, else Null. */
  @Nullable
  public SMGStateInformation forgetFunctionStackVariable(
      MemoryLocation pMemoryLocation, boolean createInfo) {

    CLangStackFrame frame = getFrame(pMemoryLocation);
    String variableName = pMemoryLocation.getIdentifier();

    if (frame == null || !frame.containsVariable(variableName)) {
      return SMGStateInformation.of();
    }

    SMGObject reg = frame.getVariable(variableName);

    SMGStateInformation info = createInfo ? createStateInfo(reg) : null; // lazy

    stack_objects = stack_objects.replace(f -> f == frame, frame.removeVariable(variableName));

    removeObjectAndEdges(reg);

    return info;
  }

  /**
   * get the stack frame containing the given variable.
   *
   * <p>returns <code>Null</code> if variable is not available on the stack.
   */
  private @Nullable CLangStackFrame getFrame(final MemoryLocation pMemoryLocation) {
    for (CLangStackFrame frame : stack_objects) {
      if (frame.getFunctionDeclaration().getName().equals(pMemoryLocation.getFunctionName())) {
        return frame;
      }
    }
    return null;
  }

  public void remember(MemoryLocation pMemoryLocation, SMGRegion pRegion,
      SMGStateInformation pInfo) {

    rememberRegion(pMemoryLocation, pRegion, pInfo);
    rememberEdges(pInfo);
  }

  public void rememberEdges(SMGStateInformation pForgottenInformation) {
    SMGHasValueEdges forgottenHvEdges = pForgottenInformation.getHvEdges();
    SMGHasValueEdges toAddHvEdges = new SMGHasValueEdgeSet();
    for (SMGEdgeHasValue edge : forgottenHvEdges) {
      if (!getHVEdges().contains(edge)) {
        toAddHvEdges = toAddHvEdges.addEdgeAndCopy(edge);
      }
     }
    for(SMGEdgeHasValue edge : toAddHvEdges) {
      addHasValueEdge(edge);
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
      if (frame != null) {
        stack_objects = stack_objects.replace(
                f -> f == frame, frame.addStackVariable(pMemoryLocation.getIdentifier(), pRegion));
      }
    } else {
      global_objects = global_objects.putAndCopy(pRegion.getLabel(), pRegion);
    }

    addObject(pRegion, pInfo.isValid(), pInfo.isExternal());
  }

  public void unknownWrite() {
    clearValues();
  }
}
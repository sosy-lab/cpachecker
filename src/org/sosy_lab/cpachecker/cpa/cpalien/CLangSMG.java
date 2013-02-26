/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

import com.google.common.collect.Sets;

/**
 * Extending SMG with notions specific for programs in C language:
 *  - separation of global, heap and stack objects
 *  - null object and value
 *
 * TODO: [RUNTIME-CONSISTENCY-CHECKS]
 *       Implement configurable consistency checking on various places:
 *  - none
 *  - on interesting places
 *  - on every operation (=debug)
 */
public class CLangSMG extends SMG {
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
  final private HashMap<String, SMGObject> global_objects = new HashMap<>();

  /**
   * A flag signifying the edge leading to this state caused memory to be leaked
   */
  private boolean has_leaks = false;

  /**
   * Constructor
   *
   * Newly constructed CLangSMG contains a single nullObject with an address
   * pointing to it, and is empty otherwise.
   */
  public CLangSMG(MachineModel pMachineModel) {
    super(pMachineModel);
    this.heap_objects.add(this.getNullObject());
  }

  /**
   * @param pHeap original CLangSMG
   *
   * Copy constructor.
   */
  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);

    for (CLangStackFrame stack_frame : pHeap.stack_objects){
      CLangStackFrame new_frame = new CLangStackFrame(stack_frame);
      stack_objects.push(new_frame);
    }

    heap_objects.addAll(pHeap.heap_objects);
    global_objects.putAll(pHeap.global_objects);
  }

  /**
   * @param pObject
   *
   * Adds an object to the heap.
   */
  public void addHeapObject(SMGObject pObject) {
    if (this.heap_objects.contains(pObject)){
      throw new IllegalArgumentException("Heap object already in the SMG: [" + pObject + "]");
    }
    this.heap_objects.add(pObject);
    super.addObject(pObject);
  }

  /**
   * @param pObject
   *
   * Adds a global object
   */
  public void addGlobalObject(SMGObject pObject) {
    if (this.global_objects.values().contains(pObject)){
      throw new IllegalArgumentException("Global object already in the SMG: [" + pObject + "]");
    }
    if (this.global_objects.containsKey(pObject.getLabel())){
      throw new IllegalArgumentException("Global object with label [" + pObject.getLabel() + "] already in the SMG");
    }
    this.global_objects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  /**
   * @param pObject
   * @throws IllegalAccessException
   *
   * Adds an object to the current stack frame
   *
   * TODO: [SCOPES] Scope visibility vs. stack frame issues: handle cases where a variable is visible
   * but is is allowed to override (inner blocks)
   * TODO: Consistency check (allow): different objects with same label inside a frame, but in different block
   * TODO: Test for this consistency check
   *
   * TODO: Shall we need an extension for putting objects to upper frames?
   */
  public void addStackObject(SMGObject pObject) {
    super.addObject(pObject);
    stack_objects.peek().addStackVariable(pObject.getLabel(), pObject);
  }

  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects + "\n global_objects="
        + global_objects + "\n " + this.valuesToString() + "\n " + this.ptToString() + "\n " + this.hvToString() + "\n]";
  }

  /**
   * @param pVariableName
   * @return
   *
   * Returns an SMGObject tied to the variable name. The name must be visible in
   * the current scope: it needs to be visible either in the current frame, or it
   * is a global variable.
   *
   * TODO: [SCOPES] Test for getting visible local object hiding other local object
   */
  public SMGObject getObjectForVisibleVariable(CIdExpression pVariableName) {
    // Look in the local frame
    if (stack_objects.size() != 0){
      if (stack_objects.peek().containsVariable(pVariableName.getName())){
        return stack_objects.peek().getVariable(pVariableName.getName());
      }
    }
    if (global_objects.containsKey(pVariableName.getName())){
      return global_objects.get(pVariableName.getName());
    }
    return null;
  }

  /**
   * @param pFunctionDeclaration
   *
   * Add a new stack frame for the passed function
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) {
    CLangStackFrame newFrame = new CLangStackFrame(pFunctionDeclaration);
    stack_objects.push(newFrame);
  }

  /**
   * @return
   *
   * Returns the (modifiable) stack of frames containing objects.
   */
  public ArrayDeque<CLangStackFrame> getStackFrames() {
    //TODO: [FRAMES-STACK-STRUCTURE] This still allows modification, as queues
    // do not have the appropriate unmodifiable method. There is probably some good
    // way how to provide a read-only view for iteration, but I do not know it
    return stack_objects;
  }

  /**
   * @return
   *
   * Returns an unmodifiable set of objects on the heap.
   */
  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects);
  }

  /**
   * @return
   *
   * Returns an unmodifiable map from variable names to global objects.
   */
  public Map<String, SMGObject> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  /**
   * @return
   *
   * Returns true if the SMG is a successor over the edge causing some memory
   * to be leaked. Returns false otherwise.
   */
  public boolean hasMemoryLeaks() {
    // TODO: [MEMLEAK DETECTION] There needs to be a proper graph algorithm
    //       in the future. Right now, we can discover memory leaks only
    //       after unassigned malloc call result, so we know that immediately.
    return has_leaks;
  }

  /**
   * Sets a flag indicating this SMG is a successor over the edge causing a
   * memory leak.
   */
  public void setMemoryLeak() {
    has_leaks = true;
  }
}

/**
 * Represents a C language stack frame
 */
final class CLangStackFrame{

  /**
   * Function to which this stack frame belongs
   */
  private final CFunctionDeclaration stack_function;

  /**
   * A mapping from variable names to a set of SMG objects, representing
   * local variables.
   */
  final HashMap <String, SMGObject> stack_variables = new HashMap<>();

  /**
   * Constructor. Creates an empty frame.
   *
   * @param pFunctionDeclaration Function for which the frame is created
   *
   * TODO: [PARAMETERS] Create objects for function parameters
   */
  public CLangStackFrame(CFunctionDeclaration pFunctionDeclaration){
    stack_function = pFunctionDeclaration;
  }

  /**
   * Copy constructor.
   *
   * @param origFrame
   */
  public CLangStackFrame(CLangStackFrame origFrame){
    stack_function = origFrame.stack_function;
    stack_variables.putAll(origFrame.stack_variables);
  }

  /**
   * @param pName Variable name
   * @return SMG object corresponding to pName in the frame
   */
  public SMGObject getVariable(String pName) {
    SMGObject to_return = stack_variables.get(pName);

    if (to_return == null){
      throw new NoSuchElementException("No variable with name '" +
                                       pName + "' in stack frame for function '" +
                                       stack_function.toASTString() + "'");
    }

    return to_return;
  }

  /**
   * @param pName Variable name
   * @return True if variable pName is present, false otherwise
   */
  public boolean containsVariable(String pName) {
    return stack_variables.containsKey(pName);
  }

  /**
   * Adds a SMG object pObj to a stack frame, representing variable pVariableName
   * @param pLabel
   * @param pObj
   */
  public void addStackVariable(String pVariableName, SMGObject pObj) {
    if (stack_variables.containsKey(pVariableName)){
      throw new IllegalArgumentException("Stack frame for function '" +
                                       stack_function.toASTString() +
                                       "' already contains a variable '" +
                                       pVariableName + "'");
    }
    stack_variables.put(pVariableName, pObj);
  }

  /**
   * @return Declaration of a function corresponding to the frame
   */
  public CFunctionDeclaration getFunctionDeclaration() {
    return stack_function;
  }

  /**
   * @return a mapping from variables name to SMGObjects
   */
  public HashMap<String, SMGObject> getVariables() {
    HashMap<String, SMGObject> variableMap = new HashMap<>();
    variableMap.putAll(stack_variables);
    return variableMap;
  }
}

class CLangSMGConsistencyVerifier{
  private CLangSMGConsistencyVerifier() {} /* utility class */

  static private boolean verifyCLangSMGProperty(boolean result, LogManager pLogger, String message){
    pLogger.log(Level.FINEST, message, ":", result);
    return result;
  }

  static private boolean verifyDisjunctHeapAndGlobal(LogManager pLogger, CLangSMG smg){
    Map<String, SMGObject> globals = smg.getGlobalObjects();
    Set<SMGObject> heap = smg.getHeapObjects();

    boolean toReturn = Collections.disjoint(globals.values(), heap);

    if (! toReturn){
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, heap and global objects are not disjoint");
    }

    return toReturn;
  }

  static private boolean verifyDisjunctHeapAndStack(LogManager pLogger, CLangSMG smg){
    ArrayDeque<CLangStackFrame> stack_frames = smg.getStackFrames();
    Set<SMGObject> stack = new HashSet<>();

    for (CLangStackFrame frame: stack_frames){
      stack.addAll(frame.stack_variables.values());
    }
    Set<SMGObject> heap = smg.getHeapObjects();

    boolean toReturn = Collections.disjoint(stack, heap);

    if (! toReturn){
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, heap and stack objects are not disjoint: " + Sets.intersection(stack, heap));
    }

    return toReturn;
  }

  static private boolean verifyDisjunctGlobalAndStack(LogManager pLogger, CLangSMG smg){
    ArrayDeque<CLangStackFrame> stack_frames = smg.getStackFrames();
    Set<SMGObject> stack = new HashSet<>();

    for (CLangStackFrame frame: stack_frames){
      stack.addAll(frame.stack_variables.values());
    }
    Map<String, SMGObject> globals = smg.getGlobalObjects();

    boolean toReturn = Collections.disjoint(stack, globals.values());

    if (! toReturn){
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent, global and stack objects are not disjoint");
    }

    return toReturn;
  }

  static private boolean verifyStackGlobalHeapUnion(LogManager pLogger, CLangSMG smg){
    HashSet<SMGObject> object_union = new HashSet<>();

    object_union.addAll(smg.getHeapObjects());
    object_union.addAll(smg.getGlobalObjects().values());

    for (CLangStackFrame frame : smg.getStackFrames()){
      object_union.addAll(frame.getVariables().values());
    }

    boolean toReturn = object_union.containsAll(smg.getObjects()) &&
                       smg.getObjects().containsAll(object_union);

    if (! toReturn){
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent: union of stack, heap and global object is not the same set as the set of SMG objects");
    }

    return toReturn;
  }

  static private boolean verifyNullObjectCLangProperties(LogManager pLogger, CLangSMG smg){
    for (SMGObject obj: smg.getGlobalObjects().values()){
      if(! obj.notNull()){
        pLogger.log(Level.SEVERE, "CLangSMG inconsistent: null object in global object set [" + obj + "]");
        return false;
      }
    }

    SMGObject firstNull = null;
    for (SMGObject obj: smg.getHeapObjects()){
      if(! obj.notNull()){
        if (firstNull != null){
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: second null object in heap object set [first=" + firstNull + ", second=" + obj +"]" );
          return false;
        }
        else{
          firstNull = obj;
        }
      }
    }

    for (CLangStackFrame frame: smg.getStackFrames()){
      for (SMGObject obj: frame.getVariables().values()){
        if (! obj.notNull()){
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: null object in stack object set [" + obj + "]");
          return false;
        }
      }
    }

    if (firstNull == null){
      pLogger.log(Level.SEVERE, "CLangSMG inconsistent: no null object");
      return false;
    }

    return true;
  }

  static private boolean verifyGlobalNamespace(LogManager pLogger, CLangSMG smg){
    for (String label: smg.getGlobalObjects().keySet()){
      if (smg.getGlobalObjects().get(label).getLabel() != label){
        pLogger.log(Level.SEVERE,  "CLangSMG inconsistent: label [" + label + "] points to an object with label [" + smg.getGlobalObjects().get(label).getLabel() + "]");
        return false;
      }
    }

    return true;
  }

  static private boolean verifyStackNamespaces(LogManager pLogger, CLangSMG smg){
    HashSet<SMGObject> stack_objects = new HashSet<>();

    for (CLangStackFrame frame : smg.getStackFrames()){
      for (SMGObject object : frame.getVariables().values()){
        if (stack_objects.contains(object)){
          pLogger.log(Level.SEVERE, "CLangSMG inconsistent: object [" + object + "] present multiple times in the stack");
          return false;
        }
        stack_objects.add(object);
      }
    }

    return true;
  }

  static public boolean verifyCLangSMG(LogManager pLogger, CLangSMG smg){
    boolean toReturn = SMGConsistencyVerifier.verifySMG(pLogger, smg);

    pLogger.log(Level.FINEST, "Starting constistency check of a CLangSMG");

    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctHeapAndGlobal(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: heap and global object sets are disjunt");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctHeapAndStack(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: heap and stack objects are disjunct");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyDisjunctGlobalAndStack(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: global and stack objects are disjunct");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyStackGlobalHeapUnion(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: global, stack and heap object union contains all objects in SMG");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyNullObjectCLangProperties(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: null object invariants hold");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyGlobalNamespace(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: global namespace problem");
    toReturn = toReturn && verifyCLangSMGProperty(
        verifyStackNamespaces(pLogger, smg),
        pLogger,
        "Checking CLangSMG consistency: stack namespace");

    pLogger.log(Level.FINEST, "Ending consistency check of a CLangSMG");

    return toReturn;
  }
}

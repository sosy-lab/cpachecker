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

import com.google.common.collect.Sets;

/**
 * Extending SMG with notions specific for programs in C language:
 *  - separation of global, heap and stack objects
 *  - null object and value
 *
 * TODO: Implement configurable consistency checking on various places:
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
   * TODO: Perhaps it could be wrapped in a class?
   */
  final private ArrayDeque<CLangStackFrame> stack_objects = new ArrayDeque<>();

  /**
   * A container for objects allocated on heap
   */
  final private HashSet<SMGObject> heap_objects = new HashSet<>();

  /**
   * A container for global objects
   *
   * TODO: Full consistency check
   *  - all three container classes are disjunct
   *  - union of all containers is the generic object container in parent class
   */
  final private HashMap<String, SMGObject> global_objects = new HashMap<>();

  /**
   * A special object representing NULL
   */
  final private SMGObject nullObject = new SMGObject();
  /**
   * An adress of the special object representing null
   */
  final private int nullAddress = 0;

  /**
   * A flag signifying the edge leading to this state caused memory to be leaked
   */
  private boolean has_leaks = false;

  /**
   * Constructor
   *
   * Newly constructed CLangSMG contains a single nullObject with an address
   * pointing to it, and is empty otherwise.
   *
   * TODO: Test
   */
  public CLangSMG() {
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    addHeapObject(nullObject);
    addValue(nullAddress);
    addPointsToEdge(nullPointer);
  }

  /**
   * @param pHeap original CLangSMG
   *
   * Copy constructor.
   *
   * TODO: Test
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
   *
   * TODO: Test adding an object
   * TODO: Test adding an object already present
   * TODO: Test consistency after both cases above
   *
   * TODO: Consistency check: same object on heap and global
   * TODO: Test for this consistency check
   *
   * TODO: Consistency check: same object on heap and stack
   * TODO: Test for this consistency check
   */
  public void addHeapObject(SMGObject pObject) {
    this.heap_objects.add(pObject);
    super.addObject(pObject);
  }

  /**
   * @param pObject
   *
   * Adds a global object
   *
   * TODO: Test adding an object
   * TODO: Test adding an object twice
   * TODO: Test consistency after both cases above
   *
   * TODO: Consistency check: same object on global and stack
   * TODO: Test for this consistency check
   *
   * TODO: Consistency check: different objects with same label (invalid C)
   * TODO: Test for this consistency check
   */
  public void addGlobalObject(SMGObject pObject) {
    this.global_objects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  /**
   * @param pObject
   * @throws IllegalAccessException
   *
   * Adds an object to the current stack frame
   *
   * TODO: Scope visibility vs. stack frame issues: handle cases where a variable is visible
   * but is is allowed to override (inner blocks)
   *
   *
   * TODO: Shall we need an extension for putting objects to upper frames?
   * TODO: Test adding an object: successful
   * TODO: Test adding an object twice to the same stack frame: exc
   *
   * TODO: Consistency check (deny): same object in different stack frames
   * TODO: Test for this consistency check
   *
   * TODO: Consistency check (allow): different objects with same label inside a frame
   * TODO: Test for this consistency check
   *
   * TODO: Consistency check (allow): different objects with same label across frames
   * TODO: Test for this consistency check
   *
   * TODO: Consistency check (allow): different objects with same label as global object
   * TODO: Test for this consistency check
   */
  public void addStackObject(SMGObject pObject) throws IllegalAccessException {
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
   * TODO: Test for getting visible local object
   * TODO: Test for getting visible local object hiding other local object
   * TODO: Test for getting visible local object hiding global object
   * TODO: Test for getting visible local object with invisible object on stack
   * TODO: Test for getting visible global object
   * TODO: Test for getting visible global object with invisible object on stack
   */
  public SMGObject getObjectForVisibleVariable(CIdExpression pVariableName) {
    // Look in the local frame
    if (stack_objects.peek().containsVariable(pVariableName.getName())){
      return stack_objects.peek().getVariable(pVariableName.getName());
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
   *
   * TODO: Test for stack frame addition
   *  - hides current objects
   *  - does not hide global objects
   */
  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) {
    CLangStackFrame newFrame = new CLangStackFrame(pFunctionDeclaration);
    stack_objects.push(newFrame);
  }

  /**
   * @return
   *
   * Returns the (modifiable) stack of frames containing objects.
   *
   * TODO: Test
   */
  public ArrayDeque<CLangStackFrame> getStackFrames() {
    //TODO: This still allows modification, as queues do not have
    // the appropriate unmodifiable method. There is probably some good
    // way how to provide a read-only view for iteration, but I do not know it
    return stack_objects;
  }

  /**
   * @return
   *
   * Returns an unmodifiable set of objects on the heap.
   *
   * TODO: Test
   */
  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects);
  }

  /**
   * @return
   *
   * Returns an unmodifiable map from variable names to global objects
   *
   * TODO: Test
   */
  public Map<String, SMGObject> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  /**
   * @return
   *
   * Returns true if the SMG is a successor over the edge causing some memory
   * to be leaked. Returns false otherwise.
   *
   * TODO: Test
   */
  public boolean hasMemoryLeaks() {
    // TODO: There needs to be a proper graph algorithm in the future
    //       Right now, we can discover memory leaks only after unassigned
    //       malloc call result, so we know that immediately.
    return has_leaks;
  }

  /**
   * Sets a flag indicating this SMG is a successor over the edge causing a
   * memory leak.
   *
   * TODO: Test
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
   * TODO: Create objects for function parameters
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
   *
   * TODO: Test
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

  static public boolean verifyCLangSMG(LogManager pLogger, CLangSMG smg){
    boolean toReturn = true;
    pLogger.log(Level.FINEST, "Starting constistency check of a CLangSMG");
    // TODO: Verify consistency using public interface

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

    pLogger.log(Level.FINEST, "Ending consistency check of a CLangSMG");

    return toReturn;
  }
}

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


public class CLangSMG extends SMG {
  final private ArrayDeque<CLangStackFrame> stack_objects = new ArrayDeque<>();
  final private HashSet<SMGObject> heap_objects = new HashSet<>();
  final private HashMap<String, SMGObject> global_objects = new HashMap<>();

  final private SMGObject nullObject = new SMGObject();
  final private int nullAddress = 0;
  private boolean has_leaks = false;

  public CLangSMG() {
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    addHeapObject(nullObject);
    addValue(nullAddress);
    addPointsToEdge(nullPointer);
  }

  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);

    for (CLangStackFrame stack_frame : pHeap.stack_objects){
      CLangStackFrame new_frame = new CLangStackFrame(stack_frame);
      stack_objects.push(new_frame);
    }

    heap_objects.addAll(pHeap.heap_objects);
    global_objects.putAll(pHeap.global_objects);
  }

  public void addHeapObject(SMGObject pObject) {
    this.heap_objects.add(pObject);
    super.addObject(pObject);
  }

  private void addGlobalObject(SMGObject pObject) {
    this.global_objects.put(pObject.getLabel(), pObject);
    super.addObject(pObject);
  }

  public void addStackObject(SMGObject pObject) throws IllegalAccessException {
    super.addObject(pObject);
    stack_objects.peek().addStackVariable(pObject.getLabel(), pObject);
  }

  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects + "\n global_objects="
        + global_objects + "\n " + this.valuesToString() + "\n " + this.ptToString() + "\n " + this.hvToString() + "\n]";
  }

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

  public void addStackFrame(CFunctionDeclaration pFunctionDeclaration) {
    CLangStackFrame newFrame = new CLangStackFrame(pFunctionDeclaration);
    stack_objects.push(newFrame);
  }

  public ArrayDeque<CLangStackFrame> getStackFrames() {
    //TODO: This still allows modification, as queues do not have
    // the appropriate unmodifiable method. There is probably some good
    // way how to provide a read-only view for iteration, but I do not know it
    return stack_objects;
  }

  public Set<SMGObject> getHeapObjects() {
    return Collections.unmodifiableSet(heap_objects);
  }

  public Map<String, SMGObject> getGlobalObjects() {
    return Collections.unmodifiableMap(global_objects);
  }

  public boolean hasMemoryLeaks() {
    // TODO: There needs to be a proper graph algorithm in the future
    //       Right now, we can discover memory leaks only after unassigned
    //       malloc call result, so we know that immediately.
    return has_leaks;
  }

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

  static public boolean verifyCLangSMG(LogManager pLogger, CLangSMG smg){
    boolean toReturn = true;
    pLogger.log(Level.FINEST, "Starting constistency check of a CLangSMG");
    // TODO: Verify consistency using public interface
    // toReturn = toReturn && verifyCLangSMGProperty(smg.isObjectConsistent(), pLogger, "Checking object consistency of CLangSMG");
    pLogger.log(Level.FINEST, "Ending consistency check of a CLangSMG");

    return toReturn;
  }
}
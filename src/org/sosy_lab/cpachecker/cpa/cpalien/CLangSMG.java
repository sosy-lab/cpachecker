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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;


public class CLangSMG extends SMG {
  final Stack<CLangStackFrame> stack_objects = new Stack<CLangStackFrame>();
  final HashSet<SMGObject> heap_objects = new HashSet<SMGObject>();
  final HashMap<String, SMGObject> global_objects = new HashMap<String, SMGObject>();

  private SMGObject nullObject;
  private Integer nullAddress;

  public CLangSMG() {
    super();

    nullObject = new SMGObject();
    nullAddress = new Integer(0);
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    addHeapObject(nullObject);
    addValue(nullAddress);
    addPointsToEdge(nullPointer);
  }

  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);
    nullAddress = pHeap.nullAddress;
    nullObject = pHeap.nullObject;

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

  public void addStackObject(SMGObject pObj) {
    super.addObject(pObj);
    stack_objects.peek().addStackVariable(pObj.getLabel(), pObj);
  }

  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects + "\n global_objects="
        + global_objects + "\n values=" + values + "\n pointsTo=" + pt_edges + "\n hasValue=" + hv_edges + "\n]";
  }

  public SMGObject getObjectForVariable(CIdExpression pVariableName) {
    // Look in the local state
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
}

final class CLangStackFrame{
  private final CFunctionDeclaration stackFunction;
  final HashMap <String, SMGObject> stack_variables = new HashMap<String, SMGObject>();

  public CLangStackFrame(CFunctionDeclaration pFunctionDeclaration){
    stackFunction = pFunctionDeclaration;
  }

  public SMGObject getVariable(String pName) {
    return stack_variables.get(pName);
  }

  public boolean containsVariable(String pName) {
    return stack_variables.containsKey(pName);
  }

  public void addStackVariable(String pLabel, SMGObject pObj) {
    stack_variables.put(pLabel, pObj);
  }

  public CLangStackFrame(CLangStackFrame origFrame){
    stackFunction = origFrame.stackFunction;
    stack_variables.putAll(origFrame.stack_variables);
  }

  public String getFunctionSignature() {
    return stackFunction.toASTString();
  }

  public HashMap<String, SMGObject> getVariables() {
    HashMap<String, SMGObject> variableMap = new HashMap<String, SMGObject>();
    variableMap.putAll(stack_variables);
    return variableMap;
  }

  public String getFunctionName() {
    return stackFunction.getName();
  }
}
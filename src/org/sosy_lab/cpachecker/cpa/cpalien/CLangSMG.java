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

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;


public class CLangSMG extends SMG {
  private Stack<HashMap<String, SMGObject>> stack_objects;
  private HashSet<SMGObject> heap_objects;
  private HashMap<String, SMGObject> global_objects;

  private SMGObject nullObject;
  private Integer nullAddress;

  public CLangSMG() {
    super();
    stack_objects = new Stack<HashMap<String, SMGObject>>();
    heap_objects = new HashSet<SMGObject>();
    global_objects = new HashMap<String, SMGObject>();
  }

  public CLangSMG(CLangSMG pHeap) {
    super(pHeap);
    nullAddress = pHeap.nullAddress;
    nullObject = pHeap.nullObject;

    stack_objects = new Stack<HashMap<String, SMGObject>>();
    heap_objects = new HashSet<SMGObject>();
    global_objects = new HashMap<String, SMGObject>();

    for (HashMap<String, SMGObject> stack_item : pHeap.stack_objects){
      HashMap<String, SMGObject> si = new HashMap<String, SMGObject>();
      si.putAll(stack_item);
      stack_objects.push(si);
    }
    heap_objects.addAll(pHeap.heap_objects);
    global_objects.putAll(pHeap.global_objects);
  }

  public void createInitialFrame(){
    stack_objects.add(new HashMap<String, SMGObject>());
  }

  public void createNullObjects(){
    nullObject = new SMGObject(0, "NULL");
    nullAddress = new Integer(0);
    SMGEdgePointsTo nullPointer = new SMGEdgePointsTo(nullAddress, nullObject, 0);

    addHeapObject(nullObject);
    addValue(nullAddress);
    addPointsToEdge(nullPointer);
  }

  public void addHeapObject(SMGObject pObject) {
    this.heap_objects.add(pObject);
    super.addObject(pObject);
  }

  public void addStackObject(SMGObject pObj) {
    super.addObject(pObj);
    stack_objects.peek().put(pObj.getLabel(), pObj);
  }

  @Override
  public String toString() {
    return "CLangSMG [\n stack_objects=" + stack_objects + "\n heap_objects=" + heap_objects + "\n global_objects="
        + global_objects + "\n values=" + values + "\n pointsTo=" + pt_edges + "\n hasValue=" + hv_edges + "\n]";
  }

  public SMGObject getObjectForVariable(CIdExpression pVariableName) {
    // Look in the local state
    if (stack_objects.peek().containsKey(pVariableName.getName())){
      return stack_objects.peek().get(pVariableName.getName());
    }
    if (global_objects.containsKey(pVariableName.getName())){
      return global_objects.get(pVariableName.getName());
    }
    return null;
  }

  public void addHasValueEdge(SMGEdgeHasValue pNewEdge) {
    hv_edges.add(pNewEdge);
  }
}
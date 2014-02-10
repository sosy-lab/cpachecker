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
package org.sosy_lab.cpachecker.cpa.smg.SMGJoin;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smg.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

final public class SMGJoin {
  static public void performChecks(boolean pOn) {
    SMGJoinSubSMGs.performChecks(pOn);
  }

  private boolean defined = false;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;
  private final CLangSMG smg;

  public SMGJoin(CLangSMG pSMG1, CLangSMG pSMG2) throws SMGInconsistentException {
    CLangSMG opSMG1 = new CLangSMG(pSMG1);
    CLangSMG opSMG2 = new CLangSMG(pSMG2);
    smg = new CLangSMG(opSMG1.getMachineModel());

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    Map<String, SMGRegion> globals_in_smg1 = opSMG1.getGlobalObjects();
    ArrayDeque<CLangStackFrame> stack_in_smg1 = opSMG1.getStackFrames();
    Map<String, SMGRegion> globals_in_smg2 = opSMG2.getGlobalObjects();
    ArrayDeque<CLangStackFrame> stack_in_smg2 = opSMG2.getStackFrames();

    Set<String> globalVars = new HashSet<>();
    globalVars.addAll(globals_in_smg1.keySet());
    globalVars.addAll(globals_in_smg2.keySet());

    for (String globalVar : globalVars) {
      SMGRegion globalInSMG1 = globals_in_smg1.get(globalVar);
      SMGRegion globalInSMG2 = globals_in_smg2.get(globalVar);
      if (globalInSMG1 == null || globalInSMG2 == null) {
        // This weird situation happens with function static variables, which are created
        // as globals when a declaration is met. So if one path goes through function and other
        // does not, then one SMG will have that global and the other one won't.
        // TODO: We could actually just add that object, as that should not influence the result of
        // the join. For now, we will treat this situation as unjoinable.
        return;
      }
      SMGRegion finalObject = new SMGRegion(globalInSMG1);
      smg.addGlobalObject(finalObject);
      mapping1.map(globalInSMG1, finalObject);
      mapping2.map(globalInSMG2, finalObject);
    }

    Iterator<CLangStackFrame> smg1stackIterator = stack_in_smg1.descendingIterator();
    Iterator<CLangStackFrame> smg2stackIterator = stack_in_smg2.descendingIterator();

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext() ){
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      smg.addStackFrame(frameInSMG1.getFunctionDeclaration());

      Set<String> localVars = new HashSet<>();
      localVars.addAll(frameInSMG1.getVariables().keySet());
      localVars.addAll(frameInSMG2.getVariables().keySet());

      for (String localVar : localVars) {
        if ((!frameInSMG1.containsVariable(localVar)) || (!frameInSMG2.containsVariable(localVar))) {
          return;
        }
        SMGRegion localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGRegion localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGRegion finalObject = new SMGRegion(localInSMG1);
        smg.addStackObject(finalObject);
        mapping1.map(localInSMG1, finalObject);
        mapping2.map(localInSMG2, finalObject);
      }
    }

    for (String globalVar : globals_in_smg1.keySet()) {
      SMGObject globalInSMG1 = globals_in_smg1.get(globalVar);
      SMGObject globalInSMG2 = globals_in_smg2.get(globalVar);
      SMGObject destinationGlobal = mapping1.get(globalInSMG1);
      SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, globalInSMG1, globalInSMG2, destinationGlobal);
      if (! jss.isDefined()) {
        return;
      }
      status = jss.getStatus();
    }

    smg1stackIterator = stack_in_smg1.iterator();
    smg2stackIterator = stack_in_smg2.iterator();

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext() ){
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();

      for (String localVar : frameInSMG1.getVariables().keySet()) {
        SMGObject localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGObject localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGObject destinationLocal = mapping1.get(localInSMG1);
        SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, localInSMG1, localInSMG2, destinationLocal);
        if (! jss.isDefined()) {
          return;
        }
        status = jss.getStatus();
      }
    }



    defined = true;
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public CLangSMG getJointSMG() {
    return smg;
  }
}

class SMGNodeMapping {
  final private Map<SMGObject, SMGObject> object_map = new HashMap<>();
  final private Map<Integer, Integer> value_map = new HashMap<>();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((object_map == null) ? 0 : object_map.hashCode());
    result = prime * result + ((value_map == null) ? 0 : value_map.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGNodeMapping other = (SMGNodeMapping) obj;
    if (object_map == null) {
      if (other.object_map != null) {
        return false;
      }
    } else if (!object_map.equals(other.object_map)) {
      return false;
    }
    if (value_map == null) {
      if (other.value_map != null) {
        return false;
      }
    } else if (!value_map.equals(other.value_map)) {
      return false;
    }
    return true;
  }

  public SMGNodeMapping() {}

  public SMGNodeMapping(SMGNodeMapping origin) {
    object_map.putAll(origin.object_map);
    value_map.putAll(origin.value_map);
  }

  public Integer get(Integer i) {
    return value_map.get(i);
  }

  public SMGObject get (SMGObject o) {
    return object_map.get(o);
  }

  public void map(SMGObject key, SMGObject value){
    object_map.put(key, value);
  }

  public void map(Integer key, Integer value) {
    value_map.put(key, value);
  }

  public boolean containsKey(Integer key) {
    return value_map.containsKey(key);
  }

  public boolean containsKey(SMGObject key) {
    return object_map.containsKey(key);
  }

  public boolean containsValue(SMGObject value) {
    return object_map.containsValue(value);
  }
}

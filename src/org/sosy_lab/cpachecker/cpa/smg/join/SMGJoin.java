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
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final public class SMGJoin {
  static public void performChecks(boolean pOn) {
    SMGJoinSubSMGs.performChecks(pOn);
  }

  private boolean defined = false;
  private SMGJoinStatus status = SMGJoinStatus.EQUAL;
  private final CLangSMG smg;
  SMGLevelMapping levelMap = SMGLevelMapping.createDefaultLevelMap();

  public SMGJoin(CLangSMG pSMG1, CLangSMG pSMG2, SMGState pStateOfSmg1, SMGState pStateOfSmg2) throws SMGInconsistentException {
    CLangSMG opSMG1 = new CLangSMG(pSMG1);
    CLangSMG opSMG2 = new CLangSMG(pSMG2);
    smg = new CLangSMG(opSMG1.getMachineModel());

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    Map<String, SMGRegion> globals_in_smg1 = opSMG1.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg1 = opSMG1.getStackFrames();
    Map<String, SMGRegion> globals_in_smg2 = opSMG2.getGlobalObjects();
    Deque<CLangStackFrame> stack_in_smg2 = opSMG2.getStackFrames();

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
      SMGRegion finalObject = globalInSMG1;
      smg.addGlobalObject(finalObject);
      mapping1.map(globalInSMG1, finalObject);
      mapping2.map(globalInSMG2, finalObject);
    }

    Iterator<CLangStackFrame> smg1stackIterator = stack_in_smg1.descendingIterator();
    Iterator<CLangStackFrame> smg2stackIterator = stack_in_smg2.descendingIterator();

    //TODO assert stack smg1 == stack smg2

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext() ) {
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
        SMGRegion finalObject = localInSMG1;
        smg.addStackObject(finalObject);
        mapping1.map(localInSMG1, finalObject);
        mapping2.map(localInSMG2, finalObject);
      }
    }

    for (Entry<String, SMGRegion> entry : globals_in_smg1.entrySet()) {
      SMGObject globalInSMG1 = entry.getValue();
      SMGObject globalInSMG2 = globals_in_smg2.get(entry.getKey());
      SMGObject destinationGlobal = mapping1.get(globalInSMG1);
      SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, levelMap, globalInSMG1, globalInSMG2, destinationGlobal, 0,false, pStateOfSmg1, pStateOfSmg2);
      if (! jss.isDefined()) {
        return;
      }
      status = jss.getStatus();
    }

    smg1stackIterator = stack_in_smg1.iterator();
    smg2stackIterator = stack_in_smg2.iterator();
    Deque<CLangStackFrame> stack_in_destSMG = smg.getStackFrames();
    Iterator<CLangStackFrame> destSmgStackIterator = stack_in_destSMG.iterator();

    while ( smg1stackIterator.hasNext() && smg2stackIterator.hasNext()) {
      CLangStackFrame frameInSMG1 = smg1stackIterator.next();
      CLangStackFrame frameInSMG2 = smg2stackIterator.next();
      CLangStackFrame destStackFrame = destSmgStackIterator.next();

      for (String localVar : frameInSMG1.getVariables().keySet()) {
        SMGObject localInSMG1 = frameInSMG1.getVariable(localVar);
        SMGObject localInSMG2 = frameInSMG2.getVariable(localVar);
        SMGObject destinationLocal = mapping1.get(localInSMG1);
        SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, levelMap, localInSMG1, localInSMG2, destinationLocal, 0, false, pStateOfSmg1, pStateOfSmg2);
        if (! jss.isDefined()) {
          return;
        }
        status = jss.getStatus();
      }

      /* Don't forget to join the return object */

      if (frameInSMG1.getReturnObject() != null) {
        SMGObject returnObjectInSmg1 = frameInSMG1.getReturnObject();
        SMGObject returnObjectInSmg2 = frameInSMG2.getReturnObject();
        SMGObject destinationLocal = destStackFrame.getReturnObject();
        mapping1.map(returnObjectInSmg1, destinationLocal);
        mapping2.map(returnObjectInSmg2, destinationLocal);
        SMGJoinSubSMGs jss =
            new SMGJoinSubSMGs(status, opSMG1, opSMG2, smg, mapping1, mapping2, levelMap, returnObjectInSmg1,
                returnObjectInSmg2, destinationLocal, 0, false, pStateOfSmg1, pStateOfSmg2);
        if (!jss.isDefined()) {
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

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class CBMCMergeNode {

  private int elementId;
  private int numberOfProcessed;
  private Map<Integer, Boolean> branchesMap;
  private Set<Integer> processedConditions;

  public CBMCMergeNode(int pElementId) {
    elementId = pElementId;
    numberOfProcessed = 0;
    branchesMap = new HashMap<Integer, Boolean>();
    processedConditions = new HashSet<Integer>();
  }

  public Set<Integer> getProcessedConditions(){
    return processedConditions;
  }

  public int addBranch(CBMCEdge pNextCBMCEdge) {

    numberOfProcessed++;

    Stack<CBMCStackElement> addedStackElement = pNextCBMCEdge.getStack().peek();

    for(CBMCStackElement elementInStack: addedStackElement){
      int idOfElementInStack = elementInStack.getElementId();
      boolean nextConditionValue = elementInStack.isCondition();

      // if we already have a value for the same initial node of the condition
      if(branchesMap.containsKey(idOfElementInStack)){
        boolean firstConditionValue = branchesMap.get(idOfElementInStack);
        // if this is the end of the branch
        if(firstConditionValue ^ nextConditionValue){
          processedConditions.add(idOfElementInStack);
        }
        // else do nothing
      }
      // create the first entry in the map
      else{
        branchesMap.put(idOfElementInStack, nextConditionValue);
      }
    }
    return numberOfProcessed;
  }

  @Override
  public String toString() {
    return "id: " + elementId + " >> " + branchesMap;
  }

}

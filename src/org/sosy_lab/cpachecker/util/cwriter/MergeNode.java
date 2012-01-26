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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;

class MergeNode {

  private final int elementId;
  private final Map<Integer, Pair<Boolean, Boolean>> branchesMap;
  private final List<FunctionBody> incomingElements;

  public MergeNode(int pElementId) {
    elementId = pElementId;
    branchesMap = new HashMap<Integer,  Pair<Boolean, Boolean>>();
    incomingElements = new ArrayList<FunctionBody>();
  }

  public int addBranch(FunctionBody currentFunction) {
    incomingElements.add(currentFunction);
    Set<Integer> processedConditions = new HashSet<Integer>();

    for (BasicBlock elementInStack: currentFunction) {
      int idOfElementInStack = elementInStack.getElementId();
      boolean nextConditionValue = elementInStack.isCondition();
      boolean isClosedBefore = elementInStack.isClosedBefore();

      // if we already have a value for the same initial node of the condition
      if (branchesMap.containsKey(idOfElementInStack)) {
        // if it was closed earlier somewhere else
        Pair<Boolean, Boolean> conditionPair = branchesMap.get(idOfElementInStack);
        boolean firstConditionValue = conditionPair.getFirst();
        boolean secondConditionValue = conditionPair.getSecond();
        // if this is the end of the branch
        if (isClosedBefore || secondConditionValue ||
            (firstConditionValue ^ nextConditionValue)) {
//          elementInStack.setClosedBefore(true);
          processedConditions.add(idOfElementInStack);
        }
        // else do nothing
      } else {
        // create the first entry in the map
        branchesMap.put(idOfElementInStack, Pair.of(nextConditionValue, isClosedBefore));
      }
    }

    setProcessedElements(processedConditions);

    return incomingElements.size();
  }

  private void setProcessedElements(Set<Integer> pProcessedConditions) {
    for (FunctionBody stack: incomingElements) {
      for (BasicBlock elem: stack) {
        if (pProcessedConditions.contains(elem.getElementId())) {
          elem.setClosedBefore(true);
        }
      }
    }
  }

  public List<FunctionBody> getIncomingElements() {
    return incomingElements;
  }

  @Override
  public String toString() {
    return "id: " + elementId + " >> " + branchesMap;
  }
}
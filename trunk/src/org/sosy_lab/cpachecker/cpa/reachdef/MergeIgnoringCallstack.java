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
package org.sosy_lab.cpachecker.cpa.reachdef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class MergeIgnoringCallstack implements MergeOperator{

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException {
    if (pState1 instanceof ReachingDefState && pState2 instanceof ReachingDefState) {
      ReachingDefState e1 = (ReachingDefState) pState1;
      ReachingDefState e2 = (ReachingDefState) pState2;
      Map<String, Set<DefinitionPoint>> local = unionMaps(e1.getLocalReachingDefinitions(),
          e2.getLocalReachingDefinitions());
      Map<String, Set<DefinitionPoint>> global = unionMaps(e1.getGlobalReachingDefinitions(),
          e2.getGlobalReachingDefinitions());
      if (local != e2.getLocalReachingDefinitions() || global != e2.getGlobalReachingDefinitions()) {
        return new ReachingDefState(local, global, null);
      }
    }
    return pState2;
  }


  private Map<String, Set<DefinitionPoint>> unionMaps(Map<String, Set<DefinitionPoint>> map1,
      Map<String, Set<DefinitionPoint>> map2) {
    Map<String, Set<DefinitionPoint>> newMap = new HashMap<>();
    HashSet<String> vars = new HashSet<>();
    vars.addAll(map1.keySet());
    vars.addAll(map2.keySet());

    HashSet<DefinitionPoint> unionResult;
    boolean changed = false;
    if (map1 == map2) {
      return map2;
    }
    for (String var : vars) {
      // decrease merge time, avoid building union if unnecessary
      if (map1.get(var)== map2.get(var)) {
        newMap.put(var, map2.get(var));
        continue;
      }

      if (map1.get(var)==null) {
        newMap.put(var, map2.get(var));
      } else if(map2.get(var)==null) {
        newMap.put(var, map1.get(var));
        changed = true;
      } else {
        unionResult = new HashSet<>();
        unionResult.addAll(map1.get(var));
        unionResult.addAll(map2.get(var));
        if (unionResult.size() != map2.get(var).size()) {
          changed = true;
        }
        newMap.put(var, unionResult);
      }
    }
    if (changed) { return newMap; }
    return map2;
  }


}

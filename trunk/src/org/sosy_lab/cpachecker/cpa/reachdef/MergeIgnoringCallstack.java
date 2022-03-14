// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MergeIgnoringCallstack implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException {
    if (pState1 instanceof ReachingDefState && pState2 instanceof ReachingDefState) {
      ReachingDefState e1 = (ReachingDefState) pState1;
      ReachingDefState e2 = (ReachingDefState) pState2;
      Map<MemoryLocation, Set<DefinitionPoint>> local =
          unionMaps(e1.getLocalReachingDefinitions(), e2.getLocalReachingDefinitions());
      Map<MemoryLocation, Set<DefinitionPoint>> global =
          unionMaps(e1.getGlobalReachingDefinitions(), e2.getGlobalReachingDefinitions());
      if (local != e2.getLocalReachingDefinitions()
          || global != e2.getGlobalReachingDefinitions()) {
        return new ReachingDefState(local, global);
      }
    }
    return pState2;
  }

  private Map<MemoryLocation, Set<DefinitionPoint>> unionMaps(
      Map<MemoryLocation, Set<DefinitionPoint>> map1,
      Map<MemoryLocation, Set<DefinitionPoint>> map2) {
    Map<MemoryLocation, Set<DefinitionPoint>> newMap = new HashMap<>();
    Set<MemoryLocation> vars = new HashSet<>(map1.keySet());

    vars.addAll(map2.keySet());

    Set<DefinitionPoint> unionResult;
    boolean changed = false;
    if (map1 == map2) {
      return map2;
    }
    for (MemoryLocation var : vars) {
      // decrease merge time, avoid building union if unnecessary
      if (map1.get(var) == map2.get(var)) {
        newMap.put(var, map2.get(var));
        continue;
      }

      if (map1.get(var) == null) {
        newMap.put(var, map2.get(var));
      } else if (map2.get(var) == null) {
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
    if (changed) {
      return newMap;
    }
    return map2;
  }
}

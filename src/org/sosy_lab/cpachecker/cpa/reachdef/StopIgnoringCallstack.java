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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.Sets;


public class StopIgnoringCallstack implements StopOperator{

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException {
    try {
      ReachingDefState e1 = (ReachingDefState) pState;
      ReachingDefState e2;
      for (AbstractState p : pReached) {
        e2 = (ReachingDefState) p;
        if (isSubsetOf(e1.getLocalReachingDefinitions(), e2.getLocalReachingDefinitions())
            && isSubsetOf(e1.getGlobalReachingDefinitions(), e2.getGlobalReachingDefinitions())) {
          return true;
        }
      }
    } catch (ClassCastException e) {
    }
    return false;
  }

  private boolean isSubsetOf(Map<String, Set<DefinitionPoint>> subset, Map<String, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset) {
      return true;
    }
    for (Entry<String, Set<DefinitionPoint>> entry : subset.entrySet()) {
      setSub = entry.getValue();
      setSuper = superset.get(entry.getKey());
      if (setSub == setSuper) {
        continue;
      }
      if (setSuper == null || Sets.intersection(setSub, setSuper).size()!=setSub.size()) {
        return false;
      }
    }
    return true;
  }

}

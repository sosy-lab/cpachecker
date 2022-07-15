// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.reachdef;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class StopIgnoringCallstack implements StopOperator {

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision) {
    ReachingDefState e1 = (ReachingDefState) pState;
    ReachingDefState e2;
    for (AbstractState p : pReached) {
      e2 = (ReachingDefState) p;
      if (isSubsetOf(e1.getLocalReachingDefinitions(), e2.getLocalReachingDefinitions())
          && isSubsetOf(e1.getGlobalReachingDefinitions(), e2.getGlobalReachingDefinitions())) {
        return true;
      }
    }
    return false;
  }

  private boolean isSubsetOf(
      Map<MemoryLocation, Set<DefinitionPoint>> subset,
      Map<MemoryLocation, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset) {
      return true;
    }
    for (Entry<MemoryLocation, Set<DefinitionPoint>> entry : subset.entrySet()) {
      setSub = entry.getValue();
      setSuper = superset.get(entry.getKey());
      if (setSub == setSuper) {
        continue;
      }
      if (setSuper == null || Sets.intersection(setSub, setSuper).size() != setSub.size()) {
        return false;
      }
    }
    return true;
  }
}

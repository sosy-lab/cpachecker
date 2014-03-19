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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

@Options(prefix="cpa.octagon.domain")
class OctDomain implements AbstractDomain {

  private static long totaltime = 0;
  private LogManager logger;

  public OctDomain(LogManager log, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    logger = log;
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {

    Map<OctState, Set<OctState>> covers = new HashMap<>();

    long start = System.currentTimeMillis();
    OctState octState1 = (OctState) element1;
    OctState octState2 = (OctState) element2;

    if (covers.containsKey(octState2) && ((HashSet<OctState>)(covers.get(octState2))).contains(octState1)) {
      return true;
    }

    int result = octState1.isLessOrEquals(octState2);
    if (result == 1) {
      totaltime = totaltime + (System.currentTimeMillis() - start);
      return true;
    } else if (result == 2) {
      totaltime = totaltime + (System.currentTimeMillis() - start);
      return false;
    } else {
      assert (result == 3);
      boolean included = OctagonManager.isIncludedIn(octState1.getOctagon(), octState2.getOctagon());
      if (included) {
        Set<OctState> s;
        if (covers.containsKey(octState2)) {
          s = covers.get(octState2);
        } else {
          s = new HashSet<>();
        }
        s.add(octState1);
        covers.put(octState2, s);
      }
      totaltime = totaltime + (System.currentTimeMillis() - start);
      return included;
    }
  }

  @Override
  public AbstractState join(AbstractState successor, AbstractState reached) {
    Pair<OctState, OctState> shrinkedStates = getShrinkedStates((OctState)successor, (OctState)reached);
    Octagon newOctagon = OctagonManager.union(shrinkedStates.getFirst().getOctagon(), shrinkedStates.getSecond().getOctagon());

    OctState newState = new OctState(newOctagon, shrinkedStates.getFirst().getVariableToIndexMap(), ((OctState)successor).getBlock(), logger);
    if (newState.equals(reached)) {
      return reached;
    } else if (newState.equals(successor)) {
      return successor;
    } else {
      return newState;
    }
  }

  public AbstractState joinWidening(OctState successorOct, OctState reachedOct) {
    Pair<OctState, OctState> shrinkedStates = getShrinkedStates(successorOct, reachedOct);
    successorOct = shrinkedStates.getFirst();
    reachedOct = shrinkedStates.getSecond();

    Octagon newOctagon = OctagonManager.widening(reachedOct.getOctagon(), successorOct.getOctagon());

    OctState newState = new OctState(newOctagon, successorOct.getVariableToIndexMap(), successorOct.getBlock(), logger);
    if (newState.equals(successorOct)) {
      return successorOct;
    } else if (newState.equals(reachedOct)) {
      return reachedOct;
    } else {
      return newState;
    }
  }

  private Pair<OctState, OctState> getShrinkedStates(OctState succ, OctState reached) {
    if (succ.sizeOfVariables() > reached.sizeOfVariables()) {
      Pair<OctState, OctState> tmp = succ.shrinkToFittingSize(reached);
      succ = tmp.getFirst();
      reached = tmp.getSecond();
    } else {
      Pair<OctState, OctState> tmp = reached.shrinkToFittingSize(succ);
      succ = tmp.getSecond();
      reached = tmp.getFirst();
    }
    return Pair.of(succ, reached);
  }
}

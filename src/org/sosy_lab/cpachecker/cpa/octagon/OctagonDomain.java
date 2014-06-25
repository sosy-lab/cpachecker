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
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.Octagon;

class OctagonDomain implements AbstractDomain {

  private static long totaltime = 0;
  private final LogManager logger;

  public OctagonDomain(LogManager log) throws InvalidConfigurationException {
    logger = log;
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {

    Map<OctagonState, Set<OctagonState>> covers = new HashMap<>();

    long start = System.currentTimeMillis();
    OctagonState octState1 = (OctagonState) element1;
    OctagonState octState2 = (OctagonState) element2;

    if (covers.containsKey(octState2) && ((HashSet<OctagonState>)(covers.get(octState2))).contains(octState1)) {
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
      boolean included = octState1.getOctagon().getManager().isIncludedIn(octState1.getOctagon(), octState2.getOctagon());
      if (included) {
        Set<OctagonState> s;
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
    Pair<OctagonState, OctagonState> shrinkedStates = getShrinkedStates((OctagonState)successor, (OctagonState)reached);
    Octagon newOctagon = shrinkedStates.getFirst().getOctagon().getManager()
                           .union(shrinkedStates.getFirst().getOctagon(), shrinkedStates.getSecond().getOctagon());

    //TODO this should not be necessary however it occurs that a widened state is bottom
    if (shrinkedStates.getFirst().getOctagon().getManager().isEmpty(newOctagon)) {
      throw new AssertionError("bottom state occured where it should not be");
    }

    OctagonState newState = new OctagonState(newOctagon,
                                     shrinkedStates.getFirst().getVariableToIndexMap(),
                                     shrinkedStates.getFirst().getVariableToTypeMap(),
                                     ((OctagonState)successor).getBlock(),
                                     logger);
    if (newState.equals(reached)) {
      return reached;
    } else if (newState.equals(successor)) {
      return successor;
    } else {
      return newState;
    }
  }

  public AbstractState joinWidening(OctagonState successorOct, OctagonState reachedOct) {
    Pair<OctagonState, OctagonState> shrinkedStates = getShrinkedStates(successorOct, reachedOct);
    successorOct = shrinkedStates.getFirst();
    reachedOct = shrinkedStates.getSecond();

    Octagon newOctagon = reachedOct.getOctagon().getManager()
                            .widening(reachedOct.getOctagon(), successorOct.getOctagon());

    //TODO this should not be necessary however it occurs that a widened state is bottom
    if (reachedOct.getOctagon().getManager().isEmpty(newOctagon)) {
      newOctagon = reachedOct.getOctagon().getManager()
                        .union(reachedOct.getOctagon(), successorOct.getOctagon());
      logger.log(Level.WARNING, "bottom state occured where it should not be, using union instead of widening as a fallback");
      if (reachedOct.getOctagon().getManager().isEmpty(newOctagon)) {
         throw new AssertionError("bottom state occured where it should not be");
      }
    }

    OctagonState newState = new OctagonState(newOctagon,
                                     successorOct.getVariableToIndexMap(),
                                     successorOct.getVariableToTypeMap(),
                                     successorOct.getBlock(),
                                     logger);
    if (newState.equals(successorOct)) {
      return successorOct;
    } else if (newState.equals(reachedOct)) {
      return reachedOct;
    } else {
      return newState;
    }
  }

  private Pair<OctagonState, OctagonState> getShrinkedStates(OctagonState succ, OctagonState reached) {
    if (succ.sizeOfVariables() > reached.sizeOfVariables()) {
      Pair<OctagonState, OctagonState> tmp = succ.shrinkToFittingSize(reached);
      succ = tmp.getFirst();
      reached = tmp.getSecond();
    } else {
      Pair<OctagonState, OctagonState> tmp = reached.shrinkToFittingSize(succ);
      succ = tmp.getSecond();
      reached = tmp.getFirst();
    }
    return Pair.of(succ, reached);
  }
}

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
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.collect.BiMap;

class OctDomain implements AbstractDomain {

  private static long totaltime = 0;
  private LogManager logger;


  public OctDomain(LogManager log) {
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
  public AbstractState join(AbstractState element1, AbstractState element2) {
    OctState octEl1 = (OctState) element1;
    OctState octEl2 = (OctState) element2;
    Octagon newOctagon = OctagonManager.union(octEl1.getOctagon(), octEl2.getOctagon());
    BiMap<String, Integer> newMap =
      octEl1.sizeOfVariables() > octEl2.sizeOfVariables()? octEl1.getVariableToIndexMap() : octEl2.getVariableToIndexMap();
    Map<String, IOctCoefficients> newCoeffMap =
        octEl1.sizeOfVariables() > octEl2.sizeOfVariables()? octEl1.getVariableToCoeffMap() : octEl2.getVariableToCoeffMap();

      // TODO should it be null
      return new OctState(newOctagon, newMap, newCoeffMap, logger);
      // TODO add widening
      //    return LibraryAccess.widening(octEl1, octEl2);
  }
}

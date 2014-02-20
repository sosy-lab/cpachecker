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
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

@Options(prefix="cpa.octagon.domain")
class OctDomain implements AbstractDomain {

  private static long totaltime = 0;
  private LogManager logger;

  @Option(name="joinType", toUppercase=true, values={"NORMAL", "WIDENING"},
      description="of which type should the merge be? normal, for usual join, widening for"
                + " a widening instead of a join")
  private String joinType = "NORMAL";

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
    OctState octEl1 = (OctState) successor;
    OctState octEl2 = (OctState) reached;

    if (octEl1.sizeOfVariables() > octEl2.sizeOfVariables()) {
      Pair<OctState, OctState> tmp = octEl1.shrinkToFittingSize(octEl2);
      octEl1 = tmp.getFirst();
      octEl2 = tmp.getSecond();
    } else {
      Pair<OctState, OctState> tmp = octEl2.shrinkToFittingSize(octEl1);
      octEl1 = tmp.getSecond();
      octEl2 = tmp.getFirst();
    }
    if (joinType.equals("NORMAL")) {
      return joinNormal(octEl1, octEl2, successor, reached);
    } else if (joinType.equals("WIDENING")) {
      return joinWidening(octEl1, octEl2);

      // default should be normal
    } else {
      return joinNormal(octEl1, octEl2, successor, reached);
    }
  }

  private AbstractState joinNormal(OctState successorOct, OctState reachedOct, AbstractState successorAbs, AbstractState reachedAbs) {
    assert (successorOct.sizeOfVariables() == reachedOct.sizeOfVariables());

    Octagon newOctagon = OctagonManager.union(successorOct.getOctagon(), reachedOct.getOctagon());

    OctState newState = new OctState(newOctagon, successorOct.getVariableToIndexMap(), logger);
    if (newState.equals(reachedAbs)) {
      return reachedAbs;
    } else if (newState.equals(successorAbs)) {
      return successorAbs;
    } else {
      return newState;
    }
  }

  private AbstractState joinWidening(OctState successorOct, OctState reachedOct) {
    assert (successorOct.sizeOfVariables() == reachedOct.sizeOfVariables());

    Octagon newOctagon = OctagonManager.widening(reachedOct.getOctagon(), successorOct.getOctagon());

    OctState newState = new OctState(newOctagon, successorOct.getVariableToIndexMap(), logger);
    if (newState.equals(successorOct)) {
      return successorOct;
    } else if (newState.equals(reachedOct)) {
      return reachedOct;
    } else {
      return newState;
    }
  }
}

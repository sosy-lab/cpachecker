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
package org.sosy_lab.cpachecker.cpa.pointer2;

import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public enum PointerDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    PointerState state1 = (PointerState) pState1;
    PointerState state2 = (PointerState) pState2;
    PointerState result = state2;
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : state1.getPointsToMap().entrySet()) {
      result = result.addPointsToInformation(pointsToEntry.getKey(), pointsToEntry.getValue());
    }
    if (result.equals(state2)) {
      return state2;
    }
    if (result.equals(state1)) {
      return state1;
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) throws CPAException, InterruptedException {
    if (pState1 == pState2) {
      return true;
    }
    PointerState state1 = (PointerState) pState1;
    PointerState state2 = (PointerState) pState2;
    for (Entry<MemoryLocation, LocationSet> pointsToEntry : state1.getPointsToMap().entrySet()) {
      LocationSet rightSide = state2.getPointsToSet(pointsToEntry.getKey());
      if (!rightSide.containsAll(pointsToEntry.getValue())) {
        return false;
      }
    }
    return true;
  }

}

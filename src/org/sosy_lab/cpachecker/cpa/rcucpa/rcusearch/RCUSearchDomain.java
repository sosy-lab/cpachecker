/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerDomain;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class RCUSearchDomain implements AbstractDomain {
  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    RCUSearchState searchState1 = (RCUSearchState) state1;
    RCUSearchState searchState2 = (RCUSearchState) state2;
    RCUSearchState result;

    Set<MemoryLocation> pointers = new HashSet<>(searchState1.getRcuPointers());
    pointers.addAll(searchState2.getRcuPointers());
    PointerState pointerState1 = (PointerState) searchState1.getWrappedStates().iterator().next();
    PointerState pointerState2 = (PointerState) searchState2.getWrappedStates().iterator().next();

    PointerDomain pDomain = PointerDomain.INSTANCE;
    result = new RCUSearchState(pointers,
                                (PointerState) pDomain.join(pointerState1, pointerState2));

    if (result.equals(searchState1)) {
      return searchState1;
    } else if (result.equals(searchState2)) {
      return searchState2;
    } else {
      return result;
    }
  }

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException, InterruptedException {
    if (state1 == state2) {
      return true;
    }

    RCUSearchState searchState1 = (RCUSearchState) state1;
    RCUSearchState searchState2 = (RCUSearchState) state2;

    if (!searchState2.getRcuPointers().containsAll(searchState1.getRcuPointers())) {
      return false;
    }

    return true;
  }
}

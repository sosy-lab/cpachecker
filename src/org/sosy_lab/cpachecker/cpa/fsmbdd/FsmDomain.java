/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Definition of the abstract domain of the FsmBdd-CPA.
 */
public class FsmDomain implements AbstractDomain {

  /**
   * The JOIN operator (of the semi-lattice) of the abstract domain.
   * The BDDs of the states get disjunct (OR).
   */
  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    FsmState state1 = (FsmState) pState1;
    FsmState state2 = (FsmState) pState2;

    FsmState joined = state1.cloneState();
    joined.setStateBdd(joined.getStateBdd().or(state2.getStateBdd()));

    if (joined.getStateBdd().equals(state2.getStateBdd())) {
      return state2;
    } else {
      return joined;
    }
  }

  /**
   * The partial order (of the semi-lattice)  of the the abstract domain.
   * This is done by checking the implication (==>) of the BDDs of the given states.
   */
  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) throws CPAException {
    FsmState state1 = (FsmState) pState1;
    FsmState state2 = (FsmState) pState2;

    return state1.getStateBdd().imp(state2.getStateBdd()).isOne();
  }

}

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
 *
 * See "Program Analysis with Dynamic Precision Adjustment" [Beyer et.al. 2008]
 * for details on configurable program analysis.
 */
public class FsmDomain implements AbstractDomain {

  /**
   * The JOIN operator (of the semi-lattice) of the abstract domain.
   * It must yield the least upper bound of two abstract states;
   * it must be precise or overapproximate.
   * The BDDs of the states get disjunct (OR).
   *
   * @param pState1   Newly constructed state that should be merged.
   * @param pState2   One of the states that was reached in an earlier iteration
   *                  of the CPA algorithm.
   *
   */
  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    FsmState state1 = (FsmState) pState1;
    FsmState state2 = (FsmState) pState2;

    // Create the joined state by
    // constructing a disjunction (OR) of the BDDs of the given states.
    FsmState joined = state1.cloneState();
    joined.disjunctWithState(state2);

    // Check whether the BDDs of the
    if (joined.getStateBdd().equals(state2.getStateBdd())) {
      // Return the existing (second) state if the new (first) state makes
      // no additional states reachable.
      return state2;
    } else {
      // Return the joined state if it (caused by conjunction with the new, first, state)
      // makes states reachable that were not reachable before.
      return joined;
    }

    /* Example:
     *     State 1: FALSE
     *     State 2: a=1 AND b=2
     *      Joined: FALSE OR (a=1 AND b=2)
     *          ==  a=1 AND b=2
     *  --> Result: State 2
     *          ==  a=1 AND b=2
     *          ( state 1 does not make additional states reachable)
     */
  }

  /**
   * The partial order (of the semi-lattice) of the the abstract domain.
   * We check whether pState1 <= pState2 or not.
   * This is done by checking the implication (==>) of the BDDs of the given states.
   *
   * Examples:
   *  State1: FALSE
   *  State2: a=2 OR b=3
   *  Result: FALSE ==> a=2 OR b=3
   *      ==  TRUE
   *
   * @param pState1   First component of the partial-order relation.
   * @param pState2   Second component of the partial-order relation.
   */
  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) throws CPAException {
    FsmState state1 = (FsmState) pState1;
    FsmState state2 = (FsmState) pState2;

    if (state1.getUnencodedAssumptions() != null
      || state2.getUnencodedAssumptions() != null) {
      return false;
    } else {
      return state1.getStateBdd().imp(state2.getStateBdd()).isOne();
    }
  }

}

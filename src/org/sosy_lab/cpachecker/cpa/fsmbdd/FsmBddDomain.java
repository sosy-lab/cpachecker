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

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Definition of the abstract domain of the FsmBdd-CPA.
 *
 * See "Program Analysis with Dynamic Precision Adjustment" [Beyer et.al. 2008]
 * for details on configurable program analysis.
 */
public class FsmBddDomain implements AbstractDomain {

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
    throw new NotImplementedException();

//    FsmBddState state1 = (FsmBddState) pState1;
//    FsmBddState state2 = (FsmBddState) pState2;
//
//    // Create the joined state by
//    // constructing a disjunction (OR) of the BDDs of the given states.
//    FsmBddState joined = state1.cloneState(state1.getCfaNode());
//    joined.disjunctWithState(state2);
//
//    if (joined.getStateBdd().equals(state2.getStateBdd())) {
//      // Return the existing (second) state if the new (first) state makes
//      // no additional states reachable.
//      return state2;
//    } else {
//      // Return the joined state if it (caused by conjunction with the new, first, state)
//      // makes states reachable that were not reachable before.
//      return joined;
//    }


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
    FsmBddState s1 = (FsmBddState) pState1;
    FsmBddState s2 = (FsmBddState) pState2;

    boolean bddImplies = s1.getStateBdd().imp(s2.getStateBdd()).isOne();
    boolean conditionsLessOrEqual = s1.conditionsLessOrEqual(s2);

    if (!bddImplies) {
      return false;
    } else if (conditionsLessOrEqual) {
      return true;
    } else if (s1.getMergedInto() == s2) {
      return true;
    }

//    if (s1.getConditionBlock() instanceof CBinaryExpression
//    && s2.getConditionBlock() != null) {
//      CBinaryExpression s1cond = (CBinaryExpression) s1.getConditionBlock();
//      if (s1cond.getOperator() == BinaryOperator.LOGICAL_AND) {
//        return s1cond.getOperand1() == s2.getConditionBlock()
//            || s1cond.getOperand2() == s2.getConditionBlock();
//      }
//    }
//
//    if (s2.getConditionBlock() instanceof CBinaryExpression
//    && s1.getConditionBlock() != null) {
//      CBinaryExpression s2cond = (CBinaryExpression) s2.getConditionBlock();
//      if (s2cond.getOperator() == BinaryOperator.LOGICAL_OR) {
//        return s2cond.getOperand1() == s1.getConditionBlock()
//            || s2cond.getOperand2() == s1.getConditionBlock();
//      }
//    }

    return false;
  }

}

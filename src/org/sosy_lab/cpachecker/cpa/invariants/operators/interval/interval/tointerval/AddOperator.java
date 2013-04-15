/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.IntervalHelper;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ISIOperator;

/**
 * The instance of this class is an operator for adding two simple
 * intervals together to produce another simple interval.
 */
enum AddOperator implements IIIOperator {

  INSTANCE;

  /**
   * Computes the interval of possible results from adding any value of
   * the first operand interval to any value of the second operand interval <code>pInterval</code>.
   *
   * @param pOperand1 the values of the first operand.
   * @param pOperand2 the values to add to any of the values of the first operand interval.
   * @return the interval of possible results from adding any value of
   * the first operand interval to any value of the second operand interval <code>pInterval</code>.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pOperand1, SimpleInterval pOperand2) {
    // Avoid creating a new object by checking easy special cases
    if (pOperand1.isTop()) {
      return pOperand1;
    }
    if (pOperand2.isTop()) {
      return pOperand2;
    }
    if (pOperand2.isSingleton()) {
      return ISIOperator.ADD_OPERATOR.apply(pOperand1, pOperand2.getLowerBound());
    }
    if (pOperand1.isSingleton()) {
      return ISIOperator.ADD_OPERATOR.apply(pOperand2, pOperand1.getLowerBound());
    }
    /*
     * Add up the lower bounds to the new lower bound, add up the upper
     * bounds for the new upper bound. If any of the summands is not
     * finite, the resulting bound isn't finite either.
     */
    BigInteger lowerBound = IntervalHelper.getLowerBoundOrNull(pOperand1);
    BigInteger upperBound = IntervalHelper.getUpperBoundOrNull(pOperand1);
    BigInteger pLowerBound = IntervalHelper.getLowerBoundOrNull(pOperand2);
    BigInteger pUpperBound = IntervalHelper.getUpperBoundOrNull(pOperand2);
    if (lowerBound != null) {
      if (pLowerBound == null) {
        lowerBound = null;
      } else {
        lowerBound = lowerBound.add(pLowerBound);
      }
    }
    if (upperBound != null) {
      if (pUpperBound == null) {
        upperBound = null;
      } else {
        upperBound = upperBound.add(pUpperBound);
      }
    }
    return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
  }

}

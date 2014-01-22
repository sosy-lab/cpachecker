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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.IntervalHelper;

/**
 * The instance of this class is an operator for adding a big integer to a
 * simple interval to produce another simple interval.
 */
enum AddOperator implements ISIOperator {

  INSTANCE;

  /**
   * Computes the interval of possible results from adding any value of
   * this interval to the given value <code>pValue</code>.
   *
   * @param pFirstOperand the simple interval to add the big integer value to.
   * @param pValue the value to add to any of the values of the first operand interval.
   * @return the interval of possible results from adding any value of
   * the first operand interval to the second operand big integer value.
   */
  @Override
  public
  SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
    // Avoid creating a new object by checking easy special cases
    if (pFirstOperand.isTop() || pSecondOperand.equals(BigInteger.ZERO)) {
      return pFirstOperand;
    }
    BigInteger lowerBound = IntervalHelper.getLowerBoundOrNull(pFirstOperand);
    BigInteger upperBound = IntervalHelper.getUpperBoundOrNull(pFirstOperand);
    if (lowerBound != null) {
      lowerBound = lowerBound.add(pSecondOperand);
    }
    if (upperBound != null) {
      upperBound = upperBound.add(pSecondOperand);
    }
    return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
  }

}

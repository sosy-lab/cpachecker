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
 * The instance of this class is an operator for left shifting a simple
 * interval by a simple interval to produce another simple interval.
 */
enum ShiftLeftOperator implements ISIOperator {

  INSTANCE;

  /**
   * Computes an interval representing a superset of the possible values of
   * left-shifting any value contained in the first operand interval by the
   * second operand big integer value.
   *
   * @param pFirstOperand the interval to shift by the second operand big
   * integer value.
   * @param pSecondOperand the second interval big integer value to shift
   * the values of the first operand interval by.
   * @return an interval representing a superset of the possible values of
   * left-shifting any value contained in the first operand interval by the
   * second operand big integer value.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
    /*
     * If this is top, it will stay top after any kind of shift, so the
     * identity is returned. The same applies for shifting [0] (a
     * singleton interval of zero) or shifting anything by 0.
     */
    if (pFirstOperand.isTop() || pSecondOperand.signum() == 0
        || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
      return pFirstOperand;
    }
    // Negative left shifts are right shifts.
    if (pSecondOperand.signum() < 0) {
      return apply(pFirstOperand, pSecondOperand.negate());
    }
    /*
     * BigInteger supports shifting only for integer values. If the shift
     * distance is within the integer range, both bounds are shifted
     * to obtain the new bounds (infinite bounds stay infinite).
     */
    if (pSecondOperand.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
      BigInteger lowerBound = null;
      BigInteger upperBound = null;
      if (pFirstOperand.hasLowerBound()) {
        lowerBound = pFirstOperand.getLowerBound().shiftLeft(pSecondOperand.intValue());
      }
      if (pFirstOperand.hasUpperBound()) {
        upperBound = pFirstOperand.getUpperBound().shiftLeft(pSecondOperand.intValue());
      }
      return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
    }
    /*
     * For shifting distances larger than Integer.MAX_VALUE, we assume
     * (-)infinity is the only possible result of left shifting.
     */
    return SimpleInterval.infinite();
  }

}

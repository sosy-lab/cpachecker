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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tocompound;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tocompound.ISCOperator;

/**
 * This class represents the right shift operator for left shifting
 * intervals by intervals.
 */
enum ShiftRightOperator implements IICOperator {

  /**
   * The singleton instance of this operator.
   */
  INSTANCE;

  @Override
  public CompoundInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
    /*
     * If this is top, it will stay top after any kind of shift, so the
     * identity is returned. The same applies for shifting [0] (a
     * singleton interval of zero) or shifting anything by 0.
     */
    if (pFirstOperand.isTop() || pSecondOperand.isSingleton() && pSecondOperand.containsZero()
        || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
      return CompoundInterval.of(pFirstOperand);
    }
    CompoundInterval result = CompoundInterval.bottom();
    /*
     * If zero is one of the possible shift distances, this interval is
     * contained in the overall result.
     */
    if (pSecondOperand.containsZero()) {
      result = result.unionWith(pFirstOperand);
    }
    /*
     * If there are negative shift distances, extract the negative part
     * of the shift distances from the given interval, left shift this
     * interval by that part and include the result in the overall result.
     */
    if (pSecondOperand.containsNegative()) {
      SimpleInterval negPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE.negate()).extendToNegativeInfinity());
      result = result.unionWith(ShiftLeftOperator.INSTANCE.apply(pFirstOperand, negPart.negate()));
    }
    /*
     * If there are positive shift distances, extract the positive part
     * of the shift distances, shift this interval by both the lower
     * and the upper bound of that positive part and include the result
     * in the overall result.
     */
    if (pSecondOperand.containsPositive()) {
      SimpleInterval posPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE).extendToPositiveInfinity());
      /*
       * Shift this interval by the lower bound, then by the upper bound of
       * the positive part and span over the results.
       */
      CompoundInterval posPartResult = ISCOperator.SHIFT_RIGHT_OPERATOR.apply(pFirstOperand, posPart.getLowerBound());
      if (posPart.hasUpperBound()) {
        posPartResult = CompoundInterval.span(posPartResult, ISCOperator.SHIFT_RIGHT_OPERATOR.apply(pFirstOperand, posPart.getUpperBound()));
      } else {
        // Shifting by infinitely large values will result in zero.
        posPartResult = CompoundInterval.span(posPartResult, CompoundInterval.singleton(BigInteger.ZERO));
      }
      result = result.unionWith(posPartResult);
    }
    return result;
  }

}
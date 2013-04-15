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
 * The instance of this class is an operator for multiplying a big integer
 * to a simple interval to produce another simple interval.
 */
enum MultiplyOperator implements ISIOperator {

  INSTANCE;

  /**
   * Calculates a superset of the possible results obtained by multiplying
   * any value of the first operand interval with the second operand big
   * integer value.
   *
   * @param pFirstOperand the simple interval to multiply with the second
   * operand.
   * @param pSecondOperand the value to multiply the values of the first
   * operand interval with.
   * @return a superset of the possible results obtained by multiplying any
   * value of the first operand interval with the second operand big
   * integer value.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
    /*
     * Any finite value multiplied by zero is zero. If the bounds of this
     * interval are infinite, they are exclusive bounds, so the fact that
     * (-)infinity * 0 is undefined is not a problem: all values contained
     * in this interval are considered finite.
     */
    if (pSecondOperand.equals(BigInteger.ZERO)) {
      return SimpleInterval.singleton(BigInteger.ZERO);
    }
    /*
     * If the given factor is one, which is the neutral element of
     * multiplication, or this interval is infinite in both direction,
     * this interval is returned unchanged.
     */
    if (pSecondOperand.equals(BigInteger.ONE) || pFirstOperand.isTop()) {
      return pFirstOperand;
    }
    /*
     * To avoid duplication of the negation code, negative factors are
     * negated and then applied to the negation of the interval, so that
     * the actual multiplication logic only deals with non-negative
     * co-factors.
     */
    if (pSecondOperand.signum() < 0) {
      return apply(pFirstOperand.negate(), pSecondOperand.negate());
    }
    /*
     * Infinite bounds stay infinite, finite bounds are multiplied with
     * the factor.
     */
    BigInteger lowerBound = IntervalHelper.getLowerBoundOrNull(pFirstOperand);
    BigInteger upperBound = IntervalHelper.getUpperBoundOrNull(pFirstOperand);
    if (lowerBound != null) {
      lowerBound = lowerBound.multiply(pSecondOperand);
    }
    if (upperBound != null) {
      upperBound = upperBound.multiply(pSecondOperand);
    }
    return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
  }

}

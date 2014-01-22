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

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.IntervalHelper;

/**
 * The instance of this class is an operator for dividing a simple
 * interval by a big integer value to produce another simple interval.
 */
enum DivideOperator implements ISIOperator {

  INSTANCE;

  /**
   * Calculates a superset of the possible results from dividing any
   * value of the first operand interval by the second operand big
   * integer.
   *
   * This will return <code>null</code> if any only if the second
   * operand is zero.
   *
   * @param pFirstOperand the interval of values to divide by the
   * second operand big integer value.
   * @param pSecondOperand the value to divide the values of this range by.
   * @return a superset of the possible results from dividing
   * any value of the first operand interval by the given second
   * operand big integer or <code>null</code> if
   * <code>pSecondOperand</code> is zero.
   */
  @Override
  public @Nullable SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
 // Division by zero is undefined, so null is returned
    if (pSecondOperand.equals(BigInteger.ZERO)) {
      return null;
    }
    /*
     * Dividing an interval by one will yield its identity; the same goes
     * for dividing [0, 0] (a singleton interval of zero) by anything.
     */
    if (pSecondOperand.equals(BigInteger.ONE) || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
      return pFirstOperand;
    }
    if (pSecondOperand.compareTo(BigInteger.ZERO) < 0) {
      return apply(pFirstOperand.negate(), pSecondOperand.negate());
    }
    /*
     * Divide each finite bound by the divisor to obtain the new bounds;
     * infinite bounds stay infinite.
     */
    BigInteger lowerBound = null;
    BigInteger upperBound = null;
    if (pFirstOperand.hasLowerBound()) {
      lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand);
    }
    if (pFirstOperand.hasUpperBound()) {
      upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand);
    }
    return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
  }

}

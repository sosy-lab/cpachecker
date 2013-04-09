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

/**
 * The instance of this class is an operator for computing the remainder
 * of dividing a simple interval by a big integer to produce another
 * simple interval.
 */
enum ModuloOperator implements ISIOperator {

  INSTANCE;

  /**
   * Computes a superset of the possible values resulting from calculating
   * for any value <code>a</code> of the first operand interval and the
   * second operand big integer value <code>pSecondOperand</code> the
   * operation <code>a%pSecondOperand</code>.
   *
   * However, if the second operand (the divisor) is zero, such a superset
   * cannot be calculated because division by zero is undefined and
   * therefore the same applies to the modulo operation; in such a case
   * this function will return <code>null</code>.
   *
   * Another important fact is that the modulo operation is not fully
   * standardized. This implementation will round towards zero and the
   * sign of the result will only depend on the sign of the first operand
   * interval values, not on the sign of the second operand (the divisor), which means that
   * only the absolute value of the divisor is used. This is also the usual
   * behavior in C on modern machines.
   *
   * @param pFirstOperand the interval of values to be divided by the
   * second operand.
   * @param pSecondOperand the modulo divisor.
   * @return a superset of the possible results from calculating the modulo
   * operation between any value of the first operand interval as numerators
   * and the second operand big integer value as divisor or
   * <code>null</code> if the given divisor is zero.
   */
  @Override
  public SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
 // Division by zero is undefined, so null is returned
    if (pSecondOperand.equals(BigInteger.ZERO)) {
      return null;
    }
    /*
     * Only the absolute value of the divisor is considered (see
     * documentation), so a negative divisor is negated before
     * computing the result.
     */
    if (pSecondOperand.signum() < 0) {
      return apply(pFirstOperand, pSecondOperand.negate());
    }
    /*
     * If this is a singleton, simply use the big integer remainder
     * implementation.
     */
    if (pFirstOperand.isSingleton()) {
      return SimpleInterval.singleton(pFirstOperand.getLowerBound().remainder(pSecondOperand));
    }
    BigInteger largestPossibleValue = pSecondOperand.subtract(BigInteger.ONE);
    SimpleInterval moduloRange = null;
    /*
     * If there are negative values in this interval, the resulting range
     * might contain negative values.
     */
    if (pFirstOperand.containsNegative()) {
      /*
       * The largest possible interval resulting from performing the
       * modulo operation on the negative values of this interval ranges
       * from -(divisor-1) to zero
       */
      moduloRange = SimpleInterval.of(largestPossibleValue.negate(), BigInteger.ZERO);
      /*
       * If there is a lower bound, the negative part of this interval
       * is guaranteed to be finite and the resulting range can possibly
       * be narrowed down.
       */
      if (pFirstOperand.hasLowerBound()) {
        final SimpleInterval negPart;
        /*
         * If zero is contained, the non-positive part has an upper bound
         * of zero, otherwise it must be equal to this interval because
         * both bounds of this interval are negative anyway.
         */
        if (pFirstOperand.containsZero()) {
          negPart = SimpleInterval.of(pFirstOperand.getLowerBound(), BigInteger.ZERO);
        } else {
          negPart = pFirstOperand;
        }
        /*
         * Reuse the code concerning the non-negative part by applying
         * negation to the negative part, computing its modulo result
         * and negating that result again.
         */
        moduloRange = apply(negPart.negate(), pSecondOperand).negate();
      }
    }
    /*
     * If there are positive values in this interval, the resulting range
     * might contain positive values.
     */
    if (pFirstOperand.containsPositive()) {
      /*
       * The largest possible interval resulting from performing the
       * modulo operation on the positive values of this interval ranges
       * from (divisor-1) to zero
       */
      SimpleInterval posRange = SimpleInterval.of(BigInteger.ZERO, largestPossibleValue);
      /*
       * If there is an upper bound, the positive part of this interval
       * is guaranteed to be finite and the resulting range can possibly
       * be narrowed down.
       */
      if (pFirstOperand.hasUpperBound()) {
        final SimpleInterval posPart;
        /*
         * If zero is contained, the non-negative part has a lower bound
         * of zero, otherwise it must be equal to this interval because
         * both bounds of this interval are positive anyway.
         */
        if (pFirstOperand.containsZero()) {
          posPart = SimpleInterval.of(BigInteger.ZERO, pFirstOperand.getUpperBound());
        } else {
          posPart = pFirstOperand;
        }
        BigInteger posPartLength = posPart.size();
        /*
         * If length of the non-negative part is less than the the divisor,
         * not all values from zero to (divisor-1) are possible results.
         */
        if (posPartLength.compareTo(pSecondOperand) < 0) {
          BigInteger quotient = posPart.getUpperBound().divide(pSecondOperand);
          BigInteger modBorder = quotient.multiply(pSecondOperand);
          /*
           * If posPart is between modBorder and modBorder+pValue-1, the
           * possible values resulting from performing the modulo operation
           * on the considered part of this interval can be narrowed down.
           */
          if (modBorder.compareTo(posPart.getLowerBound()) <= 0
              && modBorder.add(largestPossibleValue).compareTo(posPart.getUpperBound()) >= 0) {
            BigInteger bound1 = posPart.getLowerBound().remainder(pSecondOperand);
            BigInteger bound2 = posPart.getUpperBound().remainder(pSecondOperand);
            posRange = SimpleInterval.of(bound1.min(bound2), bound1.max(bound2));
          }
        }
      }
      /*
       * Recombine the partial results. At least one of the partial results
       * must be non-null at this point by the containsPositive() or
       * containsNegative() parts, because otherwise this interval would
       * have to be [0, 0] which is covered in a previous early-return-case.
       */
      assert (moduloRange != null || posRange != null);
      if (moduloRange == null) {
        moduloRange = posRange;
      } else if (posRange != null) {
        moduloRange = SimpleInterval.span(moduloRange, posRange);
      }
    }
    return moduloRange;
  }

}

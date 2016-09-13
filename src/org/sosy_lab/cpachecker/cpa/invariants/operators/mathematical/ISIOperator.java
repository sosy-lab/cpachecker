/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval operand and a big integer operand,
 * producing another simple interval representing the result of the operation.
 */
public enum ISIOperator implements Operator<SimpleInterval, BigInteger, SimpleInterval> {

  /**
   * The addition operator for adding intervals to big integers.
   */
  ADD {

    /**
     * Computes the interval of possible results from adding any value of
     * this interval to the given value <code>pValue</code>.
     *
     * @param pFirstOperand the simple interval to add the big integer value to.
     * @param pSecondOperand the value to add to any of the values of the first operand interval.
     * @return the interval of possible results from adding any value of
     * the first operand interval to the second operand big integer value.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
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

  },

  /**
   * The multiplication operator for multiplying intervals with big integers.
   */
  MULTIPLY {

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

  },

  /**
   * The division operator for dividing intervals by big integers.
   */
  DIVIDE {

    /**
     * Calculates a superset of the possible results from dividing any
     * value of the first operand interval by the second operand big
     * integer.
     *
     * This will return <code>null</code> iff the second operand is zero.
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
      if (pSecondOperand.equals(BigInteger.ONE)
          || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
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

  },

  /**
   * The modulo operator for computing the remainder of dividing intervals
   * by big integers.
   */
  MODULO {

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

  },

  /**
   * The left shift operator for left shifting a simple interval by a big
   * integer value.
   */
  SHIFT_LEFT {

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
      if (pFirstOperand.isTop()
          || pSecondOperand.signum() == 0
          || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
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

  },

  /**
   * The right shift operator for right shifting a simple interval by a big
   * integer value.
   */
  SHIFT_RIGHT {

    /**
     * Computes an interval representing a superset of the possible values
     * of right-shifting any value contained in the first operand simple
     * interval by the second operand big integer value.
     *
     * @param pFirstOperand the simple interval to be shifted by the second
     * operand big integer value.
     * @param pSecondOperand the value to shift the values of the first
     * operand big integer value by.
     * @return an interval representing a superset of the possible values
     * of right-shifting any value contained in the first operand simple
     * interval by the second operand big integer given value.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop()
          || pSecondOperand.signum() == 0
          || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
        return pFirstOperand;
      }
      // Negative right shifts are left shifts.
      if (pSecondOperand.signum() < 0) {
        return ISIOperator.SHIFT_LEFT.apply(pFirstOperand, pSecondOperand.negate());
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
          lowerBound = pFirstOperand.getLowerBound().shiftRight(pSecondOperand.intValue());
        }
        if (pFirstOperand.hasUpperBound()) {
          upperBound = pFirstOperand.getUpperBound().shiftRight(pSecondOperand.intValue());
        }
        return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
      }
      /*
       * For shifting distances larger than Integer.MAX_VALUE, we assume
       * zero is the only possible result of right shifting finite values.
       */
      if (!pFirstOperand.hasLowerBound()) {
        return SimpleInterval.singleton(BigInteger.ZERO).extendToNegativeInfinity();
      }
      if (!pFirstOperand.hasUpperBound()) {
        return SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity();
      }
      return SimpleInterval.singleton(BigInteger.ZERO);
    }

  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the big integer operand to apply the operator to.
   * @return the simple interval resulting from applying the first operand to the
   * second operand.
   */
  @Override
  public abstract SimpleInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand);

}

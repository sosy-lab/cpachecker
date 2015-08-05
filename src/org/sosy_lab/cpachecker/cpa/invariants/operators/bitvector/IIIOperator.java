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
package org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

import com.google.common.base.Preconditions;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to two simple interval operands, producing another simple
 * interval representing the result of the operation.
 */
public enum IIIOperator implements Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> {

  ADD {

    /**
     * Computes the interval of possible results from adding any value of
     * the first operand interval to any value of the second operand interval <code>pInterval</code>.
     *
     * @param pOperand1 the values of the first operand.
     * @param pOperand2 the values to add to any of the values of the first operand interval.
     *
     * @return the interval of possible results from adding any value of
     * the first operand interval to any value of the second operand interval <code>pInterval</code>.
     */
    @Override
    public BitVectorInterval apply(BitVectorInterval pOperand1, BitVectorInterval pOperand2) {
      checkBitVectorCompatibility(pOperand1, pOperand2);

      // Avoid creating a new object by checking easy special cases
      if (pOperand1.isTop()) {
        return pOperand1;
      }
      if (pOperand2.isTop()) {
        return pOperand2;
      }
      if (pOperand2.isSingleton()) {
        return ISIOperator.ADD.apply(pOperand1, pOperand2.getLowerBound());
      }
      if (pOperand1.isSingleton()) {
        return ISIOperator.ADD.apply(pOperand2, pOperand1.getLowerBound());
      }
      /*
       * Add up the lower bounds to the new lower bound, add up the upper
       * bounds for the new upper bound. If any of the summands is not
       * finite, the resulting bound isn't finite either.
       */
      BigInteger lowerBound = pOperand1.getLowerBound();
      BigInteger upperBound = pOperand1.getUpperBound();
      BigInteger pLowerBound = pOperand2.getLowerBound();
      BigInteger pUpperBound = pOperand2.getUpperBound();

      lowerBound = lowerBound.add(pLowerBound);
      upperBound = upperBound.add(pUpperBound);

      return BitVectorInterval.cast(pOperand1.getBitVectorInfo(), lowerBound, upperBound);
    }

  },

  DIVIDE {

    /**
     * Calculates a superset of the possible results from dividing any value
     * of the first operand interval by any of values in the second operand
     * interval.
     *
     * This will return <code>null</code> only if the second operand
     * interval is [0,0] (a singleton interval of the value 0).
     *
     * @param pFirstOperand the interval to divide by the second operand
     * interval.
     * @param pSecondOperand the values to divide the values of the first
     * operand interval by.
     *
     * @return a superset of the possible results from dividing any value of
     * the first operand interval by the values of the second operand
     * interval or <code>null</code> if <code>pSecondOperand</code> is a
     * singleton interval of the value 0.
     */
    @Override
    public @Nullable BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
      checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

      if (pSecondOperand.isSingleton()) {
        return ISIOperator.DIVIDE.apply(pFirstOperand, pSecondOperand.getLowerBound());
      }
      if (pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }

      BigInteger lowerBound = null;
      BigInteger upperBound = null;
      // Determine the upper bound
      if (pFirstOperand.containsPositive()) {
        if (pSecondOperand.containsPositive()) {
          // e.g. in [2,4] / [0,2] we want 4/1 as the upper bound
          upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand.closestPositiveToZero());
        } else {
          // e.g. in [2,4] / [-2,-1] we want 2/(-2) as the upper bound
          upperBound = pFirstOperand.getLowerBound().divide(pSecondOperand.getLowerBound());
        }
      } else {
        if (pSecondOperand.containsPositive()) {
          // e.g. in [-4,-2] / [1,2] we want -2/2 as the upper bound
          upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand.getUpperBound());
        } else {
          // e.g. in [-4,-2] / [-2,-1] we want -4/(-1) as the upper bound
          upperBound = pFirstOperand.getLowerBound().divide(pSecondOperand.closestNegativeToZero());
        }
      }
      // Determine the lower bound
      if (pFirstOperand.containsNegative()) {
        if (pSecondOperand.containsPositive()) {
          // e.g. in [-4,-2] / [1,2] we want -4/1 as the lower bound
          lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand.closestPositiveToZero());
        } else {
          // e.g. in [-4,-2] / [1,2] we want -4/1 as the lower bound
          lowerBound = pFirstOperand.getUpperBound().divide(pSecondOperand.getLowerBound());
        }
      } else {
        if (pSecondOperand.containsPositive()) {
          // e.g. in [2,4] / [1,2] we want 2/2 as the lower bound
          lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand.getUpperBound());
        } else {
          // e.g. in [2,4] / [-2,-1] we want 4/(-1) as the lower bound
          lowerBound = pFirstOperand.getUpperBound().divide(pSecondOperand.closestNegativeToZero());
        }
      }
      return BitVectorInterval.cast(pFirstOperand.getBitVectorInfo(), lowerBound, upperBound);
    }

  },

  MODULO {

    /**
     * Computes a superset of the possible values resulting from calculating
     * for any value <code>a</code> of the first operand interval and any
     * value <code>b</code> of the second operand interval the operation
     * <code>a%b</code>.
     *
     * @param pFirstOperand the first operand, which is an interval that
     * contains the values to be divided by the second operand interval
     * values.
     * @param pSecondOperand the second operand interval which represents the
     * range of modulo divisors.
     *
     * @return a superset of the possible results from calculating the modulo
     * operation between any value of the first operand interval as numerators
     * and any value of the second operand interval as divisors.
     */
    @Override
    public BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
      checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

      if (!pSecondOperand.hasLowerBound() || !pSecondOperand.hasUpperBound()) {
        return pFirstOperand;
      }
      return ISIOperator.MODULO.apply(pFirstOperand, pSecondOperand.getLowerBound().abs().max(pSecondOperand.getUpperBound().abs()));
    }

  },

  MULTIPLY {

    /**
     * Calculates a superset of the possible results obtained by multiplying
     * any value of the first operand interval with any value of the second operand interval.
     *
     * @param pFirstOperand the values to multiply the values of the second operand interval with.
     * @param pSecondOperand the values to multiply the values of the first operand interval with.
     *
     * @return a superset of the possible results obtained by multiplying
     * any value of the first operand interval with any value of the second operand interval.
     */
    @Override
    public BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
      checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

      /*
       * If one of the two intervals is a single value, use the easier
       * scalar multiplication. This also takes care of special cases
       * like multiplication with zero ore one.
       */
      if (pSecondOperand.isSingleton()) {
        return ISIOperator.MULTIPLY.apply(pFirstOperand, pSecondOperand.getLowerBound());
      }
      if (pFirstOperand.isSingleton()) {
        return ISIOperator.MULTIPLY.apply(pSecondOperand, pFirstOperand.getLowerBound());
      }

      /*
       * Multiply the bounds with each other to find the new extremes.
       * At most one bound of each interval can be infinite and neither
       * of the intervals is a singleton at this point.
       */
      BigInteger pLowerBound = pSecondOperand.getLowerBound();
      BigInteger pUpperBound = pSecondOperand.getUpperBound();

      // Both lower bounds are finite,
      // so a candidate for the new extremes can be calculated
      BigInteger lbLb = pFirstOperand.getLowerBound().multiply(pLowerBound);

      // The first lower and second upper bound are both finite,
      // so a candidate for the new extremes can be calculated
      BigInteger lbUb = pFirstOperand.getLowerBound().multiply(pUpperBound);

      // The first upper and second lower bound are both finite,
      // so a candidate for the new extremes can be calculated
      BigInteger ubLb = pFirstOperand.getUpperBound().multiply(pLowerBound);

      // Both upper bounds are finite,
      // so a candidate for the new extremes can be calculated
      BigInteger ubUb = pFirstOperand.getUpperBound().multiply(pUpperBound);

      // Find the lowest and highest extremes
      BigInteger lowerBound = lbLb.min(lbUb).min(ubLb).min(ubUb);
      BigInteger upperBound = lbLb.max(lbUb).max(ubLb).max(ubUb);
      BitVectorInterval result = BitVectorInterval.cast(pFirstOperand.getBitVectorInfo(), lowerBound, upperBound);

      return result;
    }

  },

  SHIFT_LEFT {

    /**
     * Computes an interval representing a superset of the possible values
     * of left-shifting any value contained in the first operand interval by
     * any value of the second operand interval.
     *
     * @param pFirstOperand the first operand simple interval containing the
     * values to be shifted by the second operand simple interval.
     * @param pSecondOperand the range of values to shift the values of the
     * first operand simple interval by.
     *
     * @return an interval representing a superset of the possible values
     * of left-shifting any value contained in the first operand simple
     * interval by any value of the second operand simple interval.
     */
    @Override
    public BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
      checkBitVectorCompatibility(pFirstOperand, pSecondOperand);
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop() || pSecondOperand.isSingleton() && pSecondOperand.containsZero()
          || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }
      BitVectorInterval result = null;
      /*
       * If zero is one of the possible shift distances, this interval is
       * contained in the overall result.
       */
      if (pSecondOperand.containsZero()) {
        result = pFirstOperand;
      }
      /*
       * If there are negative shift distances, extract the negative part
       * of the shift distances from the given interval, right shift this
       * interval by that part and include the result in the overall result.
       */
      if (pSecondOperand.containsNegative()) {
        BitVectorInterval negPart = pSecondOperand.intersectWith(BitVectorInterval.singleton(pFirstOperand.getBitVectorInfo(), BigInteger.ONE.negate()).extendToMinValue());
        BitVectorInterval negPartResult = IIIOperator.SHIFT_RIGHT.apply(pFirstOperand, negPart.negate());
        result = result == null ? negPartResult : BitVectorInterval.span(result, negPartResult);
      }
      /*
       * If there are positive shift distances, extract the positive part
       * of the shift distances, shift this interval by both the lower
       * and the upper bound of that positive part and include the result
       * in the overall result.
       */
      if (pSecondOperand.containsPositive()) {
        BitVectorInterval posPart = pSecondOperand.intersectWith(BitVectorInterval.singleton(pFirstOperand.getBitVectorInfo(), BigInteger.ONE).extendToMaxValue());
        BitVectorInterval posPartResult = ISIOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getLowerBound());

        posPartResult = BitVectorInterval.span(posPartResult, ISIOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getUpperBound()));

        result = result == null ? posPartResult : BitVectorInterval.span(result, posPartResult);
      }
      return result;
    }

  },

  SHIFT_RIGHT {

    /**
     * Computes an interval representing a superset of the possible values
     * of right-shifting any value contained in the first operand simple
     * interval by any value of the second operand simple interval.
     *
     * @param pFirstOperand the simple interval containing the values to be
     * right shifted.
     * @param pSecondOperand the range of values to shift the values of the first
     * operator interval by.
     *
     * @return an interval representing a superset of the possible values
     * of right-shifting any value contained in the first operand simple
     * interval by any value of the second operand simple interval.
     */
    @Override
    public BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
      checkBitVectorCompatibility(pFirstOperand, pSecondOperand);
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop() || pSecondOperand.isSingleton() && pSecondOperand.containsZero()
          || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }
      BitVectorInterval result = null;
      /*
       * If zero is one of the possible shift distances, this interval is
       * contained in the overall result.
       */
      if (pSecondOperand.containsZero()) {
        result = pFirstOperand;
      }
      /*
       * If there are negative shift distances, extract the negative part
       * of the shift distances from the given interval, left shift this
       * interval by that part and include the result in the overall result.
       */
      if (pSecondOperand.containsNegative()) {
        BitVectorInterval negPart = pSecondOperand.intersectWith(BitVectorInterval.singleton(pFirstOperand.getBitVectorInfo(), BigInteger.ONE.negate()).extendToMinValue());
        BitVectorInterval negPartResult = IIIOperator.SHIFT_LEFT.apply(pFirstOperand, negPart.negate());
        result = result == null ? negPartResult : BitVectorInterval.span(result, negPartResult);
      }
      /*
       * If there are positive shift distances, extract the positive part
       * of the shift distances, shift this interval by both the lower
       * and the upper bound of that positive part and include the result
       * in the overall result.
       */
      if (pSecondOperand.containsPositive()) {
        BitVectorInterval posPart = pSecondOperand.intersectWith(BitVectorInterval.singleton(pFirstOperand.getBitVectorInfo(), BigInteger.ONE).extendToMaxValue());
        /*
         * Shift this interval by the lower bound, then by the upper bound of
         * the positive part and combine the results.
         */
        BitVectorInterval posPartResult = ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getLowerBound());

        posPartResult = BitVectorInterval.span(posPartResult, ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getUpperBound()));

        result = result == null ? posPartResult : BitVectorInterval.span(result, posPartResult);
      }
      return result;
    }

  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the first simple interval operand to apply the operator to.
   * @param pSecondOperand the second simple interval operand to apply the operator to.
   * @return the simple interval resulting from applying the first operand to the
   * second operand.
   */
  @Override
  public abstract BitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand);

  private static void checkBitVectorCompatibility(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
    Preconditions.checkArgument(pFirstOperand.getBitVectorInfo().equals(pSecondOperand.getBitVectorInfo()), "Both operands must have the same bit length and signedness.");
  }

}

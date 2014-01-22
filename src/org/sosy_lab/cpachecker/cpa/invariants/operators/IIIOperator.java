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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to two simple interval operands, producing another simple
 * interval representing the result of the operation.
 */
public enum IIIOperator implements Operator<SimpleInterval, SimpleInterval, SimpleInterval> {

  ADD {

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

  },

  DIVIDE {

    /**
     * Calculates a superset of the possible results from dividing any value
     * of the first operand interval by any of values in the second operand
     * interval.
     *
     * This will return <code>null</code> if any only if the second operand
     * interval is [0,0] (a singleton interval of the value 0).
     *
     * @param pFirstOperand the interval to divide by the second operand
     * interval.
     * @param pSecondOperand the values to divide the values of the first
     * operand interval by.
     * @return a superset of the possible results from dividing any value of
     * the first operand interval by the values of the second operand
     * interval or <code>null</code> if <code>pSecondOperand</code> is a
     * singleton interval of the value 0.
     */
    @Override
    public @Nullable SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      if (pSecondOperand.isSingleton()) {
        return ISIOperator.DIVIDE.apply(pFirstOperand, pSecondOperand.getLowerBound());
      }
      if (pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }
      // Any bound that is infinite will stay infinite (but may change sign)
      boolean negInf =
          !pFirstOperand.hasLowerBound() && pSecondOperand.containsPositive() || !pFirstOperand.hasUpperBound() && pSecondOperand.containsNegative();
      boolean posInf =
          !pFirstOperand.hasLowerBound() && pSecondOperand.containsNegative() || !pFirstOperand.hasUpperBound() && pSecondOperand.containsPositive();
      if (negInf && posInf) {
        return SimpleInterval.infinite();
      }
      BigInteger lowerBound = null;
      BigInteger upperBound = null;
      // Determine the upper bound if it is not infinity
      if (!posInf) {
        if (pFirstOperand.containsPositive()) {
          if (pSecondOperand.containsPositive()) {
            // e.g. in [2,4] / [0,2] we want 4/1 as the upper bound
            upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand.closestPositiveToZero());
          } else {
            // e.g. in [2,4] / [-2,-1] we want 2/(-2) as the upper bound
            if (!pSecondOperand.hasLowerBound()) {
              upperBound = BigInteger.ZERO;
            } else {
              upperBound = pFirstOperand.getLowerBound().divide(pSecondOperand.getLowerBound());
            }
          }
        } else {
          if (pSecondOperand.containsPositive()) {
            // e.g. in [-4,-2] / [1,2] we want -2/2 as the upper bound
            if (!pSecondOperand.hasUpperBound()) {
              upperBound = BigInteger.ZERO;
            } else {
              upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand.getUpperBound());
            }
          } else {
            // e.g. in [-4,-2] / [-2,-1] we want -4/(-1) as the upper bound
            upperBound = pFirstOperand.getLowerBound().divide(pSecondOperand.closestNegativeToZero());
          }
        }
      }
      // Determine the lower bound if it is not negative infinity
      if (!negInf) {
        if (pFirstOperand.containsNegative()) {
          if (pSecondOperand.containsPositive()) {
            // e.g. in [-4,-2] / [1,2] we want -4/1 as the lower bound
            lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand.closestPositiveToZero());
          } else {
            // e.g. in [-4,-2] / [1,2] we want -4/1 as the lower bound
            if (!pSecondOperand.hasLowerBound()) {
              lowerBound = BigInteger.ZERO;
            } else {
              lowerBound = pFirstOperand.getUpperBound().divide(pSecondOperand.getLowerBound());
            }
          }
        } else {
          if (pSecondOperand.containsPositive()) {
            // e.g. in [2,4] / [1,2] we want 2/2 as the lower bound
            if (!pSecondOperand.hasUpperBound()) {
              lowerBound = BigInteger.ZERO;
            } else {
              lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand.getUpperBound());
            }
          } else {
            // e.g. in [2,4] / [-2,-1] we want 4/(-1) as the lower bound
            lowerBound = pFirstOperand.getUpperBound().divide(pSecondOperand.closestNegativeToZero());
          }
        }
      }
      return IntervalHelper.ofNullableBounds(lowerBound, upperBound);
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
     * @return a superset of the possible results from calculating the modulo
     * operation between any value of the first operand interval as numerators
     * and any value of the second operand interval as divisors.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
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
     * @return a superset of the possible results obtained by multiplying
     * any value of the first operand interval with any value of the second operand interval.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
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
      // If one  of the intervals is top, the resulting interval is top
      if (pFirstOperand.isTop() || pSecondOperand.isTop()) {
        return SimpleInterval.infinite();
      }
      /*
       * Multiply the bounds with each other to find the new extremes.
       * At most one bound of each interval can be infinite and neither
       * of the intervals is a singleton at this point.
       */
      BigInteger pLowerBound = IntervalHelper.getLowerBoundOrNull(pSecondOperand);
      BigInteger pUpperBound = IntervalHelper.getUpperBoundOrNull(pSecondOperand);
      BigInteger lbLb = null;
      BigInteger lbUb = null;
      BigInteger ubLb = null;
      BigInteger ubUb = null;
      boolean negInf = false;
      boolean posInf = false;
      if (!pFirstOperand.hasLowerBound()) {
        // One of the lower bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found.
        // Otherwise, zero is a candidate for an extreme.
        if (pLowerBound == null || pLowerBound.signum() < 0) {
          posInf = true;
        } else if (pLowerBound.signum() > 0) {
          negInf = true;
        } else {
          lbLb = ubLb = BigInteger.ZERO;
        }
        if (pUpperBound == null || pUpperBound.signum() > 0) {
          negInf = true;
        } else if (pUpperBound.signum() < 0) {
          posInf = true;
        }
      } else {
        int thisLowerBoundSignum = pFirstOperand.getLowerBound().signum();
        if (pLowerBound == null) {
          // One of the lower bounds is negative infinity, so if one of
          // its co-factors is non-zero, an extreme can easily be found
          // Otherwise, zero is a candidate for an extreme.
          if (thisLowerBoundSignum < 0) {
            posInf = true;
          } else if (thisLowerBoundSignum > 0) {
            negInf = true;
          } else {
            lbLb = lbUb = BigInteger.ZERO;
          }
        } else {
          // Both lower bounds are finite,
          // so a candidate for the new extremes can be calculated
          lbLb = pFirstOperand.getLowerBound().multiply(pLowerBound);
        }
        if (pUpperBound == null) {
          // One of the upper bounds is negative infinity, so if one of
          // its co-factors is non-zero, an extreme can easily be found
          // Otherwise, zero is a candidate for an extreme.
          if (thisLowerBoundSignum < 0) {
            negInf = true;
          } else if (thisLowerBoundSignum > 0) {
            posInf = true;
          } else {
            lbLb = lbUb = BigInteger.ZERO;
          }
        } else {
          // The first lower and second upper bound are both finite,
          // so a candidate for the new extremes can be calculated
          lbUb = pFirstOperand.getLowerBound().multiply(pUpperBound);
        }
      }
      if (!pFirstOperand.hasUpperBound()) {
        // One of the upper bounds is negative infinity, so if one of
        // its co-factors is non-zero, an extreme can easily be found
        // Otherwise, zero is a candidate for an extreme.
        if (pLowerBound == null || pLowerBound.signum() < 0) {
          negInf = true;
        } else if (pLowerBound.signum() > 0) {
          posInf = true;
        } else {
          lbLb = ubLb = BigInteger.ZERO;
        }
        if (pUpperBound == null || pUpperBound.signum() > 0) {
          posInf = true;
        } else if (pUpperBound.signum() < 0) {
          negInf = true;
        } else {
          lbUb = ubUb = BigInteger.ZERO;
        }
      } else { // pFirstOperand.hasUpperBound()
        int thisUpperBoundSignum = pFirstOperand.getUpperBound().signum();
        if (pLowerBound == null) {
          // One of the lower bounds is negative infinity, so if one of
          // its co-factors is non-zero, an extreme can easily be found
          if (thisUpperBoundSignum < 0) {
            posInf = true;
          } else if (thisUpperBoundSignum > 0) {
            negInf = true;
          } else {
            ubLb = ubUb = BigInteger.ZERO;
          }
        } else { // pLowerBound != null
          // The first upper and second lower bound are both finite,
          // so a candidate for the new extremes can be calculated
          ubLb = pFirstOperand.getUpperBound().multiply(pLowerBound);
        }
        if (!pSecondOperand.hasUpperBound()) {
          // One of the upper bounds is negative infinity, so if one of
          // its co-factors is non-zero, an extreme can easily be found
          if (thisUpperBoundSignum < 0) {
            negInf = true;
          } else if (thisUpperBoundSignum > 0) {
            posInf = true;
          } else {
            ubLb = ubUb = BigInteger.ZERO;
          }
        } else {
          // Both upper bounds are finite,
          // so a candidate for the new extremes can be calculated
          ubUb = pFirstOperand.getUpperBound().multiply(pUpperBound);
        }
      }
      if (negInf && posInf) { return SimpleInterval.infinite(); }
      // Find the lowest and highest extremes
      BigInteger lowerBound = min(min(lbLb, lbUb), min(ubLb, ubUb));
      BigInteger upperBound = max(max(lbLb, lbUb), max(ubLb, ubUb));
      SimpleInterval result = IntervalHelper.ofNullableBounds(lowerBound, upperBound);
      if (negInf) {
        result = result.extendToNegativeInfinity();
      }
      if (posInf) {
        result = result.extendToPositiveInfinity();
      }
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
     * @return an interval representing a superset of the possible values
     * of left-shifting any value contained in the first operand simple
     * interval by any value of the second operand simple interval.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop() || pSecondOperand.isSingleton() && pSecondOperand.containsZero()
          || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }
      SimpleInterval result = null;
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
        SimpleInterval negPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE.negate()).extendToNegativeInfinity());
        SimpleInterval negPartResult = IIIOperator.SHIFT_RIGHT.apply(pFirstOperand, negPart.negate());
        result = result == null ? negPartResult : SimpleInterval.span(result, negPartResult);
      }
      /*
       * If there are positive shift distances, extract the positive part
       * of the shift distances, shift this interval by both the lower
       * and the upper bound of that positive part and include the result
       * in the overall result.
       */
      if (pSecondOperand.containsPositive()) {
        SimpleInterval posPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE).extendToPositiveInfinity());
        SimpleInterval posPartResult = ISIOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getLowerBound());
        if (posPart.hasUpperBound()) {
          posPartResult = SimpleInterval.span(posPartResult, ISIOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getUpperBound()));
        } else {
          // Left shifting by infinitely large values results in infinity.
          if (pFirstOperand.containsPositive()) {
            posPartResult = posPartResult.extendToPositiveInfinity();
          }
          if (pFirstOperand.containsNegative()) {
            posPartResult = posPartResult.extendToNegativeInfinity();
          }
        }
        result = result == null ? posPartResult : SimpleInterval.span(result, posPartResult);
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
     * @return an interval representing a superset of the possible values
     * of right-shifting any value contained in the first operand simple
     * interval by any value of the second operand simple interval.
     */
    @Override
    public SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop() || pSecondOperand.isSingleton() && pSecondOperand.containsZero()
          || pFirstOperand.isSingleton() && pFirstOperand.containsZero()) {
        return pFirstOperand;
      }
      SimpleInterval result = null;
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
        SimpleInterval negPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE.negate()).extendToNegativeInfinity());
        SimpleInterval negPartResult = IIIOperator.SHIFT_LEFT.apply(pFirstOperand, negPart.negate());
        result = result == null ? negPartResult : SimpleInterval.span(result, negPartResult);
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
         * the positive part and combine the results.
         */
        SimpleInterval posPartResult = ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getLowerBound());
        if (posPart.hasUpperBound()) {
          posPartResult = SimpleInterval.span(posPartResult, ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getUpperBound()));
        } else {
          // Shifting by infinitely large values will result in zero.
          posPartResult = SimpleInterval.span(posPartResult, SimpleInterval.singleton(BigInteger.ZERO));
        }
        result = result == null ? posPartResult : SimpleInterval.span(result, posPartResult);
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
  public abstract SimpleInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand);

  /**
   * If any of the given values is <code>null</code>,
   * the other value is returned, otherwise the maximum is determined.
   *
   * @param first the first of the values to determine the maximum for. May be <code>null</code>.
   * @param second the second of the values to determine the maximum for. May be <code>null</code>.
   * @return <code>null</code> if both given values are <code>null</code>,
   * the first value if the second value is <code>null</code>,
   * the second value if the first value is <code>null</code>,
   * otherwise the maximum of the given values.
   */
  private static @Nullable BigInteger max(@Nullable BigInteger first, @Nullable BigInteger second) {
    if (first == null) {
      return second;
    } else if (second == null) {
      return first;
    }
    return first.max(second);
  }

  /**
   * If any of the given values is <code>null</code>,
   * the other value is returned, otherwise the minimum is determined.
   *
   * @param first the first of the values to determine the minimum for. May be <code>null</code>.
   * @param second the second of the values to determine the minimum for. May be <code>null</code>.
   * @return <code>null</code> if both given values are <code>null</code>,
   * the first value if the second value is <code>null</code>,
   * the second value if the first value is <code>null</code>,
   * otherwise the minimum of the given values.
   */
  private static @Nullable BigInteger min(@Nullable BigInteger first, @Nullable BigInteger second) {
    if (first == null) {
      return second;
    } else if (second == null) {
      return first;
    }
    return first.min(second);
  }

}

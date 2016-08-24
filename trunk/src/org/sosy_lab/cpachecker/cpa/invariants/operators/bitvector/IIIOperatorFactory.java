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

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * This factory provides operators that can be applied to two bit-vector
 * interval operands, producing another bit-vector interval representing
 * the result of the operation.
 */
public enum IIIOperatorFactory {

  INSTANCE;

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getAdd(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
          return ISIOperatorFactory.INSTANCE
              .getAdd(pAllowSignedWrapAround, pOverflowEventHandler)
              .apply(pOperand1, pOperand2.getLowerBound());
        }
        if (pOperand1.isSingleton()) {
          return ISIOperatorFactory.INSTANCE
              .getAdd(pAllowSignedWrapAround, pOverflowEventHandler)
              .apply(pOperand2, pOperand1.getLowerBound());
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

        return BitVectorInterval.cast(
            pOperand1.getTypeInfo(),
            lowerBound,
            upperBound,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getDivide(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
      public @Nullable BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

        if (pSecondOperand.isSingleton()) {
          return ISIOperatorFactory.INSTANCE
              .getDivide(pAllowSignedWrapAround, pOverflowEventHandler)
              .apply(pFirstOperand, pSecondOperand.getLowerBound());
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
            upperBound =
                pFirstOperand.getUpperBound().divide(pSecondOperand.closestPositiveToZero());
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
            upperBound =
                pFirstOperand.getLowerBound().divide(pSecondOperand.closestNegativeToZero());
          }
        }
        // Determine the lower bound
        if (pFirstOperand.containsNegative()) {
          if (pSecondOperand.containsPositive()) {
            // e.g. in [-4,-2] / [1,2] we want -4/1 as the lower bound
            lowerBound =
                pFirstOperand.getLowerBound().divide(pSecondOperand.closestPositiveToZero());
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
            lowerBound =
                pFirstOperand.getUpperBound().divide(pSecondOperand.closestNegativeToZero());
          }
        }
        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            lowerBound,
            upperBound,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getModulo(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
      public BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

        if (!pSecondOperand.hasLowerBound() || !pSecondOperand.hasUpperBound()) {
          return pFirstOperand;
        }

        if (pSecondOperand.isSingleton()) {
          Operator<BitVectorInterval, BigInteger, BitVectorInterval> operator =
              ISIOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler);
          return operator.apply(pFirstOperand, pSecondOperand.getLowerBound());
        }

        BitVectorInfo info = pFirstOperand.getTypeInfo();
        BitVectorInterval result = null;

        if (pFirstOperand.containsNegative()) {
          BigInteger negUB = pFirstOperand.closestNegativeToZero();
          BitVectorInterval asPositive =
              pFirstOperand.hasLowerBound()
                  ? pFirstOperand.getLowerBound().equals(info.getMinValue())
                      ? BitVectorInterval.of(info, negUB.negate(), info.getMaxValue())
                      : BitVectorInterval.of(
                          info, negUB.negate(), pFirstOperand.getLowerBound().negate())
                  : BitVectorInterval.singleton(info, negUB.negate()).extendToMaxValue();
          result =
              applyPositiveUnknown(asPositive, pSecondOperand)
                  .negate(pAllowSignedWrapAround, pOverflowEventHandler);
        }

        if (pFirstOperand.containsPositive()) {
          BigInteger posLB = pFirstOperand.closestPositiveToZero();
          BitVectorInterval positivePart =
              pFirstOperand.hasUpperBound()
                  ? BitVectorInterval.of(info, posLB, pFirstOperand.getUpperBound())
                  : BitVectorInterval.singleton(info, posLB).extendToMaxValue();
          BitVectorInterval posResult = applyPositiveUnknown(positivePart, pSecondOperand);
          result = result == null ? posResult : BitVectorInterval.span(result, posResult);
        }

        assert result != null;
        return result;
      }

      private BitVectorInterval applyPositiveUnknown(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        if (pSecondOperand.isSingleton()) {
          Operator<BitVectorInterval, BigInteger, BitVectorInterval> operator =
              ISIOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler);
          return operator.apply(pFirstOperand, pSecondOperand.getLowerBound());
        }

        Preconditions.checkArgument(!pFirstOperand.containsNegative());
        Preconditions.checkArgument(!pFirstOperand.containsZero());

        BitVectorInfo info = pFirstOperand.getTypeInfo();
        BitVectorInterval result = null;

        if (pSecondOperand.containsNegative()) {
          BigInteger negUB = pSecondOperand.closestNegativeToZero();
          BitVectorInterval asPositive =
              pSecondOperand.hasLowerBound()
                  ? pSecondOperand.getLowerBound().equals(info.getMinValue())
                      ? BitVectorInterval.of(info, negUB.negate(), info.getMaxValue())
                      : BitVectorInterval.of(
                          info, negUB.negate(), pSecondOperand.getLowerBound().negate())
                  : BitVectorInterval.singleton(info, negUB.negate()).extendToMaxValue();
          result = applyPositivePositive(pFirstOperand, asPositive);
        }

        if (pSecondOperand.containsPositive()) {
          BigInteger posLB = pSecondOperand.closestPositiveToZero();
          BitVectorInterval positivePart =
              pSecondOperand.hasUpperBound()
                  ? BitVectorInterval.of(info, posLB, pSecondOperand.getUpperBound())
                  : BitVectorInterval.singleton(info, posLB).extendToMaxValue();
          BitVectorInterval posResult = applyPositivePositive(pFirstOperand, positivePart);
          result = result == null ? posResult : BitVectorInterval.span(result, posResult);
        }

        assert result != null;
        return result;
      }

      private BitVectorInterval applyPositivePositive(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        if (pSecondOperand.isSingleton()) {
          Operator<BitVectorInterval, BigInteger, BitVectorInterval> operator =
              ISIOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler);
          return operator.apply(pFirstOperand, pSecondOperand.getLowerBound());
        }

        Preconditions.checkArgument(!pFirstOperand.containsNegative());
        Preconditions.checkArgument(!pFirstOperand.containsZero());
        Preconditions.checkArgument(!pSecondOperand.containsNegative());
        Preconditions.checkArgument(!pSecondOperand.containsZero());

        if (pFirstOperand.hasUpperBound()
            && pSecondOperand.getLowerBound().compareTo(pFirstOperand.getUpperBound()) > 0) {
          return pFirstOperand;
        }
        BitVectorInfo info = pFirstOperand.getTypeInfo();
        BigInteger resultUpperBound;
        if (pFirstOperand.hasUpperBound()) {
          if (pSecondOperand.hasUpperBound()) {
            resultUpperBound =
                pFirstOperand
                    .getUpperBound()
                    .min(pSecondOperand.getUpperBound().subtract(BigInteger.ONE));
          } else {
            resultUpperBound = pFirstOperand.getUpperBound();
          }
        } else if (pSecondOperand.hasUpperBound()) {
          resultUpperBound = pSecondOperand.getUpperBound().subtract(BigInteger.ONE);
        } else {
          return BitVectorInterval.singleton(info, BigInteger.ZERO).extendToMaxValue();
        }
        return BitVectorInterval.of(info, BigInteger.ZERO, resultUpperBound);
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getMultiply(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
      public BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        checkBitVectorCompatibility(pFirstOperand, pSecondOperand);

        /*
         * If one of the two intervals is a single value, use the easier
         * scalar multiplication. This also takes care of special cases
         * like multiplication with zero ore one.
         */
        if (pSecondOperand.isSingleton()) {
          return ISIOperatorFactory.INSTANCE
              .getMultiply(pAllowSignedWrapAround, pOverflowEventHandler)
              .apply(pFirstOperand, pSecondOperand.getLowerBound());
        }
        if (pFirstOperand.isSingleton()) {
          return ISIOperatorFactory.INSTANCE
              .getMultiply(pAllowSignedWrapAround, pOverflowEventHandler)
              .apply(pSecondOperand, pFirstOperand.getLowerBound());
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
        BitVectorInterval result =
            BitVectorInterval.cast(
                pFirstOperand.getTypeInfo(),
                lowerBound,
                upperBound,
                pAllowSignedWrapAround,
                pOverflowEventHandler);

        return result;
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getShiftLeft(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
      public BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        checkBitVectorCompatibility(pFirstOperand, pSecondOperand);
        /*
         * If this is top, it will stay top after any kind of shift, so the
         * identity is returned. The same applies for shifting [0] (a
         * singleton interval of zero) or shifting anything by 0.
         */
        if (pFirstOperand.isTop()
            || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
            || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
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
         * Negative shifts are not defined.
         */
        if (pSecondOperand.containsNegative()) {
          return pFirstOperand.getTypeInfo().getRange();
        }
        /*
         * If there are positive shift distances, extract the positive part
         * of the shift distances, shift this interval by both the lower
         * and the upper bound of that positive part and include the result
         * in the overall result.
         */
        if (pSecondOperand.containsPositive()) {
          BitVectorInterval posPart =
              pSecondOperand.intersectWith(
                  BitVectorInterval.singleton(pFirstOperand.getTypeInfo(), BigInteger.ONE)
                      .extendToMaxValue());
          BitVectorInterval posPartResult =
              ISIOperatorFactory.INSTANCE
                  .getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, posPart.getLowerBound());

          posPartResult =
              BitVectorInterval.span(
                  posPartResult,
                  ISIOperatorFactory.INSTANCE
                      .getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler)
                      .apply(pFirstOperand, posPart.getUpperBound()));

          result = result == null ? posPartResult : BitVectorInterval.span(result, posPartResult);
        }
        return result;
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval> getShiftRight(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, BitVectorInterval>() {

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
      public BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        checkBitVectorCompatibility(pFirstOperand, pSecondOperand);
        /*
         * If this is top, it will stay top after any kind of shift, so the
         * identity is returned. The same applies for shifting [0] (a
         * singleton interval of zero) or shifting anything by 0.
         */
        if (pFirstOperand.isTop()
            || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
            || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
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
         * Negative shift distances are not defined.
         */
        if (pSecondOperand.containsNegative()) {
          return pFirstOperand.getTypeInfo().getRange();
        }
        /*
         * If there are positive shift distances, extract the positive part
         * of the shift distances, shift this interval by both the lower
         * and the upper bound of that positive part and include the result
         * in the overall result.
         */
        if (pSecondOperand.containsPositive()) {
          BitVectorInterval posPart =
              pSecondOperand.intersectWith(
                  BitVectorInterval.singleton(pFirstOperand.getTypeInfo(), BigInteger.ONE)
                      .extendToMaxValue());
          /*
           * Shift this interval by the lower bound, then by the upper bound of
           * the positive part and combine the results.
           */
          BitVectorInterval posPartResult =
              ISIOperatorFactory.INSTANCE
                  .getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, posPart.getLowerBound());

          posPartResult =
              BitVectorInterval.span(
                  posPartResult,
                  ISIOperatorFactory.INSTANCE
                      .getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler)
                      .apply(pFirstOperand, posPart.getUpperBound()));

          result = result == null ? posPartResult : BitVectorInterval.span(result, posPartResult);
        }
        return result;
      }
    };
  }

  private static void checkBitVectorCompatibility(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
    Preconditions.checkArgument(
        pFirstOperand.getTypeInfo().equals(pSecondOperand.getTypeInfo()),
        "Both operands must have the same bit length and signedness.");
  }

}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector;

import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * This factory provides operators that can be applied to an interval operand and a big integer
 * operand, producing another interval representing the result of the operation.
 */
enum ISIOperatorFactory {
  INSTANCE;

  /** The addition operator for adding intervals to big integers. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getAdd(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Computes the interval of possible results from adding any value of this interval to the
       * given value <code>pValue</code>.
       *
       * @param pFirstOperand the simple interval to add the big integer value to.
       * @param pSecondOperand the value to add to the values of the first operand interval.
       * @return the interval of possible results from adding any value of the first operand
       *     interval to the second operand big integer value.
       */
      @Override
      public BitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        // Avoid creating a new object by checking easy special cases
        if (pFirstOperand.isTop() || pSecondOperand.equals(BigInteger.ZERO)) {
          return pFirstOperand;
        }
        BigInteger lowerBound = pFirstOperand.getLowerBound().add(pSecondOperand);
        BigInteger upperBound = pFirstOperand.getUpperBound().add(pSecondOperand);
        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            lowerBound,
            upperBound,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  /** The multiplication operator for multiplying intervals with big integers. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getMultiply(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Calculates a superset of the possible results obtained by multiplying any value of the
       * first operand interval with the second operand big integer value.
       *
       * @param pFirstOperand the simple interval to multiply with the second operand.
       * @param pSecondOperand the value to multiply the values of the first operand interval with.
       * @return a superset of the possible results obtained by multiplying any value of the first
       *     operand interval with the second operand big integer value.
       */
      @Override
      public BitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        /*
         * Any finite value multiplied by zero is zero. If the bounds of this
         * interval are infinite, they are exclusive bounds, so the fact that
         * (-)infinity * 0 is undefined is not a problem: all values contained
         * in this interval are considered finite.
         */
        if (pSecondOperand.equals(BigInteger.ZERO)) {
          return BitVectorInterval.cast(
              pFirstOperand.getTypeInfo(),
              BigInteger.ZERO,
              pAllowSignedWrapAround,
              pOverflowEventHandler);
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
          return apply(
              pFirstOperand.negate(pAllowSignedWrapAround, pOverflowEventHandler),
              pSecondOperand.negate());
        }
        /*
         * Infinite bounds stay infinite, finite bounds are multiplied with
         * the factor.
         */
        BigInteger lowerBound = pFirstOperand.getLowerBound().multiply(pSecondOperand);
        BigInteger upperBound = pFirstOperand.getUpperBound().multiply(pSecondOperand);
        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            lowerBound,
            upperBound,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  /** The division operator for dividing intervals by big integers. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getDivide(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Calculates a superset of the possible results from dividing any value of the first operand
       * interval by the second operand big integer.
       *
       * <p>This will return <code>null</code> iff the second operand is zero.
       *
       * @param pFirstOperand the interval of values to divide by the second operand big integer
       *     value.
       * @param pSecondOperand the value to divide the values of this range by.
       * @return a superset of the possible results from dividing any value of the first operand
       *     interval by the given second operand big integer or <code>null</code> if <code>
       *     pSecondOperand</code> is zero.
       */
      @Override
      public @Nullable BitVectorInterval apply(
          BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
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
          return apply(
              pFirstOperand.negate(pAllowSignedWrapAround, pOverflowEventHandler),
              pSecondOperand.negate());
        }
        /*
         * Divide each finite bound by the divisor to obtain the new bounds;
         * infinite bounds stay infinite.
         */
        BigInteger lowerBound = pFirstOperand.getLowerBound().divide(pSecondOperand);
        BigInteger upperBound = pFirstOperand.getUpperBound().divide(pSecondOperand);

        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            lowerBound,
            upperBound,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  /** The modulo operator for computing the remainder of dividing intervals by big integers. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getModulo(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Computes a superset of the possible values resulting from calculating for any value <code>a
       * </code> of the first operand interval and the second operand big integer value <code>
       * pSecondOperand</code> the operation <code>a%pSecondOperand</code>.
       *
       * <p>However, if the second operand (the divisor) is zero, such a superset cannot be
       * calculated because division by zero is undefined and therefore the same applies to the
       * modulo operation; in such a case this function will return <code>null</code>.
       *
       * <p>This implementation will round towards zero and the sign of the result will only depend
       * on the sign of the first operand interval values, not on the sign of the second operand
       * (the divisor), which means that only the absolute value of the divisor is used. This is
       * also the usual behavior in C on modern machines.
       *
       * @param pFirstOperand the interval of values to be divided by the second operand.
       * @param pSecondOperand the modulo divisor.
       * @return a superset of the possible results from calculating the modulo operation between
       *     any value of the first operand interval as numerators and the second operand big
       *     integer value as divisor or <code>null</code> if the given divisor is zero.
       */
      @Override
      public BitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        // Division by zero is undefined, so null is returned
        if (pSecondOperand.equals(BigInteger.ZERO)) {
          return null;
        }
        BitVectorInfo typeInfo = pFirstOperand.getTypeInfo();
        /*
         * Only the absolute value of the divisor is considered (see
         * documentation), so a negative divisor is negated before
         * computing the result.
         */
        if (pSecondOperand.signum() < 0) {
          // We cannot negate MIN_INT without an overflow, so we handle it
          // explicitly: all values except MIN_INT are in the range of the modulo
          if (pSecondOperand.equals(pFirstOperand.getTypeInfo().getMinValue())) {
            if (pFirstOperand.contains(pSecondOperand)) {
              return BitVectorInterval.span(
                  pFirstOperand, BitVectorInterval.singleton(typeInfo, BigInteger.ZERO));
            }
            return pFirstOperand;
          }
          return apply(pFirstOperand, pSecondOperand.negate());
        }
        // x % 1 is always zero
        if (pSecondOperand.equals(BigInteger.ONE)) {
          return BitVectorInterval.singleton(typeInfo, BigInteger.ZERO);
        }

        /*
         * If this is a singleton, simply use the big-integer remainder
         * implementation.
         */
        if (pFirstOperand.isSingleton()) {
          return BitVectorInterval.cast(
              pFirstOperand.getTypeInfo(),
              pFirstOperand.getLowerBound().remainder(pSecondOperand),
              pAllowSignedWrapAround,
              pOverflowEventHandler);
        }

        // If MIN_INT is contained, handle it separately
        if (pFirstOperand.contains(typeInfo.getMinValue())) {
          BigInteger minValue = typeInfo.getMinValue();
          BigInteger minValueRemainder = minValue.remainder(pSecondOperand);
          BitVectorInterval rest =
              BitVectorInterval.singleton(typeInfo, minValue.add(BigInteger.ONE))
                  .extendToMaxValue()
                  .intersectWith(pFirstOperand);
          return BitVectorInterval.span(
              BitVectorInterval.singleton(typeInfo, minValueRemainder),
              apply(rest, pSecondOperand));
        }

        BigInteger largestPossibleValue = pSecondOperand.subtract(BigInteger.ONE);
        BitVectorInterval moduloRange = null;
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
          moduloRange =
              BitVectorInterval.cast(
                  pFirstOperand.getTypeInfo(),
                  largestPossibleValue.negate(),
                  BigInteger.ZERO,
                  pAllowSignedWrapAround,
                  pOverflowEventHandler);
          /*
           * The negative part of this interval is guaranteed to be finite and
           * the resulting range can possibly be narrowed down.
           */
          final BitVectorInterval negPart;
          /*
           * If zero is contained, the non-positive part has an upper bound
           * of zero, otherwise it must be equal to this interval because
           * both bounds of this interval are negative anyway.
           */
          if (pFirstOperand.containsZero()) {
            negPart =
                BitVectorInterval.cast(
                    pFirstOperand.getTypeInfo(),
                    pFirstOperand.getLowerBound(),
                    BigInteger.ZERO,
                    pAllowSignedWrapAround,
                    pOverflowEventHandler);
          } else {
            negPart = pFirstOperand;
          }
          /*
           * Reuse the code concerning the non-negative part by applying
           * negation to the negative part, computing its modulo result
           * and negating that result again.
           */
          moduloRange =
              apply(negPart.negate(pAllowSignedWrapAround, pOverflowEventHandler), pSecondOperand)
                  .negate(pAllowSignedWrapAround, pOverflowEventHandler);
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
          BitVectorInterval posRange =
              BitVectorInterval.cast(
                  pFirstOperand.getTypeInfo(),
                  BigInteger.ZERO,
                  largestPossibleValue,
                  pAllowSignedWrapAround,
                  pOverflowEventHandler);
          /*
           * The positive part of this interval is guaranteed to be finite and
           * the resulting range can possibly be narrowed down.
           */
          final BitVectorInterval posPart;
          /*
           * If zero is contained, the non-negative part has a lower bound
           * of zero, otherwise it must be equal to this interval because
           * both bounds of this interval are positive anyway.
           */
          if (pFirstOperand.containsZero()) {
            posPart =
                BitVectorInterval.cast(
                    pFirstOperand.getTypeInfo(),
                    BigInteger.ZERO,
                    pFirstOperand.getUpperBound(),
                    pAllowSignedWrapAround,
                    pOverflowEventHandler);
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
              posRange =
                  BitVectorInterval.cast(
                      pFirstOperand.getTypeInfo(),
                      bound1.min(bound2),
                      bound1.max(bound2),
                      pAllowSignedWrapAround,
                      pOverflowEventHandler);
            }
          }
          /*
           * Recombine the partial results. At least one of the partial results
           * must be non-null at this point by the containsPositive() or
           * containsNegative() parts, because otherwise this interval would
           * have to be [0, 0] which is covered in a previous early-return-case.
           */
          assert moduloRange != null || posRange != null;
          if (moduloRange == null) {
            moduloRange = posRange;
          } else if (posRange != null) {
            moduloRange = BitVectorInterval.span(moduloRange, posRange);
          }
        }
        return moduloRange;
      }
    };
  }

  /** The left shift operator for left shifting a simple interval by a big integer value. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getShiftLeft(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Computes an interval representing a superset of the possible values of left-shifting any
       * value contained in the first operand interval by the second operand big integer value.
       *
       * @param pFirstOperand the interval to shift by the second operand big integer value.
       * @param pSecondOperand the second interval big integer value to shift the values of the
       *     first operand interval by.
       * @return an interval representing a superset of the possible values of left-shifting any
       *     value contained in the first operand interval by the second operand big integer value.
       */
      @Override
      public BitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
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
        // Negative left shifts are not defined.
        if (pSecondOperand.signum() < 0) {
          return pFirstOperand.getTypeInfo().getRange();
        }
        /*
         * BigInteger supports shifting only for integer values. If the shift
         * distance is within the integer range, both bounds are shifted
         * to obtain the new bounds (infinite bounds stay infinite).
         */
        if (pSecondOperand.compareTo(BigInteger.valueOf(pFirstOperand.getTypeInfo().getSize()))
            <= 0) {
          BigInteger lowerBound =
              pFirstOperand.getLowerBound().shiftLeft(pSecondOperand.intValue());
          BigInteger upperBound =
              pFirstOperand.getUpperBound().shiftLeft(pSecondOperand.intValue());
          return BitVectorInterval.cast(
              pFirstOperand.getTypeInfo(),
              lowerBound,
              upperBound,
              pAllowSignedWrapAround,
              pOverflowEventHandler);
        }
        /*
         * For shifting distances larger than the size of the bit vector,
         * we assume zero is the only possible result of left shifting.
         */
        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            BigInteger.ZERO,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }

  /** The right shift operator for right shifting a simple interval by a big integer value. */
  public Operator<BitVectorInterval, BigInteger, BitVectorInterval> getShiftRight(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      /**
       * Computes an interval representing a superset of the possible values of right-shifting any
       * value contained in the first operand simple interval by the second operand big integer
       * value.
       *
       * @param pFirstOperand the simple interval to be shifted by the second operand big integer
       *     value.
       * @param pSecondOperand the value to shift the values of the first operand big integer value
       *     by.
       * @return an interval representing a superset of the possible values of right-shifting any
       *     value contained in the first operand simple interval by the second operand big integer
       *     given value.
       */
      @Override
      public BitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
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
        // Negative right shifts are not defined.
        if (pSecondOperand.signum() < 0) {
          return pFirstOperand.getTypeInfo().getRange();
        }
        /*
         * BigInteger supports shifting only for integer values. If the shift
         * distance is within the integer range, both bounds are shifted
         * to obtain the new bounds (infinite bounds stay infinite).
         */
        if (pSecondOperand.compareTo(BigInteger.valueOf(pFirstOperand.getTypeInfo().getSize()))
            <= 0) {
          BigInteger lowerBound =
              pFirstOperand.getLowerBound().shiftRight(pSecondOperand.intValue());
          BigInteger upperBound =
              pFirstOperand.getUpperBound().shiftRight(pSecondOperand.intValue());
          return BitVectorInterval.cast(
              pFirstOperand.getTypeInfo(),
              lowerBound,
              upperBound,
              pAllowSignedWrapAround,
              pOverflowEventHandler);
        }
        /*
         * For shifting distances larger than the size of the bit vector,
         * we assume zero is the only possible result of right shifting.
         */
        return BitVectorInterval.cast(
            pFirstOperand.getTypeInfo(),
            BigInteger.ZERO,
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
    };
  }
}

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
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

import java.math.BigInteger;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to two simple interval operands, producing a compound state
 * representing the result of the operation.
 */
public enum IICOperatorFactory {

  INSTANCE;

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getAdd(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        return CompoundBitVectorInterval.of(IIIOperatorFactory.INSTANCE.getAdd(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getDivide(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        BitVectorInterval result =
            IIIOperatorFactory.INSTANCE
                .getDivide(pAllowSignedWrapAround, pOverflowEventHandler)
                .apply(pFirstOperand, pSecondOperand);
        return result == null
            ? CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo())
            : CompoundBitVectorInterval.of(result);
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getModulo(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        if (pSecondOperand.isSingleton()) {
          return CompoundBitVectorInterval.of(pFirstOperand)
              .modulo(
                  pSecondOperand.getLowerBound(), pAllowSignedWrapAround, pOverflowEventHandler);
        }

        BitVectorInfo info = pFirstOperand.getTypeInfo();
        CompoundBitVectorInterval result = CompoundBitVectorInterval.bottom(info);

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
              result.unionWith(
                  applyPositiveUnknown(asPositive, pSecondOperand)
                      .negate(pAllowSignedWrapAround, pOverflowEventHandler));
        }

        if (pFirstOperand.containsPositive()) {
          BigInteger posLB = pFirstOperand.closestPositiveToZero();
          BitVectorInterval positivePart =
              pFirstOperand.hasUpperBound()
                  ? BitVectorInterval.of(info, posLB, pFirstOperand.getUpperBound())
                  : BitVectorInterval.singleton(info, posLB).extendToMaxValue();
          result = result.unionWith(applyPositiveUnknown(positivePart, pSecondOperand));
        }

        assert result != null;
        return result;
      }

      private CompoundBitVectorInterval applyPositiveUnknown(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        if (pSecondOperand.isSingleton()) {
          Operator<BitVectorInterval, BigInteger, BitVectorInterval> operator =
              ISIOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler);
          return CompoundBitVectorInterval.of(
              operator.apply(pFirstOperand, pSecondOperand.getLowerBound()));
        }

        Preconditions.checkArgument(!pFirstOperand.containsNegative());
        Preconditions.checkArgument(!pFirstOperand.containsZero());

        BitVectorInfo info = pFirstOperand.getTypeInfo();
        CompoundBitVectorInterval result = CompoundBitVectorInterval.bottom(info);

        if (pSecondOperand.containsNegative()) {
          BigInteger negUB = pSecondOperand.closestNegativeToZero();
          BitVectorInterval asPositive =
              pSecondOperand.hasLowerBound()
                  ? pSecondOperand.getLowerBound().equals(info.getMinValue())
                      ? BitVectorInterval.of(info, negUB.negate(), info.getMaxValue())
                      : BitVectorInterval.of(
                          info, negUB.negate(), pSecondOperand.getLowerBound().negate())
                  : BitVectorInterval.singleton(info, negUB.negate()).extendToMaxValue();
          result = result.unionWith(applyPositivePositive(pFirstOperand, asPositive));
        }

        if (pSecondOperand.containsPositive()) {
          BigInteger posLB = pSecondOperand.closestPositiveToZero();
          BitVectorInterval positivePart =
              pSecondOperand.hasUpperBound()
                  ? BitVectorInterval.of(info, posLB, pSecondOperand.getUpperBound())
                  : BitVectorInterval.singleton(info, posLB).extendToMaxValue();
          result = result.unionWith(applyPositivePositive(pFirstOperand, positivePart));
        }

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

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getMultiply(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
      return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        return CompoundBitVectorInterval.of(IIIOperatorFactory.INSTANCE.getMultiply(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getShiftLeft(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        /*
         * If this is top, it will stay top after any kind of shift, so the
         * identity is returned. The same applies for shifting [0] (a
         * singleton interval of zero) or shifting anything by 0.
         */
        if (pFirstOperand.isTop()
            || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
            || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
          return CompoundBitVectorInterval.of(pFirstOperand);
        }
        BitVectorInfo bitVectorInfo = pFirstOperand.getTypeInfo();
        CompoundBitVectorInterval result = CompoundBitVectorInterval.bottom(bitVectorInfo);
        /*
         * If zero is one of the possible shift distances, this interval is
         * contained in the overall result.
         */
        if (pSecondOperand.containsZero()) {
          result = result.unionWith(pFirstOperand);
        }
        /*
         * If there are negative shift distances
         * or distances larger than the bit length,
         * the result is undefined.
         */
        if (pSecondOperand.containsNegative()
            || pSecondOperand.intersectsWith(
                BitVectorInterval.singleton(
                        bitVectorInfo, BigInteger.valueOf(bitVectorInfo.getSize()))
                    .extendToMaxValue())) {
          return CompoundBitVectorInterval.of(bitVectorInfo.getRange());
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
                  BitVectorInterval.cast(
                          pFirstOperand.getTypeInfo(),
                          BigInteger.ONE,
                          pAllowSignedWrapAround,
                          pOverflowEventHandler)
                      .extendToMaxValue());
          /*
           * Shift this interval by the lower bound, then by the upper bound of
           * the positive part and span over the results.
           */
          CompoundBitVectorInterval posPartResult =
              ISCOperatorFactory.INSTANCE
                  .getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, posPart.getLowerBound());

          posPartResult =
              CompoundBitVectorInterval.span(
                  posPartResult,
                  ISCOperatorFactory.INSTANCE
                      .getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler)
                      .apply(pFirstOperand, posPart.getUpperBound()));

          result = result.unionWith(posPartResult);
        }
        return result;
      }
    };
  }

  public Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval> getShiftRight(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, BitVectorInterval pSecondOperand) {
        /*
         * If this is top, it will stay top after any kind of shift, so the
         * identity is returned. The same applies for shifting [0] (a
         * singleton interval of zero) or shifting anything by 0.
         */
        if (pFirstOperand.isTop()
            || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
            || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
          return CompoundBitVectorInterval.of(pFirstOperand);
        }
        BitVectorInfo bitVectorInfo = pFirstOperand.getTypeInfo();
        CompoundBitVectorInterval result = CompoundBitVectorInterval.bottom(bitVectorInfo);
        /*
         * If zero is one of the possible shift distances, this interval is
         * contained in the overall result.
         */
        if (pSecondOperand.containsZero()) {
          result = result.unionWith(pFirstOperand);
        }
        /*
         * If there are negative shift distances
         * or distances larger than the bit length,
         * the result is undefined.
         */
        if (pSecondOperand.containsNegative()
            || pSecondOperand.intersectsWith(
                BitVectorInterval.singleton(
                        bitVectorInfo, BigInteger.valueOf(bitVectorInfo.getSize()))
                    .extendToMaxValue())) {
          return CompoundBitVectorInterval.of(bitVectorInfo.getRange());
        }
        /*
         * If there are positive shift distances, extract the positive part
         * of the shift distances, shift this interval by both the lower
         * and the upper bound of that positive part and include the result
         * in the overall result.
         */
        if (pSecondOperand.containsPositive()) {
          BitVectorInterval posPart = pSecondOperand.getPositivePart();
          /*
           * Shift this interval by the lower bound, then by the upper bound of
           * the positive part and span over the results.
           */
          CompoundBitVectorInterval posPartResult =
              ISCOperatorFactory.INSTANCE
                  .getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, posPart.getLowerBound());

          posPartResult =
              CompoundBitVectorInterval.span(
                  posPartResult,
                  ISCOperatorFactory.INSTANCE
                      .getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler)
                      .apply(pFirstOperand, posPart.getUpperBound()));

          result = result.unionWith(posPartResult);
        }
        return result;
      }
    };
  }

}

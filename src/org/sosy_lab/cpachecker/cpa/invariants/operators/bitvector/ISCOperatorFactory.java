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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval and a scalar value, producing a
 * compound state representing the result of the operation.
 */
public enum ISCOperatorFactory {

  INSTANCE;

  /**
   * The addition operator for adding scalar values to intervals.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getAdd(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        return CompoundBitVectorInterval.of(ISIOperatorFactory.INSTANCE.getAdd(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

  /**
   * The division operator for dividing intervals by scalar values.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getDivide(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
      return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        return CompoundBitVectorInterval.of(ISIOperatorFactory.INSTANCE.getDivide(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

  /**
   * The modulo operator for computing the remainders of dividing intervals by scalar values.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getModulo(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pValue) {
        // Division by zero is undefined, so bottom is returned
        if (pValue.signum() == 0) {
          return CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        }
        /*
         * Only the absolute value of the divisor is considered (see
         * documentation), so a negative divisor is negated before
         * computing the result.
         */
        if (pValue.signum() < 0) {
          return apply(pFirstOperand, pValue.negate());
        }
        /*
         * If the interval is a singleton, simply use the big integer
         * remainder implementation.
         */
        if (pFirstOperand.isSingleton()) {
          return CompoundBitVectorInterval.singleton(
              pFirstOperand.getTypeInfo(), pFirstOperand.getLowerBound().remainder(pValue));
        }
        BigInteger largestPossibleValue = pValue.subtract(BigInteger.ONE);
        CompoundBitVectorInterval result =
            CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        if (pFirstOperand.containsZero()) {
          result =
              result.unionWith(
                  BitVectorInterval.singleton(pFirstOperand.getTypeInfo(), BigInteger.ZERO));
        }
        if (pFirstOperand.containsNegative()) {
          CompoundBitVectorInterval negRange =
              CompoundBitVectorInterval.cast(
                  pFirstOperand.getTypeInfo(),
                  largestPossibleValue.negate(),
                  BigInteger.ZERO,
                  pAllowSignedWrapAround,
                  pOverflowEventHandler);
          if (pFirstOperand.hasLowerBound()) {
            BitVectorInterval negPart = pFirstOperand.getNegativePart();
            BitVectorInterval negatedNegPart =
                negPart.negate(pAllowSignedWrapAround, pOverflowEventHandler);
            if (!negatedNegPart.containsNegative()) {
              negRange =
                  apply(negatedNegPart, pValue)
                      .negate(pAllowSignedWrapAround, pOverflowEventHandler);
            }
          }
          result = result.unionWith(negRange);
        }
        if (pFirstOperand.containsPositive()) {
          CompoundBitVectorInterval posRange =
              CompoundBitVectorInterval.cast(
                  pFirstOperand.getTypeInfo(),
                  BigInteger.ZERO,
                  largestPossibleValue,
                  pAllowSignedWrapAround,
                  pOverflowEventHandler);
          if (pFirstOperand.hasUpperBound()) {
            BitVectorInterval posPart = pFirstOperand.getPositivePart();
            BigInteger posPartLength = posPart.size();
            if (posPartLength.compareTo(pValue) < 0) {
              BigInteger quotient = posPart.getUpperBound().divide(pValue);
              BigInteger modBorder = quotient.multiply(pValue);
              BigInteger nextModBorder = modBorder.add(pValue);
              /*
               * If posPart is between modBorder and modBorder+pValue-1, the
               * possible values resulting from performing the modulo operation
               * on the considered part of this interval can be narrowed down.
               */
              if (modBorder.compareTo(posPart.getLowerBound()) <= 0
                  && nextModBorder.compareTo(posPart.getUpperBound()) >= 0) {
                BigInteger bound1 = posPart.getLowerBound().remainder(pValue);
                BigInteger bound2 = posPart.getUpperBound().remainder(pValue);
                posRange =
                    CompoundBitVectorInterval.cast(
                        pFirstOperand.getTypeInfo(),
                        bound1.min(bound2),
                        bound1.max(bound2),
                        pAllowSignedWrapAround,
                        pOverflowEventHandler);
              } else if (modBorder.compareTo(posPart.getLowerBound()) > 0
                  && modBorder.compareTo(posPart.getUpperBound()) < 0) {
                BitVectorInterval posPart1 =
                    BitVectorInterval.cast(
                        pFirstOperand.getTypeInfo(),
                        posPart.getLowerBound(),
                        modBorder.subtract(BigInteger.ONE),
                        pAllowSignedWrapAround,
                        pOverflowEventHandler);
                BitVectorInterval posPart2 =
                    BitVectorInterval.cast(
                        pFirstOperand.getTypeInfo(),
                        modBorder,
                        posPart.getUpperBound(),
                        pAllowSignedWrapAround,
                        pOverflowEventHandler);
                posRange = apply(posPart1, pValue).unionWith(apply(posPart2, pValue));
              }
            }
          }
          result = result.unionWith(posRange);
        }
        return result;
      }
    };
  }

  /**
   * The multiplication operator for multiplying intervals with scalar values.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getMultiply(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        return CompoundBitVectorInterval.of(ISIOperatorFactory.INSTANCE.getMultiply(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

  /**
   * The left shift operator for left shifting intervals by scalar values.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getShiftLeft(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        return CompoundBitVectorInterval.of(ISIOperatorFactory.INSTANCE.getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

  /**
   * The right shift operator for right shifting intervals by scalar values.
   */
  public Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval> getShiftRight(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
      return new Operator<BitVectorInterval, BigInteger, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, BigInteger pSecondOperand) {
        return CompoundBitVectorInterval.of(ISIOperatorFactory.INSTANCE.getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler).apply(pFirstOperand, pSecondOperand));
      }

    };
  }

}

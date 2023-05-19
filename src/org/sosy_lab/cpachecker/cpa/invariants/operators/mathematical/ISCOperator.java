// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundMathematicalInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can be applied to a simple
 * interval and a scalar value, producing a compound state representing the result of the operation.
 */
public enum ISCOperator
    implements Operator<SimpleInterval, BigInteger, CompoundMathematicalInterval> {

  /** The addition operator for adding scalar values to simple intervals. */
  ADD {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundMathematicalInterval.of(ISIOperator.ADD.apply(pFirstOperand, pSecondOperand));
    }
  },

  /** The division operator for dividing simple intervals by scalar values. */
  DIVIDE {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundMathematicalInterval.of(
          ISIOperator.DIVIDE.apply(pFirstOperand, pSecondOperand));
    }
  },

  /** The modulo operator for computing the remainders of dividing intervals by scalar values. */
  MODULO {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, BigInteger pValue) {
      // Division by zero is undefined, so bottom is returned
      if (pValue.signum() == 0) {
        return CompoundMathematicalInterval.bottom();
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
        return CompoundMathematicalInterval.singleton(
            pFirstOperand.getLowerBound().remainder(pValue));
      }
      BigInteger largestPossibleValue = pValue.subtract(BigInteger.ONE);
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      if (pFirstOperand.containsNegative()) {
        CompoundMathematicalInterval negRange =
            CompoundMathematicalInterval.of(
                SimpleInterval.of(largestPossibleValue.negate(), BigInteger.ZERO));
        if (pFirstOperand.hasLowerBound()) {
          final SimpleInterval negPart;
          if (pFirstOperand.containsZero()) {
            negPart = SimpleInterval.of(pFirstOperand.getLowerBound(), BigInteger.ZERO);
          } else {
            negPart = pFirstOperand;
          }
          negRange = apply(negPart.negate(), pValue).negate();
        }
        result = result.unionWith(negRange);
      }
      if (pFirstOperand.containsPositive()) {
        CompoundMathematicalInterval posRange =
            CompoundMathematicalInterval.of(
                SimpleInterval.of(BigInteger.ZERO, largestPossibleValue));
        if (pFirstOperand.hasUpperBound()) {
          final SimpleInterval posPart;
          if (pFirstOperand.containsZero()) {
            posPart = SimpleInterval.of(BigInteger.ZERO, pFirstOperand.getUpperBound());
          } else {
            posPart = pFirstOperand;
          }
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
                  CompoundMathematicalInterval.of(
                      SimpleInterval.of(bound1.min(bound2), bound1.max(bound2)));
            } else if (modBorder.compareTo(posPart.getLowerBound()) > 0
                && modBorder.compareTo(posPart.getUpperBound()) < 0) {
              SimpleInterval posPart1 =
                  SimpleInterval.of(posPart.getLowerBound(), modBorder.subtract(BigInteger.ONE));
              SimpleInterval posPart2 = SimpleInterval.of(modBorder, posPart.getUpperBound());
              posRange = apply(posPart1, pValue).unionWith(apply(posPart2, pValue));
            }
          }
        }
        result = result.unionWith(posRange);
      }
      return result;
    }
  },

  /** The multiplication operator for multiplying simple intervals with scalar values. */
  MULTIPLY {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundMathematicalInterval.of(
          ISIOperator.MULTIPLY.apply(pFirstOperand, pSecondOperand));
    }
  },

  /** The left shift operator for left shifting simple intervals by scalar values. */
  SHIFT_LEFT {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundMathematicalInterval.of(
          ISIOperator.SHIFT_LEFT.apply(pFirstOperand, pSecondOperand));
    }
  },

  /** The right shift operator for right shifting simple intervals by scalar values. */
  SHIFT_RIGHT {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundMathematicalInterval.of(
          ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, pSecondOperand));
    }
  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the scalar operand to apply the operator to.
   * @return the compound state resulting from applying the first operand to the second operand.
   */
  @Override
  public abstract CompoundMathematicalInterval apply(
      SimpleInterval pFirstOperand, BigInteger pSecondOperand);
}

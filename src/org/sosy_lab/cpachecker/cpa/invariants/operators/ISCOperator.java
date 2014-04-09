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
package org.sosy_lab.cpachecker.cpa.invariants.operators;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval and a scalar value, producing a
 * compound state representing the result of the operation.
 */
public enum ISCOperator implements Operator<SimpleInterval, BigInteger, CompoundInterval> {

  /**
   * The addition operator for adding scalar values to simple intervals.
   */
  ADD {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundInterval.of(ISIOperator.ADD.apply(pFirstOperand, pSecondOperand));
    }

  },

  /**
   * The division operator for dividing simple intervals by scalar values.
   */
  DIVIDE {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundInterval.of(ISIOperator.DIVIDE.apply(pFirstOperand, pSecondOperand));
    }

  },

  /**
   * The modulo operator for computing the remainders of dividing intervals by scalar values.
   */
  MODULO {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pValue) {
      // Division by zero is undefined, so bottom is returned
      if (pValue.signum() == 0) {
        return CompoundInterval.bottom();
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
        return CompoundInterval.singleton(pFirstOperand.getLowerBound().remainder(pValue));
      }
      BigInteger largestPossibleValue = pValue.subtract(BigInteger.ONE);
      CompoundInterval result = CompoundInterval.bottom();
      if (pFirstOperand.containsNegative()) {
        CompoundInterval negRange = CompoundInterval.of(SimpleInterval.of(largestPossibleValue.negate(), BigInteger.ZERO));
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
        CompoundInterval posRange = CompoundInterval.of(SimpleInterval.of(BigInteger.ZERO, largestPossibleValue));
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
              posRange = CompoundInterval.of(SimpleInterval.of(bound1.min(bound2), bound1.max(bound2)));
            } else if (modBorder.compareTo(posPart.getLowerBound()) > 0
                && modBorder.compareTo(posPart.getUpperBound()) < 0) {
              SimpleInterval posPart1 = SimpleInterval.of(posPart.getLowerBound(), modBorder.subtract(BigInteger.ONE));
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

  /**
   * The multiplication operator for multiplying simple intervals with scalar values.
   */
  MULTIPLY {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundInterval.of(ISIOperator.MULTIPLY.apply(pFirstOperand, pSecondOperand));
    }

  },

  /**
   * The left shift operator for left shifting simple intervals by scalar values.
   */
  SHIFT_LEFT {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundInterval.of(ISIOperator.SHIFT_LEFT.apply(pFirstOperand, pSecondOperand));
    }

  },

  /**
   * The right shift operator for right shifting simple intervals by scalar values.
   */
  SHIFT_RIGHT {

    @Override
    public CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand) {
      return CompoundInterval.of(ISIOperator.SHIFT_RIGHT.apply(pFirstOperand, pSecondOperand));
    }

  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the scalar operand to apply the operator to.
   * @return the compound state resulting from applying the first operand to the
   * second operand.
   */
  @Override
  public abstract CompoundInterval apply(SimpleInterval pFirstOperand, BigInteger pSecondOperand);

}

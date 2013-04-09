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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tocompound;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;

/**
 * This class represents the modulo operator for computing the
 * remainders of dividing intervals by scalar values.
 */
enum ModuloOperator implements ISCOperator {

  /**
   * The singleton instance of this operator.
   */
  INSTANCE;

  @Override
  public CompoundState apply(SimpleInterval pFirstOperand, BigInteger pValue) {
    // Division by zero is undefined, so bottom is returned
    if (pValue.signum() == 0) {
      return CompoundState.bottom();
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
      return CompoundState.singleton(pFirstOperand.getLowerBound().remainder(pValue));
    }
    BigInteger largestPossibleValue = pValue.subtract(BigInteger.ONE);
    CompoundState result = CompoundState.bottom();
    if (pFirstOperand.containsNegative()) {
      CompoundState negRange = CompoundState.of(SimpleInterval.of(largestPossibleValue.negate(), BigInteger.ZERO));
      if (pFirstOperand.hasLowerBound()) {
        final SimpleInterval negPart;
        if (pFirstOperand.containsZero()) {
          negPart = SimpleInterval.of(pFirstOperand.getLowerBound(), BigInteger.ZERO);
        } else {
          negPart = pFirstOperand;
        }
        negRange = apply(negPart.negate(), pValue).negate();
      }
      result.unionWith(negRange);
    }
    if (pFirstOperand.containsPositive()) {
      CompoundState posRange = CompoundState.of(SimpleInterval.of(BigInteger.ZERO, largestPossibleValue));
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
            posRange = CompoundState.of(SimpleInterval.of(bound1.min(bound2), bound1.max(bound2)));
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

}
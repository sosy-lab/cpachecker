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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tointerval;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.IntervalHelper;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tointerval.ISIOperator;

/**
 * The instance of this class is an operator for dividing a simple interval
 * by a second interval to produce another simple interval.
 */
enum DivideOperator implements IIIOperator {

  INSTANCE;

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
      return ISIOperator.DIVIDE_OPERATOR.apply(pFirstOperand, pSecondOperand.getLowerBound());
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

}

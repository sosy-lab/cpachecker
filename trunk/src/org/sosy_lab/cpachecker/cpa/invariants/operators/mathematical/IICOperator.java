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
package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundMathematicalInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

import java.math.BigInteger;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to two simple interval operands, producing a compound state
 * representing the result of the operation.
 */
public enum IICOperator implements Operator<SimpleInterval, SimpleInterval, CompoundMathematicalInterval> {

  ADD {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      return CompoundMathematicalInterval.of(IIIOperator.ADD.apply(pFirstOperand, pSecondOperand));
    }
  },

  DIVIDE {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      return CompoundMathematicalInterval.of(IIIOperator.DIVIDE.apply(pFirstOperand, pSecondOperand));
    }

  },

  MODULO {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      if (!pSecondOperand.hasLowerBound() || !pSecondOperand.hasUpperBound()) {
        return CompoundMathematicalInterval.of(pFirstOperand);
      }
      if (pSecondOperand.isSingleton()) {
        return CompoundMathematicalInterval.of(
            ISIOperator.MODULO.apply(pFirstOperand, pSecondOperand.getLowerBound()));
      }

      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      if (pFirstOperand.containsNegative()) {
        BigInteger negUB = pFirstOperand.closestNegativeToZero();
        SimpleInterval asPositive =
            pFirstOperand.hasLowerBound()
                ? SimpleInterval.of(negUB.negate(), pFirstOperand.getLowerBound().negate())
                : SimpleInterval.singleton(negUB.negate()).extendToPositiveInfinity();
        result = result.unionWith(applyPositiveUnknown(asPositive, pSecondOperand).negate());
      }

      if (pFirstOperand.containsPositive()) {
        BigInteger posLB = pFirstOperand.closestPositiveToZero();
        SimpleInterval positivePart =
            pFirstOperand.hasUpperBound()
                ? SimpleInterval.of(posLB, pFirstOperand.getUpperBound())
                : SimpleInterval.singleton(posLB).extendToPositiveInfinity();
        result = result.unionWith(applyPositiveUnknown(positivePart, pSecondOperand));
      }

      return result;
    }

    private CompoundMathematicalInterval applyPositiveUnknown(
        SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      if (pSecondOperand.isSingleton()) {
        return CompoundMathematicalInterval.of(
            ISIOperator.MODULO.apply(pFirstOperand, pSecondOperand.getLowerBound()));
      }

      Preconditions.checkArgument(!pFirstOperand.containsNegative());
      Preconditions.checkArgument(!pFirstOperand.containsZero());

      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      if (pSecondOperand.containsNegative()) {
        BigInteger negUB = pSecondOperand.closestNegativeToZero();
        SimpleInterval asPositive =
            pSecondOperand.hasLowerBound()
                ? SimpleInterval.of(negUB.negate(), pSecondOperand.getLowerBound().negate())
                : SimpleInterval.singleton(negUB.negate()).extendToPositiveInfinity();
        result = result.unionWith(applyPositivePositive(pFirstOperand, asPositive));
      }

      if (pSecondOperand.containsPositive()) {
        BigInteger posLB = pSecondOperand.closestPositiveToZero();
        SimpleInterval positivePart =
            pSecondOperand.hasUpperBound()
                ? SimpleInterval.of(posLB, pSecondOperand.getUpperBound())
                : SimpleInterval.singleton(posLB).extendToPositiveInfinity();
        result = result.unionWith(applyPositivePositive(pFirstOperand, positivePart));
      }

      return result;
    }

    private CompoundMathematicalInterval applyPositivePositive(
        SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      if (pSecondOperand.isSingleton()) {
        return CompoundMathematicalInterval.of(
            ISIOperator.MODULO.apply(pFirstOperand, pSecondOperand.getLowerBound()));
      }

      Preconditions.checkArgument(!pFirstOperand.containsNegative());
      Preconditions.checkArgument(!pFirstOperand.containsZero());
      Preconditions.checkArgument(!pSecondOperand.containsNegative());
      Preconditions.checkArgument(!pSecondOperand.containsZero());

      if (pFirstOperand.hasUpperBound()
          && pSecondOperand.getLowerBound().compareTo(pFirstOperand.getUpperBound()) > 0) {
        return CompoundMathematicalInterval.of(pFirstOperand);
      }
      BigInteger resultUpperBound;
      if (pFirstOperand.hasUpperBound()) {
        if (pSecondOperand.hasUpperBound()) {
          resultUpperBound = pFirstOperand.getUpperBound().min(pSecondOperand.getUpperBound());
        } else {
          resultUpperBound = pFirstOperand.getUpperBound();
        }
      } else if (pSecondOperand.hasUpperBound()) {
        resultUpperBound = pSecondOperand.getUpperBound();
      } else {
        return CompoundMathematicalInterval.of(
            SimpleInterval.singleton(BigInteger.ZERO).extendToPositiveInfinity());
      }
      return CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, resultUpperBound));
    }

  },

  MULTIPLY {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      return CompoundMathematicalInterval.of(IIIOperator.MULTIPLY.apply(pFirstOperand, pSecondOperand));
    }

  },

  SHIFT_LEFT {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop()
          || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
          || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
        return CompoundMathematicalInterval.of(pFirstOperand);
      }
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      /*
       * If zero is one of the possible shift distances, this interval is
       * contained in the overall result.
       */
      if (pSecondOperand.containsZero()) {
        result = result.unionWith(pFirstOperand);
      }
      /*
       * If there are negative shift distances, extract the negative part
       * of the shift distances from the given interval, right shift this
       * interval by that part and include the result in the overall result.
       */
      if (pSecondOperand.containsNegative()) {
        SimpleInterval negPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE.negate()).extendToNegativeInfinity());
        result = result.unionWith(SHIFT_RIGHT.apply(pFirstOperand, negPart.negate()));
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
         * the positive part and span over the results.
         */
        CompoundMathematicalInterval posPartResult = ISCOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getLowerBound());
        if (posPart.hasUpperBound()) {
          posPartResult = CompoundMathematicalInterval.span(posPartResult, ISCOperator.SHIFT_LEFT.apply(pFirstOperand, posPart.getUpperBound()));
        } else {
          // Left shifting by infinitely large values results in infinity.
          if (pFirstOperand.containsPositive()) {
            posPartResult = posPartResult.extendToMaxValue();
          }
          if (pFirstOperand.containsNegative()) {
            posPartResult = posPartResult.extendToMinValue();
          }
        }
        result = result.unionWith(posPartResult);
      }
      return result;
    }

  },

  SHIFT_RIGHT {

    @Override
    public CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand) {
      /*
       * If this is top, it will stay top after any kind of shift, so the
       * identity is returned. The same applies for shifting [0] (a
       * singleton interval of zero) or shifting anything by 0.
       */
      if (pFirstOperand.isTop()
          || (pSecondOperand.isSingleton() && pSecondOperand.containsZero())
          || (pFirstOperand.isSingleton() && pFirstOperand.containsZero())) {
        return CompoundMathematicalInterval.of(pFirstOperand);
      }
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      /*
       * If zero is one of the possible shift distances, this interval is
       * contained in the overall result.
       */
      if (pSecondOperand.containsZero()) {
        result = result.unionWith(pFirstOperand);
      }
      /*
       * If there are negative shift distances, extract the negative part
       * of the shift distances from the given interval, left shift this
       * interval by that part and include the result in the overall result.
       */
      if (pSecondOperand.containsNegative()) {
        SimpleInterval negPart = pSecondOperand.intersectWith(SimpleInterval.singleton(BigInteger.ONE.negate()).extendToNegativeInfinity());
        result = result.unionWith(SHIFT_LEFT.apply(pFirstOperand, negPart.negate()));
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
         * the positive part and span over the results.
         */
        CompoundMathematicalInterval posPartResult = ISCOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getLowerBound());
        if (posPart.hasUpperBound()) {
          posPartResult = CompoundMathematicalInterval.span(posPartResult, ISCOperator.SHIFT_RIGHT.apply(pFirstOperand, posPart.getUpperBound()));
        } else {
          // Shifting by infinitely large values will result in zero.
          posPartResult = CompoundMathematicalInterval.span(posPartResult, CompoundMathematicalInterval.singleton(BigInteger.ZERO));
        }
        result = result.unionWith(posPartResult);
      }
      return result;
    }

  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the first simple interval operand to apply the operator to.
   * @param pSecondOperand the second simple interval operand to apply the operator to.
   *
   * @return the compound state resulting from applying the first operand to the
   * second operand.
   */
  @Override
  public abstract CompoundMathematicalInterval apply(SimpleInterval pFirstOperand, SimpleInterval pSecondOperand);

}

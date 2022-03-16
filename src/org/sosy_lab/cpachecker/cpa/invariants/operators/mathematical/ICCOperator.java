// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundMathematicalInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can be applied to a simple
 * interval and a compound state, producing a compound state representing the result of the
 * operation.
 */
public enum ICCOperator
    implements
        Operator<SimpleInterval, CompoundMathematicalInterval, CompoundMathematicalInterval> {

  /** The addition operator for adding compound states to simple intervals. */
  ADD {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      return pSecondOperand.add(pFirstOperand);
    }
  },

  /** The division operator for dividing simple intervals by compound states. */
  DIVIDE {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      for (SimpleInterval interval : pSecondOperand.getIntervals()) {
        CompoundMathematicalInterval current = IICOperator.DIVIDE.apply(pFirstOperand, interval);
        if (current != null) {
          result = result.unionWith(current);
          if (result.isTop()) {
            return result;
          }
        }
      }
      return result;
    }
  },

  /** The modulo operator for computing the remainders of dividing intervals by compound states. */
  MODULO {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      for (SimpleInterval interval : pSecondOperand.getIntervals()) {
        CompoundMathematicalInterval current = IICOperator.MODULO.apply(pFirstOperand, interval);
        if (current != null) {
          result = result.unionWith(current);
          if (result.isTop()) {
            return result;
          }
        }
      }
      return result;
    }
  },

  /** The multiplication operator for multiplying simple intervals with compound states. */
  MULTIPLY {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      return pSecondOperand.multiply(pFirstOperand);
    }
  },

  /** The left shift operator for left shifting simple intervals by compound states. */
  SHIFT_LEFT {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      for (SimpleInterval interval : pSecondOperand.getIntervals()) {
        CompoundMathematicalInterval current =
            IICOperator.SHIFT_LEFT.apply(pFirstOperand, interval);
        if (current != null) {
          result = result.unionWith(current);
          if (result.isTop()) {
            return result;
          }
        }
      }
      return result;
    }
  },

  /** The right shift operator for right shifting simple intervals by compound states. */
  SHIFT_RIGHT {

    @Override
    public CompoundMathematicalInterval apply(
        SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand) {
      CompoundMathematicalInterval result = CompoundMathematicalInterval.bottom();
      for (SimpleInterval interval : pSecondOperand.getIntervals()) {
        CompoundMathematicalInterval current =
            IICOperator.SHIFT_RIGHT.apply(pFirstOperand, interval);
        if (current != null) {
          result = result.unionWith(current);
          if (result.isTop()) {
            return result;
          }
        }
      }
      return result;
    }
  };

  /**
   * Applies this operator to the given operands.
   *
   * @param pFirstOperand the simple interval operand to apply the operator to.
   * @param pSecondOperand the compound state operand to apply the operator to.
   * @return the compound state resulting from applying the first operand to the second operand.
   */
  @Override
  public abstract CompoundMathematicalInterval apply(
      SimpleInterval pFirstOperand, CompoundMathematicalInterval pSecondOperand);
}

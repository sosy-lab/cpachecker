// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can be applied to a simple
 * interval and a compound state, producing a compound state representing the result of the
 * operation.
 */
public enum ICCOperatorFactory {
  INSTANCE;

  /** The addition operator for adding compound states to simple intervals. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getAdd(
      final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return (pFirstOperand, pSecondOperand) ->
        pSecondOperand.add(pFirstOperand, pAllowSignedWrapAround, pOverflowEventHandler);
  }

  /** The division operator for dividing simple intervals by compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getDivide(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        CompoundBitVectorInterval result =
            CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        for (BitVectorInterval interval : pSecondOperand.getBitVectorIntervals()) {
          CompoundBitVectorInterval current =
              IICOperatorFactory.INSTANCE
                  .getDivide(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, interval);
          if (current != null) {
            result = result.unionWith(current);
            if (result.containsAllPossibleValues()) {
              return result;
            }
          }
        }
        return result;
      }
    };
  }

  /** The modulo operator for computing the remainders of dividing intervals by compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getModulo(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        CompoundBitVectorInterval result =
            CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        for (BitVectorInterval interval : pSecondOperand.getBitVectorIntervals()) {
          CompoundBitVectorInterval current =
              IICOperatorFactory.INSTANCE
                  .getModulo(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, interval);
          if (current != null) {
            result = result.unionWith(current);
            if (result.containsAllPossibleValues()) {
              return result;
            }
          }
        }
        return result;
      }
    };
  }

  /** The multiplication operator for multiplying simple intervals with compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getMultiply(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return (pFirstOperand, pSecondOperand) ->
        pSecondOperand.multiply(pFirstOperand, pAllowSignedWrapAround, pOverflowEventHandler);
  }

  /** The left shift operator for left shifting simple intervals by compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getShiftLeft(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        CompoundBitVectorInterval result =
            CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        for (BitVectorInterval interval : pSecondOperand.getBitVectorIntervals()) {
          CompoundBitVectorInterval current =
              IICOperatorFactory.INSTANCE
                  .getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, interval);
          if (current != null) {
            result = result.unionWith(current);
            if (result.containsAllPossibleValues()) {
              return result;
            }
          }
        }
        return result;
      }
    };
  }

  /** The right shift operator for right shifting simple intervals by compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getShiftRight(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<>() {

      @Override
      public CompoundBitVectorInterval apply(
          BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        CompoundBitVectorInterval result =
            CompoundBitVectorInterval.bottom(pFirstOperand.getTypeInfo());
        for (BitVectorInterval interval : pSecondOperand.getBitVectorIntervals()) {
          CompoundBitVectorInterval current =
              IICOperatorFactory.INSTANCE
                  .getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler)
                  .apply(pFirstOperand, interval);
          if (current != null) {
            result = result.unionWith(current);
            if (result.containsAllPossibleValues()) {
              return result;
            }
          }
        }
        return result;
      }
    };
  }
}

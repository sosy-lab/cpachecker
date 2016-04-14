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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.OverflowEventHandler;
import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;

/**
 * Instances of implementations of this interface are operators that can
 * be applied to a simple interval and a compound state, producing a
 * compound state representing the result of the operation.
 */
public enum ICCOperatorFactory {

  INSTANCE;

  /**
   * The addition operator for adding compound states to simple intervals.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getAdd(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        return pSecondOperand.add(pFirstOperand, pAllowSignedWrapAround, pOverflowEventHandler);
      }

    };
  }

  /**
   * The division operator for dividing simple intervals by compound states.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getDivide(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

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

  /**
   * The modulo operator for computing the remainders of dividing intervals by compound states.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getModulo(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

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

  /**
   * The multiplication operator for multiplying simple intervals with compound states.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getMultiply(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

      @Override
      public CompoundBitVectorInterval apply(BitVectorInterval pFirstOperand, CompoundBitVectorInterval pSecondOperand) {
        return pSecondOperand.multiply(pFirstOperand, pAllowSignedWrapAround, pOverflowEventHandler);
      }

    };
  }

  /**
   * The left shift operator for left shifting simple intervals by compound states.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getShiftLeft(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

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

  /**
   * The right shift operator for right shifting simple intervals by compound states.
   */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval> getShiftRight(final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return new Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>() {

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

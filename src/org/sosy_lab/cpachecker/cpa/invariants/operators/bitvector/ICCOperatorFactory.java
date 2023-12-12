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

  /** The multiplication operator for multiplying simple intervals with compound states. */
  public Operator<BitVectorInterval, CompoundBitVectorInterval, CompoundBitVectorInterval>
      getMultiply(
          final boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return (pFirstOperand, pSecondOperand) ->
        pSecondOperand.multiply(pFirstOperand, pAllowSignedWrapAround, pOverflowEventHandler);
  }
}

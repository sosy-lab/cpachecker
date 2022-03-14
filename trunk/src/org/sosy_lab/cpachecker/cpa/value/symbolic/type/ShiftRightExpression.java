// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link BinarySymbolicExpression} representing the 'shift right' operation.
 *
 * <p>There is no differentiation between signed and unsigned shifts.
 */
public final class ShiftRightExpression extends BinarySymbolicExpression {

  private static final long serialVersionUID = -9068365554036095329L;

  public enum ShiftType {
    SIGNED,
    UNSIGNED
  }

  private final ShiftType shiftType;

  ShiftRightExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType,
      ShiftType pShiftType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
    shiftType = pShiftType;
  }

  ShiftRightExpression(
      final ShiftType pShiftType,
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
    shiftType = pShiftType;
  }

  @Override
  public ShiftRightExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new ShiftRightExpression(
        shiftType,
        getOperand1(),
        getOperand2(),
        getType(),
        getCalculationType(),
        pRepresentedLocation);
  }

  public boolean isSigned() {
    return shiftType == ShiftType.SIGNED;
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    if (isSigned()) {
      return ">>";
    } else {
      return ">>>";
    }
  }
}

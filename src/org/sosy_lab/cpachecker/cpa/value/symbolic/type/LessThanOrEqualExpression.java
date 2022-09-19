// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** {@link BinarySymbolicExpression} representing the 'less than/equal' operation. */
public final class LessThanOrEqualExpression extends BinarySymbolicExpression
    implements BinaryConstraint {

  private static final long serialVersionUID = -805938567569160336L;

  LessThanOrEqualExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  LessThanOrEqualExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
  }

  @Override
  public LessThanOrEqualExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new LessThanOrEqualExpression(
        getOperand1(), getOperand2(), getType(), getCalculationType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "<=";
  }
}

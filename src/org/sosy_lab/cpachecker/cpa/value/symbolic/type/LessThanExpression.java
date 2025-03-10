// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** {@link BinarySymbolicExpression} representing the 'less than' operation. */
public final class LessThanExpression extends BinarySymbolicExpression implements BinaryConstraint {

  @Serial private static final long serialVersionUID = -711307360731984235L;

  LessThanExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  LessThanExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
  }

  private LessThanExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final AbstractState pAbstractState) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pAbstractState);
  }

  @Override
  public LessThanExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new LessThanExpression(
        getOperand1(), getOperand2(), getType(), getCalculationType(), pRepresentedLocation);
  }

  @Override
  public SymbolicExpression copyForState(AbstractState pCurrentState) {
    return new LessThanExpression(
        getOperand1(), getOperand2(), getType(), getCalculationType(), pCurrentState);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "<";
  }
}

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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** {@link BinarySymbolicExpression} representing the 'logical and' operation. */
public final class LogicalAndExpression extends BinarySymbolicExpression {

  @Serial private static final long serialVersionUID = 8274694737043926521L;

  LogicalAndExpression(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pExpressionType,
      Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  LogicalAndExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
  }

  private LogicalAndExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final AbstractState pAbstractState) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pAbstractState);
  }

  @Override
  public LogicalAndExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new LogicalAndExpression(
        getOperand1(), getOperand2(), getType(), getCalculationType(), pRepresentedLocation);
  }

  @Override
  public SymbolicExpression copyForState(AbstractState pCurrentState) {
    return new LogicalAndExpression(
        getOperand1(), getOperand2(), getType(), getCalculationType(), pCurrentState);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "&&";
  }
}

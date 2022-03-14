// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.UnaryConstraint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** {@link UnarySymbolicExpression} representing the 'logical not' operation. */
public final class LogicalNotExpression extends UnarySymbolicExpression implements UnaryConstraint {

  private static final long serialVersionUID = 1538819641715577876L;

  LogicalNotExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  LogicalNotExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public LogicalNotExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new LogicalNotExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "!";
  }
}

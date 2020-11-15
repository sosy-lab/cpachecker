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

/** {@link UnarySymbolicExpression} representing negation of a value. */
public final class NegationExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = 6785934572402802380L;

  public NegationExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  NegationExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public NegationExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new NegationExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "-";
  }
}

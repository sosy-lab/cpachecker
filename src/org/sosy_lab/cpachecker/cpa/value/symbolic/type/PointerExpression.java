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
 * {@link SymbolicExpression} that represents a pointer expression.
 *
 * <p>This can be a classic pointer expression like <code>*p</code> in C or a reference as in Java.
 */
public final class PointerExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = -7348176261979912313L;

  PointerExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  PointerExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public PointerExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new PointerExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "*";
  }
}

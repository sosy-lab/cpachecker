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

/** Representation of ampersand expression in C. Example: <code>int a; int b = &a</code> */
public final class AddressOfExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = -4583179464566332040L;

  AddressOfExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  AddressOfExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public AddressOfExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new AddressOfExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "&";
  }
}

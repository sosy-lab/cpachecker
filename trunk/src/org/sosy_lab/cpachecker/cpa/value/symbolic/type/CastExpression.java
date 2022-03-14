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

/** {@link SymbolicExpression} representing a cast. */
public final class CastExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = 3928318112889309143L;

  /**
   * Create a new <code>CastExpression</code> with the given operand and {@link Type}.
   *
   * <p>The given <code>Type</code> represents the type the operand should be casted to. No checks
   * for compatibility between operand type and
   *
   * @param pOperand the operand to cast
   * @param pType the type to cast the operand to
   */
  CastExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  /**
   * Create a new <code>CastExpression</code> with the given operand and {@link Type} representing
   * the value of the given memory location.
   *
   * <p>The given <code>Type</code> represents the type the operand should be casted to. No checks
   * for compatibility between operand type and
   *
   * @param pOperand the operand to cast
   * @param pType the type to cast the operand to
   * @param pRepresentedLocation the memory location this cast expression represents
   */
  CastExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public CastExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new CastExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "Cast[" + getType() + ", " + getOperand() + "]";
  }

  @Override
  public String getOperationString() {
    return "Cast>";
  }
}

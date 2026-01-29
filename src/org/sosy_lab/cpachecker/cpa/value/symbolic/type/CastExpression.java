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

/** {@link SymbolicExpression} representing a cast. */
public final class CastExpression extends UnarySymbolicExpression {

  @Serial private static final long serialVersionUID = 3928318112889309143L;

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

  private CastExpression(
      final SymbolicExpression pOperand, final Type pType, final AbstractState pAbstractState) {
    super(pOperand, pType, pAbstractState);
  }

  /**
   * Creates a {@link SymbolicExpression} representing the cast of the given value to the given
   * type. If multiple casts occur sequentially, it is tried to simplify them.
   *
   * @param pValue the value to cast
   * @param pTargetType the type to cast to
   * @return a <code>SymbolicExpression</code> representing the cast of the given value to the given
   *     type
   */
  public static SymbolicExpression of(SymbolicValue pValue, Type pTargetType) {
    Type canonicalTargetType = getCanonicalType(pTargetType);

    SymbolicExpression operand;

    if (pValue instanceof AddressExpression) {
      // TODO:
      // We want to cast AddressExpressions only if the cast type is smaller than signed int
      // (default for pointers) because only then the cast would make a difference.
      // In all smaller cases we need to make sure that there are 2 possibilities later on.
      // One where due to the cast the values still match and one where they don't.
      return (SymbolicExpression) pValue;
    }

    if (!(pValue instanceof SymbolicExpression symbolicExpression)) {
      return ConstantSymbolicExpression.of(pValue, canonicalTargetType);
    } else {
      operand = symbolicExpression;
    }

    if (operand.getType().equals(canonicalTargetType)) {
      return operand;

    } else {
      return new CastExpression(operand, canonicalTargetType);
    }
  }

  @Override
  public CastExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new CastExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public SymbolicExpression copyForState(AbstractState pCurrentState) {
    return new CastExpression(getOperand(), getType(), pCurrentState);
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

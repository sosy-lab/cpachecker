// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** {@link SymbolicExpression} that represents a single constant value of a specific type. */
public final class ConstantSymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = 8720056661933193765L;

  private final Value value;
  private final Type type;

  /**
   * Create a new <code>ConstantSymbolicExpression</code> object with the given value and type.
   *
   * @param pValue the value of the new object
   * @param pType the type of the value of the new object
   */
  public ConstantSymbolicExpression(Value pValue, Type pType) {
    value = pValue;
    type = pType;
  }

  /**
   * Create a new <code>ConstantSymbolicExpression</code> object with the given value and type
   * representing the given memory location.
   *
   * @param pValue the value of the new object
   * @param pType the type of the value of the new object
   * @param pRepresentedLocation the memory location this symbolic expression represents
   */
  public ConstantSymbolicExpression(
      final Value pValue, final Type pType, final MemoryLocation pRepresentedLocation) {
    super(pRepresentedLocation);
    value = pValue;
    type = pType;
  }

  @Override
  public SymbolicExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new ConstantSymbolicExpression(value, type, pRepresentedLocation);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().orElseThrow().toString();
    } else {
      return toString();
    }
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  public Value getValue() {
    return value;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public boolean isTrivial() {
    return !(value instanceof SymbolicValue);
  }

  @Override
  public String toString() {
    return "SymEx[" + value + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantSymbolicExpression that = (ConstantSymbolicExpression) o;

    return super.equals(o) && Objects.equals(type, that.type) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 71;
    result = type != null ? 31 * result + type.hashCode() : result;
    return result;
  }
}

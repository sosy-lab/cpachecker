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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Represents a unary {@link SymbolicExpression}. Represents all <code>SymbolicExpression</code>s
 * that consist of only one operand.
 */
public abstract class UnarySymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = -2727356523115713518L;

  private final SymbolicExpression operand;
  private final Type type;

  UnarySymbolicExpression(SymbolicExpression pOperand, Type pType) {
    operand = pOperand;
    type = pType;
  }

  UnarySymbolicExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation) {
    super(pRepresentedLocation);
    operand = pOperand;
    type = pType;
  }

  @Override
  public Type getType() {
    return type;
  }

  public SymbolicExpression getOperand() {
    return operand;
  }

  @Override
  public boolean isTrivial() {
    return operand.isTrivial();
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnarySymbolicExpression that = (UnarySymbolicExpression) o;

    return super.equals(that) && operand.equals(that.operand) && type.equals(that.type);
  }

  @Override
  public final int hashCode() {
    return super.hashCode() + Objects.hash(getClass(), operand, type);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().orElseThrow().toString();

    } else {
      return getOperationString() + operand.getRepresentation();
    }
  }

  @Override
  public String toString() {
    return getOperationString() + "(" + operand + ")";
  }

  public abstract String getOperationString();
}

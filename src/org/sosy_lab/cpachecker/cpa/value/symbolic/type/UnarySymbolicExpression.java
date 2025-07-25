// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Represents a unary {@link SymbolicExpression}. Represents all <code>SymbolicExpression</code>s
 * that consist of only one operand.
 */
public abstract sealed class UnarySymbolicExpression extends SymbolicExpression
    permits AddressOfExpression,
        BinaryNotExpression,
        CastExpression,
        LogicalNotExpression,
        NegationExpression,
        PointerExpression {

  @Serial private static final long serialVersionUID = -2727356523115713518L;

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

  UnarySymbolicExpression(
      final SymbolicExpression pOperand, final Type pType, final AbstractState pAbstractState) {
    super(pAbstractState);
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
    // Comment to silence CI
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnarySymbolicExpression that = (UnarySymbolicExpression) o;

    if (hasAbstractState()
        && that.hasAbstractState()
        && getAbstractState() instanceof SMGState thisState
        && that.getAbstractState() instanceof SMGState thatState) {
      // SMG values do not really care about the type, as the SMG knows their types and checks
      // that as well
      return SMGState.areValuesEqual(thisState, operand, thatState, that.operand);
    }

    return super.equals(that) && operand.equals(that.operand) && type.equals(that.type);
  }

  @Override
  public final int hashCode() {
    return super.hashCode() + Objects.hash(getClass().getCanonicalName(), operand, type);
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

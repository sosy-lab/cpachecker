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
import java.util.Optional;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** An expression containing {@link SymbolicValue}s. */
public abstract sealed class SymbolicExpression implements SymbolicValue
    permits AddressExpression,
        BinarySymbolicExpression,
        ConstantSymbolicExpression,
        UnarySymbolicExpression {

  @Serial private static final long serialVersionUID = 2228733300503173691L;

  private final @Nullable MemoryLocation representedLocation;

  // For some analysis, we need the state to decide equality of abstract expressions instead of
  // MemLoc
  private final @Nullable AbstractState stateWithRepresentation;

  protected SymbolicExpression(final MemoryLocation pRepresentedLocation) {
    representedLocation = pRepresentedLocation;
    stateWithRepresentation = null;
  }

  protected SymbolicExpression(final AbstractState pStateWithRepresentation) {
    representedLocation = null;
    stateWithRepresentation = pStateWithRepresentation;
  }

  protected SymbolicExpression() {
    representedLocation = null;
    stateWithRepresentation = null;
  }

  @Override
  public abstract SymbolicExpression copyForLocation(MemoryLocation pRepresentedLocation);

  public abstract SymbolicExpression copyForState(AbstractState pCurrentState);

  @Override
  public Optional<MemoryLocation> getRepresentedLocation() {
    return Optional.ofNullable(representedLocation);
  }

  /**
   * Accepts the given {@link SymbolicValueVisitor}.
   *
   * @param pVisitor the visitor to accept
   * @param <VisitorReturnT> the return type of the visitor's specific <code>visit</code> method
   * @return the value returned by the visitor's <code>visit</code> method
   */
  @Override
  public abstract <VisitorReturnT> VisitorReturnT accept(
      SymbolicValueVisitor<VisitorReturnT> pVisitor);

  /**
   * Returns the expression type of this <code>SymbolicExpression</code>.
   *
   * @return the expression type of this <code>SymbolicExpression</code>
   */
  public abstract Type getType();

  /**
   * Returns whether this <code>SymbolicExpression</code> only contains explicit values.
   *
   * @return <code>true</code> if this <code>SymbolicExpression</code> is always true and does only
   *     contain explicit values. if the expression contains any SymbolicIdentifier, this method
   *     returns <code>false</code>.
   */
  public abstract boolean isTrivial();

  public AbstractState getAbstractState() {
    return Optional.ofNullable(stateWithRepresentation).orElseThrow();
  }

  public boolean hasAbstractState() {
    return stateWithRepresentation != null;
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public OptionalLong asLong(CType type) {
    return OptionalLong.empty();
  }

  @Override
  public int hashCode() {
    // TODO: for all values this needs to be overridden with new hashcodes based on value + state
    return Objects.hashCode(representedLocation);
  }

  @Override
  public boolean equals(final Object pObj) {
    // This equals should be overridden for state dependant values, as the states might be equal,
    // but the values not. -> Override always!
    return pObj instanceof SymbolicExpression symbolicExpression
        && Objects.equals(representedLocation, symbolicExpression.representedLocation);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Locates all {@link SymbolicIdentifier}s contained in a {@link SymbolicValue}.
 */
public class SymbolicIdentifierLocator
    implements SymbolicValueVisitor<Set<SymbolicIdentifier>> {

  private final static SymbolicIdentifierLocator SINGLETON = new SymbolicIdentifierLocator();

  private SymbolicIdentifierLocator() {
    // DO NOTHING
  }

  public static SymbolicIdentifierLocator getInstance() {
    return SINGLETON;
  }

  @Override
  public Set<SymbolicIdentifier> visit(final SymbolicIdentifier pValue) {
    return ImmutableSet.of(pValue);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final ConstantSymbolicExpression pExpression) {
    final Value containedValue = pExpression.getValue();

    if (containedValue instanceof SymbolicValue) {
      return ((SymbolicValue) containedValue).accept(this);

    } else {
      return ImmutableSet.of();
    }
  }

  private Set<SymbolicIdentifier> handleBinaryExpression(
      final BinarySymbolicExpression pExpression
  ) {
    final Set<SymbolicIdentifier> identifiersOnLeft = pExpression.getOperand1().accept(this);
    final Set<SymbolicIdentifier> identifiersOnRight = pExpression.getOperand2().accept(this);

    Set<SymbolicIdentifier> identifiersInBoth = new HashSet<>(identifiersOnLeft);

    identifiersInBoth.addAll(identifiersOnRight);

    return ImmutableSet.copyOf(identifiersInBoth);
  }

  private Set<SymbolicIdentifier> handleUnaryExpression(final UnarySymbolicExpression pExpression) {
    return pExpression.getOperand().accept(this);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final AdditionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final SubtractionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final MultiplicationExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final DivisionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final ModuloExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final BinaryAndExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final BinaryNotExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final BinaryOrExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final BinaryXorExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final ShiftRightExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final ShiftLeftExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final LogicalNotExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final LessThanOrEqualExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final LessThanExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final EqualsExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final LogicalOrExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final LogicalAndExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final CastExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final PointerExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final AddressOfExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<SymbolicIdentifier> visit(final NegationExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
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
 * Locates all {@link ConstantSymbolicExpression}s with {@link SymbolicIdentifier}s inside,
 * contained in a {@link SymbolicValue}.
 */
public class ConstantSymbolicExpressionLocator
    implements SymbolicValueVisitor<Set<ConstantSymbolicExpression>> {

  private static final ConstantSymbolicExpressionLocator SINGLETON =
      new ConstantSymbolicExpressionLocator();

  private ConstantSymbolicExpressionLocator() {
    // DO NOTHING
  }

  public static ConstantSymbolicExpressionLocator getInstance() {
    return SINGLETON;
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final SymbolicIdentifier pValue) {
    return ImmutableSet.of();
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final ConstantSymbolicExpression pExpression) {
    final Value containedValue = pExpression.getValue();

    if (containedValue instanceof SymbolicIdentifier) {
      return ImmutableSet.of(pExpression);

    } else if (containedValue instanceof SymbolicValue symVal) {
      return symVal.accept(this);

    } else {
      return ImmutableSet.of();
    }
  }

  private Set<ConstantSymbolicExpression> handleBinaryExpression(
      final BinarySymbolicExpression pExpression) {
    final Set<ConstantSymbolicExpression> identifiersOnLeft =
        pExpression.getOperand1().accept(this);
    final Set<ConstantSymbolicExpression> identifiersOnRight =
        pExpression.getOperand2().accept(this);

    // all of the produced sets in this visitor are immutable sets,
    // so the union will also be immutable
    // and there is no need to generate a separate ImmutableSet.
    return Sets.union(identifiersOnLeft, identifiersOnRight);
  }

  private Set<ConstantSymbolicExpression> handleUnaryExpression(
      final UnarySymbolicExpression pExpression) {
    return pExpression.getOperand().accept(this);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final AdditionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final SubtractionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final MultiplicationExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final DivisionExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final ModuloExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final BinaryAndExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final BinaryNotExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final BinaryOrExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final BinaryXorExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final ShiftRightExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final ShiftLeftExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final LogicalNotExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final LessThanOrEqualExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final LessThanExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final EqualsExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final LogicalOrExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final LogicalAndExpression pExpression) {
    return handleBinaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final CastExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final PointerExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final AddressOfExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }

  @Override
  public Set<ConstantSymbolicExpression> visit(final NegationExpression pExpression) {
    return handleUnaryExpression(pExpression);
  }
}

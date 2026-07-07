// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cpa.interval.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A segment boundary in a {@link FunArray}, holding a set of {@link NormalFormExpression}s that are
 * all equal to the same concrete program value at a given point in time. A bound separates two
 * adjacent segments of the array; all expressions in the bound denote the same index value under
 * the current abstract state.
 *
 * <p>A bound may contain more than one expression — for example, after {@code j = i}, the bound at
 * position {@code i} may contain both {@code i} and {@code j}. A bound with no expressions is empty
 * and must be removed by {@link FunArray#removeEmptyBounds()}. This also merges the adjacent
 * segments.
 *
 * @param expressions the set of normal-form expressions that are mutually equal at this boundary.
 */
public record Bound(Set<NormalFormExpression> expressions) {
  public Bound {
    expressions = ImmutableSet.copyOf(expressions);
  }

  /**
   * Creates a bound containing exactly one expression.
   *
   * @param expression the single expression for this boundary.
   */
  public Bound(NormalFormExpression expression) {
    this(ImmutableSet.of(expression));
  }

  /**
   * Returns a new bound that reflects the assignment {@code changedVariableId = newValues}. For
   * each expression in this bound, the method substitutes the changed variable with each element of
   * {@code newValues} via {@link #adaptSingleExpression}. Expressions that lose all variable
   * occurrences as a result are dropped, and new expressions are added for each applicable new
   * value.
   *
   * @param changedVariableId the variable whose value was updated.
   * @param newValues the set of normal-form expressions representing the new possible values of the
   *     changed variable.
   * @return a new bound with expressions updated to the post-assignment state.
   */
  public Bound adaptForChangedVariableValues(
      CIdExpression changedVariableId, Set<NormalFormExpression> newValues) {

    Set<NormalFormExpression> modifiedExpressions = new HashSet<>();

    for (NormalFormExpression expression : expressions()) {
      for (NormalFormExpression newValue : newValues) {
        modifiedExpressions.addAll(adaptSingleExpression(expression, changedVariableId, newValue));
      }
    }

    return new Bound(modifiedExpressions);
  }

  /**
   * Adapts a single expression given that {@code changedVariableId} now equals {@code newValue}.
   * The four cases are:
   *
   * <ul>
   *   <li>Expression uses the same variable as {@code newValue} and also uses {@code
   *       changedVariableId}: shift the expression's constant by {@code -newValue.constant}.
   *   <li>Expression uses the same variable as {@code newValue} but not {@code changedVariableId}:
   *       keep the expression and add a new one with {@code changedVariableId} offset
   *       appropriately.
   *   <li>Expression uses {@code changedVariableId} but not {@code newValue}'s variable: the
   *       expression is stale — drop it.
   *   <li>Expression uses neither: keep it unchanged.
   * </ul>
   *
   * @param expression the expression to adapt.
   * @param changedVariableId the variable that was assigned.
   * @param newValue the normal-form expression representing the new value.
   * @return the adapted set of expressions (may be empty, one, or two elements).
   */
  private static ImmutableSet<NormalFormExpression> adaptSingleExpression(
      NormalFormExpression expression,
      CIdExpression changedVariableId,
      NormalFormExpression newValue) {
    if (expression.containsVariable(newValue.getVariable())) {
      if (expression.containsVariable(changedVariableId)) {
        return ImmutableSet.of(expression.increase(-newValue.getConstant()));
      } else {
        return ImmutableSet.of(
            expression,
            new NormalFormExpression(
                changedVariableId, expression.getConstant() - newValue.getConstant()));
      }
    } else {
      if (expression.containsVariable(changedVariableId)) {
        return ImmutableSet.of();
      } else {
        return ImmutableSet.of(expression);
      }
    }
  }

  /** Returns {@code true} if this bound contains {@code expression}. */
  public boolean contains(NormalFormExpression expression) {
    return expressions().contains(expression);
  }

  /** Returns {@code true} if any expression in this bound satisfies {@code predicate}. */
  public boolean contains(Predicate<NormalFormExpression> predicate) {
    return expressions().stream().anyMatch(predicate);
  }

  /** Returns {@code true} if this bound contains no expressions. */
  public boolean isEmpty() {
    return expressions.isEmpty();
  }

  /** Returns the union of a collection of bounds — the set of all their expressions combined. */
  public static Bound union(Collection<Bound> bounds) {
    return new Bound(
        bounds.stream()
            .flatMap(e -> e.expressions().stream())
            .collect(ImmutableSet.toImmutableSet()));
  }

  /** Returns the union of this bound with {@code other}. */
  public Bound union(Bound other) {
    return union(other.expressions);
  }

  /** Returns the union of this bound with the given set of expressions. */
  public Bound union(Set<NormalFormExpression> otherExpressions) {
    ImmutableSet<NormalFormExpression> newExpressions =
        Stream.concat(this.expressions.stream(), otherExpressions.stream())
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  /** Returns the intersection of this bound with {@code other}. */
  public Bound intersection(Bound other) {
    return intersection(other.expressions);
  }

  /** Returns the intersection of this bound with the given set of expressions. */
  public Bound intersection(Set<NormalFormExpression> otherExpressions) {
    ImmutableSet<NormalFormExpression> newExpressions =
        this.expressions.stream()
            .filter(otherExpressions::contains)
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  /**
   * Returns the set difference of this bound minus {@code other} (elements in {@code this} not in
   * {@code other}).
   */
  public Bound difference(Bound other) {
    return difference(other.expressions);
  }

  /** Returns the set difference of this bound minus the given expression set. */
  public Bound difference(Set<NormalFormExpression> otherExpressions) {
    ImmutableSet<NormalFormExpression> newExpressions =
        this.expressions.stream()
            .filter(o -> !otherExpressions.contains(o))
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  /**
   * Returns the relative complement {@code other \ this} — the expressions in {@code other} that
   * are not present in this bound. Note that the operand order is the reverse of {@link
   * #difference}: it is {@code other} minus {@code this}, not {@code this} minus {@code other}.
   */
  public Bound relativeComplement(Bound other) {
    return relativeComplement(other.expressions);
  }

  /**
   * Returns the relative complement {@code otherExpressions \ this} — the expressions in {@code
   * otherExpressions} that are not present in this bound.
   */
  public Bound relativeComplement(Set<NormalFormExpression> otherExpressions) {
    ImmutableSet<NormalFormExpression> newExpressions =
        otherExpressions.stream()
            .filter(o -> !this.expressions.contains(o))
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  /**
   * Returns {@code true} if any expression in this bound is strictly greater than {@code other}
   * under the current abstract state.
   *
   * @param other the expression to compare against.
   * @param visitor the expression value visitor for the current abstract state.
   * @return {@code true} if at least one expression in this bound is greater than {@code other}.
   * @throws UnrecognizedCodeException if {@code other} cannot be evaluated.
   */
  public boolean isGreaterThan(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterThan(v));
  }

  /**
   * Returns {@code true} if any expression in this bound is greater than or equal to {@code other}
   * under the current abstract state.
   *
   * @param other the expression to compare against.
   * @param visitor the expression value visitor for the current abstract state.
   * @return {@code true} if at least one expression in this bound is greater than or equal to
   *     {@code other}.
   * @throws UnrecognizedCodeException if {@code other} cannot be evaluated.
   */
  public boolean isGreaterOrEqualThan(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterOrEqualThan(v) || u.equals(v));
  }

  /**
   * Returns {@code true} if any expression in this bound is equal to {@code other} under the
   * current abstract state.
   *
   * @param other the expression to compare against.
   * @param visitor the expression value visitor for the current abstract state.
   * @return {@code true} if at least one expression in this bound equals {@code other}.
   * @throws UnrecognizedCodeException if {@code other} cannot be evaluated.
   */
  public boolean isEqualTo(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isEqualTo(v) || u.equals(v));
  }

  /**
   * Evaluates {@code predicate} between the interval value of each expression in this bound and the
   * interval value of {@code other}. Returns {@code true} if the predicate holds for at least one
   * expression. Used to implement the comparison methods.
   *
   * @param other the expression to compare against.
   * @param visitor the expression value visitor for the current abstract state.
   * @param predicate the comparison predicate applied to interval pairs.
   * @return {@code true} if the predicate holds for at least one expression in this bound.
   * @throws UnrecognizedCodeException if {@code other} cannot be evaluated.
   */
  private boolean compare(
      NormalFormExpression other,
      ExpressionValueVisitor visitor,
      BiPredicate<Interval, Interval> predicate)
      throws UnrecognizedCodeException {
    Interval otherValue = other.toInterval(visitor);
    for (NormalFormExpression expression : expressions) {
      Interval value;
      try {
        value = expression.toInterval(visitor);
      } catch (UnrecognizedCodeException exception) {
        value = Interval.EMPTY;
      }
      if (predicate.test(value, otherValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "{%s}"
        .formatted(
            String.join(
                " ", expressions.stream().map(NormalFormExpression::toString).sorted().toList()));
  }
}

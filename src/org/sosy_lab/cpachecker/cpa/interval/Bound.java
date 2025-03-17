// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A segment bound in a {@link FunArray}.
 *
 * @param expressions the expressions contained within.
 */
public record Bound(Set<NormalFormExpression> expressions) {
  public Bound {
    expressions = ImmutableSet.copyOf(expressions);
  }

  public Bound(NormalFormExpression expression) {
    this(ImmutableSet.of(expression));
  }

  //  public Bound adaptForChangedVariableValues(
  //          String changedVariableRef,
  //          Set<CExpression> newValues
  //  ) {
  //    var modifiedExpressions = expressions.stream().flatMap(
  //            expression -> newValues.stream().flatMap(newValue -> {
  //              if (expression.containsVariable(newValue.varRef())) {
  //                if (expression.containsVariable(changedVariableRef)) {
  //                  return Stream.of(expression.increase(-newValue.constant()));
  //                } else {
  //                  return Stream.of(
  //                          expression,
  //                          new CExpression(changedVariableRef, expression.constant() -
  // newValue.constant())
  //                  );
  //                }
  //              } else {
  //                if (expression.containsVariable(changedVariableRef)) {
  //                  return Stream.of();
  //                } else {
  //                  return Stream.of(expression);
  //                }
  //              }
  //            })
  //    ).collect(Collectors.toSet());
  //
  //    return new Bound(modifiedExpressions);
  //
  //    throw new RuntimeException("Not yet implemented");
  //  }

  //  /**
  //   * Removes all expressions containing the specified variable.
  //   *
  //   * @param varRef the variable.
  //   * @return the modified bound.
  //   */
  //  public Bound removeVariableOccurrences(String varRef) {
  //    var modifiedExpressions = expressions.stream()
  //            .filter(e -> !e.containsVariable(varRef))
  //            .collect(Collectors.toSet());
  //    return new Bound(modifiedExpressions);
  //    throw new RuntimeException("Not yet implemented");
  //  }

  public boolean contains(NormalFormExpression expression) {
    return expressions().stream().anyMatch(e -> e.equals(expression));
  }

  public boolean contains(Predicate<NormalFormExpression> predicate) {
    return expressions().stream().anyMatch(predicate);
  }

  public boolean isEmpty() {
    return expressions.isEmpty();
  }

  public Bound increase(long amount) {
    return new Bound(transformedImmutableSetCopy(expressions, e -> e.add(amount)));
  }

  public static Bound union(Collection<Bound> bounds) {
    return new Bound(
        bounds.stream()
            .flatMap(e -> e.expressions().stream())
            .collect(ImmutableSet.toImmutableSet()));
  }

  public Bound union(Bound other) {
    return union(other.expressions);
  }

  public Bound union(Set<NormalFormExpression> otherExpressions) {
    var newExpressions =
        Stream.concat(this.expressions.stream(), otherExpressions.stream())
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  public Bound intersection(Bound other) {
    return intersection(other.expressions);
  }

  public Bound intersection(Set<NormalFormExpression> otherExpressions) {
    var newExpressions =
        this.expressions.stream()
            .filter(otherExpressions::contains)
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  public Bound difference(Bound other) {
    return difference(other.expressions);
  }

  public Bound difference(Set<NormalFormExpression> otherExpressions) {
    var newExpressions =
        this.expressions.stream()
            .filter(o -> !otherExpressions.contains(o))
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  public Bound relativeComplement(Bound other) {
    return relativeComplement(other.expressions);
  }

  public Bound relativeComplement(Set<NormalFormExpression> otherExpressions) {
    var newExpressions =
        otherExpressions.stream()
            .filter(o -> !this.expressions.contains(o))
            .collect(ImmutableSet.toImmutableSet());
    return new Bound(newExpressions);
  }

  public boolean isGreaterThan(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterThan(v));
  }

  public boolean isGreaterOrEqualThan(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterOrEqualThan(v));
  }

  public boolean isEqualTo(NormalFormExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isEqualTo(v));
  }

  private boolean compare(
      NormalFormExpression other, ExpressionValueVisitor visitor, BiPredicate<Interval, Interval> predicate)
      throws UnrecognizedCodeException {
    Interval otherValue = other.toInterval(visitor);
    return expressions.stream()
        .map(
            e -> {
              try {
                return e.toInterval(visitor);
              } catch (UnrecognizedCodeException exception) {
                return Interval.EMPTY;
              }
            })
        .anyMatch(e -> predicate.test(e, otherValue));
  }

  @Override
  public String toString() {
    return "{%s}"
        .formatted(
            String.join(" ", expressions.stream().map(NormalFormExpression::toString).sorted().toList()));
  }
}

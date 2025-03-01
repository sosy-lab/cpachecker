package org.sosy_lab.cpachecker.cpa.interval;

import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.incrementExpression;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A segment bound in a {@link FunArray}.
 *
 * @param expressions the expressions contained within.
 */
public record Bound(Set<CExpression> expressions) {
  public Bound {
    expressions = Set.copyOf(expressions);
  }

  public Bound(CExpression expression) {
    this(Set.of(expression));
  }

  public Bound adaptForChangedVariableValues(
          String changedVariableRef,
          Set<CExpression> newValues
  ) {
//    var modifiedExpressions = expressions.stream().flatMap(
//            expression -> newValues.stream().flatMap(newValue -> {
//              if (expression.containsVariable(newValue.varRef())) {
//                if (expression.containsVariable(changedVariableRef)) {
//                  return Stream.of(expression.increase(-newValue.constant()));
//                } else {
//                  return Stream.of(
//                          expression,
//                          new CExpression(changedVariableRef, expression.constant() - newValue.constant())
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

    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Removes all expressions containing the specified variable.
   *
   * @param varRef the variable.
   * @return the modified bound.
   */
  public Bound removeVariableOccurrences(String varRef) {
//    var modifiedExpressions = expressions.stream()
//            .filter(e -> !e.containsVariable(varRef))
//            .collect(Collectors.toSet());
//    return new Bound(modifiedExpressions);
    throw new RuntimeException("Not yet implemented");
  }

  public boolean contains(CExpression expression) {
    return expressions().stream().anyMatch(e -> e.equals(expression));
  }

  public boolean contains(Predicate<CExpression> predicate) {
    return expressions().stream().anyMatch(predicate);
  }

  public boolean isEmpty() {
    return expressions.isEmpty();
  }

  public Bound increase(long amount) {
    return new Bound(expressions.stream()
            .map(e -> incrementExpression(e))
            .collect(Collectors.toSet()));
  }

  public static Bound union(Collection<Bound> bounds) {
    return new Bound(
            bounds.stream()
                    .flatMap(e -> e.expressions().stream())
                    .collect(Collectors.toSet())
    );
  }

  public Bound union(Bound other) {
    return union(other.expressions);
  }

  public Bound union(Set<CExpression> otherExpressions) {
    var newExpressions = Stream.concat(
            this.expressions.stream(),
            otherExpressions.stream()
    ).collect(Collectors.toSet());
    return new Bound(newExpressions);
  }

  public Bound intersection(Bound other) {
    return intersection(other.expressions);
  }

  public Bound intersection(Set<CExpression> otherExpressions) {
    var newExpressions = this.expressions.stream()
            .filter(otherExpressions::contains)
            .collect(Collectors.toSet());
    return new Bound(newExpressions);
  }

  public Bound difference(Bound other) {
    return difference(other.expressions);
  }

  public Bound difference(Set<CExpression> otherExpressions) {
    var newExpressions = this.expressions.stream()
            .filter(o -> !otherExpressions.contains(o))
            .collect(Collectors.toSet());
    return new Bound(newExpressions);
  }

  public Bound relativeComplement(Bound other) {
    return relativeComplement(other.expressions);
  }

  public Bound relativeComplement(Set<CExpression> otherExpressions) {
    var newExpressions = otherExpressions.stream()
            .filter(o -> !this.expressions.contains(o))
            .collect(Collectors.toSet());
    return new Bound(newExpressions);
  }

  public boolean isGreaterThan(CExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterThan(v));
  }

  public boolean isGreaterOrEqualThan(CExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isGreaterOrEqualThan(v));
  }

  public boolean isEqualTo(CExpression other, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return compare(other, visitor, (u, v) -> u.isEqualTo(v));
  }

  private boolean compare(CExpression other, ExpressionValueVisitor visitor, BiPredicate<Interval, Interval> predicate) throws UnrecognizedCodeException {
    Interval otherValue = other.accept(visitor);
    return expressions.stream().map(e -> {
      try {
        return e.accept(visitor);
      } catch (UnrecognizedCodeException pE) {
        return Interval.EMPTY;
      }
    }).anyMatch(e -> predicate.test(e, otherValue));
  }

  @Override
  public String toString() {
    return "{%s}".formatted(
            String.join(" ", expressions.stream().map(CExpression::toASTString).sorted().toList()));
  }
}

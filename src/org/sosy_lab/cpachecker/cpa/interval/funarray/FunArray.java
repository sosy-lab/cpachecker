// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.interval.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayUnification.UnifyResult;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A FunArray to represent an Array in abstract interpretation static analysis. See Patrick Cousot,
 * Radhia Cousot, and Francesco Logozzo. 2011. A parametric segmentation functor for fully automatic
 * and scalable array content analysis. SIGPLAN Not. 46, 1 (January 2011), 105–118. <a
 * href="https://doi.org/10.1145/1925844.1926399">https://doi.org/10.1145/1925844.1926399</a>
 *
 * @param bounds the FunArray's segment bounds.
 * @param values the FunArray's values.
 * @param emptiness a list determining whether a segment might be empty.
 */
public record FunArray(
    ImmutableList<Bound> bounds, ImmutableList<Interval> values, ImmutableList<Boolean> emptiness)
    implements Serializable, LatticeAbstractState<FunArray> {

  @Serial private static final long serialVersionUID = 7169472946910382516L;

  public static final FunArray BOTTOM =
      new FunArray(
          ImmutableList.of(
              new Bound(new NormalFormExpression(0)), new Bound(new NormalFormExpression(1))),
          ImmutableList.of(Interval.EMPTY),
          ImmutableList.of(false));

  public FunArray {
    checkArgument(bounds.size() >= 2, "FunArray requires at least two bounds.");

    checkArgument(
        values.size() == bounds.size() - 1,
        "Number of segment values does not match up with count of bound count. Needs to be"
            + "exactly one less.");

    checkArgument(
        emptiness.size() == bounds.size() - 1,
        "Number of emptiness values does not match up with count of bound count. Needs to be"
            + "exactly one less.");

    // Check if there are duplicate expressions in bounds
    List<NormalFormExpression> expressions =
        bounds.stream().flatMap(e -> e.expressions().stream()).toList();
    if (expressions.size() != expressions.stream().distinct().count()) {
      throw new IllegalArgumentException(
          "Given list of bounds contains duplicate expressions: %s".formatted(bounds));
    }
  }

  public FunArray(List<Bound> pBounds, List<Interval> pValues, List<Boolean> pEmptiness) {
    this(
        ImmutableList.copyOf(pBounds),
        ImmutableList.copyOf(pValues),
        ImmutableList.copyOf(pEmptiness));
  }

  public FunArray(Set<NormalFormExpression> lengthExpressions) {
    this(new Bound(lengthExpressions));
  }

  public FunArray(Bound length) {
    this(
        ImmutableList.of(new Bound(new NormalFormExpression(0)), length),
        ImmutableList.of(Interval.UNBOUND),
        ImmutableList.of(true));
  }

  public static FunArray ofInitializerList(
      List<CInitializer> initializers, ExpressionValueVisitor visitor) {
    List<Interval> values = new ArrayList<>();
    for (CInitializer e : initializers) {
      if (e instanceof CInitializerExpression cInitializerExpression) {
        try {
          values.add(cInitializerExpression.getExpression().accept(visitor));
        } catch (UnrecognizedCodeException exception) {
          values.add(Interval.UNBOUND);
        }
      } else {
        values.add(Interval.UNBOUND);
      }
    }

    if (initializers.isEmpty()) {
      return BOTTOM;
    }

    List<Bound> bounds = new ArrayList<>();
    for (int i = 0; i <= initializers.size(); i++) {
      bounds.add(new Bound(new NormalFormExpression(i)));
    }

    List<Boolean> emptiness = Collections.nCopies(initializers.size(), false);

    return new FunArray(bounds, values, emptiness);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bounds.size(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      if (values().size() <= i) {
        sb.append("%s%s".formatted(bounds.get(i), emptiness.get(i - 1) ? "?" : ""));
      } else if (i == 0) {
        sb.append("%s %s".formatted(bounds.get(i), values.get(i)));
      } else {
        sb.append(
            "%s%s %s".formatted(bounds.get(i), emptiness.get(i - 1) ? "?" : "", values.get(i)));
      }
    }
    return sb.toString();
  }

  public FunArray adaptToVariableAssignment(
      CIdExpression changedVariable, Set<NormalFormExpression> expressions) {
    ArrayList<Bound> newBounds =
        new ArrayList<>(
            bounds.stream()
                .map(b -> b.adaptForChangedVariableValues(changedVariable, expressions))
                .toList());
    ArrayList<Interval> newValues = new ArrayList<>(values);
    ArrayList<Boolean> newEmptiness = new ArrayList<>(emptiness);

    return new FunArray(newBounds, newValues, newEmptiness);
  }

  public FunArray removeEmptyBounds() {
    ArrayList<Bound> newBounds = new ArrayList<>(bounds);
    ArrayList<Interval> newValues = new ArrayList<>(values);
    ArrayList<Boolean> newEmptiness = new ArrayList<>(emptiness);

    int i = 1;
    while (i < newBounds.size()) {
      if (newBounds.get(i).isEmpty()) {
        if (newBounds.size() <= 2) {
          return BOTTOM;
        }
        joinValueWithPredecessor(newValues, i);
        newBounds.remove(i);
        newEmptiness.set(i - 1, newEmptiness.get(i - 1) && newEmptiness.get(i));
        newEmptiness.remove(i);
        continue;
      }
      i++;
    }
    return new FunArray(newBounds, newValues, newEmptiness);
  }

  public FunArray insert(
      NormalFormExpression index, Interval value, ExpressionValueVisitor visitor) {
    return insert(ImmutableSet.of(index), value, visitor);
  }

  /**
   * Inserts a value into the FunArray.
   *
   * @param indeces the leading bound expressions for the new value.
   * @param value the value to be inserted.
   * @return the modified Segmentation.
   */
  public FunArray insert(
      Set<NormalFormExpression> indeces, Interval value, ExpressionValueVisitor visitor) {
    if (indeces.isEmpty()) {
      return this;
    }
    ImmutableSet<NormalFormExpression> trailingIndeces =
        transformedImmutableSetCopy(indeces, e -> e.add(1L));
    int greatestLowerBoundIndex = getRightmostLowerBoundIndex(indeces, visitor);
    int leastUpperBoundIndex = getLeastUpperBoundIndex(trailingIndeces, visitor);

    if (greatestLowerBoundIndex >= values.size()) {
      return this;
    }

    final Bound greatestLowerBound = bounds.get(greatestLowerBoundIndex);
    final Bound leastUpperBound = bounds.get(leastUpperBoundIndex);

    boolean leftAdjacent = false;
    for (NormalFormExpression e : indeces) {
      try {
        if (greatestLowerBound.isEqualTo(e, visitor)) {
          leftAdjacent = true;
          break;
        }
      } catch (UnrecognizedCodeException exception) {
        throw new RuntimeException(exception);
      }
    }
    boolean rightAdjacent = false;
    for (NormalFormExpression e : trailingIndeces) {
      try {
        if (leastUpperBound.isEqualTo(e, visitor)) {
          rightAdjacent = true;
          break;
        }
      } catch (UnrecognizedCodeException exception) {
        throw new RuntimeException(exception);
      }
    }

    ArrayList<Bound> newBounds = new ArrayList<>(bounds);
    ArrayList<Interval> newValues = new ArrayList<>(values);
    ArrayList<Boolean> newEmptiness = new ArrayList<>(this.emptiness);

    if (leftAdjacent && rightAdjacent) {
      newValues.set(greatestLowerBoundIndex, value);
      return new FunArray(newBounds, newValues, newEmptiness);
    }

    Bound leftBound = new Bound(indeces);
    Bound rightBound = new Bound(trailingIndeces);

    if (rightAdjacent) {
      for (int i = leastUpperBoundIndex - 1; i > greatestLowerBoundIndex; i--) {
        if (!newEmptiness.get(i)) {
          greatestLowerBoundIndex = i;
          break;
        }
      }
      rightBound = newBounds.get(leastUpperBoundIndex);
    }

    if (leftAdjacent) {
      for (int i = greatestLowerBoundIndex; i < leastUpperBoundIndex; i++) {
        if (!newEmptiness.get(i)) {
          leastUpperBoundIndex = i + 1;
          break;
        }
      }
      leftBound = newBounds.get(greatestLowerBoundIndex);
    }

    Interval jointValue = getJointValue(greatestLowerBoundIndex, leastUpperBoundIndex);

    List<Bound> boundsSubList =
        newBounds.subList(greatestLowerBoundIndex + 1, leastUpperBoundIndex);
    List<Interval> valuesSubList = newValues.subList(greatestLowerBoundIndex, leastUpperBoundIndex);
    List<Boolean> emptinessSubList =
        newEmptiness.subList(greatestLowerBoundIndex, leastUpperBoundIndex);

    boundsSubList.clear();
    valuesSubList.clear();
    emptinessSubList.clear();

    if (!leftAdjacent) {
      emptinessSubList.add(true);
      valuesSubList.add(jointValue);
      boundsSubList.add(leftBound);
    }

    emptinessSubList.add(false);
    valuesSubList.add(value);

    if (!rightAdjacent) {
      emptinessSubList.add(true);
      valuesSubList.add(jointValue);
      boundsSubList.add(rightBound);
    }

    return new FunArray(newBounds, newValues, newEmptiness);
  }

  public Interval get(NormalFormExpression abstractIndex, ExpressionValueVisitor visitor) {
    int greatestLowerBoundIndex = getRightmostLowerBoundIndex(abstractIndex, visitor);
    if (greatestLowerBoundIndex >= values.size()) {
      return Interval.EMPTY;
    }
    int leastUpperBoundIndex = getLeastUpperBoundIndex(abstractIndex.add(1L), visitor);
    return getJointValue(greatestLowerBoundIndex, leastUpperBoundIndex);
  }

  private int getRightmostLowerBoundIndex(
      Set<NormalFormExpression> expressions, ExpressionValueVisitor visitor) {
    return expressions.stream()
        .mapToInt(e -> getRightmostLowerBoundIndex(e, visitor))
        .max()
        .orElse(0);
  }

  /**
   * Gets the index of the rightmost segment s such that the trailing bound of the segment s
   * contains an expression that is equal to or less than the given expression.
   *
   * @param expression the expression
   * @return the calculated index
   */
  private int getRightmostLowerBoundIndex(
      NormalFormExpression expression, ExpressionValueVisitor visitor) {
    int greatestLowerBoundIndex = 0;
    for (int i = 0; i <= bounds.size() - 1; i++) {
      if (bounds.get(i).contains(e -> isLessEqualThan(e, expression, visitor))) {
        greatestLowerBoundIndex = i;
      }
    }
    return greatestLowerBoundIndex;
  }

  private static boolean isLessEqualThan(
      NormalFormExpression a, NormalFormExpression b, ExpressionValueVisitor visitor) {
    try {
      if (a.isSyntacticallyLessThanOrEqualTo(b)) {
        return true;
      }
      return b.toInterval(visitor).isGreaterThan(a.toInterval(visitor));
    } catch (UnrecognizedCodeException exception) {
      return false;
    }
  }

  private int getLeastUpperBoundIndex(
      Set<NormalFormExpression> expressions, ExpressionValueVisitor visitor) {
    return expressions.stream()
        .mapToInt(e -> getLeastUpperBoundIndex(e, visitor))
        .max()
        .orElse(bounds.size() - 1);
  }

  /**
   * Gets the index of the leftmost segment s such that the trailing bound of the segment s contains
   * an expression that is greater than the given expression.
   *
   * @param expression the expression
   * @return the calculated index
   */
  private int getLeastUpperBoundIndex(
      NormalFormExpression expression, ExpressionValueVisitor visitor) {
    int leastUpperBoundIndex = bounds.size() - 1;
    for (int i = bounds.size() - 1; i >= 0; i--) {
      if (bounds.get(i).contains(e -> isGreaterEqualThan(e, expression, visitor))) {
        leastUpperBoundIndex = i;
      }
    }
    return leastUpperBoundIndex;
  }

  private static boolean isGreaterEqualThan(
      NormalFormExpression a, NormalFormExpression b, ExpressionValueVisitor visitor) {
    try {
      if (a.isSyntacticallyGreaterThanOrEqualTo(b)) {
        return true;
      }
      return a.toInterval(visitor).isGreaterOrEqualThan(b.toInterval(visitor));
    } catch (UnrecognizedCodeException exception) {
      return false;
    }
  }

  /**
   * Returns the joint of the values from the given segments.
   *
   * @param from the index of the first segment.
   * @param to the index of the last segment (inclusive).
   * @return the joint of all values.
   */
  private Interval getJointValue(int from, int to) {
    Interval jointValue = values.get(from);
    for (int i = from + 1; i < to; i++) {
      jointValue = jointValue.union(values.get(i));
    }
    return jointValue;
  }

  /**
   * Unifies this FunArray with another one, so their segment bounds coincide.
   *
   * @param other the other.
   * @param thisNeutralElement the neutral element for this.
   * @param otherNeutralElement the neutral element for the other.
   * @return two unified FunArrays.
   */
  public UnifyResult unify(
      FunArray other, Interval thisNeutralElement, Interval otherNeutralElement) {
    FunArrayUnification unification = new FunArrayUnification(this, other);
    return unification.unify(thisNeutralElement, otherNeutralElement);
  }

  /**
   * Joins the value in a list at a given index with the element proceeding it.
   *
   * @param list the list.
   * @param i the index.
   */
  private static void joinValueWithPredecessor(List<Interval> list, int i) {

    Interval joinedValue = list.get(i - 1).union(list.get(i));
    list.remove(i);
    list.remove(i - 1);
    list.add(i - 1, joinedValue);
  }

  /**
   * Utility function. The abstract domain functions join, meet, widen and narrow utilise the same
   * with only the neutral elements and the operation that is applied on the values is different.
   *
   * @param operation the operation applied on the values.
   * @param other the other FunArray.
   * @param thisNeutralElement the neutral element for unifying of this FunArray.
   * @param otherNeutralElement the neutral element for unifying of the other FunArray.
   * @return the joined/met/widened/narrowed FunArray
   */
  private FunArray unifyOperation(
      BinaryOperator<Interval> operation,
      FunArray other,
      Interval thisNeutralElement,
      Interval otherNeutralElement) {

    UnifyResult unifiedArrays = this.unify(other, thisNeutralElement, otherNeutralElement);
    FunArray thisUnified = unifiedArrays.resultA();
    FunArray otherUnified = unifiedArrays.resultB();

    ArrayList<Interval> modifiedValues = new ArrayList<>();
    for (int i = 0; i < thisUnified.values.size(); i++) {
      modifiedValues.add(operation.apply(thisUnified.values.get(i), otherUnified.values.get(i)));
    }

    ArrayList<Boolean> modifiedEmptiness = new ArrayList<>();
    for (int i = 0; i < thisUnified.emptiness.size(); i++) {
      modifiedEmptiness.add(thisUnified.emptiness.get(i) || otherUnified.emptiness.get(i));
    }

    return new FunArray(thisUnified.bounds, modifiedValues, modifiedEmptiness);
  }

  public int findIndex(NormalFormExpression expression) {
    for (int i = 0; i < bounds.size(); i++) {
      if (bounds.get(i).contains(expression)) {
        return i;
      }
    }
    throw new IndexOutOfBoundsException();
  }

  @Override
  public FunArray join(FunArray other) {
    return unifyOperation(Interval::union, other, Interval.EMPTY, Interval.EMPTY);
  }

  public FunArray meet(FunArray other, Interval unknown) {
    return unifyOperation(Interval::intersect, other, unknown, unknown);
  }

  public FunArray widen(FunArray other) {
    Interval unreachable = Interval.EMPTY;
    return unifyOperation(Interval::widen, other, unreachable, unreachable);
  }

  public Set<NormalFormExpression> getExpressions() {
    return bounds.stream()
        .flatMap(e -> e.expressions().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  public boolean isReachable() {
    return values.stream().noneMatch(Interval::isEmpty);
  }

  public FunArray satisfyStrictLessThan(NormalFormExpression lesser, NormalFormExpression greater) {
    int leftIndex;
    int rightIndex;
    try {
      leftIndex = this.findIndex(lesser);
      rightIndex = this.findIndex(greater);
    } catch (IndexOutOfBoundsException e) {
      return this;
    }

    if (leftIndex + 1 == rightIndex) {
      // Since the condition requires strict inequality, a single segment between the expressions
      // cannot be empty.
      ArrayList<Boolean> modifiedEmptiness = new ArrayList<>(this.emptiness());
      modifiedEmptiness.set(leftIndex, false);
      return new FunArray(this.bounds(), this.values(), modifiedEmptiness);
    } else if (leftIndex < rightIndex) {
      // If there is more than one segment in between the expressions it cannot be decided which one
      // is not empty.
      return this;
    } else {
      // Bound order states that left expression is greater than right expression
      // Condition states that left expression is less than right expression
      // --> Condition cannot be satisfied --> Remove expressions in question

      List<Bound> newBounds =
          this.bounds().stream().map(b -> b.difference(ImmutableSet.of(lesser, greater))).toList();
      return new FunArray(newBounds, this.values(), this.emptiness()).removeEmptyBounds();
    }
  }

  public FunArray satisfyLessEqual(NormalFormExpression lesser, NormalFormExpression greater) {
    int leftIndex;
    int rightIndex;
    try {
      leftIndex = this.findIndex(lesser);
      rightIndex = this.findIndex(greater);
    } catch (IndexOutOfBoundsException e) {
      return this;
    }

    ArrayList<Bound> modifiedBounds = new ArrayList<>(this.bounds());
    ArrayList<Interval> modifiedValues = new ArrayList<>(this.values());
    ArrayList<Boolean> modifiedEmptiness = new ArrayList<>(this.emptiness());

    if (leftIndex <= rightIndex) {
      // Condition is already met, change nothing
      return this;
    } else if (modifiedEmptiness.subList(rightIndex, leftIndex).stream().anyMatch(e -> !e)) {
      // Bound order states that left expression is greater than right expression
      // Condition states that left expression is less equal than right expression
      // --> Condition cannot be satisfied --> Remove expressions in question
      List<Bound> newBounds =
          modifiedBounds.stream().map(b -> b.difference(ImmutableSet.of(lesser, greater))).toList();
      return new FunArray(newBounds, this.values(), this.emptiness()).removeEmptyBounds();
    } else {
      // Bound order states that left expression is greater equal than right expression
      // Condition states that left expression is less equal than right expression
      // --> left expression has to be equal to right expression --> Squash segments
      List<Bound> boundsToBeSquashed = modifiedBounds.subList(rightIndex, leftIndex + 1);
      Bound squashedBound = Bound.union(boundsToBeSquashed);
      boundsToBeSquashed.clear();
      boundsToBeSquashed.add(squashedBound);

      modifiedValues.subList(rightIndex, leftIndex).clear();
      modifiedEmptiness.subList(rightIndex, leftIndex).clear();

      if (modifiedBounds.size() < 2) {
        return BOTTOM;
      }
      return new FunArray(modifiedBounds, modifiedValues, modifiedEmptiness);
    }
  }

  // TODO: Satisfy equal and not equal

  @Override
  public boolean isLessOrEqual(FunArray other) {
    UnifyResult unifyResult = unify(other, Interval.EMPTY, Interval.UNBOUND);

    for (int i = 0; i < unifyResult.resultA().values.size(); i++) {
      if (!unifyResult
          .resultA()
          .values
          .get(i)
          .abstractLatticeIsLessEqualThan(unifyResult.resultB().values.get(i))) {
        return false;
      }
    }
    for (int i = 0; i < unifyResult.resultA().values.size(); i++) {
      if (unifyResult.resultA().emptiness.get(i) && !unifyResult.resultB().emptiness.get(i)) {
        return false;
      }
    }
    return true;
  }
}

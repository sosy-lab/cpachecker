package org.sosy_lab.cpachecker.cpa.interval;

import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.getIntegerExpression;
import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.incrementExpression;
import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.isSyntacticallyGreaterThanOrEqualTo;
import static org.sosy_lab.cpachecker.cpa.interval.ExpressionUtility.isSyntacticallyLessThanOrEqualTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


/**
 * A FunArray to represent an Array in abstract interpretation static analysis. See Patrick Cousot,
 * Radhia Cousot, and Francesco Logozzo. 2011. A parametric segmentation functor for fully automatic
 * and scalable array content analysis. SIGPLAN Not. 46, 1 (January 2011), 105–118. <a
 * href="https://doi.org/10.1145/1925844.1926399">https://doi.org/10.1145/1925844.1926399</a>
 *
 * @param bounds    the FunArray's segment bounds.
 * @param values    the FunArray's values.
 * @param emptiness a list determining whether a segment might be empty.
 */
public record FunArray(List<Bound> bounds, List<Interval> values, List<Boolean> emptiness) {

  /**
   * Constructor for FunArray.
   *
   * @param bounds    the FunArray's segment bounds.
   * @param values    the FunArray's values.
   * @param emptiness a list determining whether a segment might be empty.
   */
  public FunArray(List<Bound> bounds, List<Interval> values, List<Boolean> emptiness) {

    if (bounds.size() < 2) {
      throw new IllegalArgumentException("FunArray requires at least two bounds.");
    }

    if (values.size() != bounds.size() - 1) {
      throw new IllegalArgumentException(
              "Number of segment values does not match up with count of bound count. Needs to be"
                      + "exactly one less."
      );
    }

    if (emptiness.size() != bounds.size() - 1) {
      throw new IllegalArgumentException(
              "Number of emptiness values does not match up with count of bound count. Needs to be"
                      + "exactly one less."
      );
    }

    this.bounds = List.copyOf(bounds);
    this.values = List.copyOf(values);
    this.emptiness = List.copyOf(emptiness);
  }

  public FunArray(CExpression length) {
    this(new Bound(length));
  }

  public FunArray(Bound length) {
    this(
            List.of(
                    new Bound(getIntegerExpression(0)),
                    length
            ),
            List.of(Interval.UNBOUND),
            List.of(true)
    );
  }

  public static FunArray ofInitializerList(List<CInitializer> initializers, ExpressionValueVisitor visitor) {
    List<Interval> values = initializers.stream().map(e -> {
          if (e instanceof CInitializerExpression cInitializerExpression) {
            try {
              return cInitializerExpression.getExpression().accept(visitor);
            } catch (UnrecognizedCodeException pE) {
              return Interval.UNBOUND;
            }
          }
          return Interval.UNBOUND;
        }).toList();

    List<Bound> bounds = IntStream.range(0, initializers.size() + 1)
        .mapToObj(e -> getIntegerExpression(e))
        .map(e -> new Bound(e))
        .toList();

    List<Boolean> emptiness = Collections.nCopies(initializers.size(), false);

    return new FunArray(bounds, values, emptiness);
  }

  @Override
  public String toString() {
    return IntStream.range(0, bounds.size())
            .mapToObj(i -> {
              if (values().size() <= i) {
                return "%s%s".formatted(
                        bounds.get(i),
                        emptiness.get(i - 1) ? "?" : ""
                );
              }
              if (i == 0) {
                return "%s %s".formatted(
                        bounds.get(i),
                        values.get(i)
                );
              }
              return "%s%s %s".formatted(
                      bounds.get(i),
                      emptiness.get(i - 1) ? "?" : "",
                      values.get(i)
              );
            })
            .collect(Collectors.joining(" "));
  }

  public FunArray insertExpression(String varRef, Set<CExpression> expressions) {
    var newBounds = new ArrayList<>(bounds.stream()
            .map(b -> b.adaptForChangedVariableValues(varRef, expressions))
            .toList());
    var newValues = new ArrayList<>(values);
    var newEmptiness = new ArrayList<>(emptiness);

    return new FunArray(newBounds, newValues, newEmptiness);
  }

  public FunArray removeVariableOccurrences(String varRef) {
    return new FunArray(
            bounds.stream().map(b -> b.removeVariableOccurrences(varRef)).toList(),
            values, emptiness
    ).removeEmptyBounds();
  }


  public FunArray restrictExpressionOccurrences(Set<CExpression> allowedExpressions) {
    var newBounds = bounds.stream()
            .map(b -> b.intersection(allowedExpressions))
            .toList();
    return new FunArray(newBounds, values, emptiness)
            .removeEmptyBounds();
  }

  public FunArray removeEmptyBounds() {
    var newBounds = new ArrayList<>(bounds);
    var newValues = new ArrayList<>(values);
    var newEmptiness = new ArrayList<>(emptiness);

    var i = 1;
    while (i < newBounds.size()) {
      if (newBounds.get(i).isEmpty()) {
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

  public FunArray insert(CExpression index, Interval value, ExpressionValueVisitor visitor) {
    return insert(Set.of(index), value, visitor);
  }

  /**
   * Inserts a value into the FunArray.
   *
   * @param indeces the leading bound expressions for the new value.
   * @param value the value to be inserted.
   * @return the modified Segmentation.
   */
  public FunArray insert(Set<CExpression> indeces, Interval value, ExpressionValueVisitor visitor) {
    if (indeces.isEmpty()) {
      return this;
    }
    var trailingIndeces = indeces.stream()
            .map(ExpressionUtility::incrementExpression)
            .collect(Collectors.toSet());
    int greatestLowerBoundIndex = getRightmostLowerBoundIndex(indeces, visitor);
    int leastUpperBoundIndex = getLeastUpperBoundIndex(trailingIndeces, visitor);

    final Bound greatestLowerBound = bounds.get(greatestLowerBoundIndex);
    final Bound leastUpperBound = bounds.get(leastUpperBoundIndex);

    var leftAdjacent = indeces.stream().anyMatch(e -> {
      try {
        return greatestLowerBound.isEqualTo(e, visitor);
      } catch (UnrecognizedCodeException pE) {
        return false;
      }
    });
    var rightAdjacent = trailingIndeces.stream().anyMatch(e -> {
      try {
        return leastUpperBound.isEqualTo(e, visitor);
      } catch (UnrecognizedCodeException pE) {
        return false;
      }
    });


    var newBounds = new ArrayList<>(bounds);
    var newValues = new ArrayList<>(values);
    var newEmptiness = new ArrayList<>(this.emptiness);



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
      leftBound = rightBound.increase(-1);
    }

    if (leftAdjacent) {
      for (int i = greatestLowerBoundIndex; i < leastUpperBoundIndex; i++) {
        if (!newEmptiness.get(i)) {
          leastUpperBoundIndex = i + 1;
          break;
        }
      }
      leftBound = newBounds.get(greatestLowerBoundIndex);
      rightBound = leftBound.increase(1);
    }

    var jointValue = getJointValue(greatestLowerBoundIndex, leastUpperBoundIndex);

    var boundsSubList = newBounds.subList(greatestLowerBoundIndex + 1, leastUpperBoundIndex);
    var valuesSubList = newValues.subList(greatestLowerBoundIndex, leastUpperBoundIndex);
    var emptinessSubList = newEmptiness.subList(greatestLowerBoundIndex, leastUpperBoundIndex);

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

  public Interval get(CExpression abstractIndex, ExpressionValueVisitor visitor) {
    int greatestLowerBoundIndex = getRightmostLowerBoundIndex(abstractIndex, visitor);
    int leastUpperBoundIndex = getLeastUpperBoundIndex(incrementExpression(abstractIndex), visitor);
    return getJointValue(greatestLowerBoundIndex, leastUpperBoundIndex);
  }

  private int getRightmostLowerBoundIndex(Set<CExpression> expressions, ExpressionValueVisitor visitor) {
    return expressions.stream().mapToInt(e -> getRightmostLowerBoundIndex(e, visitor)).max().orElse(0);
  }

  /**
   * Gets the index of the rightmost segment s such that the trailing bound of the segment s
   * contains an expression that is equal to or less than the given expression.
   *
   * @param expression the expression
   * @return the calculated index
   */
  private int getRightmostLowerBoundIndex(CExpression expression, ExpressionValueVisitor visitor) {
    int greatestLowerBoundIndex = 0;
    for (int i = 0; i <= bounds.size() - 1; i++) {
      if (bounds.get(i).contains(e -> isLessEqualThan(e, expression, visitor))) {
        greatestLowerBoundIndex = i;
      }
    }
    return greatestLowerBoundIndex;
  }

  private static boolean isLessEqualThan(CExpression a, CExpression b, ExpressionValueVisitor visitor) {
    try {
      if (isSyntacticallyLessThanOrEqualTo(a, b, visitor)) {
        return true;
      }
      return b.accept(visitor).isGreaterThan(a.accept(visitor));
    } catch (UnrecognizedCodeException pE) {
      return false;
    }
  }

  private int getLeastUpperBoundIndex(Set<CExpression> expressions, ExpressionValueVisitor visitor) {
    return expressions.stream().mapToInt(e -> getLeastUpperBoundIndex(e, visitor)).max().orElse(bounds.size() - 1);
  }

  /**
   * Gets the index of the leftmost segment s such that the trailing bound of the segment s contains
   * an expression that is greater than the given expression.
   *
   * @param expression the expression
   * @return the calculated index
   */
  private int getLeastUpperBoundIndex(CExpression expression, ExpressionValueVisitor visitor) {
    int leastUpperBoundIndex = bounds.size() - 1;
    for (int i = bounds.size() - 1; i >= 0; i--) {
      if (bounds.get(i).contains(e -> isGreaterEqualThan(e, expression, visitor))) {
        leastUpperBoundIndex = i;
      }
    }
    return leastUpperBoundIndex;
  }

  private static boolean isGreaterEqualThan(CExpression a, CExpression b, ExpressionValueVisitor visitor) {
    try {
      if (isSyntacticallyGreaterThanOrEqualTo(a, b, visitor)) {
        return true;
      }
      return a.accept(visitor).isGreaterOrEqualThan(b.accept(visitor));
    } catch (UnrecognizedCodeException pE) {
      return false;
    }
  }

  /**
   * Returns the joint of the values from the given segments.
   *
   * @param from the index of the first segment.
   * @param to   the index of the last segment (inclusive).
   * @return the joint of all values.
   */
  private Interval getJointValue(int from, int to) {
    var jointValue = values.get(from);
    for (int i = from + 1; i < to; i++) {
      jointValue = jointValue.union(values.get(i));
    }
    return jointValue;
  }

  public record UnifyResult(
          FunArray resultThis,
          FunArray resultOther) {
  }

  private static final int UNIFY_LOOP_HARD_LIMIT = 10000;

  /**
   * Unifies this FunArray with another one, so their segment bounds coincide.
   *
   * @param other               the other.
   * @param thisNeutralElement  the neutral element for this.
   * @param otherNeutralElement the neutral element for the other.
   * @return two unified FunArrays.
   */
  public UnifyResult unify(FunArray other,
                                     Interval thisNeutralElement, Interval otherNeutralElement) {

    var thisExpressions = this.getExpressions();
    var otherExpressions = other.getExpressions();

    var commonExpressions = new HashSet<>(thisExpressions);
    commonExpressions.retainAll(otherExpressions);

    var thisReduced = this.restrictExpressionOccurrences(commonExpressions);
    var otherReduced = other.restrictExpressionOccurrences(commonExpressions);

    List<Bound> boundsThis = new ArrayList<>(thisReduced.bounds);
    List<Interval> valuesThis = new ArrayList<>(thisReduced.values);
    List<Boolean> emptinessThis = new ArrayList<>(thisReduced.emptiness);

    List<Bound> boundsOther = new ArrayList<>(otherReduced.bounds);
    List<Interval> valuesOther = new ArrayList<>(otherReduced.values);
    List<Boolean> emptinessOther = new ArrayList<>(otherReduced.emptiness);


    int i = 0;
    int loopLimitCount = 0;

    while (i < boundsThis.size() && i < boundsOther.size()) {
      if (loopLimitCount >= UNIFY_LOOP_HARD_LIMIT) {
        throw new RuntimeException("Something went wrong in the unifying algorithm.");
      }
      loopLimitCount++;

      var currentBoundThis = boundsThis.get(i);
      var currentBoundOther = boundsOther.get(i);

      var intersection = currentBoundThis.intersection(currentBoundOther);
      var difference = currentBoundThis.difference(currentBoundOther);
      var relativeComplement = currentBoundThis.relativeComplement(currentBoundOther);

      if (intersection.isEmpty()) {
        boundsThis.set(i, intersection);
        boundsOther.set(i, intersection);
        i++;
        continue;
      }

      if (!difference.isEmpty()) {
        boundsThis.set(i, difference);
        boundsThis.add(i, intersection);
        valuesThis.add(i, thisNeutralElement);
        emptinessThis.add(i, true);
      }

      if (!relativeComplement.isEmpty()) {
        boundsOther.set(i, relativeComplement);
        boundsOther.add(i, intersection);
        valuesOther.add(i, otherNeutralElement);
        emptinessOther.add(i, true);
      }
      i++;
    }

    return new UnifyResult(
            new FunArray(boundsThis, valuesThis, emptinessThis).removeEmptyBounds(),
            new FunArray(boundsOther, valuesOther, emptinessOther).removeEmptyBounds()
    );
  }

  /**
   * Joins the value in a list at a given index with the element proceeding it.
   *
   * @param list the list.
   * @param i    the index.
   */
  private static void joinValueWithPredecessor(List<Interval> list, int i) {

    var joinedValue = list.get(i - 1).union(list.get(i));
    list.remove(i);
    list.remove(i - 1);
    list.add(i - 1, joinedValue);
  }

  /**
   * Utility function. The abstract domain functions join, meet, widen and narrow utilise the same
   * with only the neutral elements and the operation that is applied on the values is different.
   *
   * @param operation           the operation applied on the values.
   * @param other               the other FunArray.
   * @param thisNeutralElement  the neutral element for unifying of this FunArray.
   * @param otherNeutralElement the neutral element for unifying of the other FunArray.
   * @return the joined/met/widened/narrowed FunArray
   */
  private FunArray unifyOperation(
      BinaryOperator<Interval> operation,
      FunArray other,
      Interval thisNeutralElement,
      Interval otherNeutralElement) {

    var unifiedArrays = this.unify(other, thisNeutralElement, otherNeutralElement);
    var thisUnified = unifiedArrays.resultThis();
    var otherUnified = unifiedArrays.resultOther();

    var modifiedValues = IntStream.range(0, thisUnified.values.size())
            .mapToObj(i -> operation.apply(thisUnified.values.get(i), otherUnified.values.get(i)))
            .toList();

    var modifiedEmptiness = IntStream.range(0, thisUnified.emptiness.size())
            .mapToObj(i -> thisUnified.emptiness.get(i) || otherUnified.emptiness.get(i))
            .toList();

    return new FunArray(thisUnified.bounds, modifiedValues, modifiedEmptiness);
  }

  public int findIndex(CExpression expression) {
    for (int i = 0; i < bounds.size(); i++) {
      if (bounds.get(i).contains(expression)) {
        return i;
      }
    }
    throw new IndexOutOfBoundsException();
  }

  public FunArray join(FunArray other, Interval unreachable) {
    return unifyOperation(Interval::union, other, unreachable, unreachable);
  }

  public FunArray meet(FunArray other, Interval unknown) {
    return unifyOperation(Interval::intersect, other, unknown, unknown);
  }

//  public FunArray widen(FunArray other, Interval unreachable) {
//    return unifyOperation(Interval::widen, other, unreachable, unreachable);
//  }
// TODO

//  public FunArray narrow(FunArray other, Interval unknown) {
//    return unifyOperation(Interval::narrow, other, unknown, unknown);
// TODO

  public Set<CExpression> getExpressions() {
    return bounds.stream().flatMap(e -> e.expressions().stream()).collect(Collectors.toSet());
  }

  public boolean isReachable() {
    return values.stream().noneMatch(Interval::isEmpty);
  }
}

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
 * Abstract representation of an array's contents as a sequence of contiguous, non-overlapping
 * segments, each assigned an abstract interval value. Segments are delimited by {@link Bound}s;
 * within each bound, all index expressions are equal in the concrete program state.
 *
 * <p>A FunArray {@code {0} v {i} w {n}} reads as: all elements at indices between {@code 0} and
 * {@code i} are abstracted by {@code v}; all elements between {@code i} and {@code n} are
 * abstracted by {@code w}. The leftmost bound always contains the constant expression {@code 0};
 * the rightmost bound contains the length expression(s) of the array.
 *
 * <p>Each segment additionally carries an <em>emptiness flag</em>: {@code true} means the segment
 * may span zero elements (its two delimiting bounds may be equal at runtime).
 *
 * <p>See Patrick Cousot, Radhia Cousot, and Francesco Logozzo. 2011. A parametric segmentation
 * functor for fully automatic and scalable array content analysis. SIGPLAN Not. 46, 1 (January
 * 2011), 105–118. <a href="https://doi.org/10.1145/1925844.1926399">
 * https://doi.org/10.1145/1925844.1926399</a>
 *
 * @param bounds the ordered list of segment boundaries, leftmost first. Must contain at least two
 *     elements; no two bounds may share a {@link NormalFormExpression}.
 * @param values the abstract interval value of each segment; must satisfy {@code values.size() ==
 *     bounds.size() - 1}.
 * @param emptiness for each segment, whether it may span zero elements; must satisfy {@code
 *     emptiness.size() == bounds.size() - 1}.
 */
public record FunArray(
    ImmutableList<Bound> bounds, ImmutableList<Interval> values, ImmutableList<Boolean> emptiness)
    implements Serializable, LatticeAbstractState<FunArray> {

  @Serial private static final long serialVersionUID = 7169472946910382516L;

  /**
   * The bottom element of the FunArray lattice, representing an unreachable program state. Used as
   * the absorbing element for analysis paths that contain contradictory constraints, and returned
   * by {@link #removeEmptyBounds()} and {@link #ofInitializerList} when no valid segmentation
   * remains.
   */
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

  /**
   * Convenience constructor that copies mutable lists into their immutable equivalents.
   *
   * @param pBounds the segment bounds.
   * @param pValues the segment values.
   * @param pEmptiness the emptiness flags.
   */
  public FunArray(List<Bound> pBounds, List<Interval> pValues, List<Boolean> pEmptiness) {
    this(
        ImmutableList.copyOf(pBounds),
        ImmutableList.copyOf(pValues),
        ImmutableList.copyOf(pEmptiness));
  }

  /**
   * Creates a FunArray for an array whose contents are entirely unknown. The single segment spans
   * the entire array with value {@link Interval#UNBOUND} and is marked possibly empty because the
   * array length may be zero.
   *
   * @param lengthExpressions the set of expressions representing the array length.
   */
  public FunArray(Set<NormalFormExpression> lengthExpressions) {
    this(new Bound(lengthExpressions));
  }

  /**
   * Creates a FunArray for an array whose contents are entirely unknown, using a pre-constructed
   * {@link Bound} as the length. Equivalent to {@link #FunArray(Set)} but accepts a {@link Bound}
   * directly.
   *
   * @param length the bound representing the array length.
   */
  public FunArray(Bound length) {
    this(
        ImmutableList.of(new Bound(new NormalFormExpression(0)), length),
        ImmutableList.of(Interval.UNBOUND),
        ImmutableList.of(true));
  }

  /**
   * Creates a FunArray for an array whose elements are all initialized to {@code pInitialValue}.
   * The single segment spans the entire array and is marked possibly empty because the array length
   * may be zero at the time of construction.
   *
   * @param pLength the bound representing the array length.
   * @param pInitialValue the abstract value shared by all array elements.
   */
  public FunArray(Bound pLength, Interval pInitialValue) {
    this(
        ImmutableList.of(new Bound(new NormalFormExpression(0)), pLength),
        ImmutableList.of(pInitialValue),
        ImmutableList.of(true));
  }

  /**
   * Creates a FunArray from a C initializer list (e.g., the right-hand side of {@code int a[] = {1,
   * 2, 3}}). Each element becomes its own non-empty single-element segment with a concrete integer
   * bound at its position. Returns {@link #BOTTOM} if the list is empty.
   *
   * @param initializers the list of C initializer expressions from the AST.
   * @param visitor the expression value visitor for the current abstract state.
   * @return a FunArray with one segment per initializer element, or {@link #BOTTOM} if the list is
   *     empty.
   */
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

  /**
   * Returns a human-readable representation of this FunArray, interleaving bounds and segment
   * values. The closing bound of each possibly-empty segment is suffixed with {@code ?}. Example:
   * {@code 0 [-inf,+inf] {i}? [0,10] {n}}.
   */
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

  /**
   * Returns a new FunArray with all bounds updated to reflect an assignment to {@code
   * changedVariable}. Each bound is adapted by substituting the old variable with {@code
   * expressions} via {@link Bound#adaptForChangedVariableValues}. Segment values and emptiness
   * flags are unchanged.
   *
   * @param changedVariable the variable that was assigned a new value.
   * @param expressions the set of normal-form expressions representing the new possible values of
   *     {@code changedVariable}.
   * @return a new FunArray with updated bounds.
   */
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

  /**
   * Returns a new FunArray with all bounds that contain no expressions removed. Empty bounds arise
   * after a variable assignment invalidates every expression in a bound. When a bound is removed,
   * the values of the two adjacent segments are joined. Returns {@link #BOTTOM} if removal would
   * leave fewer than two bounds.
   *
   * @return a new FunArray free of empty bounds, or {@link #BOTTOM} if no valid segmentation
   *     remains.
   */
  public FunArray removeEmptyBounds() {
    ArrayList<Bound> newBounds = new ArrayList<>(bounds);
    ArrayList<Interval> newValues = new ArrayList<>(values);
    ArrayList<Boolean> newEmptiness = new ArrayList<>(emptiness);

    int i = 1;
    while (i < newBounds.size()) {
      if (newBounds.get(i).isEmpty()) {
        if (newBounds.size() <= 2 || i == newBounds.size() - 1) {
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

  /**
   * Single-expression convenience wrapper that delegates to {@link #insert(Set, Interval,
   * ExpressionValueVisitor)}.
   *
   * @param index the index expression for the assignment.
   * @param value the value to write at the index.
   * @param visitor the expression value visitor for the current abstract state.
   * @return the updated FunArray.
   */
  public FunArray insert(
      NormalFormExpression index, Interval value, ExpressionValueVisitor visitor) {
    return insert(ImmutableSet.of(index), value, visitor);
  }

  /**
   * Records an abstract array assignment {@code a[index] = value} by updating the segment(s)
   * covering {@code indeces}. The method locates the greatest lower bound for {@code indeces} and
   * the least upper bound for {@code indeces + 1}, then replaces the covered region with up to
   * three new segments:
   *
   * <ol>
   *   <li>A left overhang spanning {@code [lowerBound, index)} holding the previous joint value,
   *       marked possibly empty.
   *   <li>An exact segment spanning {@code [index, index+1)} with {@code value}, marked non-empty.
   *   <li>A right overhang spanning {@code [index+1, upperBound)} holding the previous joint value,
   *       marked possibly empty.
   * </ol>
   *
   * If the lower bound already equals {@code index} (left-adjacent), the left overhang is omitted.
   * If the upper bound already equals {@code index+1} (right-adjacent), the right overhang is
   * omitted.
   *
   * <p>Implements the assignment operation described in Section 11.6 of Cousot, Cousot, and Logozzo
   * (2011).
   *
   * @param indeces the set of normal-form index expressions for the assignment target.
   * @param value the abstract value written at the index.
   * @param visitor the expression value visitor for the current abstract state.
   * @return the updated FunArray, or {@code this} if {@code indeces} is empty or out of bounds.
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

  /**
   * Returns the abstract value at array index {@code abstractIndex} by joining all segment values
   * whose range overlaps with the index.
   *
   * <p>Implements the 'Abstract value of an indexed array element' operation described in section
   * 11.6 of Cousot, Cousot, and Logozzo (2011).
   *
   * @param abstractIndex the index expression to read.
   * @param visitor the expression value visitor for the current abstract state.
   * @return the join of all segment values that may cover {@code abstractIndex}, or {@link
   *     Interval#EMPTY} if the index falls outside all segments.
   */
  public Interval get(NormalFormExpression abstractIndex, ExpressionValueVisitor visitor) {
    int greatestLowerBoundIndex = getRightmostLowerBoundIndex(abstractIndex, visitor);
    if (greatestLowerBoundIndex >= values.size()) {
      return Interval.EMPTY;
    }
    int leastUpperBoundIndex = getLeastUpperBoundIndex(abstractIndex.add(1L), visitor);
    return getJointValue(greatestLowerBoundIndex, leastUpperBoundIndex);
  }

  /**
   * Returns the greatest lower-bound index across all expressions in the set, by taking the maximum
   * of {@link #getRightmostLowerBoundIndex(NormalFormExpression, ExpressionValueVisitor)} over
   * each.
   */
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

  /**
   * Utility method. Returns {@code true} if expression {@code a} is less than or equal to
   * expression {@code b}. Checks syntactically first; falls back to interval comparison when the
   * two expressions differ in variable and cannot be ordered by constant alone.
   */
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

  /**
   * Returns the greatest upper-bound index across all expressions in the set, by taking the maximum
   * of {@link #getLeastUpperBoundIndex(NormalFormExpression, ExpressionValueVisitor)} over each.
   */
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

  /**
   * Utility method. Returns {@code true} if expression {@code a} is greater than or equal to
   * expression {@code b}. Checks syntactically first; falls back to interval comparison when
   * syntactic ordering is not applicable.
   */
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
   * Aligns this FunArray with {@code other} so their segment bounds coincide, inserting neutral
   * segments as needed. Delegates to {@link FunArrayUnification#unify}.
   *
   * @param other the FunArray to unify with.
   * @param thisNeutralElement the interval used to fill newly inserted segments in this array.
   * @param otherNeutralElement the interval used to fill newly inserted segments in {@code other}.
   * @return a {@link UnifyResult} containing two new FunArrays with identical bound structure.
   */
  public UnifyResult unify(
      FunArray other, Interval thisNeutralElement, Interval otherNeutralElement) {
    FunArrayUnification unification = new FunArrayUnification(this, other);
    return unification.unify(thisNeutralElement, otherNeutralElement);
  }

  /**
   * Variant of {@link #unify(FunArray, Interval, Interval)} with explicit collapse operators for
   * Case 6 of the unification algorithm. See {@link FunArrayUnification#unify(Interval, Interval,
   * java.util.function.BinaryOperator, java.util.function.BinaryOperator)}.
   */
  public UnifyResult unify(
      FunArray pOther,
      Interval pThisNeutralElement,
      Interval pOtherNeutralElement,
      BinaryOperator<Interval> pCollapseOpThis,
      BinaryOperator<Interval> pCollapseOpOther) {
    FunArrayUnification unification = new FunArrayUnification(this, pOther);
    return unification.unify(
        pThisNeutralElement, pOtherNeutralElement, pCollapseOpThis, pCollapseOpOther);
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

  /**
   * Returns the index of the bound in {@link #bounds} that contains {@code expression}.
   *
   * @param expression the expression to locate.
   * @return the index of the bound containing {@code expression}.
   * @throws IndexOutOfBoundsException if no bound contains {@code expression}.
   */
  public int findIndex(NormalFormExpression expression) {
    for (int i = 0; i < bounds.size(); i++) {
      if (bounds.get(i).contains(expression)) {
        return i;
      }
    }
    throw new IndexOutOfBoundsException();
  }

  /**
   * Returns a new FunArray in which the value of every segment has been joined with {@code pValue}.
   * Used when an array element is assigned at an index that cannot be resolved to a specific
   * segment — since any element may have been updated, {@code pValue} is joined into every segment.
   *
   * @param pValue the value to join into every segment.
   * @return a new FunArray with each segment value joined with {@code pValue}.
   */
  public FunArray assignAllSegments(Interval pValue) {
    ArrayList<Interval> newValues = new ArrayList<>();
    for (Interval v : values) {
      newValues.add(v.union(pValue));
    }
    return new FunArray(bounds, newValues, emptiness);
  }

  /**
   * Returns the least upper bound (join) of this FunArray and {@code other} in the abstract domain.
   * The arrays are unified with {@link Interval#EMPTY} as the neutral element for both sides, then
   * corresponding segment values are joined pointwise with {@link Interval#union}. Corresponds to
   * the join operator of the FunArray abstract domain (Section 11.5 of Cousot, Cousot, and Logozzo
   * (2011)).
   *
   * @param other the FunArray to join with.
   * @return the least upper bound of the two FunArrays.
   */
  @Override
  public FunArray join(FunArray other) {
    return unifyOperation(Interval::union, other, Interval.EMPTY, Interval.EMPTY);
  }

  /**
   * Returns the greatest lower bound (meet) of this FunArray and {@code other} in the abstract
   * domain. The arrays are unified with {@code unknown} as the neutral element for both sides, then
   * corresponding segment values are intersected pointwise with {@link Interval#intersect}.
   * Corresponds to the meet operator of the FunArray abstract domain (Section 11.5 of Cousot,
   * Cousot, and Logozzo (2011)).
   *
   * @param other the FunArray to meet with.
   * @param unknown the neutral element for unification; should be the top interval when the content
   *     of a newly introduced segment is not known.
   * @return the greatest lower bound of the two FunArrays.
   */
  public FunArray meet(FunArray other, Interval unknown) {
    return unifyOperation(Interval::intersect, other, unknown, unknown);
  }

  /**
   * Returns the widening of this FunArray with {@code other} in the abstract domain. The arrays are
   * unified with {@link Interval#EMPTY} as the neutral element for both sides, then corresponding
   * segment values are widened pointwise with {@link Interval#widen}. Corresponds to the widening
   * operator of the FunArray abstract domain (Section 11.5 of Cousot, Cousot, and Logozzo (2011)).
   *
   * @param other the FunArray to widen with.
   * @return the widened FunArray.
   */
  public FunArray widen(FunArray other) {
    Interval unreachable = Interval.EMPTY;
    return unifyOperation(Interval::widen, other, unreachable, unreachable);
  }

  /**
   * Returns all {@link NormalFormExpression}s appearing in any bound of this FunArray. Used to
   * determine which program variables this FunArray tracks. Used for testing.
   *
   * @return an immutable set of all bound expressions.
   */
  public Set<NormalFormExpression> getExpressions() {
    return bounds.stream()
        .flatMap(e -> e.expressions().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns {@code true} if this FunArray represents a reachable abstract state. A FunArray is
   * unreachable when any segment has an empty ({@link Interval#isEmpty}) value, since no concrete
   * array element could satisfy an empty interval.
   *
   * @return {@code true} if all segment values are non-empty.
   */
  public boolean isReachable() {
    return values.stream().noneMatch(Interval::isEmpty);
  }

  /**
   * Narrows the abstract value of the single element at {@code pIndex} by intersecting the
   * containing segment's value with {@code pNarrowedTo}. Equivalent to asserting {@code
   * array[pIndex] ∈ pNarrowedTo}: reads the current value at {@code pIndex} via {@link #get},
   * intersects it with {@code pNarrowedTo}, then writes the result back via {@link #insert}, which
   * splits the segment at {@code pIndex} so only that element is affected.
   *
   * <p>If the intersection is empty (contradiction), {@link #BOTTOM} is returned.
   *
   * @param pIndex the index expression of the array element being constrained.
   * @param pNarrowedTo the interval the element is asserted to lie within.
   * @param pVisitor the expression value visitor for the current abstract state.
   * @return a FunArray with the element at {@code pIndex} narrowed, or {@link #BOTTOM} on
   *     contradiction.
   */
  public FunArray narrowElement(
      NormalFormExpression pIndex, Interval pNarrowedTo, ExpressionValueVisitor pVisitor) {
    Interval current = get(pIndex, pVisitor);
    Interval narrowed = current.intersect(pNarrowedTo);
    if (narrowed.isEmpty()) {
      return BOTTOM;
    }
    return insert(pIndex, narrowed, pVisitor);
  }

  /**
   * Narrows this FunArray to reflect the constraint {@code lesser < greater}. The method compares
   * the positions of the two expressions in the bound list:
   *
   * <ul>
   *   <li>If {@code lesser} immediately precedes {@code greater} (exactly one segment between
   *       them), that segment is marked non-empty, since strict inequality implies at least one
   *       element exists between the two bounds.
   *   <li>If there is more than one segment between them, no change is made (it cannot be
   *       determined which segment is non-empty).
   *   <li>If {@code lesser} follows {@code greater} (contradiction), {@link #BOTTOM} is returned.
   * </ul>
   *
   * @param lesser the expression asserted to be strictly less than {@code greater}.
   * @param greater the expression asserted to be strictly greater than {@code lesser}.
   * @return a narrowed FunArray satisfying the constraint, or the unchanged FunArray if either
   *     expression is absent from all bounds.
   */
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
      // Bound order states that left expression is greater than or equal to right expression
      // Condition states that left expression is less than right expression
      // --> Condition cannot be satisfied --> constraint is contradictory
      return BOTTOM;
    }
  }

  /**
   * Narrows this FunArray to reflect the constraint {@code lesser <= greater}. The method compares
   * the positions of the two expressions in the bound list:
   *
   * <ul>
   *   <li>If {@code lesser} already precedes or equals {@code greater} in bound order, the
   *       constraint is trivially satisfied and no change is made.
   *   <li>If {@code lesser} follows {@code greater} and all intermediate segments are possibly
   *       empty (the expressions may be equal), the bounds between them are squashed into a single
   *       bound containing both expressions.
   *   <li>If {@code lesser} follows {@code greater} and any intermediate segment is non-empty
   *       (contradiction), {@link #BOTTOM} is returned.
   * </ul>
   *
   * @param lesser the expression asserted to be less than or equal to {@code greater}.
   * @param greater the expression asserted to be greater than or equal to {@code lesser}.
   * @return a narrowed FunArray satisfying the constraint, or the unchanged FunArray if either
   *     expression is absent from all bounds.
   */
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
      // --> Condition cannot be satisfied --> constraint is contradictory
      return BOTTOM;
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

  /**
   * Narrows this FunArray to reflect the constraint {@code pExpr1 == pExpr2}. The equality is
   * decomposed into {@code pExpr1 <= pExpr2} AND {@code pExpr2 <= pExpr1} and each half is
   * delegated to {@link #satisfyLessEqual}.
   *
   * <ul>
   *   <li>If the two expressions are already in the same bound, no change is made.
   *   <li>If all segments between them are possibly-empty, those segments are squashed into a
   *       single bound containing both expressions.
   *   <li>If any segment between them is non-empty (contradiction), {@link #BOTTOM} is returned.
   * </ul>
   *
   * @param pExpr1 the first expression asserted to be equal to {@code pExpr2}.
   * @param pExpr2 the second expression asserted to be equal to {@code pExpr1}.
   * @return a narrowed FunArray satisfying the equality constraint, or the unchanged FunArray if
   *     either expression is absent from all bounds.
   */
  public FunArray satisfyEquals(NormalFormExpression pExpr1, NormalFormExpression pExpr2) {
    return satisfyLessEqual(pExpr1, pExpr2).satisfyLessEqual(pExpr2, pExpr1);
  }

  /**
   * Narrows this FunArray to reflect the constraint {@code pExpr1 != pExpr2}. The relative position
   * of the two expressions in the bound list determines the outcome:
   *
   * <ul>
   *   <li>If either expression is absent from all bounds, no change is made.
   *   <li>If the two expressions are in the same bound (considered equal), their equality
   *       contradicts the constraint and {@link #BOTTOM} is returned.
   *   <li>If the expressions are in different bounds, the not-equal constraint is equivalent to a
   *       strict inequality in the direction already implied by bound order. The method delegates
   *       to {@link #satisfyStrictLessThan} with the left-of expression as {@code lesser}.
   * </ul>
   *
   * @param pExpr1 the first expression asserted to be not equal to {@code pExpr2}.
   * @param pExpr2 the second expression asserted to be not equal to {@code pExpr1}.
   * @return a narrowed FunArray satisfying the constraint, or the unchanged FunArray if either
   *     expression is absent from all bounds.
   */
  public FunArray satisfyNotEquals(NormalFormExpression pExpr1, NormalFormExpression pExpr2) {
    int index1;
    int index2;
    try {
      index1 = findIndex(pExpr1);
      index2 = findIndex(pExpr2);
    } catch (IndexOutOfBoundsException e) {
      return this;
    }

    if (index1 == index2) {
      return BOTTOM;
    } else if (index1 < index2) {
      return satisfyStrictLessThan(pExpr1, pExpr2);
    } else {
      return satisfyStrictLessThan(pExpr2, pExpr1);
    }
  }

  /**
   * Returns {@code true} if this FunArray is below {@code other} in the abstract domain partial
   * order. The arrays are unified with {@link Interval#EMPTY} as the neutral element for this and
   * {@link Interval#UNBOUND} for {@code other}, then the following conditions are checked pointwise
   * for every segment:
   *
   * <ol>
   *   <li>The segment value of this array is contained in the corresponding segment value of {@code
   *       other} (checked via {@link Interval#abstractLatticeIsLessEqualThan}).
   *   <li>No segment that is definitely non-empty in this array is marked possibly empty in {@code
   *       other}.
   * </ol>
   *
   * Implements the partial order of the FunArray abstract domain (Section 11.5 of Cousot, Cousot,
   * and Logozzo (2011)).
   *
   * @param other the FunArray to compare against.
   * @return {@code true} if this FunArray is below {@code other} in the lattice.
   */
  @Override
  public boolean isLessOrEqual(FunArray other) {
    UnifyResult unifyResult =
        unify(other, Interval.EMPTY, Interval.UNBOUND, Interval::union, Interval::intersect);

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

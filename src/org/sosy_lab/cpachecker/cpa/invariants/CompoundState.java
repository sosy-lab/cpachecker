/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.compound.tocompound.ICCOperator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tocompound.IICOperator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.scalar.tocompound.ISCOperator;

import com.google.common.base.Objects;

/**
 * Instances of this class represent compound states of intervals.
 */
public class CompoundState {

  /**
   * The compound state representing "bottom".
   */
  private static final CompoundState BOTTOM = new CompoundState();

  /**
   * The compound state representing "top".
   */
  private static final CompoundState TOP = new CompoundState(SimpleInterval.infinite());

  /**
   * The compound state representing "false".
   */
  private static final CompoundState FALSE = CompoundState.singleton(BigInteger.ZERO);

  /**
   * The compound state representing "true":
   */
  private static final CompoundState TRUE = logicalFalse().invert();

  /**
   * The list of intervals this state is composed from.
   */
  private final List<SimpleInterval> intervals = new ArrayList<>();

  /**
   * Copies the given compound state.
   * @param pToCopy the compound state to copy.
   */
  private CompoundState(CompoundState pToCopy) {
    for (SimpleInterval interval : pToCopy.intervals) {
      intervals.add(interval);
    }
  }

  /**
   * Creates a new compound state from the given intervals.
   * @param pIntervals the intervals to compose this state from.
   */
  private CompoundState(SimpleInterval... pIntervals) {
    for (SimpleInterval interval : pIntervals) {
      if (interval != null) {
        intervals.add(interval);
      }
    }
  }

  /**
   * Gets an unmodifiable list containing the intervals this compound
   * state consists of.
   *
   * @return an unmodifiable list containing the intervals this compound
   * state consists of.
   */
  public List<SimpleInterval> getIntervals() {
    return Collections.unmodifiableList(this.intervals);
  }

  /**
   * Computes the union of this compound state with the given compound state.
   * @param pOther the state to unite this state with.
   * @return the union of this compound state with the given compound state.
   */
  public CompoundState unionWith(CompoundState pOther) {
    if (pOther == this || isTop() || pOther.isBottom()) { return this; }
    CompoundState current = this;
    for (SimpleInterval interval : pOther.intervals) {
      current = current.unionWith(interval);
    }
    return current;
  }

  /**
   * Computes the union of this compound state with the given simple interval.
   * @param pOther the interval to unite this state with.
   * @return the union of this compound state with the given simple interval.
   */
  public CompoundState unionWith(SimpleInterval pOther) {
    if (isBottom() || pOther.isTop()) { return new CompoundState(pOther); }
    CompoundState result = new CompoundState();
    boolean inserted = false;
    SimpleInterval lastInterval = null;
    for (SimpleInterval interval : this.intervals) {
      boolean currentInserted = false;
      if (interval.touches(lastInterval)) {
        result.intervals.remove(result.intervals.size() - 1);
        lastInterval = union(interval, lastInterval);
        result.intervals.add(lastInterval);
        currentInserted = true;
      }
      if (!inserted) {
        if (pOther.touches(lastInterval)) {
          result.intervals.remove(result.intervals.size() - 1);
          lastInterval = union(pOther, lastInterval);
          result.intervals.add(lastInterval);
          inserted = true;
        } else if (pOther.touches(interval)) {
          lastInterval = union(pOther, interval);
          result.intervals.add(lastInterval);
          inserted = true;
          currentInserted = true;
        } else {
          if (!pOther.hasLowerBound()
              || (interval.hasLowerBound() && less(pOther.getLowerBound(), interval.getLowerBound()))) {
            result.intervals.add(pOther);
            inserted = true;
          }
        }
        if (!currentInserted) {
          lastInterval = interval;
          result.intervals.add(lastInterval);
        }
      }
    }
    if (!inserted) {
      result.intervals.add(pOther);
    }
    return result;
  }

  /**
   * Computes the compound state resulting from the intersection of this compound state with the given state.
   * @param pOther the state to intersect this state with.
   * @return the compound state resulting from the intersection of this compound state with the given state.
   */
  public CompoundState intersectWith(CompoundState pOther) {
    if (isBottom() || pOther.isTop() || this == pOther) { return this; }
    if (isTop() || pOther.isBottom()) { return pOther; }
    CompoundState result = bottom();
    for (SimpleInterval otherInterval : pOther.intervals) {
      result = result.unionWith(intersectWith(otherInterval));
    }
    return result;
  }

  /**
   * Computes the compound state resulting from the intersection of this compound state with the given interval.
   * @param pOther the interval to intersect this state with.
   * @return the compound state resulting from the intersection of this compound state with the given interval.
   */
  public CompoundState intersectWith(SimpleInterval pOther) {
    if (pOther.isTop()) { return this; }
    CompoundState result = new CompoundState();
    for (SimpleInterval interval : this.intervals) {
      if (interval.intersectsWith(pOther)) {
        result = result.unionWith(interval.intersectWith(pOther));
      }
    }
    return result;
  }

  /**
   * Checks if the given state intersects with this state.
   * @param pOther the state to check for intersection with this state.
   * @return <code>true</code> if this state intersects with the given state, <code>false</code> otherwise.
   */
  public boolean intersectsWith(CompoundState pOther) {
    return !intersectWith(pOther).isBottom();
  }

  /**
   * Checks if the given interval intersects with this state.
   * @param pOther the interval to check for intersection with this state.
   * @return <code>true</code> if this state intersects with the given interval, <code>false</code> otherwise.
   */
  public boolean intersectsWith(SimpleInterval pOther) {
    return !intersectWith(pOther).isBottom();
  }

  /**
   * Checks if the given state is contained in this state.
   * @param pState the state to check for.
   * @return <code>true</code> if the given state is contained in this compound state, <code>false</code> otherwise.
   */
  public boolean contains(CompoundState pState) {
    if (this == pState) {
      return true;
    }
    return pState.equals(intersectWith(pState));
  }

  /**
   * Checks if the given interval is contained in this state.
   * @param pInterval the interval to check for.
   * @return <code>true</code> if the given interval is contained in the state, <code>false</code> otherwise.
   */
  public boolean contains(SimpleInterval pInterval) {
    if (isTop()) { return true; }
    if (isBottom() || pInterval.isTop()) { return false; }
    if (!pInterval.hasLowerBound() && hasLowerBound()) {
      return false;
    }
    if (!pInterval.hasUpperBound() && hasUpperBound()) {
      return false;
    }
    boolean hasLowerBound = pInterval.hasLowerBound();
    boolean hasUpperBound = pInterval.hasUpperBound();
    BigInteger lb = hasLowerBound ? pInterval.getLowerBound() : null;
    BigInteger ub = hasUpperBound ? pInterval.getUpperBound() : null;
    int leftInclusive = 0;
    int rightExclusive = this.intervals.size();
    while (leftInclusive < rightExclusive) {
      int index = (rightExclusive - leftInclusive) / 2;
      SimpleInterval intervalAtIndex = this.intervals.get(index);
      boolean lbIndexLeqLb = !intervalAtIndex.hasLowerBound() || hasLowerBound && intervalAtIndex.getLowerBound().compareTo(lb) <= 0;
      boolean ubIndexGeqUb = !intervalAtIndex.hasUpperBound() || hasUpperBound && intervalAtIndex.getUpperBound().compareTo(ub) >= 0;
      if (lbIndexLeqLb) { // Interval at index starts before interval
        if (ubIndexGeqUb) { // Interval at index ends after interval
          return true;
        }
        leftInclusive = index + 1;
      } else { // Interval at index starts after interval
        rightExclusive = index;
      }
    }
    return false;
  }

  /**
   * Checks if the given big integer value is contained in this state.
   * @param pValue the value to check for.
   * @return <code>true</code> if the given value is contained in the state, <code>false</code> otherwise.
   */
  public boolean contains(BigInteger pValue) {
    if (isTop()) { return true; }
    if (isBottom()) { return false; }
    return contains(SimpleInterval.singleton(pValue));
  }

  /**
   * Checks if the given long value is contained in this state.
   * @param pValue the value to check for.
   * @return <code>true</code> if the given value is contained in the state,
   * <code>false</code> otherwise.
   */
  public boolean contains(long pValue) {
    if (isTop()) { return true; }
    if (isBottom()) { return false; }
    int leftInclusive = 0;
    int rightExclusive = this.intervals.size();
    BigInteger value = BigInteger.valueOf(pValue);
    while (leftInclusive < rightExclusive) {
      int index = (rightExclusive - leftInclusive) / 2;
      SimpleInterval intervalAtIndex = this.intervals.get(index);
      boolean lbIndexLeqLb = !intervalAtIndex.hasLowerBound() || intervalAtIndex.getLowerBound().compareTo(value) <= 0;
      boolean ubIndexGeqUb = !intervalAtIndex.hasUpperBound() || intervalAtIndex.getUpperBound().compareTo(value) >= 0;
      if (lbIndexLeqLb) { // Interval at index starts before interval
        if (ubIndexGeqUb) { // Interval at index ends after interval
          return true;
        }
        leftInclusive = index + 1;
      } else { // Interval at index starts after interval
        rightExclusive = index;
      }
    }
    return false;
  }

  /**
   * Checks if this compound state is the bottom state,
   * which usually represents a contradiction.
   *
   * @return <code>true</code> if this is the bottom state, <code>false</code> otherwise.
   */
  public boolean isBottom() {
    return this.intervals.isEmpty();
  }

  /**
   * Checks if this compound state is the top state, including every possible value.
   *
   * @return <code>true</code> if this is the top state, <code>false</code> otherwise.
   */
  public boolean isTop() {
    return !isBottom() && this.intervals.get(0).isTop();
  }

  @Override
  public String toString() {
    if (isBottom()) {
      return Character.toString('\u22A5');
    }
    if (isTop()) {
      return Character.toString('\u22A4');
    }
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (!isBottom()) {
      Iterator<SimpleInterval> intervalIterator = this.intervals.iterator();
      sb.append(intervalIterator.next());
      while (intervalIterator.hasNext()) {
        sb.append(", ");
        sb.append(intervalIterator.next());
      }
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Checks if there is a lower bound to this compound state.
   * @return <code>true</code> if there is an lower bound to this compound state, <code>false</code> otherwise.
   */
  public boolean hasLowerBound() {
    if (isTop() || isBottom()) { return false; }
    return this.intervals.get(0).hasLowerBound();
  }

  /**
   * Checks if there is an upper bound to this compound state.
   * @return <code>true</code> if there is an upper bound to this compound state, <code>false</code> otherwise.
   */
  public boolean hasUpperBound() {
    if (isTop() || isBottom()) { return false; }
    return this.intervals.get(this.intervals.size() - 1).hasUpperBound();
  }

  /**
   * Returns the lower bound (may only be called if {@link #hasLowerBound()} returns true.
   * @return the lower bound of the compound state.
   */
  public BigInteger getLowerBound() {
    return this.intervals.get(0).getLowerBound();
  }

  /**
   * Returns the upper bound (may only be called if {@link #hasUpperBound()} returns true.
   * @return the upper bound of the compound state.
   */
  public BigInteger getUpperBound() {
    return this.intervals.get(this.intervals.size() - 1).getUpperBound();
  }

  /**
   * Checks if this state represents a single value.
   * @return <code>true</code> if this state represents a single value, <code>false</code> otherwise.
   */
  public boolean isSingleton() {
    return !isBottom() && this.intervals.get(0).isSingleton();
  }

  /**
   * Returns SOME value that is contained in the state or <code>null</code>
   * if the state is the "bottom" state.
   *
   * @return some value that is contained in the state or <code>null</code>
   * if the state is the "bottom" state.
   */
  public @Nullable
  BigInteger getValue() {
    if (isBottom()) { return null; }
    if (isTop()) { return BigInteger.ZERO; }
    for (SimpleInterval interval : this.intervals) {
      if (interval.hasLowerBound()) { return interval.getLowerBound(); }
      if (interval.hasUpperBound()) { return interval.getUpperBound(); }
    }
    return null;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    if (pOther instanceof CompoundState) {
      CompoundState other = (CompoundState) pOther;
      if (this.intervals.size() != other.intervals.size()) { return false; }
      Iterator<SimpleInterval> itThis = this.intervals.iterator();
      Iterator<SimpleInterval> itOther = other.intervals.iterator();
      while (itThis.hasNext()) {
        if (!itThis.next().equals(itOther.next())) { return false; }
      }
      return true;
    }
    return false;

  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.intervals.toArray());
  }

  /**
   * Computes the state spanning from the compound state's lower bound and its upper bound.
   * @return the state spanning from the compound state's lower bound and its upper bound.
   */
  public CompoundState span() {
    BigInteger lowerBound = null;
    BigInteger upperBound = null;
    if (hasLowerBound()) {
      lowerBound = getLowerBound();
    }
    if (hasUpperBound()) {
      upperBound = getUpperBound();
    }
    return CompoundState.of(createSimpleInterval(lowerBound, upperBound));
  }

  /**
   * Inverts the state so that all values previously contained are no longer contained and vice versa.
   * Do not confuse this with negating ({@link #negate()}) the state.
   * @return the inverted state.
   */
  public CompoundState invert() {
    if (isTop()) { return bottom(); }
    if (isBottom()) { return top(); }
    CompoundState result = new CompoundState();
    Queue<SimpleInterval> queue = new ArrayDeque<>(this.intervals);
    BigInteger currentLowerBound = null;
    if (!hasLowerBound()) {
      currentLowerBound = queue.poll().getUpperBound();
    }
    while (!queue.isEmpty()) {
      SimpleInterval current = queue.poll();
      result = result.unionWith(createSimpleInterval(currentLowerBound, current.getLowerBound().subtract(BigInteger.ONE)));
      if (current.hasUpperBound()) {
        currentLowerBound = current.getUpperBound();
      } else {
        currentLowerBound = null;
      }
    }
    if (currentLowerBound != null) {
      result.intervals.add(createSimpleInterval(currentLowerBound.add(BigInteger.ONE), null));
    }
    return result;
  }

  /**
   * Negates the state. Do not confuse this with inverting ({@link #invert()}) the state.
   * @return the negated state.
   */
  public CompoundState negate() {
    if (isTop() || isBottom()) { return this; }
    CompoundState result = new CompoundState();
    for (SimpleInterval simpleInterval : this.intervals) {
      result = result.unionWith(simpleInterval.negate());
    }
    return result;
  }

  /**
   * Checks if positive values are contained in the state.
   * @return <code>true</code> if this state contains positive values.
   */
  public boolean containsPositive() {
    if (isBottom()) { return false; }
    if (isTop()) { return true; }
    for (SimpleInterval interval : this.intervals) {
      if (interval.containsPositive()) { return true; }
    }
    return false;
  }


  /**
   * Checks if negative values are contained in the state.
   * @return <code>true</code> if this state contains negative values.
   */
  public boolean containsNegative() {
    if (isBottom()) { return false; }
    if (isTop()) { return true; }
    for (SimpleInterval interval : this.intervals) {
      if (interval.containsNegative()) { return true; }
    }
    return false;
  }

  /**
   * Checks if zero is contained in the state.
   * @return <code>true</code> if this state contains the zero value.
   */
  public boolean containsZero() {
    return contains(0);
  }

  /**
   * Creates a compound state similar to this state but with a negative infinity lower bound or bottom if this state is bottom.
   *
   * @return a compound state similar to this state but with a negative infinity lower bound or bottom if this state is bottom.
   */
  public CompoundState extendToNegativeInfinity() {
    if (!hasLowerBound()) { return this; }
    CompoundState result = new CompoundState(this);
    int index = 0;
    result.intervals.set(index, result.intervals.get(index).extendToNegativeInfinity());
    return result;
  }

  /**
   * Creates a compound state similar to this state but with a positive infinity upper bound or bottom if this state is bottom.
   *
   * @return a compound state similar to this state but with a positive infinity upper bound or bottom if this state is bottom.
   */
  public CompoundState extendToPositiveInfinity() {
    if (!hasUpperBound()) { return this; }
    CompoundState result = new CompoundState(this);
    int index = result.intervals.size() - 1;
    result.intervals.set(index, result.intervals.get(index).extendToPositiveInfinity());
    return result;
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundState add(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.ADD_OPERATOR, pValue);
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundState add(final long pValue) {
    return add(BigInteger.valueOf(pValue));
  }

  /**
   * Computes the state resulting from adding the given interval to this
   * state.
   *
   * @param pInterval the interval to add to this state.
   * @return the state resulting from adding the given interval to this
   * state.
   */
  public CompoundState add(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.ADD_OPERATOR, pInterval);
  }

  /**
   * Computes the state resulting from adding the given state to this
   * state.
   *
   * @param pState the state to add to this state.
   * @return the state resulting from adding the given state to this
   * state.
   */
  public CompoundState add(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.ADD_OPERATOR, pState);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given value.
   *
   * @param pValue the value to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given value.
   */
  public CompoundState multiply(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.MULTIPLY_OPERATOR, pValue);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given interval.
   *
   * @param pInterval the interval to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given interval.
   */
  public CompoundState multiply(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.MULTIPLY_OPERATOR, pInterval);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given state.
   *
   * @param pState the state to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given state.
   */
  public CompoundState multiply(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.MULTIPLY_OPERATOR, pState);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * value.
   *
   * @param pValue the value to divide this state by.
   * @return the state resulting from dividing this state by the given
   * value.
   */
  public CompoundState divide(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.DIVIDE_OPERATOR, pValue);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * interval.
   *
   * @param pInterval the interval to divide this state by.
   * @return the state resulting from dividing this state by the given
   * interval.
   */
  public CompoundState divide(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.DIVIDE_OPERATOR, pInterval);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * state.
   *
   * @param pState the state to divide this state by.
   * @return the state resulting from dividing this state by the given
   * state.
   */
  public CompoundState divide(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.DIVIDE_OPERATOR, pState);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given value.
   *
   * @param pValue the value to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given value.
   */
  public CompoundState modulo(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.MODULO_OPERATOR, pValue);
  }


  /**
   * Computes the state representing the remainder of dividing this state
   * by the given interval.
   *
   * @param pInterval the interval to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given interval.
   */
  public CompoundState modulo(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.MODULO_OPERATOR, pInterval);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given state.
   *
   * @param pState the state to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given state.
   */
  public CompoundState modulo(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.MODULO_OPERATOR, pState);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given value.
   */
  public CompoundState shiftLeft(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.SHIFT_LEFT_OPERATOR, pValue);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given interval.
   */
  public CompoundState shiftLeft(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.SHIFT_LEFT_OPERATOR, pInterval);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given state.
   */
  public CompoundState shiftLeft(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.SHIFT_LEFT_OPERATOR, pState);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given value.
   */
  public CompoundState shiftRight(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.SHIFT_RIGHT_OPERATOR, pValue);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given interval.
   */
  public CompoundState shiftRight(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.SHIFT_RIGHT_OPERATOR, pInterval);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given state.
   */
  public CompoundState shiftRight(final CompoundState pState) {
    return applyOperationToAllAndUnite(ICCOperator.SHIFT_RIGHT_OPERATOR, pState);
  }

  /**
   * Computes the state resulting from the logical "==" comparison
   * of the expressions represented by this state and the given state,
   * which is
   * a state representing true if both states are equal singleton states,
   * a state representing false if both states do not intersect,
   * top if they do intersect but are not equal singletons and
   * bottom if one of the states is bottom.
   *
   * Do not confuse this method with {@link #equals()} which tests two
   * states for equality; while the states [0,1] and [0,1] are equal
   * states, they do not guarantee that two different concrete states
   * abstracted by those states are equal: one might be 0 while the other
   * could be 1.
   *
   * @param pState the comparison state.
   * @return a state representing true if both states are equal singleton
   * states, a state representing false if both states do not intersect,
   * top if they do intersect but are not equal singletons and bottom
   * if one of the states is bottom.
   */
  public CompoundState logicalEquals(final CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && equals(pState)) { return CompoundState.logicalTrue(); }
    if (!intersectsWith(pState)) { return CompoundState.logicalFalse(); }
    return top();
  }

  /**
   * Checks whether this state definitely evaluates to <code>false</code>
   * or not.
   *
   * @return <code>true</code> if this state definitely evaluates to
   * <code>false</code>, <code>false</code> otherwise.
   */
  public boolean isDefinitelyFalse() {
    return equals(logicalFalse());
  }

  /**
   * Checks whether this state definitely evaluates to <code>true</code>
   * or not.
   *
   * @return <code>true</code> if this state definitely evaluates to
   * <code>true</code>, <code>false</code> otherwise.
   */
  public boolean isDefinitelyTrue() {
    return !isBottom() && !containsZero();
  }

  /**
   * Computes the boolean state representing the result of checking if
   * this state is greater than the given state. There are four
   * possible outcomes: Either all values of this state are greater
   * than all values contained in the given state, resulting in true
   * being returned, or all values of this state are less than or equal
   * to all values contained in the given state, resulting in false being
   * returned, or there are values in this state which are greater than,
   * but also values that are less than or equal to some values in the
   * given state, which results in top being returned, or one of the
   * states is bottom, which results in bottom being returned.
   *
   * @param pState the state to compare this state to.
   * @return true if all values contained in this state are greater than
   * all values in the given state, false if all values contained in this
   * state are less than or equal to all values in the given state,
   * bottom if one of the states is bottom, top otherwise.
   */
  public CompoundState greaterThan(final CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (hasLowerBound() && pState.hasUpperBound() && getLowerBound().compareTo(pState.getUpperBound()) > 0) { return TRUE; }
    if (hasUpperBound() && pState.hasLowerBound() && getUpperBound().compareTo(pState.getLowerBound()) <= 0) { return FALSE; }
    return top();
  }

  /**
   * Computes the boolean state representing the result of checking if
   * this state is greater than or equal to the given state. There are
   * basically four possible outcomes: Either all values of this state
   * are greater than or equal to all values contained in the given
   * state, resulting in true being returned, or all values of this state
   * are less than all values contained in the given state, resulting in
   * false being returned, or there are values in this state which are
   * greater than or equal to, but also values that are less than some
   * values in the given state, which results in top being returned,
   * or one of the states is bottom, which results in bottom being
   * returned.
   *
   * @param pState the state to compare this state to.
   * @return true if all values contained in this state are greater
   * than or equal to all values in the given state, false if all
   * values contained in this state are less than all values in the
   * given state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundState greaterEqual(final CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (hasLowerBound() && pState.hasUpperBound() && getLowerBound().compareTo(pState.getUpperBound()) >= 0) { return TRUE; }
    if (hasUpperBound() && pState.hasLowerBound() && getUpperBound().compareTo(pState.getLowerBound()) < 0) { return FALSE; }
    return top();
  }

  /**
   * Computes the boolean state representing the result of checking if
   * this state is less than the given state. There are four possible
   * outcomes: Either all values of this state are less than all values
   * contained in the given state, resulting in true being returned, or
   * all values of this state are greater than or equal to all values
   * contained in the given state, resulting in false being returned,
   * or there are values in this state which are less than, but also
   * values that are greater than or equal to some values in the given
   * state, which results in top being returned, or one of the states
   * is bottom, which results in bottom being returned.
   *
   * @param pState the state to compare this state to.
   * @return true if all values contained in this state are less than
   * all values in the given state, false if all values contained in
   * this state are greater than or equal to all values in the given
   * state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundState lessThan(final CompoundState pState) {
    return greaterEqual(pState).logicalNot();
  }

  /**
   * Computes the boolean state representing the result of checking if
   * this state is less than or equal to the given state. There are
   * four possible outcomes: Either all values of this state are less
   * than or equal to all values contained in the given state,
   * resulting in true being returned, or all values of this state are
   * greater than all values contained in the given state, resulting in
   * false being returned, or there are values in this state which are
   * less than or equal to, but also values that are greater than some
   * values in the given state, which results in top being returned,
   * or one of the states is bottom, which results in bottom being
   * returned.
   *
   * @param pState the state to compare this state to.
   * @return true if all values contained in this state are less than
   * or equal to all values in the given state, false if all values
   * contained in this state are greater than all values in the given
   * state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundState lessEqual(final CompoundState pState) {
    return greaterThan(pState).logicalNot();
  }

  /**
   * Computes the logical and over this state and the given state. If
   * both these states represent true, a state representing true is
   * returned. If one of the states represents false, a state
   * representing false is returned. Otherwise, top is returned.
   *
   * @param pState the state to connect to this state with a
   * conjunction.
   * @return a state representing true if this state or the given state
   * represents true, a state representing false if one of these states
   * represents false, top otherwise.
   */
  public CompoundState logicalAnd(final CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && containsZero() || pState.isSingleton() && pState.containsZero()) { return logicalFalse(); }
    if (!containsZero() && !pState.containsZero()) { return logicalTrue(); }
    return top();
  }

  /**
   * Computes the logical or over this state and the given state. If
   * one of these states represents true, a state representing true is
   * returned. If neither of the states represents true, a state
   * representing false is returned. Otherwise, top is returned.
   * @param pState the state to connect to this state with a
   * disjunction.
   * @return a state representing true if one of this state or the
   * given state represents true, a state representing false if none of
   * these states represents true, top otherwise.
   */
  public CompoundState logicalOr(final CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && containsZero() && pState.isSingleton() && pState.containsZero()) { return logicalFalse(); }
    if (!containsZero() || !pState.containsZero()) { return logicalTrue(); }
    return top();
  }

  /**
   * Computes the logical negation of this state. If this state
   * represents false, a state representing true is returned. If this
   * state does not contain false, a state representing false is
   * returned. If this state is bottom, bottom is returned. Otherwise,
   * a state representing top is returned.
   *
   * Do not confuse this method with mathematical negation or state
   * inversion. For mathematical negation, see {@link #negate()}.
   * For state inversion, see {@link #invert()}.
   *
   * @return a state representing true if this state represents false,
   * a state representing false if this state does not contain the
   * false state and a state representing top if the this state
   * contains both true and false.
   */
  public CompoundState logicalNot() {
    if (isBottom()) { return this; }
    if (isSingleton() && containsZero()) {
      return TRUE;
    } else if (!containsZero()) { return FALSE; }
    return top();
  }

  /**
   * Computes the state resulting from performing the bitwise
   * and-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise and-operation on the states' values is returned.
   * Otherwise, top is returned.
   *
   * @param pState the state to bit-wise-and with this state.
   * @return the state resulting from performing the bitwise
   * and-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise and-operation on the states' values is returned.
   * Otherwise, top is returned.
   */
  public CompoundState binaryAnd(CompoundState pState) {
    if (isBottom() || pState.isBottom()) {
      return bottom();
    }
    if (pState.isSingleton()) {
      if (isSingleton()) {
        return CompoundState.singleton(getValue().and(pState.getValue()));
      }
      if (pState.getValue().equals(BigInteger.ZERO)) {
        return CompoundState.singleton(BigInteger.ZERO);
      }
    }
    // TODO maybe a more exact implementation is possible?
    return top();
  }

  /**
   * Computes the state resulting from performing the bitwise
   * xor-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise xor-operation on the states' values is returned.
   * Otherwise, top is returned.
   *
   * @param pState the state to bit-wise-xor with this state.
   * @return the state resulting from performing the bitwise
   * xor-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise xor-operation on the states' values is returned.
   * Otherwise, top is returned.
   */
  public CompoundState binaryXor(CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && pState.isSingleton()) { return CompoundState.singleton(getValue().xor(pState.getValue())); }
    // TODO maybe a more exact implementation is possible?
    return top();
  }

  /**
   * Computes the state resulting from flipping the bits of the
   * values represented by this state.
   *
   * @return the state resulting from flipping the bits of the
   * values represented by this state.
   */
  public CompoundState binaryNot() {
    if (isBottom()) { return bottom(); }
    if (isSingleton()) {
      return CompoundState.singleton(getValue().not());
    }
    if (!containsNegative()) {
      return singleton(0).extendToNegativeInfinity();
    } else if (!containsPositive()) {
      return singleton(0).extendToPositiveInfinity();
    }
    // TODO maybe a more exact implementation is possible?
    return top();
  }

  /**
   * Computes the state resulting from performing the bitwise
   * or-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise or-operation on the states' values is returned.
   * Otherwise, top is returned.
   *
   * @param pState the state to bit-wise-or with this state.
   * @return the state resulting from performing the bitwise
   * or-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise or-operation on the states' values is returned.
   * Otherwise, top is returned.
   */
  public CompoundState binaryOr(CompoundState pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton()) {
      return CompoundState.singleton(getValue().or(pState.getValue()));
    }
    if (pState.getValue().equals(BigInteger.ZERO)) {
      return this;
    }
    // TODO maybe a more exact implementation is possible?
    return top();
  }

  /**
   * Computes the logical exclusive or over this state and the given
   * state. If one of these states represents the false state while the
   * other does not contain the false state, a state representing true is
   * returned. If either both of the states represent the false state or
   * neither of them represents false, a state representing false is
   * returned. If one of the states is bottom, bottom is returned.
   * Otherwise top is returned.
   *
   * @param pState the state to XOR with this state.
   * @return a state representing true if either this state or the given
   * state represents false while the other represents true, a state
   * representing false if either both these states represent true or
   * both represent false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public CompoundState logicalXor(CompoundState pState) {
    return logicalAnd(logicalOr(this, pState), logicalNot(logicalAnd(this, pState)));
  }

  /**
   * Computes the logical and over the given states. If both given states
   * represent true, a state representing true is returned. If one of
   * the states represents false, a state representing false is returned.
   * If one of the states is bottom, bottom is returned. Otherwise, top
   * is returned.
   *
   * @param p1 one of the states to apply the and operation on.
   * @param p2 one of the states to apply the and operation on.
   * @return a state representing true if both given states represent
   * true, a state representing false if one of the given states
   * represents false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundState logicalAnd(CompoundState p1, CompoundState p2) {
    return p1.logicalAnd(p2);
  }

  /**
   * Computes the logical or over the given states. If one of the given
   * states represents true, a state representing true is returned. If
   * neither of the states represents true, a state representing false
   * is returned. If one of the states is bottom, bottom is returned.
   * Otherwise, top is returned.
   *
   * @param p1 one of the states to apply the or operation on.
   * @param p2 one of the states to apply the or operation on.
   * @return a state representing true if one of the given states
   * represents true, a state representing false if none of the given
   * states represents true, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundState logicalOr(CompoundState p1, CompoundState p2) {
    return p1.logicalOr(p2);
  }

  /**
   * Computes the logical exclusive or over the given states. If one of
   * the given states represents the false state while the other does not
   * contain the false state, a state representing true is returned. If
   * either both of the states represent the false state or neither of
   * them represents false, a state representing false is returned. If
   * one of the states is bottom, bottom is returned. Otherwise top is
   * returned.
   *
   * @param p1 one of the states to apply the exclusive or operation on.
   * @param p2 one of the states to apply the exclusive or operation on.
   * @return a state representing true if one of the given states
   * represents false while the other represents true, a state
   * representing false if either both given states represent true or
   * both represent false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundState logicalXor(CompoundState p1, CompoundState p2) {
    return p1.logicalXor(p2);
  }

  /**
   * Logically negates the given state. If the state represents false,
   * a state representing true is returned. If the state does not contain
   * false, a state representing false is returned. If the state is
   * bottom, bottom is returned. Otherwise, a state representing top is
   * returned.
   *
   * Do not confuse this method with mathematical negation or state
   * inversion. For mathematical negation, see {@link #negate()}.
   * For state inversion, see {@link #invert()}.
   *
   * @param pState the state to logically negate.
   * @return a state representing true if the given state represents
   * false, a state representing false if the given state does not
   * contain the false state, bottom if the given state is bottom and a
   * top if the given state contains both true and false.
   */
  public static CompoundState logicalNot(CompoundState pState) {
    return pState.logicalNot();
  }

  /**
   * Applies the given operator and operand to every interval in this
   * state and unites the results.
   *
   * @param pOperator the interval operator to apply to the intervals.
   * @param pOperand the second operand of each operator application.
   * @return the state resulting from applying the given operator to
   * each interval of this state and the given value and uniting the
   * results.
   */
  private <T> CompoundState applyOperationToAllAndUnite(Operator<SimpleInterval, T, CompoundState> pOperator, T pOperand) {
    CompoundState result = bottom();
    for (SimpleInterval interval : this.intervals) {
      CompoundState current = pOperator.apply(interval, pOperand);
      if (current != null) {
        result = result.unionWith(current);
        if (result.isTop()) { return result; }
      }
    }
    return result;
  }

  /**
   * Union two simple intervals. The intervals must touch each other for this
   * operation to be possible.
   *
   * @param a one of the intervals to be united.
   * @param b one of the intervals to be united.
   * @return the union of the two intervals.
   */
  private static SimpleInterval union(SimpleInterval a, SimpleInterval b) {
    checkState(a.touches(b), "Cannot unite intervals that do not touch.");
    return createSimpleInterval(lowestBound(a, b), highestBound(a, b));
  }

  /**
   * Returns the lowest bound of the two given intervals, which might be <code>null</code> for negative infinity.
   * @param a one if the intervals to get the lowest lower bound from.
   * @param b one if the intervals to get the lowest lower bound from.
   * @return the lowest bound of the two given intervals, which might be <code>null</code> for negative infinity.
   */
  @Nullable
  private static BigInteger lowestBound(SimpleInterval a, SimpleInterval b) {
    if (!a.hasLowerBound() || !b.hasLowerBound()) { return null; }
    BigInteger aLowerBound = a.getLowerBound();
    BigInteger bLowerBound = b.getLowerBound();
    return lessOrEqual(aLowerBound, bLowerBound) ? aLowerBound : bLowerBound;
  }

  /**
   * Returns the highest bound of the two given intervals, which might be <code>null</code> for positive infinity.
   * @param a one if the intervals to get the highest upper bound from.
   * @param b one if the intervals to get the highest upper bound from.
   * @return the highest bound of the two given intervals, which might be <code>null</code> for positive infinity.
   */
  private static BigInteger highestBound(SimpleInterval a, SimpleInterval b) {
    if (!a.hasUpperBound() || !b.hasUpperBound()) { return null; }
    BigInteger aUpperBound = a.getUpperBound();
    BigInteger bUpperBound = b.getUpperBound();
    return lessOrEqual(bUpperBound, aUpperBound) ? aUpperBound : bUpperBound;
  }

  /**
   * Checks if the first given big integer is less than the second given big integer, non of which may be <code>null</code>.
   * @param first the reference value.
   * @param second the value the reference value is compared to.
   * @return <code>true</code> if <code>a</code> is less than <code>b</code>, <code>false</code> otherwise.
   */
  private static boolean less(BigInteger first, BigInteger second) {
    return first.compareTo(second) < 0;
  }

  /**
   * Checks if the first given big integer is less than or equal to the second given big integer, non of which may be <code>null</code>.
   * @param first the reference value.
   * @param second the value the reference value is compared to.
   * @return <code>true</code> if <code>a</code> is less than or equal to <code>b</code>, <code>false</code> otherwise.
   */
  private static boolean lessOrEqual(BigInteger first, BigInteger second) {
    return first.compareTo(second) <= 0;
  }

  /**
   * Creates a new compound state from the given simple interval.
   * @param interval the interval to base this compound state on.
   * @return a new compound state representation of the given simple interval.
   */
  public static CompoundState of(SimpleInterval interval) {
    return new CompoundState(interval);
  }

  /**
   * Gets a compound state representing the given big integer value.
   * @param pValue the value to be represented by the state.
   * @return a compound state representing the given big integer value.
   */
  public static CompoundState singleton(BigInteger pValue) {
    return CompoundState.of(SimpleInterval.singleton(pValue));
  }

  /**
   * Gets a compound state representing the given long value.
   * @param pValue the value to be represented by the state.
   * @return a compound state representing the given long value.
   */
  public static CompoundState singleton(long pValue) {
    return singleton(BigInteger.valueOf(pValue));
  }

  /**
   * Gets a compound state representing "bottom".
   * @return a compound state representing "bottom".
   */
  public static CompoundState bottom() {
    return BOTTOM;
  }

  /**
   * Gets a compound state representing "top".
   * @return a compound state representing "top".
   */
  public static CompoundState top() {
    return TOP;
  }

  public static CompoundState logicalFalse() {
    return FALSE;
  }

  public static CompoundState logicalTrue() {
    return TRUE;
  }

  /**
   * Creates a simple interval from the two given bounds.
   * <code>null</code> parameters are allowed and used to represent negative or
   * positive infinity.
   * @param lowerBound the lower bound of the resulting interval. Use <code>null</code> to denote negative infinity.
   * @param upperBound the upper bound of the resulting interval. Use <code>null</code> to denote positive infinity.
   * @return a simple interval with the given bounds.
   */
  private static SimpleInterval createSimpleInterval(@Nullable BigInteger lowerBound, @Nullable BigInteger upperBound) {
    if (lowerBound == null) {
      if (upperBound == null) { return SimpleInterval.infinite(); }
      return SimpleInterval.singleton(upperBound).extendToNegativeInfinity();
    }
    if (upperBound == null) { return SimpleInterval.singleton(lowerBound).extendToPositiveInfinity(); }
    return SimpleInterval.of(lowerBound, upperBound);
  }

  /**
   * Computes the compound state spanning from the lowest of the two given states' lower bounds to their highest upper bound.
   * @param pLeftValue one of the states to span over.
   * @param pRightValue one of the states to span over.
   * @return the compound state spanning from the lowest of the two given states' lower bounds to their highest upper bound.
   */
  public static CompoundState span(CompoundState pLeftValue, CompoundState pRightValue) {
    return pLeftValue.span().unionWith(pRightValue.span()).span();
  }

  /**
   * Unites the given states.
   * @param pLeftValue one of the states to be united.
   * @param pRightValue one of the states to be united.
   * @return the union of the given states.
   */
  public static CompoundState union(CompoundState pLeftValue, CompoundState pRightValue) {
    return pLeftValue.unionWith(pRightValue);
  }

  /**
   * Gets the state representing the given boolean value.
   *
   * @param value the boolean value to represent as compound state.
   * @return the state representing the given boolean value.
   */
  public static CompoundState fromBoolean(boolean value) {
    return value ? logicalTrue() : logicalFalse();
  }

}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;

import org.sosy_lab.cpachecker.cpa.invariants.operators.Operator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical.ICCOperator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical.IICOperator;
import org.sosy_lab.cpachecker.cpa.invariants.operators.mathematical.ISCOperator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Instances of this class represent compound states of intervals.
 */
public class CompoundMathematicalInterval implements CompoundIntegralInterval {

  private static final CompoundMathematicalInterval ZERO = new CompoundMathematicalInterval(SimpleInterval.singleton(BigInteger.ZERO));

  private static final CompoundMathematicalInterval ONE = new CompoundMathematicalInterval(SimpleInterval.singleton(BigInteger.ONE));

  private static final CompoundMathematicalInterval MINUS_ONE = new CompoundMathematicalInterval(SimpleInterval.singleton(BigInteger.valueOf(-1)));
  /**
   * The compound state representing "bottom".
   */
  private static final CompoundMathematicalInterval BOTTOM = new CompoundMathematicalInterval();

  /**
   * The compound state representing "top".
   */
  private static final CompoundMathematicalInterval TOP = new CompoundMathematicalInterval(SimpleInterval.infinite());

  /**
   * The compound state representing "false".
   */
  private static final CompoundMathematicalInterval FALSE = ZERO;

  /**
   * The compound state representing "true":
   */
  private static final CompoundMathematicalInterval TRUE = logicalFalse().invert();

  /**
   * The list of intervals this state is composed from.
   */
  private final SimpleInterval[] intervals;

  /**
   * Constructs the bottom state. This should only be invoked by the constant declaration.
   */
  private CompoundMathematicalInterval() {
    this.intervals = new SimpleInterval[0];
  }

  /**
   * Creates a new compound state from the given interval. This should only be
   * invoked via the {@link CompoundMathematicalInterval#getInternal} functions.
   *
   * @param pInterval the interval to compose this state from. Must not be
   * {@code null}.
   */
  private CompoundMathematicalInterval(SimpleInterval pInterval) {
    this.intervals = new SimpleInterval[] { pInterval };
  }

  /**
   * Creates a new compound state from the given intervals. This should only be
   * invoked via the {@link CompoundMathematicalInterval#getInternal} functions.
   *
   * @param pIntervals the intervals to compose this state from. None of the
   * intervals must be {@code null}.
   */
  private CompoundMathematicalInterval(SimpleInterval[] pIntervals) {
    this.intervals = pIntervals;
  }

  /**
   * Gets a compound interval represented by the given interval. Use this
   * factory method over the constructor.
   *
   * @param pInterval the interval to compose this state from. Must not be
   * {@code null}.
   *
   * @return a compound interval as represented by the given interval.
   */
  private static CompoundMathematicalInterval getInternal(SimpleInterval pInterval) {
    CompoundMathematicalInterval cached = getCached(pInterval);
    if (cached != null) {
      return cached;
    }
    return new CompoundMathematicalInterval(pInterval);
  }

  private static CompoundMathematicalInterval getCached(SimpleInterval pInterval) {
    if (pInterval.isSingleton()) {
      BigInteger value = pInterval.getLowerBound();
      if (value.equals(BigInteger.ONE)) {
        return ONE;
      }
      if (value.equals(BigInteger.ZERO)) {
        return ZERO;
      }
      if (value.equals(MINUS_ONE.intervals[0].getLowerBound())) {
        return MINUS_ONE;
      }
    }
    return null;
  }

  /**
   * Gets a compound interval represented by the given intervals. Use this
   * factory method over the constructor.
   *
   * @param pIntervals the intervals to compose this state from. None of the
   * intervals must be {@code null}.
   *
   * @return a compound interval as represented by the given intervals.
   */
  private static CompoundMathematicalInterval getInternal(SimpleInterval[] pIntervals) {
    if (pIntervals.length == 0) {
      return bottom();
    }
    if (pIntervals.length == 1) {
      CompoundMathematicalInterval cached = getCached(pIntervals[0]);
      if (cached != null) {
        return cached;
      }
    }
    return new CompoundMathematicalInterval(pIntervals);
  }

  /**
   * Gets the number of intervals.
   *
   * @return the number of intervals.
   */
  public int getNumberOfIntervals() {
    return this.intervals.length;
  }

  /**
   * Gets an unmodifiable list containing the intervals this compound
   * state consists of.
   *
   * @return an unmodifiable list containing the intervals this compound
   * state consists of.
   */
  @Override
  public List<SimpleInterval> getIntervals() {
    return Collections.unmodifiableList(Arrays.asList(this.intervals));
  }

  @Override
  public List<CompoundMathematicalInterval> splitIntoIntervals() {
    return Lists.transform(Arrays.asList(this.intervals), CompoundMathematicalInterval::of);
  }

  /**
   * Computes the union of this compound state with the given compound state.
   * @param pOther the state to unite this state with.
   *
   * @return the union of this compound state with the given compound state.
   */
  public CompoundMathematicalInterval unionWith(CompoundMathematicalInterval pOther) {
    if (pOther == this || isTop() || pOther.isBottom()) { return this; }
    if (pOther.isTop() || isBottom()) { return pOther; }
    CompoundMathematicalInterval current = this;
    for (SimpleInterval interval : pOther.intervals) {
      current = current.unionWith(interval);
    }
    return current;
  }

  /**
   * Computes the union of this compound state with the given simple interval.
   * @param pOther the interval to unite this state with.
   *
   * @return the union of this compound state with the given simple interval.
   */
  public CompoundMathematicalInterval unionWith(SimpleInterval pOther) {
    if (contains(pOther)) { return this; }
    if (isBottom() || pOther.isTop()) { return getInternal(pOther); }
    ArrayList<SimpleInterval> resultIntervals = new ArrayList<>();
    int start = 0;
    SimpleInterval lastInterval = null;
    if (pOther.hasLowerBound() && hasUpperBound()) {
      BigInteger pOtherLB = pOther.getLowerBound();
      SimpleInterval currentLocal = this.intervals[start];
      while (currentLocal != null && pOtherLB.compareTo(currentLocal.getUpperBound()) > 0) {
        resultIntervals.add(currentLocal);
        ++start;
        lastInterval = currentLocal;
        currentLocal = start < this.intervals.length ? this.intervals[start] : null;
        assert currentLocal == null || currentLocal.hasUpperBound() : toString();
      }
    }
    boolean inserted = false;
    for (int index = start; index < this.intervals.length; ++index) {
      SimpleInterval interval = this.intervals[index];
      boolean currentInserted = false;
      if (interval.touches(lastInterval)) {
        lastInterval = union(interval, lastInterval);
        resultIntervals.set(resultIntervals.size() - 1, lastInterval);
        currentInserted = true;
      }
      if (!inserted) {
        if (pOther.touches(lastInterval)) {
          lastInterval = union(pOther, lastInterval);
          if (lastInterval.touches(interval)) {
            lastInterval = union(lastInterval, interval);
            currentInserted = true;
          }
          resultIntervals.set(resultIntervals.size() - 1, lastInterval);
          inserted = true;
        } else if (pOther.touches(interval)) {
          lastInterval = union(pOther, interval);
          resultIntervals.add(lastInterval);
          inserted = true;
          currentInserted = true;
        } else {
          if (!pOther.hasLowerBound()
              || (interval.hasLowerBound() && less(pOther.getLowerBound(), interval.getLowerBound()))) {
            resultIntervals.add(pOther);
            inserted = true;
          }
        }
        if (!currentInserted) {
          lastInterval = interval;
          resultIntervals.add(lastInterval);
        }
      } else if (!currentInserted) {
        lastInterval = interval;
        resultIntervals.add(lastInterval);
      }
    }
    if (!inserted) {
      if (pOther.touches(lastInterval)) {
        resultIntervals.remove(resultIntervals.size() - 1);
        lastInterval = union(pOther, lastInterval);
        resultIntervals.add(lastInterval);
      } else {
        resultIntervals.add(pOther);
      }
    }
    SimpleInterval[] resultArray = new SimpleInterval[resultIntervals.size()];
    return getInternal(resultIntervals.toArray(resultArray));
  }

  /**
   * Computes the compound state resulting from the intersection of this compound state with the given state.
   * @param pOther the state to intersect this state with.
   *
   * @return the compound state resulting from the intersection of this compound state with the given state.
   */
  public CompoundMathematicalInterval intersectWith(CompoundMathematicalInterval pOther) {
    if (isBottom() || pOther.isTop() || this == pOther) { return this; }
    if (isTop() || pOther.isBottom()) { return pOther; }
    if (pOther.contains(this)) {
      return this;
    }
    CompoundMathematicalInterval result = bottom();
    for (SimpleInterval otherInterval : pOther.intervals) {
      result = result.unionWith(intersectWith(otherInterval));
    }
    return result;
  }

  /**
   * Computes the compound state resulting from the intersection of this compound state with the given interval.
   * @param pOther the interval to intersect this state with.
   *
   * @return the compound state resulting from the intersection of this compound state with the given interval.
   */
  public CompoundMathematicalInterval intersectWith(SimpleInterval pOther) {
    if (isBottom() || pOther.isTop()) { return this; }
    if (contains(pOther)) { return CompoundMathematicalInterval.of(pOther); }
    if (this.intervals.length == 1 && pOther.contains(this.intervals[0])) { return this; }
    CompoundMathematicalInterval result = bottom();
    final int lbIndex;
    if (pOther.hasLowerBound()) {
      int intervalIndex = intervalIndexOf(pOther.getLowerBound());
      lbIndex = intervalIndex >= 0 ? intervalIndex : (-intervalIndex - 1);
    } else {
      lbIndex = 0;
    }
    final int ubIndex;
    if (pOther.hasUpperBound()) {
      int intervalIndex = intervalIndexOf(pOther.getUpperBound());
      ubIndex = intervalIndex >= 0 ? intervalIndex : (-intervalIndex - 1);
    } else {
      ubIndex = this.intervals.length - 1;
    }
    for (int i = lbIndex; i <= ubIndex; ++i) {
      SimpleInterval interval = intervals[i];
      if (interval.intersectsWith(pOther)) {
        result = result.unionWith(interval.intersectWith(pOther));
      }
    }
    return result;
  }

  /**
   * Checks if the given state intersects with this state.
   * @param pOther the state to check for intersection with this state.
   *
   * @return <code>true</code> if this state intersects with the given state, <code>false</code> otherwise.
   */
  public boolean intersectsWith(CompoundMathematicalInterval pOther) {
    if (contains(pOther)) {
      return !pOther.isBottom();
    }
    if (pOther.contains(this)) {
      return !isBottom();
    }
    return !intersectWith(pOther).isBottom();
  }

  /**
   * Checks if the given interval intersects with this state.
   * @param pOther the interval to check for intersection with this state.
   *
   * @return <code>true</code> if this state intersects with the given interval, <code>false</code> otherwise.
   */
  public boolean intersectsWith(SimpleInterval pOther) {
    if (contains(pOther)) {
      return true;
    }
    return !intersectWith(pOther).isBottom();
  }

  /**
   * Checks if the given state is contained in this state.
   * @param pState the state to check for.
   *
   * @return <code>true</code> if the given state is contained in this compound state, <code>false</code> otherwise.
   */
  public boolean contains(CompoundMathematicalInterval pState) {
    if (this == pState || isTop()) {
      return true;
    }
    for (SimpleInterval interval : pState.intervals) {
      if (!contains(interval)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the given interval is contained in this state.
   * @param pInterval the interval to check for.
   *
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
    int rightExclusive = this.intervals.length;
    while (leftInclusive < rightExclusive) {
      int index = IntMath.mean(leftInclusive, rightExclusive);
      SimpleInterval intervalAtIndex = this.intervals[index];
      boolean lbIndexLeqLb =
          !intervalAtIndex.hasLowerBound()
              || (hasLowerBound && intervalAtIndex.getLowerBound().compareTo(lb) <= 0);
      boolean ubIndexGeqUb =
          !intervalAtIndex.hasUpperBound()
              || (hasUpperBound && intervalAtIndex.getUpperBound().compareTo(ub) >= 0);
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

  private int intervalIndexOf(BigInteger value) {
    if (isBottom()) {
      return -1;
    }
    if (isTop()) {
      return 0;
    }
    int leftInclusive = 0;
    int rightExclusive = this.intervals.length;
    int index = rightExclusive / 2;
    while (leftInclusive < rightExclusive) {
      SimpleInterval intervalAtIndex = this.intervals[index];
      boolean lbIndexLeqValue = !intervalAtIndex.hasLowerBound() || intervalAtIndex.getLowerBound().compareTo(value) <= 0;
      boolean ubIndexGeqValue = !intervalAtIndex.hasUpperBound() || intervalAtIndex.getUpperBound().compareTo(value) >= 0;
      if (lbIndexLeqValue) { // Interval at index starts before the value
        if (ubIndexGeqValue) { // Interval at index ends after the value
          return index;
        }
        // Interval at index ends before the value
        leftInclusive = index + 1;
      } else { // Interval at index starts after the value
        rightExclusive = index;
      }
      index = IntMath.mean(leftInclusive, rightExclusive);
    }
    return index == 0 ? -1 : -index;
  }

  /**
   * Checks if the given big integer value is contained in this state.
   * @param pValue the value to check for.
   *
   * @return <code>true</code> if the given value is contained in the state, <code>false</code> otherwise.
   */
  @Override
  public boolean contains(BigInteger pValue) {
    if (isTop()) { return true; }
    if (isBottom()) { return false; }
    return contains(SimpleInterval.singleton(pValue));
  }

  /**
   * Checks if the given long value is contained in this state.
   * @param pValue the value to check for.
   *
   * @return <code>true</code> if the given value is contained in the state,
   * <code>false</code> otherwise.
   */
  public boolean contains(long pValue) {
    if (isTop()) { return true; }
    if (isBottom()) { return false; }
    BigInteger value = BigInteger.valueOf(pValue);
    return intervalIndexOf(value) >= 0;
  }

  /**
   * Checks if this compound state is the bottom state,
   * which usually represents a contradiction.
   *
   * @return <code>true</code> if this is the bottom state, <code>false</code> otherwise.
   */
  @Override
  public boolean isBottom() {
    return this.intervals.length == 0;
  }

  /**
   * Checks if this compound state is the top state, including every possible value.
   *
   * @return <code>true</code> if this is the top state, <code>false</code> otherwise.
   */
  public boolean isTop() {
    return !isBottom() && this.intervals[0].isTop();
  }

  /**
   * Checks if this compound state contains every possible value.
   *
   * @return {@code true} if this state contains every possible value,
   * {@code false} otherwise.
   */
  @Override
  public boolean containsAllPossibleValues() {
    return isTop();
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
    sb.append('{');
    if (!isBottom()) {
      Iterator<SimpleInterval> intervalIterator = Arrays.asList(this.intervals).iterator();
      sb.append(intervalIterator.next());
      while (intervalIterator.hasNext()) {
        sb.append(", ");
        sb.append(intervalIterator.next());
      }
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * @param pBitVectorInfo the bitVector that should be casted
   */
  public CompoundInterval cast(BitVectorInfo pBitVectorInfo) {
    return this;
  }

  /**
   * Checks if there is a lower bound to this compound state.
   *
   * @return <code>true</code> if there is an lower bound to this compound state, <code>false</code> otherwise.
   */
  @Override
  public boolean hasLowerBound() {
    if (isTop() || isBottom()) { return false; }
    return this.intervals[0].hasLowerBound();
  }

  /**
   * Checks if there is an upper bound to this compound state.
   *
   * @return <code>true</code> if there is an upper bound to this compound state, <code>false</code> otherwise.
   */
  @Override
  public boolean hasUpperBound() {
    if (isTop() || isBottom()) { return false; }
    return this.intervals[this.intervals.length - 1].hasUpperBound();
  }

  /**
   * Returns the lower bound (may only be called if {@link #hasLowerBound()} returns true.
   *
   * @return the lower bound of the compound state.
   */
  @Override
  public BigInteger getLowerBound() {
    return this.intervals[0].getLowerBound();
  }

  /**
   * Returns the upper bound (may only be called if {@link #hasUpperBound()} returns true.
   *
   * @return the upper bound of the compound state.
   */
  @Override
  public BigInteger getUpperBound() {
    return this.intervals[this.intervals.length - 1].getUpperBound();
  }

  /**
   * Checks if this state represents a single value.
   *
   * @return <code>true</code> if this state represents a single value, <code>false</code> otherwise.
   */
  @Override
  public boolean isSingleton() {
    return !isBottom() && this.intervals.length == 1 && this.intervals[0].isSingleton();
  }

  /**
   * Returns SOME value that is contained in the state or <code>null</code>
   * if the state is the "bottom" state.
   *
   * @return some value that is contained in the state or <code>null</code>
   * if the state is the "bottom" state.
   */
  @Override
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
    if (pOther instanceof CompoundMathematicalInterval) {
      return Arrays.equals(this.intervals, ((CompoundMathematicalInterval) pOther).intervals);
    }
    return false;

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.intervals);
  }

  /**
   * Gets the signum of the compound state.
   *
   * @return the signum of the compound state.
   */
  @Override
  public CompoundMathematicalInterval signum() {
    CompoundMathematicalInterval result = bottom();
    if (containsNegative()) {
      result = result.unionWith(CompoundMathematicalInterval.singleton(-1));
    }
    if (containsZero()) {
      result = result.unionWith(CompoundMathematicalInterval.singleton(0));
    }
    if (containsPositive()) {
      result = result.unionWith(CompoundMathematicalInterval.singleton(1));
    }
    return result;
  }

  /**
   * Computes the state spanning from the compound state's lower bound and its upper bound.
   *
   * @return the state spanning from the compound state's lower bound and its upper bound.
   */
  @Override
  public CompoundMathematicalInterval span() {
    BigInteger lowerBound = null;
    BigInteger upperBound = null;
    if (hasLowerBound()) {
      lowerBound = getLowerBound();
    }
    if (hasUpperBound()) {
      upperBound = getUpperBound();
    }
    return CompoundMathematicalInterval.of(createSimpleInterval(lowerBound, upperBound));
  }

  /**
   * Inverts the state so that all values previously contained are no longer contained and vice versa.
   * Do not confuse this with negating ({@link #negate()}) the state.
   *
   * @return the inverted state.
   */
  @Override
  public CompoundMathematicalInterval invert() {
    if (isTop()) { return bottom(); }
    if (isBottom()) { return top(); }
    CompoundMathematicalInterval result = bottom();
    int index = 0;
    BigInteger currentLowerBound = null;
    if (!hasLowerBound()) {
      currentLowerBound = this.intervals[index++].getUpperBound().add(BigInteger.ONE);
    }
    while (index < this.intervals.length) {
      SimpleInterval current = this.intervals[index++];
      result = result.unionWith(createSimpleInterval(currentLowerBound, current.getLowerBound().subtract(BigInteger.ONE)));
      if (current.hasUpperBound()) {
        currentLowerBound = current.getUpperBound().add(BigInteger.ONE);
      } else {
        currentLowerBound = null;
      }
    }
    if (currentLowerBound != null) {
      SimpleInterval[] resultIntervals = new SimpleInterval[result.intervals.length + 1];
      System.arraycopy(result.intervals, 0, resultIntervals, 0, result.intervals.length);
      resultIntervals[result.intervals.length] = createSimpleInterval(currentLowerBound, null);
      result = getInternal(resultIntervals);
    }
    return result;
  }

  /**
   * Negates the state. Do not confuse this with inverting ({@link #invert()}) the state.
   * @return the negated state.
   */
  public CompoundMathematicalInterval negate() {
    if (isTop() || isBottom()) { return this; }
    CompoundMathematicalInterval result = bottom();
    for (SimpleInterval simpleInterval : this.intervals) {
      result = result.unionWith(simpleInterval.negate());
    }
    return result;
  }

  /**
   * Checks if positive values are contained in the state.
   * @return <code>true</code> if this state contains positive values.
   */
  @Override
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
  @Override
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
  @Override
  public CompoundMathematicalInterval extendToMinValue() {
    if (!hasLowerBound()) { return this; }
    SimpleInterval[] resultIntervals = new SimpleInterval[this.intervals.length];
    resultIntervals[0] = this.intervals[0].extendToNegativeInfinity();
    System.arraycopy(this.intervals, 1, resultIntervals, 1, this.intervals.length - 1);
    return getInternal(resultIntervals);
  }

  /**
   * Creates a compound state similar to this state but with a positive infinity upper bound or bottom if this state is bottom.
   *
   * @return a compound state similar to this state but with a positive infinity upper bound or bottom if this state is bottom.
   */
  @Override
  public CompoundMathematicalInterval extendToMaxValue() {
    if (!hasUpperBound()) { return this; }
    SimpleInterval[] resultIntervals = new SimpleInterval[this.intervals.length];
    int index = this.intervals.length - 1;
    System.arraycopy(this.intervals, 0, resultIntervals, 0, index);
    resultIntervals[index] = this.intervals[index].extendToPositiveInfinity();
    return getInternal(resultIntervals);
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundMathematicalInterval add(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.ADD, pValue);
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundMathematicalInterval add(final long pValue) {
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
  public CompoundMathematicalInterval add(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.ADD, pInterval);
  }

  /**
   * Computes the state resulting from adding the given state to this
   * state.
   *
   * @param pState the state to add to this state.
   * @return the state resulting from adding the given state to this
   * state.
   */
  public CompoundMathematicalInterval add(final CompoundMathematicalInterval pState) {
    return applyOperationToAllAndUnite(ICCOperator.ADD, pState);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given value.
   *
   * @param pValue the value to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given value.
   */
  public CompoundMathematicalInterval multiply(final BigInteger pValue) {
    if (pValue.equals(BigInteger.ZERO)) {
      return CompoundMathematicalInterval.singleton(pValue);
    }
    if (pValue.equals(BigInteger.ONE)) {
      return this;
    }
    CompoundMathematicalInterval result = applyOperationToAllAndUnite(ISCOperator.MULTIPLY, pValue);
    if (result.isTop()) {
      SimpleInterval[] resultIntervals = new SimpleInterval[7];
      if (pValue.signum() >= 0) {
        for (int i = -3; i <= 3; ++i) {
          resultIntervals[i + 3] = SimpleInterval.singleton(pValue.multiply(BigInteger.valueOf(i)));
        }
      } else {
        for (int i = -3; i <= 3; ++i) {
          resultIntervals[3 - i] = SimpleInterval.singleton(pValue.multiply(BigInteger.valueOf(i)));
        }
      }
      resultIntervals[0] = resultIntervals[0].extendToNegativeInfinity();
      resultIntervals[6] = resultIntervals[6].extendToPositiveInfinity();
      result = getInternal(resultIntervals);
    }
    return result;
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given interval.
   *
   * @param pInterval the interval to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given interval.
   */
  public CompoundMathematicalInterval multiply(final SimpleInterval pInterval) {
    if (pInterval.isSingleton()) {
      return multiply(pInterval.getLowerBound());
    }
    return applyOperationToAllAndUnite(IICOperator.MULTIPLY, pInterval);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given state.
   *
   * @param pState the state to multiply this state with.
   * @return the state resulting from multiplying this state with the
   * given state.
   */
  public CompoundMathematicalInterval multiply(final CompoundMathematicalInterval pState) {
    if (pState.intervals.length == 1) {
      return multiply(pState.intervals[0]);
    }
    return applyOperationToAllAndUnite(ICCOperator.MULTIPLY, pState);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * value.
   *
   * @param pValue the value to divide this state by.
   * @return the state resulting from dividing this state by the given
   * value.
   */
  public CompoundMathematicalInterval divide(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.DIVIDE, pValue);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * interval.
   *
   * @param pInterval the interval to divide this state by.
   * @return the state resulting from dividing this state by the given
   * interval.
   */
  public CompoundMathematicalInterval divide(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.DIVIDE, pInterval);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * state.
   *
   * @param pState the state to divide this state by.
   * @return the state resulting from dividing this state by the given
   * state.
   */
  public CompoundMathematicalInterval divide(final CompoundMathematicalInterval pState) {
    return applyOperationToAllAndUnite(ICCOperator.DIVIDE, pState);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given value.
   *
   * @param pValue the value to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given value.
   */
  public CompoundMathematicalInterval modulo(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.MODULO, pValue);
  }


  /**
   * Computes the state representing the remainder of dividing this state
   * by the given interval.
   *
   * @param pInterval the interval to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given interval.
   */
  public CompoundMathematicalInterval modulo(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.MODULO, pInterval);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given state.
   *
   * @param pState the state to divide this state by.
   * @return the state representing the remainder of dividing this state
   * by the given state.
   */
  public CompoundMathematicalInterval modulo(final CompoundMathematicalInterval pState) {
    return applyOperationToAllAndUnite(ICCOperator.MODULO, pState);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given value.
   */
  public CompoundMathematicalInterval shiftLeft(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.SHIFT_LEFT, pValue);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given interval.
   */
  public CompoundMathematicalInterval shiftLeft(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.SHIFT_LEFT, pInterval);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @return the state resulting from left shifting this state by the
   * given state.
   */
  public CompoundMathematicalInterval shiftLeft(final CompoundMathematicalInterval pState) {
    return applyOperationToAllAndUnite(ICCOperator.SHIFT_LEFT, pState);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given value.
   */
  public CompoundMathematicalInterval shiftRight(final BigInteger pValue) {
    return applyOperationToAllAndUnite(ISCOperator.SHIFT_RIGHT, pValue);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given interval.
   */
  public CompoundMathematicalInterval shiftRight(final SimpleInterval pInterval) {
    return applyOperationToAllAndUnite(IICOperator.SHIFT_RIGHT, pInterval);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @return the state resulting from right shifting this state by the
   * given state.
   */
  public CompoundMathematicalInterval shiftRight(final CompoundMathematicalInterval pState) {
    return applyOperationToAllAndUnite(ICCOperator.SHIFT_RIGHT, pState);
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
   * Do not confuse this method with {@link #equals(Object)} which tests two
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
  public CompoundMathematicalInterval logicalEquals(final CompoundMathematicalInterval pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && equals(pState)) { return CompoundMathematicalInterval.logicalTrue(); }
    if (!intersectsWith(pState)) { return CompoundMathematicalInterval.logicalFalse(); }
    return top();
  }

  /**
   * Checks whether this state definitely evaluates to <code>false</code>
   * or not.
   *
   * @return <code>true</code> if this state definitely evaluates to
   * <code>false</code>, <code>false</code> otherwise.
   */
  @Override
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
  @Override
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
  public CompoundMathematicalInterval greaterThan(final CompoundMathematicalInterval pState) {
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
  public CompoundMathematicalInterval greaterEqual(final CompoundMathematicalInterval pState) {
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
  public CompoundMathematicalInterval lessThan(final CompoundMathematicalInterval pState) {
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
  public CompoundMathematicalInterval lessEqual(final CompoundMathematicalInterval pState) {
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
  public CompoundMathematicalInterval logicalAnd(final CompoundMathematicalInterval pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if ((isSingleton() && containsZero()) || (pState.isSingleton() && pState.containsZero())) {
      return logicalFalse();
    }
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
  public CompoundMathematicalInterval logicalOr(final CompoundMathematicalInterval pState) {
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
  public CompoundMathematicalInterval logicalNot() {
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
  public CompoundMathematicalInterval binaryAnd(CompoundMathematicalInterval pState) {
    if (isBottom() || pState.isBottom()) {
      return bottom();
    }
    if (pState.isSingleton() && pState.containsZero()) {
      return pState;
    }
    if (isSingleton() && containsZero()) {
      return this;
    }
    CompoundMathematicalInterval result;
    if (pState.isSingleton()) {
      result = bottom();
      for (SimpleInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          // x & 1 always yields either 0 or 1
          return pState.contains(1)
              ? CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE))
              : top();
        }
        result = result.unionWith(SimpleInterval.singleton(interval.getLowerBound().and(pState.getValue())));
      }
    } else if (isSingleton()) {
      return pState.binaryAnd(this);
    } else {
      result = top();
    }
    if (!result.isSingleton()) {
      CompoundMathematicalInterval absThis = absolute();
      CompoundMathematicalInterval absOther = pState.absolute();
      BigInteger smallestUpperBound = null;
      if (absThis.hasUpperBound()) {
        smallestUpperBound = absThis.getUpperBound();
      }
      if (absOther.hasUpperBound()) {
        smallestUpperBound = smallestUpperBound == null
            ? absOther.getUpperBound()
            : smallestUpperBound.min(absOther.getUpperBound());
      }
      assert smallestUpperBound == null || smallestUpperBound.signum() >= 0;
      CompoundMathematicalInterval range;
      if (smallestUpperBound == null) {
        range = zero().extendToMaxValue();
      } else {
        range = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, smallestUpperBound));
      }
      if (containsNegative() && pState.containsNegative()) {
        range = range.unionWith(range.negate());
      }
      result = result.intersectWith(range);
    }
    return result;
  }

  /**
   * Computes the state resulting from computing the absolute values of this
   * state.
   *
   * @return the state resulting from computing the absolute values of this
   * state.
   */
  public CompoundMathematicalInterval absolute() {
    if (!containsNegative()) {
      return this;
    }
    return intersectWith(one().negate().extendToMinValue()).negate().unionWith(intersectWith(zero().extendToMaxValue()));
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
  public CompoundMathematicalInterval binaryXor(CompoundMathematicalInterval pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && pState.isSingleton()) { return CompoundMathematicalInterval.singleton(getValue().xor(pState.getValue())); }
    if (pState.isSingleton() && pState.containsZero()) {
      return this;
    }
    if (isSingleton() && containsZero()) {
      return pState;
    }
    CompoundMathematicalInterval zeroToOne = CompoundMathematicalInterval.of(SimpleInterval.of(BigInteger.ZERO, BigInteger.ONE));
    // [0,1] ^ 1 = [0,1]
    if (pState.isSingleton() && pState.contains(1) && equals(zeroToOne)) {
      return this;
    }
    // 1 ^ [0,1] = [0,1]
    if (isSingleton() && contains(1) && pState.equals(zeroToOne)) {
      return zeroToOne;
    }
    if (pState.isSingleton()) {
      CompoundMathematicalInterval result = bottom();
      for (SimpleInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          return top();
        }
        result = result.unionWith(SimpleInterval.singleton(interval.getLowerBound().xor(pState.getValue())));
      }
      return result;
    } else if (isSingleton()) {
      return pState.binaryXor(this);
    }
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
  public CompoundMathematicalInterval binaryNot() {
    if (isBottom()) { return bottom(); }
    CompoundMathematicalInterval result = bottom();
    for (SimpleInterval interval : this.intervals) {
      if (!interval.isSingleton()) {
        // TODO maybe a more exact implementation is possible?
        if (!containsNegative()) {
          return singleton(0).extendToMinValue();
        } else if (!containsPositive()) {
          return singleton(0).extendToMaxValue();
        } else {
          return top();
        }
      }
      result = result.unionWith(SimpleInterval.singleton(interval.getLowerBound().not()));
    }
    return result;
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
  public CompoundMathematicalInterval binaryOr(CompoundMathematicalInterval pState) {
    if (isBottom() || pState.isBottom()) { return bottom(); }
    if (isSingleton() && containsZero()) {
      return pState;
    }
    if (pState.isSingleton() && pState.containsZero()) {
      return this;
    }
    if (pState.isSingleton()) {
      CompoundMathematicalInterval result = bottom();
      for (SimpleInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          return top();
        }
        result = result.unionWith(SimpleInterval.singleton(interval.getLowerBound().or(pState.getValue())));
      }
      return result;
    } else if (isSingleton()) {
      return pState.binaryOr(this);
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
  public CompoundMathematicalInterval logicalXor(CompoundMathematicalInterval pState) {
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
  public static CompoundMathematicalInterval logicalAnd(CompoundMathematicalInterval p1, CompoundMathematicalInterval p2) {
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
  public static CompoundMathematicalInterval logicalOr(CompoundMathematicalInterval p1, CompoundMathematicalInterval p2) {
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
  public static CompoundMathematicalInterval logicalXor(CompoundMathematicalInterval p1, CompoundMathematicalInterval p2) {
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
  public static CompoundMathematicalInterval logicalNot(CompoundMathematicalInterval pState) {
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
  private <T> CompoundMathematicalInterval applyOperationToAllAndUnite(Operator<SimpleInterval, T, CompoundMathematicalInterval> pOperator, T pOperand) {
    CompoundMathematicalInterval result = bottom();
    for (SimpleInterval interval : this.intervals) {
      CompoundMathematicalInterval current = pOperator.apply(interval, pOperand);
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
    Preconditions.checkArgument(a.touches(b), "Cannot unite intervals that do not touch.");
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
   *
   * @param interval the interval to base this compound state on. If the
   * interval is {@code null}, bottom is returned.
   *
   * @return a new compound state representation of the given simple interval.
   */
  public static CompoundMathematicalInterval of(@Nullable SimpleInterval interval) {
    if (interval == null) {
      return bottom();
    }
    return getInternal(interval);
  }

  /**
   * Gets a compound state representing the given big integer value.
   *
   * @param pValue the value to be represented by the state.
   *
   * @return a compound state representing the given big integer value.
   */
  public static CompoundMathematicalInterval singleton(BigInteger pValue) {
    Preconditions.checkNotNull(pValue);
    return CompoundMathematicalInterval.of(SimpleInterval.singleton(pValue));
  }

  /**
   * Gets a compound state representing the given long value.
   * @param pValue the value to be represented by the state.
   * @return a compound state representing the given long value.
   */
  public static CompoundMathematicalInterval singleton(long pValue) {
    return singleton(BigInteger.valueOf(pValue));
  }

  /**
   * Gets a compound state representing "bottom".
   * @return a compound state representing "bottom".
   */
  public static CompoundMathematicalInterval bottom() {
    return BOTTOM;
  }

  /**
   * Gets a compound state representing "top".
   * @return a compound state representing "top".
   */
  public static CompoundMathematicalInterval top() {
    return TOP;
  }

  public static CompoundMathematicalInterval logicalFalse() {
    return FALSE;
  }

  public static CompoundMathematicalInterval logicalTrue() {
    return TRUE;
  }

  public static CompoundMathematicalInterval zero() {
    return ZERO;
  }

  public static CompoundMathematicalInterval one() {
    return ONE;
  }

  public static CompoundMathematicalInterval minusOne() {
    return MINUS_ONE;
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
  public static CompoundMathematicalInterval span(CompoundMathematicalInterval pLeftValue, CompoundMathematicalInterval pRightValue) {
    return pLeftValue.span().unionWith(pRightValue.span()).span();
  }

  /**
   * Unites the given states.
   * @param pLeftValue one of the states to be united.
   * @param pRightValue one of the states to be united.
   * @return the union of the given states.
   */
  public static CompoundMathematicalInterval union(CompoundMathematicalInterval pLeftValue, CompoundMathematicalInterval pRightValue) {
    return pLeftValue.unionWith(pRightValue);
  }

  /**
   * Gets the state representing the given boolean value.
   *
   * @param value the boolean value to represent as compound state.
   * @return the state representing the given boolean value.
   */
  public static CompoundMathematicalInterval fromBoolean(boolean value) {
    return value ? logicalTrue() : logicalFalse();
  }

}

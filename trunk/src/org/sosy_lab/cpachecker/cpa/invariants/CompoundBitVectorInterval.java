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
import org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector.ICCOperatorFactory;
import org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector.IICOperatorFactory;
import org.sosy_lab.cpachecker.cpa.invariants.operators.bitvector.ISCOperatorFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Instances of this class represent compound states of intervals.
 */
public class CompoundBitVectorInterval implements CompoundIntegralInterval, BitVectorType {

  private final BitVectorInfo info;

  /**
   * The list of intervals this state is composed from.
   */
  private final BitVectorInterval[] intervals;

  /**
   * Constructs the bottom state.
   *
   * @param pInfo the bit vector information.
   */
  private CompoundBitVectorInterval(BitVectorInfo pInfo) {
    Preconditions.checkNotNull(pInfo);
    this.info = pInfo;
    this.intervals = new BitVectorInterval[0];
  }

  /**
   * Creates a new compound state from the given interval. This should only be
   * invoked via the {@link CompoundBitVectorInterval#getInternal} functions.
   *
   * @param pInterval the interval to compose this state from. Must not be
   * {@code null}.
   */
  private CompoundBitVectorInterval(BitVectorInterval pInterval) {
    this.info = pInterval.getTypeInfo();
    this.intervals = new BitVectorInterval[] { pInterval };
  }

  /**
   * Creates a new compound state from the given intervals. This should only be
   * invoked via the {@link CompoundBitVectorInterval#getInternal} functions.
   *
   * @param pInfo the bit vector information.
   * @param pIntervals the intervals to compose this state from. None of the
   * intervals must be {@code null}. All intervals must have the same bit
   * vector information as the parameter.
   */
  private CompoundBitVectorInterval(BitVectorInfo pInfo, BitVectorInterval[] pIntervals) {
    Preconditions.checkNotNull(pInfo);
    Preconditions.checkNotNull(pIntervals);
    this.info = pInfo;
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
  private static CompoundBitVectorInterval getInternal(BitVectorInterval pInterval) {
    return new CompoundBitVectorInterval(pInterval);
  }

  /**
   * Gets a compound interval represented by the given intervals. Use this
   * factory method over the constructor.
   *
   * @param pInfo the bit vector information.
   * @param pIntervals the intervals to compose this state from. None of the
   * intervals must be {@code null}. All intervals must have the same bit
   * vector information as the parameter.
   *
   * @return a compound interval as represented by the given intervals.
   */
  private static CompoundBitVectorInterval getInternal(BitVectorInfo pInfo, BitVectorInterval[] pIntervals) {
    if (pIntervals.length == 0) {
      return bottom(pInfo);
    }
    return new CompoundBitVectorInterval(pInfo, pIntervals);
  }

  @Override
  public BitVectorInfo getTypeInfo() {
    return info;
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
  public List<BitVectorInterval> getBitVectorIntervals() {
    return Collections.unmodifiableList(Arrays.asList(this.intervals));
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
    return Lists.transform(getBitVectorIntervals(), pBitVectorInterval ->
        SimpleInterval.of(pBitVectorInterval.getLowerBound(), pBitVectorInterval.getUpperBound()));
  }

  @Override
  public List<CompoundBitVectorInterval> splitIntoIntervals() {
    return Lists.transform(Arrays.asList(this.intervals), CompoundBitVectorInterval::of);
  }

  public void checkBitVectorCompatibilityWith(BitVectorInfo pOtherInfo) {
    Preconditions.checkArgument(info.equals(pOtherInfo),
        "bit vectors are incompatible in size or signedness");
  }

  /**
   * Computes the union of this compound state with the given compound state.
   * @param pOther the state to unite this state with.
   *
   * @return the union of this compound state with the given compound state.
   */
  public CompoundBitVectorInterval unionWith(CompoundBitVectorInterval pOther) {
    checkBitVectorCompatibilityWith(pOther.info);
    if (pOther == this || containsAllPossibleValues() || pOther.isBottom()) { return this; }
    if (pOther.containsAllPossibleValues() || isBottom()) { return pOther; }
    CompoundBitVectorInterval current = this;
    for (BitVectorInterval interval : pOther.intervals) {
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
  public CompoundBitVectorInterval unionWith(BitVectorInterval pOther) {
    checkBitVectorCompatibilityWith(pOther.getTypeInfo());
    if (contains(pOther)) { return this; }
    if (isBottom() || pOther.isTop()) { return getInternal(pOther); }
    ArrayList<BitVectorInterval> resultIntervals = new ArrayList<>();
    int start = 0;
    BitVectorInterval lastInterval = null;
    if (pOther.hasLowerBound() && hasUpperBound()) {
      BigInteger pOtherLB = pOther.getLowerBound();
      BitVectorInterval currentLocal = this.intervals[start];
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
      BitVectorInterval interval = this.intervals[index];
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
    BitVectorInterval[] resultArray = new BitVectorInterval[resultIntervals.size()];
    return getInternal(info, resultIntervals.toArray(resultArray));
  }

  /**
   * Computes the compound state resulting from the intersection of this compound state with the given state.
   * @param pOther the state to intersect this state with.
   *
   * @return the compound state resulting from the intersection of this compound state with the given state.
   */
  public CompoundBitVectorInterval intersectWith(CompoundBitVectorInterval pOther) {
    checkBitVectorCompatibilityWith(pOther.info);
    if (isBottom() || pOther.containsAllPossibleValues() || this == pOther) { return this; }
    if (containsAllPossibleValues() || pOther.isBottom()) { return pOther; }
    if (pOther.contains(this)) {
      return this;
    }
    CompoundBitVectorInterval result = bottom(info);
    for (BitVectorInterval otherInterval : pOther.intervals) {
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
  public CompoundBitVectorInterval intersectWith(BitVectorInterval pOther) {
    checkBitVectorCompatibilityWith(pOther.getTypeInfo());
    if (isBottom() || pOther.isTop()) { return this; }
    if (contains(pOther)) { return CompoundBitVectorInterval.of(pOther); }
    if (this.intervals.length == 1 && pOther.contains(this.intervals[0])) { return this; }
    CompoundBitVectorInterval result = bottom(info);
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
      BitVectorInterval interval = intervals[i];
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
  public boolean intersectsWith(CompoundBitVectorInterval pOther) {
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
  public boolean intersectsWith(BitVectorInterval pOther) {
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
  public boolean contains(CompoundBitVectorInterval pState) {
    if (this == pState) {
      return true;
    }
    for (BitVectorInterval interval : pState.intervals) {
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
  public boolean contains(BitVectorInterval pInterval) {
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
      BitVectorInterval intervalAtIndex = this.intervals[index];
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
    if (containsAllPossibleValues()) {
      return 0;
    }
    int leftInclusive = 0;
    int rightExclusive = this.intervals.length;
    int index = rightExclusive / 2;
    while (leftInclusive < rightExclusive) {
      BitVectorInterval intervalAtIndex = this.intervals[index];
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
    if (isBottom()) { return false; }
    if (pValue.compareTo(info.getMinValue()) < 0) {
      return false;
    }
    if (pValue.compareTo(info.getMaxValue()) > 0) {
      return false;
    }
    return contains(singleton(info, pValue));
  }

  /**
   * Checks if the given long value is contained in this state.
   * @param pValue the value to check for.
   *
   * @return <code>true</code> if the given value is contained in the state,
   * <code>false</code> otherwise.
   */
  public boolean contains(long pValue) {
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
   * Checks if this compound state contains every possible value.
   *
   * @return {@code true} if this state contains every possible value,
   * {@code false} otherwise.
   */
  @Override
  public boolean containsAllPossibleValues() {
    return contains(info.getRange());
  }

  @Override
  public String toString() {
    if (isBottom()) {
      return Character.toString('\u22A5');
    }
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    if (!isBottom()) {
      Iterator<BitVectorInterval> intervalIterator = Arrays.asList(this.intervals).iterator();
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
   * Checks if there is a lower bound to this compound state.
   *
   * @return <code>true</code> if there is an lower bound to this compound state, <code>false</code> otherwise.
   */
  @Override
  public boolean hasLowerBound() {
    return !isBottom();
  }

  /**
   * Checks if there is an upper bound to this compound state.
   *
   * @return <code>true</code> if there is an upper bound to this compound state, <code>false</code> otherwise.
   */
  @Override
  public boolean hasUpperBound() {
    return !isBottom();
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
  public @Nullable BigInteger getValue() {
    if (isBottom()) { return null; }
    if (containsAllPossibleValues()) { return BigInteger.ZERO; }
    for (BitVectorInterval interval : this.intervals) {
      if (interval.hasLowerBound()) { return interval.getLowerBound(); }
      if (interval.hasUpperBound()) { return interval.getUpperBound(); }
    }
    return null;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    if (pOther instanceof CompoundBitVectorInterval) {
      CompoundBitVectorInterval other = (CompoundBitVectorInterval) pOther;
      return info.equals(other.info)
          && Arrays.equals(this.intervals, other.intervals);
    }
    return false;

  }

  @Override
  public int hashCode() {
    return Objects.hash(info, Arrays.hashCode(this.intervals));
  }

  public CompoundBitVectorInterval cast(final BitVectorInfo pBitVectorInfo, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    // Cast to the same type has no effect
    if (info.equals(pBitVectorInfo)) {
      return this;
    }
    // If the value fits in, the cast is easy
    if (pBitVectorInfo.getRange().contains(info.getRange())) {
      BitVectorInterval[] castedIntervals = new BitVectorInterval[intervals.length];
      Lists.transform(getBitVectorIntervals(), pInterval -> BitVectorInterval.of(
          pBitVectorInfo,
          pInterval.getLowerBound(),
          pInterval.getUpperBound())).toArray(castedIntervals);
      return new CompoundBitVectorInterval(
          pBitVectorInfo,
          castedIntervals);
    }
    CompoundBitVectorInterval result = bottom(pBitVectorInfo);
    for (BitVectorInterval interval : intervals) {
      result = result.unionWith(
          cast(
              pBitVectorInfo,
              interval.getLowerBound(),
              interval.getUpperBound(),
              pAllowSignedWrapAround,
              pOverflowEventHandler));
    }
    return result;
  }

  public static CompoundBitVectorInterval cast(BitVectorInfo pInfo, BigInteger pLowerBound, BigInteger pUpperBound, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (pLowerBound.equals(pUpperBound)) {
      return of(BitVectorInterval.cast(pInfo, pLowerBound, pAllowSignedWrapAround, pOverflowEventHandler));
    }
    BigInteger lowerBound = pLowerBound;
    BigInteger upperBound = pUpperBound;

    boolean lbExceedsBelow = lowerBound.compareTo(pInfo.getMinValue()) < 0;
    boolean lbExceedsAbove = !lbExceedsBelow && lowerBound.compareTo(pInfo.getMaxValue()) > 0;
    boolean ubExceedsBelow = upperBound.compareTo(pInfo.getMinValue()) < 0;
    boolean ubExceedsAbove = !ubExceedsBelow && upperBound.compareTo(pInfo.getMaxValue()) > 0;

    // If the value fits in the range, there is no problem
    if (!(lbExceedsBelow || lbExceedsAbove || ubExceedsBelow || ubExceedsAbove)) {
      return of(BitVectorInterval.of(pInfo, pLowerBound, pUpperBound));
    }

    // From here on out, we know the interval does not fit

    // If the type is signed, overflow is undefined
    if (!pAllowSignedWrapAround && pInfo.isSigned()) {
      pOverflowEventHandler.signedOverflow();
      return of(pInfo.getRange());
    }

    BigInteger rangeLength = pInfo.getRange().size();
    assert rangeLength.compareTo(BigInteger.ZERO) >= 0;

    // If the value is larger than the full range, just return the full range
    if (upperBound.subtract(lowerBound).add(BigInteger.ONE).compareTo(rangeLength) >= 0) {
      return of(pInfo.getRange());
    }

    if (ubExceedsBelow) { // Full interval is below the minimum value
      lowerBound = pLowerBound.remainder(rangeLength);
      if (lowerBound.compareTo(pInfo.getMinValue()) < 0) {
        lowerBound = lowerBound.add(rangeLength);
      }
      upperBound = lowerBound.add(pUpperBound.subtract(pLowerBound));
      assert lowerBound.compareTo(pInfo.getMinValue()) >= 0;

      // If the interval still exceeds the range, we use multiple intervals
      if (upperBound.compareTo(pInfo.getMaxValue()) > 0) {
        return union(
            singleton(pInfo, lowerBound).extendToMaxValue(),
            cast(pInfo, pInfo.getMaxValue().add(BigInteger.ONE), upperBound, pAllowSignedWrapAround, pOverflowEventHandler));
      }
    } else if (lbExceedsAbove) { // Full interval is above the maximum value
      upperBound = pUpperBound.remainder(rangeLength);
      if (upperBound.compareTo(pInfo.getMaxValue()) > 0) {
        upperBound = upperBound.subtract(rangeLength);
      }
      lowerBound = upperBound.subtract(pUpperBound.subtract(pLowerBound));
      assert upperBound.compareTo(pInfo.getMaxValue()) <= 0;

      // If the interval still exceeds the range, we use multiple intervals
      if (lowerBound.compareTo(pInfo.getMinValue()) < 0) {
        return union(
            singleton(pInfo, upperBound).extendToMinValue(),
            cast(pInfo, lowerBound, pInfo.getMinValue().subtract(BigInteger.ONE), pAllowSignedWrapAround, pOverflowEventHandler));
      }
    } else if (lbExceedsBelow) { // Part of the interval is below the minimum value
      return union(
          cast(pInfo, pLowerBound, pInfo.getMinValue().subtract(BigInteger.ONE), pAllowSignedWrapAround, pOverflowEventHandler),
          cast(pInfo, pInfo.getMinValue(), pUpperBound, pAllowSignedWrapAround, pOverflowEventHandler)
          );
    } else if (ubExceedsAbove) { // Part of the interval is above the minimum value
      return union(
          cast(pInfo, pLowerBound, pInfo.getMaxValue(), pAllowSignedWrapAround, pOverflowEventHandler),
          cast(pInfo, pInfo.getMaxValue().add(BigInteger.ONE), pUpperBound, pAllowSignedWrapAround, pOverflowEventHandler)
          );
    }

    return of(BitVectorInterval.of(pInfo, lowerBound, upperBound));
  }

  /**
   * Gets the signum of the compound state.
   *
   * @return the signum of the compound state.
   */
  @Override
  public CompoundBitVectorInterval signum() {
    CompoundBitVectorInterval result = bottom(info);
    if (containsNegative()) {
      result = result.unionWith(CompoundBitVectorInterval.singleton(info, -1));
    }
    if (containsZero()) {
      result = result.unionWith(CompoundBitVectorInterval.singleton(info, 0));
    }
    if (containsPositive()) {
      result = result.unionWith(CompoundBitVectorInterval.singleton(info, 1));
    }
    return result;
  }

  /**
   * Computes the state spanning from the compound state's lower bound and its upper bound.
   *
   * @return the state spanning from the compound state's lower bound and its upper bound.
   */
  @Override
  public CompoundBitVectorInterval span() {
    BigInteger lowerBound = getLowerBound();
    BigInteger upperBound = getUpperBound();
    return CompoundBitVectorInterval.of(BitVectorInterval.of(info, lowerBound, upperBound));
  }

  /**
   * Inverts the state so that all values previously contained are no longer contained and vice versa.
   * Do not confuse this with negating ({@link #negate(boolean, OverflowEventHandler)}) the state.
   *
   * @return the inverted state.
   */
  @Override
  public CompoundBitVectorInterval invert() {
    if (contains(info.getRange())) { return bottom(info); }
    if (isBottom()) { return getInternal(info.getRange()); }
    CompoundBitVectorInterval result = bottom(info);
    int index = 0;

    BitVectorInterval current = this.intervals[index++];

    // Add the interval before the first of the contained intervals
    if (!current.getLowerBound().equals(info.getMinValue())) {
      result = result.unionWith(BitVectorInterval.of(info, info.getMinValue(), current.getLowerBound().subtract(BigInteger.ONE)));
    }

    BigInteger lastUpperBound = current.getUpperBound();

    while (index < this.intervals.length) {
      current = this.intervals[index++];

      // Add the interval between the last and the current contained interval
      result = result.unionWith(BitVectorInterval.of(info, lastUpperBound.add(BigInteger.ONE), current.getLowerBound().subtract(BigInteger.ONE)));

      lastUpperBound = current.getUpperBound();
    }

    // Add the interval after the last of the contained intervals
    if (!lastUpperBound.equals(info.getMaxValue())) {
      result = result.unionWith(BitVectorInterval.of(info, lastUpperBound.add(BigInteger.ONE), info.getMaxValue()));
    }
    return result;
  }

  /**
   * Negates the state. Do not confuse this with inverting ({@link #invert()}) the state.
   * @return the negated state.
   */
  public CompoundBitVectorInterval negate(boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (containsAllPossibleValues() || isBottom()) { return this; }
    CompoundBitVectorInterval result = bottom(info);
    for (BitVectorInterval simpleInterval : this.intervals) {
      result = result.unionWith(negate(info, simpleInterval, pAllowSignedWrapAround, pOverflowEventHandler));
    }
    return result;
  }

  private static CompoundBitVectorInterval negate(BitVectorInfo pInfo, BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    BigInteger newLowerBound = pInterval.getUpperBound().negate();
    BigInteger newUpperBound = pInterval.getLowerBound().negate();

    boolean lbExceedsBelow = newLowerBound.compareTo(pInfo.getMinValue()) < 0;
    boolean lbExceedsAbove = !lbExceedsBelow && newLowerBound.compareTo(pInfo.getMaxValue()) > 0;
    boolean ubExceedsBelow = newUpperBound.compareTo(pInfo.getMinValue()) < 0;
    boolean ubExceedsAbove = !ubExceedsBelow && newUpperBound.compareTo(pInfo.getMaxValue()) > 0;
    if (lbExceedsBelow || lbExceedsAbove || ubExceedsBelow || ubExceedsAbove) {
      if (!pAllowSignedWrapAround && pInfo.isSigned()) {
        pOverflowEventHandler.signedOverflow();
        return of(pInfo.getRange());
      }
      final BigInteger fromLB;
      final BigInteger fromUB;
      BigInteger rangeLength = pInfo.getRange().size();
      if (lbExceedsBelow) {
        fromLB = rangeLength.add(newLowerBound);
      } else if (lbExceedsAbove) {
        fromLB = newLowerBound.subtract(rangeLength);
      } else {
        fromLB = newLowerBound;
      }
      if (ubExceedsBelow) {
        fromUB = rangeLength.add(newUpperBound);
      } else if (ubExceedsAbove) {
        fromUB = newUpperBound.subtract(rangeLength);
      } else {
        fromUB = newUpperBound;
      }
      // If, e.g., the intervals goes "from 2 to 0", use two intervals
      if (fromLB.compareTo(fromUB) > 0) {
        // If the borders touch anyway, return the full range
        if (fromUB.add(BigInteger.ONE).equals(fromLB)) {
          return of(pInfo.getRange());
        }
        BitVectorInterval[] intervals = new BitVectorInterval[2];
        intervals[0] = BitVectorInterval.singleton(pInfo, fromUB).extendToMinValue();
        intervals[1] = BitVectorInterval.singleton(pInfo, fromLB).extendToMaxValue();
        return CompoundBitVectorInterval.getInternal(pInfo, intervals);
      }
      newLowerBound = fromLB;
      newUpperBound = fromUB;
    }
    return CompoundBitVectorInterval.of(BitVectorInterval.of(pInfo, newLowerBound, newUpperBound));
  }

  /**
   * Checks if positive values are contained in the state.
   * @return <code>true</code> if this state contains positive values.
   */
  @Override
  public boolean containsPositive() {
    if (isBottom()) { return false; }
    for (BitVectorInterval interval : this.intervals) {
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
    for (BitVectorInterval interval : this.intervals) {
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
   * Creates a compound state similar to this state but with the minimum lower
   * bound of the bit vector or bottom if this state is bottom.
   *
   * @return a compound state similar to this state but with the minimum lower
   * bound of the bit vector or bottom if this state is bottom.
   */
  @Override
  public CompoundBitVectorInterval extendToMinValue() {
    if (!hasLowerBound()) { return this; }
    BitVectorInterval[] resultIntervals = new BitVectorInterval[this.intervals.length];
    resultIntervals[0] = this.intervals[0].extendToMinValue();
    System.arraycopy(this.intervals, 1, resultIntervals, 1, this.intervals.length - 1);
    return getInternal(info, resultIntervals);
  }

  /**
   * Creates a compound state similar to this state but with the maximum lower
   * bound of the bit vector or bottom if this state is bottom.
   *
   * @return a compound state similar to this state but with the maximum lower
   * bound of the bit vector or bottom if this state is bottom.
   */
  @Override
  public CompoundBitVectorInterval extendToMaxValue() {
    if (!hasUpperBound()) { return this; }
    BitVectorInterval[] resultIntervals = new BitVectorInterval[this.intervals.length];
    int index = this.intervals.length - 1;
    System.arraycopy(this.intervals, 0, resultIntervals, 0, index);
    resultIntervals[index] = this.intervals[index].extendToMaxValue();
    return getInternal(info, resultIntervals);
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   *
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundBitVectorInterval add(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ISCOperatorFactory.INSTANCE.getAdd(pAllowSignedWrapAround, pOverflowEventHandler), pValue);
  }

  /**
   * Computes the state resulting from adding the given value to this
   * state.
   *
   * @param pValue the value to add to this state.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from adding the given value to this
   * state.
   */
  public CompoundBitVectorInterval add(final long pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return add(BigInteger.valueOf(pValue), pAllowSignedWrapAround, pOverflowEventHandler);
  }

  /**
   * Computes the state resulting from adding the given interval to this
   * state.
   *
   * @param pInterval the interval to add to this state.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   * @param pOverflowEventHandler the handle for overflows
   *
   * @return the state resulting from adding the given interval to this
   * state.
   */
  public CompoundBitVectorInterval add(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getAdd(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state resulting from adding the given state to this
   * state.
   *
   * @param pState the state to add to this state.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from adding the given state to this
   * state.
   */
  public CompoundBitVectorInterval add(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getAdd(pAllowSignedWrapAround, pOverflowEventHandler), pState);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given value.
   *
   * @param pValue the value to multiply this state with.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from multiplying this state with the
   * given value.
   */
  public CompoundBitVectorInterval multiply(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (pValue.equals(BigInteger.ZERO)) {
      return CompoundBitVectorInterval.singleton(info, pValue);
    }
    if (pValue.equals(BigInteger.ONE)) {
      return this;
    }
    return applyOperationToAllAndUnite(
        ISCOperatorFactory.INSTANCE.getMultiply(pAllowSignedWrapAround, pOverflowEventHandler),
        pValue);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given interval.
   *
   * @param pInterval the interval to multiply this state with.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from multiplying this state with the
   * given interval.
   */
  public CompoundBitVectorInterval multiply(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (pInterval.isSingleton()) {
      return multiply(pInterval.getLowerBound(), pAllowSignedWrapAround, pOverflowEventHandler);
    }
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getMultiply(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state resulting from multiplying this state with the
   * given state.
   *
   * @param pState the state to multiply this state with.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from multiplying this state with the
   * given state.
   */
  public CompoundBitVectorInterval multiply(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (pState.intervals.length == 1) {
      return multiply(pState.intervals[0], pAllowSignedWrapAround, pOverflowEventHandler);
    }
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getMultiply(pAllowSignedWrapAround, pOverflowEventHandler), pState);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * value.
   *
   * @param pValue the value to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from dividing this state by the given
   * value.
   */
  public CompoundBitVectorInterval divide(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ISCOperatorFactory.INSTANCE.getDivide(pAllowSignedWrapAround, pOverflowEventHandler), pValue);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * interval.
   *
   * @param pInterval the interval to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from dividing this state by the given
   * interval.
   */
  public CompoundBitVectorInterval divide(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getDivide(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state resulting from dividing this state by the given
   * state.
   *
   * @param pState the state to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from dividing this state by the given
   * state.
   */
  public CompoundBitVectorInterval divide(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getDivide(pAllowSignedWrapAround, pOverflowEventHandler), pState);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given value.
   *
   * @param pValue the value to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state representing the remainder of dividing this state
   * by the given value.
   */
  public CompoundBitVectorInterval modulo(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ISCOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler), pValue);
  }


  /**
   * Computes the state representing the remainder of dividing this state
   * by the given interval.
   *
   * @param pInterval the interval to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state representing the remainder of dividing this state
   * by the given interval.
   */
  public CompoundBitVectorInterval modulo(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state representing the remainder of dividing this state
   * by the given state.
   *
   * @param pState the state to divide this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state representing the remainder of dividing this state
   * by the given state.
   */
  public CompoundBitVectorInterval modulo(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getModulo(pAllowSignedWrapAround, pOverflowEventHandler), pState);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from left shifting this state by the
   * given value.
   */
  public CompoundBitVectorInterval shiftLeft(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ISCOperatorFactory.INSTANCE.getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler), pValue);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from left shifting this state by the
   * given interval.
   */
  public CompoundBitVectorInterval shiftLeft(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state resulting from left shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from left shifting this state by the
   * given state.
   */
  public CompoundBitVectorInterval shiftLeft(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getShiftLeft(pAllowSignedWrapAround, pOverflowEventHandler), pState);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given value.
   *
   * @param pValue the value to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from right shifting this state by the
   * given value.
   */
  public CompoundBitVectorInterval shiftRight(final BigInteger pValue, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ISCOperatorFactory.INSTANCE.getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler), pValue);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given interval.
   *
   * @param pInterval the interval to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from right shifting this state by the
   * given interval.
   */
  public CompoundBitVectorInterval shiftRight(final BitVectorInterval pInterval, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(IICOperatorFactory.INSTANCE.getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler), pInterval);
  }

  /**
   * Computes the state resulting from right shifting this state by the
   * given state.
   *
   * @param pState the state to shift this state by.
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from right shifting this state by the
   * given state.
   */
  public CompoundBitVectorInterval shiftRight(final CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    return applyOperationToAllAndUnite(ICCOperatorFactory.INSTANCE.getShiftRight(pAllowSignedWrapAround, pOverflowEventHandler), pState);
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
  public CompoundBitVectorInterval logicalEquals(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (isSingleton() && equals(pState)) { return CompoundBitVectorInterval.logicalTrue(info); }
    if (!intersectsWith(pState)) { return CompoundBitVectorInterval.logicalFalse(info); }
    return getInternal(info.getRange());
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
    return equals(logicalFalse(info));
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
   *
   * @return true if all values contained in this state are greater than
   * all values in the given state, false if all values contained in this
   * state are less than or equal to all values in the given state,
   * bottom if one of the states is bottom, top otherwise.
   */
  public CompoundBitVectorInterval greaterThan(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (hasLowerBound() && pState.hasUpperBound() && getLowerBound().compareTo(pState.getUpperBound()) > 0) { return logicalTrue(info); }
    if (hasUpperBound() && pState.hasLowerBound() && getUpperBound().compareTo(pState.getLowerBound()) <= 0) { return logicalFalse(info); }
    return getInternal(info.getRange());
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
   *
   * @return true if all values contained in this state are greater
   * than or equal to all values in the given state, false if all
   * values contained in this state are less than all values in the
   * given state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundBitVectorInterval greaterEqual(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (hasLowerBound() && pState.hasUpperBound() && getLowerBound().compareTo(pState.getUpperBound()) >= 0) { return logicalTrue(info); }
    if (hasUpperBound() && pState.hasLowerBound() && getUpperBound().compareTo(pState.getLowerBound()) < 0) { return logicalFalse(info); }
    return getInternal(info.getRange());
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
   *
   * @return true if all values contained in this state are less than
   * all values in the given state, false if all values contained in
   * this state are greater than or equal to all values in the given
   * state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundBitVectorInterval lessThan(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
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
   *
   * @return true if all values contained in this state are less than
   * or equal to all values in the given state, false if all values
   * contained in this state are greater than all values in the given
   * state, bottom if one of the states is bottom, top otherwise.
   */
  public CompoundBitVectorInterval lessEqual(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
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
   *
   * @return a state representing true if this state or the given state
   * represents true, a state representing false if one of these states
   * represents false, top otherwise.
   */
  public CompoundBitVectorInterval logicalAnd(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if ((isSingleton() && containsZero()) || (pState.isSingleton() && pState.containsZero())) {
      return logicalFalse(info);
    }
    if (!containsZero() && !pState.containsZero()) { return logicalTrue(info); }
    return getInternal(info.getRange());
  }

  /**
   * Computes the logical or over this state and the given state. If
   * one of these states represents true, a state representing true is
   * returned. If neither of the states represents true, a state
   * representing false is returned. Otherwise, top is returned.
   *
   * @param pState the state to connect to this state with a
   * disjunction.
   *
   * @return a state representing true if one of this state or the
   * given state represents true, a state representing false if none of
   * these states represents true, top otherwise.
   */
  public CompoundBitVectorInterval logicalOr(final CompoundBitVectorInterval pState) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (isSingleton() && containsZero() && pState.isSingleton() && pState.containsZero()) { return logicalFalse(info); }
    if (!containsZero() || !pState.containsZero()) { return logicalTrue(info); }
    return getInternal(info.getRange());
  }

  /**
   * Computes the logical negation of this state. If this state
   * represents false, a state representing true is returned. If this
   * state does not contain false, a state representing false is
   * returned. If this state is bottom, bottom is returned. Otherwise,
   * a state representing top is returned.
   *
   * Do not confuse this method with mathematical negation or state
   * inversion. For mathematical negation, see {@link #negate(boolean, OverflowEventHandler)}.
   * For state inversion, see {@link #invert()}.
   *
   * @return a state representing true if this state represents false,
   * a state representing false if this state does not contain the
   * false state and a state representing top if the this state
   * contains both true and false.
   */
  public CompoundBitVectorInterval logicalNot() {
    if (isBottom()) { return this; }
    if (isSingleton() && containsZero()) {
      return logicalTrue(info);
    } else if (!containsZero()) { return logicalFalse(info); }
    return getInternal(info.getRange());
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
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from performing the bitwise
   * and-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise and-operation on the states' values is returned.
   * Otherwise, top is returned.
   */
  public CompoundBitVectorInterval binaryAnd(CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) {
      return bottom(info);
    }
    if (pState.isSingleton() && pState.containsZero()) {
      return pState;
    }
    if (isSingleton() && containsZero()) {
      return this;
    }
    CompoundBitVectorInterval result;
    if (pState.isSingleton()) {
      result = bottom(info);
      for (BitVectorInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          // x & 1 always yields either 0 or 1
          return pState.contains(1)
              ? getZeroToOne(info)
              : getInternal(pState.info.getRange());
        }
        result = result.unionWith(BitVectorInterval.singleton(info, interval.getLowerBound().and(pState.getValue())));
      }
    } else if (isSingleton()) {
      return pState.binaryAnd(this, pAllowSignedWrapAround, pOverflowEventHandler);
    } else {
      result = getInternal(info.getRange());
    }
    if (!result.isSingleton()) {
      CompoundBitVectorInterval absThis = absolute(pAllowSignedWrapAround, pOverflowEventHandler);
      CompoundBitVectorInterval absOther = pState.absolute(pAllowSignedWrapAround, pOverflowEventHandler);
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
      CompoundBitVectorInterval range;
      if (smallestUpperBound == null) {
        range = zero(info).extendToMaxValue();
      } else {
        range = CompoundBitVectorInterval.of(BitVectorInterval.of(info, BigInteger.ZERO, smallestUpperBound));
      }
      if (containsNegative() && pState.containsNegative()) {
        range = range.unionWith(range.negate(pAllowSignedWrapAround, pOverflowEventHandler));
      }
      result = result.intersectWith(range);
    }
    return result;
  }

  /**
   * Computes the state resulting from computing the absolute values of this
   * state.
   *
   * @param pAllowSignedWrapAround whether or not signed wrap-around is allowed.
   *
   * @return the state resulting from computing the absolute values of this
   * state.
   */
  public CompoundBitVectorInterval absolute(boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (!containsNegative()) {
      return this;
    }
    return intersectWith(one(info).negate(pAllowSignedWrapAround, pOverflowEventHandler).extendToMinValue()).negate(pAllowSignedWrapAround, pOverflowEventHandler).unionWith(intersectWith(zero(info).extendToMaxValue())).intersectWith(zero(info).extendToMaxValue());
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
  public CompoundBitVectorInterval binaryXor(CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (isSingleton() && pState.isSingleton()) {
      return of(BitVectorInterval.cast(
          info,
          getValue().xor(pState.getValue()),
          pAllowSignedWrapAround,
          pOverflowEventHandler));
    }
    if (pState.isSingleton() && pState.containsZero()) {
      return this;
    }
    if (isSingleton() && containsZero()) {
      return pState;
    }
    // [0,1] ^ 1 = [0,1]
    if (pState.isSingleton() && pState.contains(1) && equals(getZeroToOne(info))) {
      return this;
    }
    // 1 ^ [0,1] = [0,1]
    if (isSingleton() && contains(1) && pState.equals(getZeroToOne(info))) {
      return getZeroToOne(info);
    }
    if (pState.isSingleton()) {
      CompoundBitVectorInterval result = bottom(info);
      for (BitVectorInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          return getInternal(info.getRange());
        }
        result = result.unionWith(BitVectorInterval.cast(
            info,
            interval.getLowerBound().xor(pState.getValue()),
            pAllowSignedWrapAround,
            pOverflowEventHandler));
      }
      return result;
    } else if (isSingleton()) {
      return pState.binaryXor(this, pAllowSignedWrapAround, pOverflowEventHandler);
    }
    // TODO maybe a more exact implementation is possible?
    return getInternal(info.getRange());
  }

  /**
   * Computes the state resulting from flipping the bits of the
   * values represented by this state.
   *
   * @return the state resulting from flipping the bits of the
   * values represented by this state.
   */
  public CompoundBitVectorInterval binaryNot(boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    if (isBottom()) { return bottom(info); }
    CompoundBitVectorInterval result = bottom(info);
    for (BitVectorInterval interval : this.intervals) {
      if (!interval.isSingleton()) {
        // TODO maybe a more exact implementation is possible?
        return getInternal(info.getRange());
      }
      final BitVectorInterval partialResult;
      if (info.isSigned()) {
        partialResult = BitVectorInterval.cast(
            info,
            interval.getLowerBound().not(),
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      } else {
        partialResult = BitVectorInterval.cast(
            info,
            new BigInteger(1, interval.getLowerBound().not().toByteArray()),
            pAllowSignedWrapAround,
            pOverflowEventHandler);
      }
      result = result.unionWith(partialResult);
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
   *
   * @return the state resulting from performing the bitwise
   * or-operation on this state and the given state. If one of the
   * states is bottom, bottom is returned. If both states represent
   * single values, a state representing the value obtained by the
   * bit-wise or-operation on the states' values is returned.
   * Otherwise, top is returned.
   */
  public CompoundBitVectorInterval binaryOr(CompoundBitVectorInterval pState, boolean pAllowSignedWrapAround, final OverflowEventHandler pOverflowEventHandler) {
    checkBitVectorCompatibilityWith(pState.info);
    if (isBottom() || pState.isBottom()) { return bottom(info); }
    if (isSingleton() && containsZero()) {
      return pState;
    }
    if (pState.isSingleton() && pState.containsZero()) {
      return this;
    }
    if (pState.isSingleton()) {
      CompoundBitVectorInterval result = bottom(info);
      for (BitVectorInterval interval : this.intervals) {
        if (!interval.isSingleton()) {
          return getInternal(info.getRange());
        }
        result = result.unionWith(BitVectorInterval.cast(
            info,
            interval.getLowerBound().or(pState.getValue()),
            pAllowSignedWrapAround,
            pOverflowEventHandler));
      }
      return result;
    } else if (isSingleton()) {
      return pState.binaryOr(this, pAllowSignedWrapAround, pOverflowEventHandler);
    }
    // TODO maybe a more exact implementation is possible?
    return getInternal(info.getRange());
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
   *
   * @return a state representing true if either this state or the given
   * state represents false while the other represents true, a state
   * representing false if either both these states represent true or
   * both represent false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public CompoundBitVectorInterval logicalXor(CompoundBitVectorInterval pState) {
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
   *
   * @return a state representing true if both given states represent
   * true, a state representing false if one of the given states
   * represents false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundBitVectorInterval logicalAnd(CompoundBitVectorInterval p1, CompoundBitVectorInterval p2) {
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
   *
   * @return a state representing true if one of the given states
   * represents true, a state representing false if none of the given
   * states represents true, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundBitVectorInterval logicalOr(CompoundBitVectorInterval p1, CompoundBitVectorInterval p2) {
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
   *
   * @return a state representing true if one of the given states
   * represents false while the other represents true, a state
   * representing false if either both given states represent true or
   * both represent false, bottom if one of the states is bottom, top
   * otherwise.
   */
  public static CompoundBitVectorInterval logicalXor(CompoundBitVectorInterval p1, CompoundBitVectorInterval p2) {
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
   * inversion. For mathematical negation, see {@link #negate(boolean, OverflowEventHandler)}.
   * For state inversion, see {@link #invert()}.
   *
   * @param pState the state to logically negate.
   *
   * @return a state representing true if the given state represents
   * false, a state representing false if the given state does not
   * contain the false state, bottom if the given state is bottom and a
   * top if the given state contains both true and false.
   */
  public static CompoundBitVectorInterval logicalNot(CompoundBitVectorInterval pState) {
    return pState.logicalNot();
  }

  /**
   * Applies the given operator and operand to every interval in this
   * state and unites the results.
   *
   * @param pOperator the interval operator to apply to the intervals.
   * @param pOperand the second operand of each operator application.
   *
   * @return the state resulting from applying the given operator to
   * each interval of this state and the given value and uniting the
   * results.
   */
  private <T> CompoundBitVectorInterval applyOperationToAllAndUnite(Operator<BitVectorInterval, T, CompoundBitVectorInterval> pOperator, T pOperand) {
    CompoundBitVectorInterval result = bottom(info);
    for (BitVectorInterval interval : this.intervals) {
      CompoundBitVectorInterval current = pOperator.apply(interval, pOperand);
      if (current != null) {
        result = result.unionWith(current);
        if (result.containsAllPossibleValues()) {
          return result;
        }
      }
    }
    return result;
  }

  /**
   * Union of two intervals. The intervals must touch each other for this
   * operation to be possible.
   *
   * @param pA one of the intervals to be united.
   * @param pB one of the intervals to be united.
   *
   * @return the union of the two intervals.
   */
  private static BitVectorInterval union(BitVectorInterval pA, BitVectorInterval pB) {
    Preconditions.checkArgument(pA.getTypeInfo().equals(pB.getTypeInfo()));
    Preconditions.checkArgument(pA.touches(pB), "Cannot unite intervals that do not touch.");
    return BitVectorInterval.of(pA.getTypeInfo(), lowestBound(pA, pB), highestBound(pA, pB));
  }

  /**
   * Returns the lowest bound of the two given intervals.
   *
   * @param pA one if the intervals to get the lowest lower bound from.
   * @param pB one if the intervals to get the lowest lower bound from.
   *
   * @return the lowest bound of the two given intervals.
   */
  private static BigInteger lowestBound(BitVectorInterval pA, BitVectorInterval pB) {
    BigInteger aLowerBound = pA.getLowerBound();
    BigInteger bLowerBound = pB.getLowerBound();
    return lessOrEqual(aLowerBound, bLowerBound) ? aLowerBound : bLowerBound;
  }

  /**
   * Returns the highest bound of the two given intervals.
   *
   * @param pA one if the intervals to get the highest upper bound from.
   * @param pB one if the intervals to get the highest upper bound from.
   *
   * @return the highest bound of the two given intervals.
   */
  private static BigInteger highestBound(BitVectorInterval pA, BitVectorInterval pB) {
    BigInteger aUpperBound = pA.getUpperBound();
    BigInteger bUpperBound = pB.getUpperBound();
    return lessOrEqual(bUpperBound, aUpperBound) ? aUpperBound : bUpperBound;
  }

  /**
   * Checks if the first given big integer is less than the second given big integer,
   * none of which may be <code>null</code>.
   *
   * @param pFirst the value being compared to the reference value.
   * @param pSecond the reference value.
   *
   * @return <code>true</code> if <code>pFirst</code> is less than <code>pSecond</code>, <code>false</code> otherwise.
   */
  private static boolean less(BigInteger pFirst, BigInteger pSecond) {
    return pFirst.compareTo(pSecond) < 0;
  }

  /**
   * Checks if the first given big integer is less than or equal to the second given big integer, none of which may be <code>null</code>.
   *
   * @param pFirst the value being compared to the reference value.
   * @param pSecond the reference value.
   *
   * @return <code>true</code> if <code>pFirst</code> is less than or equal to <code>pSecond</code>, <code>false</code> otherwise.
   */
  private static boolean lessOrEqual(BigInteger pFirst, BigInteger pSecond) {
    return pFirst.compareTo(pSecond) <= 0;
  }

  /**
   * Creates a new compound state from the given simple interval.
   *
   * @param interval the interval to base this compound state on. If the
   * interval is {@code null}, bottom is returned.
   *
   * @return a new compound state representation of the given simple interval.
   */
  public static CompoundBitVectorInterval of(BitVectorInterval interval) {
    return getInternal(interval);
  }

  /**
   * Gets a compound state representing the given big integer value.
   *
   * @param pInfo the bit vector information.
   * @param pValue the value to be represented by the state.
   *
   * @return a compound state representing the given big integer value.
   */
  public static CompoundBitVectorInterval singleton(BitVectorInfo pInfo, BigInteger pValue) {
    Preconditions.checkNotNull(pValue);
    return CompoundBitVectorInterval.of(BitVectorInterval.singleton(pInfo, pValue));
  }

  /**
   * Gets a compound state representing the given long value.
   *
   * @param pInfo the bit vector information.
   * @param pValue the value to be represented by the state.
   *
   * @return a compound state representing the given long value.
   */
  public static CompoundBitVectorInterval singleton(BitVectorInfo pInfo, long pValue) {
    return singleton(pInfo, BigInteger.valueOf(pValue));
  }

  /**
   * Gets a compound state representing "bottom".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "bottom".
   */
  public static CompoundBitVectorInterval bottom(BitVectorInfo pInfo) {
    return new CompoundBitVectorInterval(pInfo);
  }

  /**
   * Gets a compound state representing "false".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "false".
   */
  public static CompoundBitVectorInterval logicalFalse(BitVectorInfo pInfo) {
    return zero(pInfo);
  }

  /**
   * Gets a compound state representing "true".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "true".
   */
  public static CompoundBitVectorInterval logicalTrue(BitVectorInfo pInfo) {
    return zero(pInfo).invert();
  }

  /**
   * Gets a compound state representing "zero".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "zero".
   */
  public static CompoundBitVectorInterval zero(BitVectorInfo pInfo) {
    return CompoundBitVectorInterval.singleton(pInfo, BigInteger.ZERO);
  }

  /**
   * Gets a compound state representing "1".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "1".
   */
  public static CompoundBitVectorInterval one(BitVectorInfo pInfo) {
    return CompoundBitVectorInterval.singleton(pInfo, BigInteger.ONE);
  }

  /**
   * Gets a compound state representing "-1".
   *
   * @param pInfo the bit vector information.
   *
   * @return a compound state representing "-1".
   */
  public static CompoundBitVectorInterval minusOne(BitVectorInfo pInfo) {
    return CompoundBitVectorInterval.singleton(pInfo, -1);
  }

  /**
   * Computes the compound state spanning from the lowest of the two given states' lower bounds to their highest upper bound.
   *
   * @param pLeftValue one of the states to span over.
   * @param pRightValue one of the states to span over.
   *
   * @return the compound state spanning from the lowest of the two given states' lower bounds to their highest upper bound.
   */
  public static CompoundBitVectorInterval span(CompoundBitVectorInterval pLeftValue, CompoundBitVectorInterval pRightValue) {
    return pLeftValue.span().unionWith(pRightValue.span()).span();
  }

  /**
   * Unites the given states.
   *
   * @param pLeftValue one of the states to be united.
   * @param pRightValue one of the states to be united.
   *
   * @return the union of the given states.
   */
  public static CompoundBitVectorInterval union(CompoundBitVectorInterval pLeftValue, CompoundBitVectorInterval pRightValue) {
    return pLeftValue.unionWith(pRightValue);
  }

  /**
   * Gets the state representing the given boolean value.
   *
   * @param pInfo the bit vector information.
   * @param value the boolean value to represent as compound state.
   *
   * @return the state representing the given boolean value.
   */
  public static CompoundBitVectorInterval fromBoolean(BitVectorInfo pInfo, boolean value) {
    return value ? logicalTrue(pInfo) : logicalFalse(pInfo);
  }

  private static CompoundBitVectorInterval getZeroToOne(BitVectorInfo pInfo) {
    return CompoundBitVectorInterval.of(BitVectorInterval.of(pInfo, BigInteger.ZERO, BigInteger.ONE));
  }

}

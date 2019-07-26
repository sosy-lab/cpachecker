/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import java.util.NavigableSet;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.interval.Interval;

/** This class extends the Interval class from package org.sosy_lab.cpachecker.cpa.interval */
public class IntervalExt extends Interval implements Comparable<IntervalExt> {

  private static final long serialVersionUID = 1652950516619882878L;


  protected static final IntervalExt EMPTY = new IntervalExt(null, null);

  @SuppressWarnings("hiding")
  public static final IntervalExt UNBOUND = new IntervalExt(Long.MIN_VALUE, Long.MAX_VALUE);

  @SuppressWarnings("hiding")
  public static final IntervalExt BOOLEAN_INTERVAL = new IntervalExt(0L, 1L);

  @SuppressWarnings("hiding")
  public static final IntervalExt ZERO = new IntervalExt(0L, 0L);

  @SuppressWarnings("hiding")
  public static final IntervalExt ONE = new IntervalExt(1L, 1L);

  /**
   * Constructor for a Interval with a single value e.g IntervalExt(2L) --> [2,2]
   *
   * @param pValue Long value for low AND high limit
   */
  public IntervalExt(Long pValue) {
    super(pValue);
  }
  /**
   * Constructor for a Interval two values e.g IntervalExt(2L,10L) --> [2,10]
   *
   * @param low low limit of the interval
   * @param high high limit of the interval
   */
  public IntervalExt(Long low, Long high) {
    super(low, high);
  }
  /**
   * Constructor for a IntervalExt given an interval from the superclass
   *
   * @param inter the interval from the superclass
   */
  public IntervalExt(Interval inter) {
    super(inter.getLow(), inter.getHigh());
  }

  /**
   * adds two intervals with overflow handling see {@link Interval#plus(Interval interval) plus}
   *
   * @param interval interval to add
   * @return resulting interval
   */
  public IntervalExt plus(IntervalExt interval) {
    return new IntervalExt(super.plus(interval));
  }

  @Override
  public IntervalExt negate() {
    return new IntervalExt(super.negate());
  }

  /**
   * This method intersects two intervals see {@link Interval#intersect(Interval other) intersect}
   *
   * @param other interval which is intersected
   * @return the intersection of the both intervals
   */
  public IntervalExt intersect(IntervalExt other) {
    return new IntervalExt(super.intersect(other));
  }

  /**
   * This method applies the union operator on two intervals see {@link Interval#union(Interval
   * other) union}
   *
   * @param other interval
   * @return the union of the both intervals
   */
  public IntervalExt union(IntervalExt other) {
    return new IntervalExt(super.union(other));
  }

  /**
   * This method returns true if a number is contained in the interval
   *
   * @param number which could be contained
   * @return true if the number is contained else false
   */
  public boolean contains(int number) {
    return (number > getLow() && number < getHigh());
  }

  /**
   * This method returns true if a number is contained in the interval
   *
   * @param number which could be contained
   * @return true if the number is contained else false
   */
  public boolean contains(long number) {
    return (number > getLow() && number < getHigh());
  }

  /**
   * This method returns the size of the Interval TODO if the interval is completely unbound the
   * size cannot be displayed as a long (maybe use BigInteger)
   *
   * @return the size of this interval
   */
  public long size() {
    if (isEmpty()) {
      return 0;
    }
    if (getLow() == Long.MIN_VALUE && getHigh() == Long.MAX_VALUE) {
      // throw new ArithmeticException();
      assert false : "Completely unbound intervals are currently not supported";
      return Long.MAX_VALUE;
    }
    return getHigh() - getLow() + 1;
  }

  @Override
  public int compareTo(IntervalExt obj) {

    if (this.equals(obj)) {
      return 0;
    }
    if ((this.getLow() < obj.getLow())
        || ((this.getLow().equals(obj.getLow())) && (this.size() < obj.size()))) {
      return -1;
    } else {
      return 1;
    }
  }
  /**
   * This method allows to split one interval into two intervals. eg [1,10].split([5,6]) returns
   * [1,4],[7,10] This method maybe could also be used to cut off another interval from the current
   * one(not tested)
   *
   * @param other the splitting interval
   * @return one or two Intervals which is the original interval splitted. Alternatively and empty
   *     list if the parameter interval contains this interval.
   */
  public NavigableSet<Interval> split(Interval other) {
    // assert this.contains(other);

    TreeSet<Interval> ts = new TreeSet<>();
    if (this.getLow() < other.getLow()) {
      ts.add(new Interval(this.getLow(), other.getLow() - 1));
    }
    if (this.getHigh() > other.getHigh()) {
      ts.add(new Interval(other.getHigh() + 1, this.getHigh()));
    }

    return ts;
  }


}

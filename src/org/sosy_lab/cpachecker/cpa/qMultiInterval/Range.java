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

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

// this class represents the Intervals
/**
 * In: the interval with the highest and the lowest possible value Out: A SortedList of Intervals
 */
public class Range implements Comparable<Range> {
  public static final Range EMPTY = new Range(IntervalExt.EMPTY);
  public static final Range UNBOUND = new Range(IntervalExt.UNBOUND);
  public static final Range BOOLEAN_INTERVAL = new Range(IntervalExt.BOOLEAN_INTERVAL);
  public static final Range ZERO = new Range(IntervalExt.ZERO);
  public static final Range ONE = new Range(IntervalExt.ONE);

  // config
  /**
   * The maximum amount of out Intervals. If the maximum is reached the smallest out intervals are
   * thrown away.
   */
  public static int maxOut;

  /** The single Interval with the max and min possible value */
  private IntervalExt in;
  /** This variable contains the set of intervals which are NOT included in the in interval */
  private TreeSet<IntervalExt> out;

  /**
   * Constructor for a Range with just an in interval and no out intervals
   *
   * @param in The interval for the in interval.
   */
  public Range(IntervalExt in) {
    this.in = in;
    out = new TreeSet<>();
  }

  /**
   * Constructor for a Range with just an Interval of size 1 as in interval
   *
   * @param in the interval limits (e.g. new Range(2L) --> In: [2,2] out [])
   */
  public Range(Long in) {
    this.in = new IntervalExt(in);
    out = new TreeSet<>();
  }

  /**
   * Constructor for a Range with two custom limits for the in interval
   *
   * @param in the low limit of the in interval
   * @param in2 the high limit of the in interval
   */
  public Range(Long in, Long in2) {
    this.in = new IntervalExt(in, in2);
    out = new TreeSet<>();
  }

  public IntervalExt getIn() {
    return in;
  }

  public Long getLow() {
    return in.getLow();
  }

  public Long getHigh() {
    return in.getHigh();
  }

  /**
   * Method which checks if the Range could also be displayed as a single Interval
   *
   * @return true if it can (i.e the out set is empty) else false
   */
  public boolean canBeANormalInterval() {
    return out.isEmpty();
  }

  /**
   * Adds an out Interval to the Range. It also checks if the out Interval would intersect with the
   * limits and updates them accordingly if so.
   *
   * @param o the Interval which should be added to the out(exlusion)-set of the Range
   */
  public void addOut(IntervalExt o) {
    // one out Interval cuts the a front segment of the in Interval, so we can summarize it in a
    // new in interval without adding one in out
    if (o.getLow() <= in.getLow() && o.getHigh() >= in.getLow() && o.getHigh() < in.getHigh()) {
      long high = in.getHigh();
      in = new IntervalExt(o.getHigh() + 1, high);

      // one out Interval cuts the a tail segment of the in Interval, so we can summarize it in a
      // new in interval without touching out
    } else if (o.getHigh() >= in.getHigh()
        && o.getLow() <= in.getHigh()
        && o.getLow() > in.getLow()) {
      long low = in.getLow();
      in = new IntervalExt(low, o.getLow() - 1);
    } else if (!(o.getLow() > in.getHigh() || o.getHigh() < in.getLow())) {
      out.add(o);
    }
  }

  /**
   * Checks wheter there are more out Intervals than allowed. If so first this method tries to
   * summarize {@link #summarize()} Range. If its not sufficient it will throw away the smallest out
   * Interval (If two out Intervals are bothe the smallest it will throw away the one with the
   * biggest low(or high, doesnt matther its the same) Interval)
   *
   * <p>The soundness isnt violated by this because the size of the Range will just grow. If the
   * current variable (since every variable has an assigned Range) is a "low" variable and dependent
   * on a "high" Variable, the Min-Entropy of the "high" variable can just get smaller. So we are
   * just overapproximating. If the current variable is a "high" variable it doesnt matter since in
   * the Min-Entropy calculation we just compare with the initial value of the variable, not the
   * value at the end
   */
  public void collapse() {
    if (out.size()>maxOut) {
      summarize();
    }
    for (int overstep = out.size() - maxOut; overstep > 0; overstep--) {
      Long minlen = Long.MAX_VALUE;
      IntervalExt low = null;
      for (IntervalExt i : out.descendingSet()) {
        if (i.size() < minlen) {
          minlen = i.size();
          low = i;
        }
      }
      if (low != null) {
      out.remove(low);
      }
    }
  }

  /**
   * This Method summarizes the Range. It tries to cut off the in interval if possible (e.g in:
   * [1,10] out: [0,5],[8,9] -->in: [6,10] out:[8,9] ) or summarize two out Intervals if they
   * intersect or follow each other (e.g out: [1,2],[3,4] --> out:[1,4] or out: [1,4],[3,6] -->
   * out:[1,6])
   */
  public void summarize() {
    // or dont use while and if(!isconsistent){summarize()} at the end. Dont know which has  a
    // better Performance
    while (!isConsistent()) {
      TreeSet<IntervalExt> outnew = new TreeSet<>();
    while (!out.isEmpty()) {
        IntervalExt temp = out.pollFirst();

      // one out Interval is the same as the in Interval, so the final Interval is empty
      if (temp.equals(in)) {
          in = IntervalExt.EMPTY;
      } else

      // one out Interval cuts the a front segment of the in Interval, so we can summarize it in a
      // new in interval
      if (temp.getLow() <= in.getLow()
          && temp.getHigh() >= in.getLow()
          && temp.getHigh() < in.getHigh()) {
        long high = in.getHigh();
          in = new IntervalExt(temp.getHigh() + 1, high);
        // one out Interval cuts the a tail segment of the in Interval, so we can summarize it in a
        // new in interval
      } else if (temp.getHigh() >= in.getHigh()
          && temp.getLow() <= in.getHigh()
          && temp.getLow() > in.getLow()) {
               long low = in.getLow();
          in = new IntervalExt(low, temp.getLow() - 1);

        // if the out Interval lies outside of the in Interval we can ignore it
      } else if (!(temp.getLow() > in.getHigh() || temp.getHigh() < in.getLow())) {
        if (outnew.isEmpty()) {
          outnew.add(temp);
        } else if (temp.getLow() - 1 <= outnew.last().getHigh()) {
            IntervalExt temp2 =
                new IntervalExt(
                    outnew.last().getLow(), Math.max(temp.getHigh(), outnew.pollLast().getHigh()));
          outnew.add(temp2);

        } else {
        outnew.add(temp);
        }
      }
    }
    out = outnew;
    }
  }

  /**
   * Checks the consistency of the range. It is consistent iff {@link #summarize()} would not change
   * anything of the interval. So this method is used to check if its necessary to call {@link
   * #summarize()}
   *
   * @return true if this range is consistent else false
   */
  public boolean isConsistent() {
    // check the in interval
    if (in.isEmpty()) {
      return true;
    }
    // check the out lntervals
    for (IntervalExt i : out) {
      if (!(in.getLow() < i.getLow() && in.getHigh() > i.getHigh())) {
        return false;
      }
      if ((out.higher(i) != null) && i.getHigh() >= out.higher(i).getLow() - 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return the size of this Range
   *
   * @return the size of this Range
   */
  public long size() {
    summarize();
    Long size = in.size();
    for (IntervalExt i : out) {
      size -= i.size();
    }
    return size;
  }

  /**
   * Applies the union operator to this Range. See {@link IntervalExt#union(IntervalExt other)}
   *
   * @param other the other Range to union with
   * @return the resulting Range
   */
  @SuppressWarnings("unchecked")
  public Range union(Range other) {
    // handle the in- Interval

    Range uni = new Range(in.union(other.in));

    if (!in.intersects(other.in)) {
      assert in.compareTo(other.in) != 0;
      if (in.compareTo(other.in) == -1 && in.getHigh() + 1 <= other.in.getLow() - 1) {
        uni.addOut(new IntervalExt(in.getHigh() + 1, other.in.getLow() - 1));
      } else if (in.compareTo(other.in) == 1 && other.in.getHigh() + 1 <= in.getLow() - 1) {
        uni.addOut(new IntervalExt(other.in.getHigh() + 1, in.getLow() - 1));
      }
    }

    // handle the out-intervals
    TreeSet<IntervalExt> out1 = (TreeSet<IntervalExt>) out.clone();
    TreeSet<IntervalExt> out2 = (TreeSet<IntervalExt>) other.out.clone();
    IntervalExt curr1 = null;
    IntervalExt curr2 = null;
    // do some pre processing first
    if (getLow() < other.getLow()) {
      out2.add(new IntervalExt(getLow(), other.getLow() - 1));
    } else if (other.getLow() < getLow()) {
      out1.add(new IntervalExt(other.getLow(), getLow() - 1));
    }

    if (getHigh() > other.getHigh()) {
      out2.add(new IntervalExt(other.getHigh() + 1, getHigh()));
    } else if (other.getHigh() > getHigh()) {
      out1.add(new IntervalExt(getHigh() + 1, other.getHigh()));
    }

    boolean con = true;
    while (!(out1.isEmpty() && curr1 == null) && !(out2.isEmpty() && curr2 == null) && con) {
      if (curr1 == null && !out1.isEmpty()) {
        curr1 = out1.pollFirst();
      }
      if (curr2 == null && !out2.isEmpty()) {
        curr2 = out2.pollFirst();
      }
      if (curr1 == null || curr2 == null) {
        con = false;
      }
      assert (curr1 != null && curr2 != null);
      if (curr1.intersects(curr2)) {
        uni.addOut(curr1.intersect(curr2));
        if (curr1.contains(curr2)) {
          curr2 = null;
        } else if (curr2.contains(curr1)) {
          curr1 = null;
        }

      }


      if (curr1 != null && curr2 != null) {
        int com = curr1.compareTo(curr2);
        assert com != 0;
        if (com == -1) {
          curr1 = null;
        } else {
          curr2 = null;
        }
      }
    }
    uni.summarize();
    return uni;
  }

  /**
   * Intersects this Range with another Range
   *
   * @param other other Range
   */
  public Range intersect(Range other) {
    Range intersection = new Range(in.intersect(other.in));
    for (IntervalExt i : other.out) {
      intersection.addOut(i);
    }
    for (IntervalExt i : out) {
      intersection.addOut(i);
    }
    intersection.summarize();
    return intersection;
  }
  /**
   * Intersects this Range with an Interval
   *
   * @param other other Interval
   */
  public Range intersect(IntervalExt other) {
    Range intersection = new Range(in.intersect(other));

    for (IntervalExt i : out) {
      intersection.addOut(i);
    }
    // Maybe not necessary but i let it here
    intersection.summarize();
    return intersection;
  }


  @Override
  public String toString() {
    return "[In:" + in.toString() + " Out: " + out.toString() + "] Size: " + size();
    // optional with probability
    // return "[In:" + in.toString() + " Out: " + out.toString() + "] Prob:" + probability;
  }



  @Override
  public int compareTo(Range r) {
    if (in.compareTo(r.in) == 0) {
      // infinityloop? look here if there is one and change 1 to 0
      if (hashCode() < r.hashCode()) {
        return -1;
      } else if (hashCode() > r.hashCode()) {
        return 1;
      } else {
        return 0;
      }
    }
    return in.compareTo(r.in);
  }
  /**
   * This method determines whether the Range is empty or not.
   *
   * @return true, if the interval is empty, i.e. the lower and upper bounds are null
   */
  public boolean isEmpty() {
    // return in.getLow() == null && in.getHigh() == null;
    return in.isEmpty();
  }

  //  Its possible to do a more performant method ...

  @Override
  public boolean equals(Object other) {

    if (!(other instanceof Range)) {
      return false;
    }
    if (!(((Range) other).in.equals(this.in))) {
      return false;
    }

    if (((Range) other).size() != this.size()) {
      return false;
    }
    if (((Range) other).out.size() != this.out.size()) {
      return false;
    }

    for (IntervalExt i : ((Range) other).out) {
      if (!(out.contains(i))) {
        return false;
      }
    }
    // not nessessary any more
    //    for (Interval i : out) {
    //      if (!(((Range) other).out.contains(i))) {
    //        return false;
    //      }
    //    }
    return true;
  }

  /**
   * This method determines if this Range is definitely greater than the other Range.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is always strictly greater than the upper
   *     bound of the other interval, else false
   */
  public boolean isGreaterThan(Range other) {
    return !isEmpty() && !other.isEmpty() && in.getLow() > other.getHigh();
  }

  @Override
  public int hashCode() {
    // Auto-generated method stub
    return super.hashCode();
  }

  @Override
  public Range clone() {
    Range r = new Range(in);
    for (IntervalExt i : out) {
      r.addOut(i);
    }
    return r;
  }

  /**
   * This method adds an Range to this Range, overflow is handled by setting the bound to
   * Long.MIN_VALUE or Long.MAX_VALUE respectively. This overapproximates since its hard to find
   * values which cant be taken for example [in: [1,9], out: [2]] + [in: [5,10], out: [6]] = [in
   * :[6,19], out :[7]] but [in: [0,9], out: [2]] + [in: [5,10], out: [6]] = [in : [6,19], out :[]]
   * the difference is in the starting point of both [in] intervals
   *
   * @param interval the Range to add
   * @return a new Range with the respective bounds
   */
  public Range plus(Range interval) {
    if (isEmpty() || interval.isEmpty()) {
      return EMPTY;
    }
    if (interval.canBeANormalInterval()) {
      return plus(interval.in);
    } else if (canBeANormalInterval()) {
      return interval.plus(in);

    } else {
      Range rplus =
          new Range(
              new IntervalExt(
                  scalarPlus(in.getLow(), interval.getLow()),
                  scalarPlus(in.getHigh(), interval.getHigh())));
      // how to get good value to solve the problem in the description? Is there a way to calculate
      // without approximating?

      return rplus;
    }
  }
  /**
   * This method adds an Interval to this Range, overflow is handled by setting the bound to
   * Long.MIN_VALUE or Long.MAX_VALUE respectively. This is also a approximation.
   *
   * @param interval the Interval to add
   * @return a new Range with the respective bounds
   */
  public Range plus(IntervalExt interval) {
    if (isEmpty() || interval.isEmpty()) {
      return EMPTY;
    }
    Range rplus =
        new Range(
            new IntervalExt(
                scalarPlus(in.getLow(), interval.getLow()),
                scalarPlus(in.getHigh(), interval.getHigh())));
    // how to get good value to solve the problem in the description? Is there a way to calculate
    // without approximating?

    // we can solve without approximation if interval has only size 1 i.e. it the high and low are
    // the same
    if (interval.size() == 1) {
      for (IntervalExt i : out) {
        rplus.addOut(i.plus(interval));
      }
    }

    // an other experimental would be if interval.size is 2 and there is an out with size 2 it could
    // also be calculated (too expensive maybe)
    return rplus;
  }

  /**
   * see {@link #plus(Range)} with the negation of the parameter
   *
   * @param other the other range to subtract
   * @return this range subtracted the other other range
   */
  public Range minus(Range other) {
    return plus(other.negate());
  }

  /**
   * see {@link #plus(IntervalExt)} with the Interval [-l,-l]
   *
   * @param l the number to subtract
   * @return a new Range which is the current range subtracted by the given number
   */
  public Range minus(Long l) {
    return plus(new IntervalExt(-l, -l));
  }

  /**
   * see {@link #plus(IntervalExt)} with the Interval [l,l]
   *
   * @param l the number to add
   * @return a new Range which is the current range with the given number added
   */
  public Range plus(Long l) {
    return plus(new IntervalExt(l, l));
  }

  /**
   * This method adds two scalar values and returns their sum, or on overflow Long.MAX_VALUE or
   * Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the sum of the first and second scalar operand or on overflow Long.MAX_VALUE and
   *     Long.MIN_VALUE, respectively.
   */
  private static Long scalarPlus(Long x, Long y) {
    Long result = x + y;

    // both operands are positive but the result is negative
    if ((Long.signum(x) + Long.signum(y) == 2) && Long.signum(result) == -1) {
      result = Long.MAX_VALUE;
      // and vice versa
    } else if ((Long.signum(x) + Long.signum(y) == -2) && Long.signum(result) == +1) {
      result = Long.MIN_VALUE;
    }

    return result;
  }
  /**
   * This method multiplies this Range with another Range. In case of an overflow Long.MAX_VALUE and
   * Long.MIN_VALUE are used instead.
   *
   * @param other interval to multiply this interval with
   * @return new interval that represents the result of the multiplication of the two intervals
   */
  public Range times(Range other) {
    Long[] values = {
      scalarTimes(getLow(), other.getLow()),
      scalarTimes(getLow(), other.getHigh()),
      scalarTimes(getHigh(), other.getLow()),
      scalarTimes(getHigh(), other.getHigh())
    };

    return new Range(
        new IntervalExt(
            Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values))));
  }

  /**
   * This method multiplies two scalar values and returns their product, or on overflow
   * Long.MAX_VALUE or Long.MIN_VALUE, respectively.
   *
   * @param x the first scalar operand
   * @param y the second scalar operand
   * @return the product of the first and second scalar operand or on overflow Long.MAX_VALUE and
   *     Long.MIN_VALUE, respectively.
   */
  private static Long scalarTimes(Long x, Long y) {

    Long bound = (Long.signum(x) == Long.signum(y)) ? Long.MAX_VALUE : Long.MIN_VALUE;

    // if overflow occurs, return the respective bound
    if (x != 0 && ((y > 0 && y > (bound / x)) || (y < 0 && y < (bound / x)))) {
      return bound;
    } else {
      return x * y;
    }
  }

  /**
   * This method divides this interval by another interval. If the other interval contains "0" an
   * unbound interval is returned.
   *
   * @param other interval to divide this interval by
   * @return new interval that represents the result of the division of the two intervals
   */
  public Range divide(Range other) {
    // other interval contains "0", return unbound interval

    // lets assume we dont divide through 0
    /*if (other.contains(0L)) {
      return UNBOUND;
    } */
    Long[] values = new Long[4];
    if (other.getHigh() == 0L && other.getLow() == 0L) {
      return EMPTY;
    }
    if (other.getLow() == 0L) {

      values[0] = getLow() / other.getHigh();
      values[1] = getHigh() / other.getHigh();
      values[2] = getLow() / other.getHigh();
      values[3] = getHigh() / other.getHigh();

    } else if (other.getHigh() == 0L) {

      values[0] = getLow() / other.getLow();
      values[1] = getHigh() / other.getLow();
      values[2] = getLow() / other.getLow();
      values[3] = getHigh() / other.getLow();

    } else {

      values[0] = getLow() / other.getLow();
      values[1] = getLow() / other.getHigh();
      values[2] = getHigh() / other.getLow();
      values[3] = getHigh() / other.getHigh();
    }
    return new Range(
        new IntervalExt(
            Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values))));
  }

  /**
   * This method determines if this Range contains a single number
   *
   * @param value the number which could be contained
   * @return true if this Range contains the given number else false
   */
  public boolean contains(Long value) {
    if (!(getLow() <= value && getHigh() >= value)) {
      return false;
    } else {
      for (IntervalExt i : out) {
        if (i.contains(value)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * This method determines if this Range contains another Range.
   *
   * <p>An empty interval does not contain any interval and is not contained in any interval either.
   * So if the callee or parameter is an empty interval, this method will return false.
   *
   * @param other the other Range
   * @return true if this Range contains the other Range, else false
   */
  public boolean contains(Range other) {
    if (isEmpty() || other.isEmpty()) {
      return false;
    }
    if (!(getLow() <= other.getLow() && other.getHigh() <= getHigh())) {
      return false;
    }
    Range temp = this.intersect(other);
    if (temp.equals(other)) {
      return true;
    }

    return false;
  }

  /**
   * This method determines if this Range contains another Interval.
   *
   * <p>An empty interval does not contain any interval and is not contained in any interval either.
   * So if the callee or parameter is an empty interval, this method will return false.
   *
   * @param other the other Range
   * @return true if this Range contains the other Range, else false
   */
  public boolean contains(IntervalExt other) {
    if (isEmpty() || other.isEmpty()) {
      return false;
    }
    if (!(getLow() <= other.getLow() && other.getHigh() <= getHigh())) {
      return false;
    }

    for (IntervalExt i : out) {
      if (other.contains(i)) {
        return false;
      }
    }

    return true;
  }

  /**
   * This method determines if this Range maybe less or equal another Range. More exactly if I take
   * p1 as single possible number out of this interval and p2 out of the other interval. Both
   * uniformly at random. This method determines is it possible that p1 <= p2 ?
   *
   * @param other the other range
   * @return True if this Range could be less or equal else false
   */
  public boolean maybeLessOrEqual(Range other) {
    if (getLow() > other.getHigh()) {
      return false;
    }
    return true;
  }

  /**
   * This method limits the upper bound of this range, by the upper bound of the other range.
   *
   * @param other Range
   * @return a new Range with the upper bound limited by the other range.
   */
  public Range limitUpperBound(Range other) {
    if (isEmpty() || other.isEmpty() || getLow() > other.getHigh()) {
      return EMPTY;
    }
    if (!other.isConsistent()) {
      other.summarize();
    }
    Range ret = new Range(getLow(), Math.min(getHigh(), other.getHigh()));
    return ret;
  }

  /**
   * This method limits the lower bound of this range, by the lower bound of the other range.
   *
   * @param other Range
   * @return a new Range with the lower bound limited by the other range.
   */
  public Range limitLowerBound(Range other) {
    if (isEmpty() || other.isEmpty() || getHigh() < other.getLow()) {
      return EMPTY;
    }
    if (!other.isConsistent()) {
      other.summarize();
    }
    Range ret = new Range(Math.max(getLow(), other.getLow()), getHigh());
    return ret;
  }

  /**
   * This method negates this Range.
   *
   * @return new negated Range
   */
  public Range negate() {
    Range nRange = new Range(in.negate());

    for (IntervalExt i : out) {
      nRange.addOut(i.negate());
    }
    return nRange;
  }

  // quite expensive operation
  /**
   * This method determines if this Range intersects with another given Range
   *
   * @param pOther the other Range
   * @return True if they intersect else false
   */
  public boolean intersects(Range pOther) {
    if (!in.intersects(pOther.in)) {
      return false;
    }
    if (this.intersect(pOther).isEmpty()) {
      return false;
    }
    return true;
  }

  // Main for testing the Range in different cases.
  public static void main(String[] args) {

    Range r = new Range(new IntervalExt(0L, 100L));
    r.addOut(new IntervalExt(1L));
    r.addOut(new IntervalExt(3L));
    r.addOut(new IntervalExt(5L));
    r.addOut(new IntervalExt(7L));
    r.addOut(new IntervalExt(9L));
    r.addOut(new IntervalExt(11L));
    r.addOut(new IntervalExt(14L));
    r.addOut(new IntervalExt(13L));
    r.addOut(new IntervalExt(16L));
    r.addOut(new IntervalExt(18L));
    r.addOut(new IntervalExt(18L, 20L));
    r.addOut(new IntervalExt(19L, 25L));

    Log.Log2(r);
    r.summarize();
    Log.Log2(r);
    r.collapse();
    Log.Log2(r);


  }
}

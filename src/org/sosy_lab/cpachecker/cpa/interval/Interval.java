package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Arrays;
import java.util.Collections;

public class Interval
{
  /**
   * the lower bound of the interval
   */
  protected Long low;

  /**
   * the upper bound of the interval
   */
  protected Long high;

  /**
   * an interval representing a false value
   */
  public static final Interval FALSE = new Interval(0, 0);

  /**
   * an interval representing an impossible interval
   */
  public static final Interval EMPTY = createEmptyInterval();

  /**
   * This method acts as constructor for an empty interval.
   *
   */
  private Interval()
  {
  }

  /**
   * This method acts as constructor for an integer-based interval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public Interval(Integer low, Integer high)
  {
    this.low  = low.longValue();

    this.high = high.longValue();
  }

  /**
   * This method acts as constructor for a long-based interval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public Interval(Long low, Long high)
  {
    this.low  = low;

    this.high = high;
  }

  /**
   * This method acts as constructor for a single-value interval.
   *
   * @param value for the lower and upper bound
   */
  public Interval(Long value)
  {
    this.low  = value;

    this.high = value;
  }

  /**
   * This method returns the lower bound of the interval.
   *
   * @return the lower bound
   */
  public Long getLow()
  {
    return low;
  }

  /**
   * This method returns the upper bound of the interval.
   *
   * @return the upper bound
   */
  public Long getHigh()
  {
    return high;
  }

  /**
   * This method set the lower bound of the interval.
   *
   * @param the lower bound
   */
  public void setLow(Long low)
  {
    this.low = low;
  }

  /**
   * This method sets the upper bound of the interval.
   *
   * @param the upper bound
   */
  public void setHigh(Long high)
  {
    this.high = high;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object other)
  {
    if(getClass().equals(other.getClass()))
    {
      Interval another = (Interval)other;

      if(isEmpty() && another.isEmpty())
        return true;

      else if(isEmpty() || another.isEmpty())
        return false;

      return low.equals(another.low) && high.equals(another.high);
    }
    else
      return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    if(isEmpty())
      return 0;

    int result = 17;

    result = 31 * result + low.hashCode();
    result = 31 * result + high.hashCode();

    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public Interval clone()
  {
    return new Interval(low, high);
  }

  /**
   * This method creates a new interval instance representing the union of this interval with another interval.
   *
   * The lower bound and upper bound of the new interval is the minimum of both lower bounds and the maximum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval union(Interval other)
  {
      if(isEmpty() || other.isEmpty())
        return createEmptyInterval();

      else
        return new Interval(Math.min(low, other.low), Math.max(high, other.high));
  }

  /**
   * This method creates a new interval instance representing the intersection of this interval with another interval.
   *
   * The lower bound and upper bound of the new interval is the maximum of both lower bounds and the minimum of both upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval intersect(Interval other)
  {
    Interval interval = null;

    if(this.intersects(other))
      interval = new Interval(Math.max(low, other.low), Math.min(high, other.high));

    else
      interval = createEmptyInterval();

    return interval;
  }

  /**
   * This method determines of this interval is less than the other interval.
   *
   * @param other interval to compare with
   * @return true if the upper bound of this interval is strictly lower than the lower bound of the other interval, else false
   */
  public boolean isLessThan(Interval other)
  {
    return !isEmpty() && !other.isEmpty() && high < other.low;
  }

  /**
   * This method determines of this interval is greater than the other interval.
   *
   * @param other interval to compare with
   * @return true if the lower bound of this interval is strictly greater than the upper bound of the other interval, else false
   */
  public boolean isGreaterThan(Interval other)
  {
    return !isEmpty() && !other.isEmpty() && low > other.high;
  }

  /**
   * This method determines if this interval represents a false value.
   *
   * @return true if this interval represents only values in the interval [0, 0].
   */
  public boolean isFalse()
  {
    return equals(FALSE);
  }

  /**
   * This method determines if this interval represents a true value.
   *
   * @return true if this interval represents values that are strictly less than 0 or greater than 0.
   */
  public boolean isTrue()
  {
    return !isEmpty() && (high < 0 || low > 0);
  }

  /**
   * This method creates a new interval instance with the lower and upper bounds being the minimum of both the lower and upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval minimum(Interval other)
  {
    Interval interval = new Interval(Math.min(low, other.low), Math.min(high, other.high));

    return interval;
  }

  /**
   * This method creates a new interval instance with the lower and upper bounds being the maximum of both the lower and upper bounds, respectively.
   *
   * @param other the other interval
   * @return the new interval with the respective bounds
   */
  public Interval maximum(Interval other)
  {
    Interval interval = new Interval(Math.max(low, other.low), Math.max(high, other.high));

    return interval;
  }

  /**
   * This method returns a new interval with a limited, i.e. higher, lower bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the upper bound of this interval and the lower bound set to the maximum of this interval's and the other interval's lower bound or an empty interval if this interval is less than the other interval.
   */
  public Interval limitLowerBoundBy(Interval other)
  {
    Interval interval = null;

    if(isEmpty() || other.isEmpty() || high < other.low)
      interval = createEmptyInterval();

    else
      interval = new Interval(Math.max(low, other.low), high);

    return interval;
  }

  /**
   * This method returns a new interval with a limited, i.e. lower, upper bound.
   *
   * @param other the interval to limit this interval
   * @return the new interval with the lower bound of this interval and the upper bound set to the minimum of this interval's and the other interval's upper bound or an empty interval if this interval is greater than the other interval.
   */
  public Interval limitUpperBoundBy(Interval other)
  {
    Interval interval = null;

    if(isEmpty() || other.isEmpty() || low > other.high)
      interval = createEmptyInterval();

    else
      interval = new Interval(low, Math.min(high, other.high));

    return interval;
  }

  /**
   * This method determines if this interval intersects with another interval.
   *
   * @param other the other interval
   * @return true if the intervals intersect, else false
   */
  public boolean intersects(Interval other)
  {
      if(isEmpty() || other.isEmpty())
        return false;

      return (low >= other.low && low <= other.high)
        || (high >= other.low && high <= other.high)
        || (low <= other.low && high >= other.high);
  }

  /**
   * This method determines if this interval contains another interval.
   *
   * The method still returns true, if the borders match. An empty interval does not contain any interval and is not contained in any interval either. So if the callee or parameter is an empty interval, this method will return false.
   *
   * @param other the other interval
   * @return true if this interval contains the other interval, else false
   */
  public boolean contains(Interval other)
  {
     return (!isEmpty() && !other.isEmpty()
               && low <= other.low && other.high <= high);
  }

  /**
   * This method adds an interval from this interval, underflow and overflow are being handled by setting the respective bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param interval the interval to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(Interval interval)
  {
    if(isEmpty() || interval.isEmpty())
      return createEmptyInterval();

    Interval result = new Interval(low + interval.low, high + interval.high);

    // handle underflow and overflow
    if((Long.signum(low) + Long.signum(interval.low) == -2) && Long.signum(result.low) == +1)
      result.low = Long.MIN_VALUE;

    else if((Long.signum(low) + Long.signum(interval.low) == 2) && Long.signum(result.low) == -1)
      result.low = Long.MAX_VALUE;

    if((Long.signum(high) + Long.signum(interval.high) == -2) && Long.signum(result.high) == +1)
      result.high = Long.MIN_VALUE;

    else if((Long.signum(high) + Long.signum(interval.high) == 2) && Long.signum(result.high) == -1)
      result.high = Long.MAX_VALUE;

    return result;
  }

  /**
   * This method adds a constant offset to this interval, underflow and overflow are being handled by setting the respective bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to add
   * @return a new interval with the respective bounds
   */
  public Interval plus(int offset)
  {
    return plus(new Interval(offset, offset));
  }

  /**
   * This method subtracts an interval from this interval, underflow and overflow are being handled by setting the respective bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param interval the interval to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(Interval interval)
  {
    return plus(new Interval(interval.low * (-1), interval.high * (-1)));
  }

  /**
   * This method subtracts a constant offset to this interval, underflow and overflow are being handled by setting the respective bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to subtract
   * @return a new interval with the respective bounds
   */
  public Interval minus(int offset)
  {
    return plus(-offset);
  }

  /**
   * This method multiplies this interval with another interval.
   *
   * @param other interval to multiply this interval with
   * @return new interval that represents the multiplication of the two intervals
   */
  public Interval times(Interval other)
  {
    Long[] values = {
                      low.longValue() * other.low.longValue(),
                      low.longValue() * other.high.longValue(),
                      high.longValue() * other.low.longValue(),
                      high.longValue() * other.high.longValue()
                    };

    return new Interval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
  }

  /**
   * This method negates this interval.
   *
   * @return new negated interval
   */
  public Interval negate()
  {
    return new Interval(high.longValue() * (-1), low.longValue() * (-1));
  }

  /**
   * This method determines whether the interval is empty or not.
   *
   * @return true, if the interval is empty, i.e. the lower and upper bounds are null
   */
  public boolean isEmpty()
  {
    return low == null && high == null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return "[" + low + "; " + high + "]";
  }

  /**
   * This method is a factory method for an empty interval
   *
   * @return an empty interval
   */
  private static Interval createEmptyInterval()
  {
    return new Interval();
  }

  /**
   * This method is a factory method for an unbounded interval
   *
   * @return an unbounded interval, i.e. the lower and upper bound are set to Long.MIN_VALUE and Long.MAX_VALUE respectively
   */
  public static Interval createUnboundInterval()
  {
    return new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for a lower bounded interval.
   *
   * @param lowerBound the lower bound to set
   * @return a lower bounded interval, i.e. the lower bound is set to the given lower bound, the upper bound is set to Long.MAX_VALUE
   */
  public static Interval createLowerBoundedInterval(Long lowerBound)
  {
    return new Interval(lowerBound, Long.MAX_VALUE);
  }

  /**
   * This method is a factory method for an upper bounded interval.
   *
   * @param upperBound the upper bound to set
   * @return an upper bounded interval, i.e. the lower bound is set to Long.MIN_VALUE, the upper bound is set to the given upper bound
   */
  public static Interval createUpperBoundedInterval(Long upperBound)
  {
    return new Interval(Long.MIN_VALUE, upperBound);
  }
}
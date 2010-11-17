package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Arrays;
import java.util.Collections;

public class Interval
{
  protected Long low;

  protected Long high;

  /**
   * an interval representing a false value
   */
  public static final Interval FALSE = new Interval(0, 0);

  /**
   * an interval representing an impossible interval
   */
  public static final Interval EMPTY = createEmptyInterval();

  private Interval()
  {
  }

  public Interval(Integer low, Integer high)
  {
    this.low  = low.longValue();

    this.high = high.longValue();
  }

  public Interval(Long low, Long high)
  {
    this.low  = low;

    this.high = high;
  }

  public Interval(Long value)
  {
    this.low  = value;

    this.high = value;
  }

  public Long getLow()
  {
    return low;
  }

  public Long getHigh()
  {
    return high;
  }

  public void setLow(Long low)
  {
    this.low = low;
  }

  public void setHigh(Long high)
  {
    this.high = high;
  }

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

  public Interval limitLowerBoundBy(Interval other)
  {
    Interval interval = null;

    if(high < other.low)
      interval = createEmptyInterval();

    else
      interval = new Interval(Math.max(low, other.low), high);

    return interval;
  }

  public Interval limitUpperBoundBy(Interval other)
  {
    Interval interval = null;

    if(low > other.high)
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

  public Interval plus(Interval interval)
  {
    Interval result = new Interval(low + interval.low, high + interval.high);

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

  public Interval plus(int offset)
  {
    return plus(new Interval(offset, offset));
  }

  public Interval minus(Interval interval)
  {
    return plus(new Interval(interval.low * (-1), interval.high * (-1)));

/* probably faster, but somewhat redundant
    Interval result = new Interval(low - interval.high, high - interval.low);

    if((Long.signum(low) == -1 && Long.signum(interval.high) == +1) && Long.signum(result.low) == 1)
      result.low = Long.MIN_VALUE;
    else if((Long.signum(low) == +1 && Long.signum(interval.high) == -1) && Long.signum(result.low) == -1)
      result.low = Long.MAX_VALUE;

    if((Long.signum(high) == -1 && Long.signum(interval.low) == +1) && Long.signum(result.high) == 1)
      result.high = Long.MIN_VALUE;
    else if((Long.signum(high) == +1 && Long.signum(interval.low) == -1) && Long.signum(result.high) == -1)
      result.high = Long.MAX_VALUE;

    return result;
*/
  }

  public Interval times(Interval interval)
  {
    Long[] values = {
                      low.longValue() * interval.low.longValue(),
                      low.longValue() * interval.high.longValue(),
                      high.longValue() * interval.low.longValue(),
                      high.longValue() * interval.high.longValue()
                    };

    return new Interval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
  }

  public Interval negate()
  {
    Long temp  = low;

    low     = high.longValue() * (-1);

    high    = temp.longValue() * (-1);

    return this;
  }

  public boolean isEmpty()
  {
    return low == null && high == null;
  }

  @Override
  public String toString()
  {
    return "[" + low + "; " + high + "]";
  }

  private static Interval createEmptyInterval()
  {
    return new Interval();
  }

  public static Interval createUnboundInterval()
  {
    return new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  public static Interval createLowerBoundedInterval(Long lowerBound)
  {
    return new Interval(lowerBound, Long.MAX_VALUE);
  }

  public static Interval createUpperBoundedInterval(Long upperBound)
  {
    return new Interval(Long.MIN_VALUE, upperBound);
  }
}
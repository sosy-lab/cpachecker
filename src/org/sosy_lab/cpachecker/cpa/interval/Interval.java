package org.sosy_lab.cpachecker.cpa.interval;


import java.util.Objects;

/**
 * An abstract domain representing integers as intervals.
 */
public abstract sealed class Interval
        permits Interval.ReachableInterval, Interval.Unreachable {

  public static Interval unreachable() {
    return new Unreachable();
  }

  public static Interval unknown() {
    return new ReachableInterval(InfInt.negInf(), InfInt.posInf());
  }

  public static Interval of(InfInt lowerLimit, InfInt upperLimit) {
    return new ReachableInterval(lowerLimit, upperLimit);
  }

  public static Interval of(InfInt lowerLimit, long upperLimit) {
    return new ReachableInterval(lowerLimit, InfInt.of(upperLimit));
  }

  public static Interval of(long lowerLimit, InfInt upperLimit) {
    return new ReachableInterval(InfInt.of(lowerLimit), upperLimit);
  }

  public static Interval of(long lowerLimit, long upperLimit) {
    return new ReachableInterval(InfInt.of(lowerLimit), InfInt.of(upperLimit));
  }

  public static Interval of(long bothLimits) {
    return new ReachableInterval(InfInt.of(bothLimits), InfInt.of(bothLimits));
  }

  /**
   * The join operation.
   *
   * @param other another value.
   * @return the joined value.
   */
  abstract Interval join(Interval other);

  /**
   * The meet operation.
   *
   * @param other another value.
   * @return the intersection of both values.
   */
  abstract Interval meet(Interval other);

  /**
   * The widening operation.
   *
   * @param other another value.
   * @return the widened value.
   */
  abstract Interval widen(Interval other);


  /**
   * The addition transformation.
   *
   * @param other another value.
   * @return the sum of both.
   */
  abstract Interval add(Interval other);

  /**
   * The subtraction transformation.
   *
   * @param other another value.
   * @return the difference of both.
   */
  abstract Interval subtract(Interval other);

  /**
   * The addition transformation.
   *
   * @param constant an integer.
   * @return the transformed value.
   */
  abstract Interval addConstant(long constant);

  /**
   * The subtraction transformation.
   *
   * @param constant an integer.
   * @return the transformed value.
   */
  abstract Interval subtractConstant(long constant);

  /**
   * The multiplication transformation.
   *
   * @param other another value.
   * @return the product of both.
   */
  abstract Interval multiply(Interval other);

  /**
   * The multiplication transformation.
   *
   * @param constant an integer.
   * @return the transformed value.
   */
  abstract Interval multiplyByConstant(long constant);

  /**
   * The division transformation.
   *
   * @param other another value.
   * @return the division of both.
   */
  abstract Interval divide(Interval other);

  /**
   * The division transformation.
   *
   * @param constant an integer.
   * @return the transformed value.
   */
  abstract Interval divideByConstant(long constant);

  /**
   * The modulo transformation.
   *
   * @param other another value.
   * @return the modulo of both.
   */
  abstract Interval modulo(Interval other);

  /**
   * The modulo transformation.
   *
   * @param constant an integer.
   * @return the modulo.
   */
  abstract Interval modulo(long constant);

  /**
   * The absolute value transformation.
   *
   * @return the absolute value.
   */
  abstract Interval absoluteValue();

  /**
   * The negation transformation. Inverses the sign in the concrete. Equal to
   * multiplication by -1.
   *
   * @return the transformed value.
   */
  abstract Interval negate();

  abstract boolean isGreaterThan(Interval other);

  abstract boolean isGreaterEqualThan(Interval other);

  abstract boolean isEqual(Interval other);

  abstract boolean mayBeGreaterThan(Interval other);

  abstract boolean mayBeGreaterEqualThan(Interval other);

  abstract boolean isReachable();

  /**
   * An {@link Interval}, that is not unreachable.
   */
  public final static class ReachableInterval extends Interval {

    private final InfInt lowerLimit;
    private final InfInt upperLimit;

    public ReachableInterval(InfInt pLowerLimit, InfInt pUpperLimit) {
      this.lowerLimit = pLowerLimit;
      this.upperLimit = pUpperLimit;
    }

    @Override
    public Interval join(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var lower = InfInt.min(this.lowerLimit, reachableOther.lowerLimit);
        var upper = InfInt.max(this.upperLimit, reachableOther.upperLimit);
        return new ReachableInterval(lower, upper);
      }
      return this;
    }


    @Override
    public Interval meet(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var lower = InfInt.max(this.lowerLimit, reachableOther.lowerLimit);
        var upper = InfInt.min(this.upperLimit, reachableOther.upperLimit);

        if (!lower.isGreaterThan(upper)) {
          return new ReachableInterval(lower, upper);
        }
      }

      return new Unreachable();
    }

    /**
     * The abstract widening operator. See: Cousot, P., Cousot, R. (1992). Comparing the Galois
     * connection and widening/narrowing approaches to abstract interpretation. In: Bruynooghe, M.,
     * Wirsing, M. (eds) Programming Language Implementation and Logic Programming. PLILP 1992.
     * Lecture Notes in Computer Science, vol 631. Springer, Berlin, Heidelberg. <a
     * href="https://doi.org/10.1007/3-540-55844-6_142">https://doi.org/10.1007/3-540-55844-6_142</a>.
     *
     * @param other the interval to widen this with.
     * @return the widened interval.
     */
    @Override
    public Interval widen(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var lower = this.lowerLimit;
        var upper = this.upperLimit;

        if (reachableOther.lowerLimit.isLessThan(this.lowerLimit)) {
          lower = InfInt.negInf();
        }
        if (reachableOther.upperLimit.isGreaterThan(this.upperLimit)) {
          upper = InfInt.posInf();
        }
        return new ReachableInterval(lower, upper);
      }
      return this;
    }

    @Override
    public String toString() {
      return "[%s, %s]".formatted(lowerLimit, upperLimit);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof ReachableInterval reachableOther) {
        return this.lowerLimit.equals(reachableOther.lowerLimit)
            && this.upperLimit.equals(reachableOther.upperLimit);
      }
      return false;
    }

    @Override
    public Interval add(Interval value) {
      if (value instanceof ReachableInterval reachableValue) {
        return new ReachableInterval(
            lowerLimit.add(reachableValue.lowerLimit),
            upperLimit.add(reachableValue.upperLimit)
        );
      }
      return new Unreachable();
    }

    @Override
    public ReachableInterval negate() {
      return new ReachableInterval(upperLimit.negate(), lowerLimit.negate());
    }

    @Override
    public Interval subtract(Interval value) {
      return add(value.negate());
    }

    @Override
    public Interval addConstant(long constant) {
      return add(of(constant, constant));
    }

    @Override
    public Interval subtractConstant(long constant) {
      return addConstant(-constant);
    }

    @Override
    public Interval multiply(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var lowerBound = InfInt.min(
            this.lowerLimit.multiply(reachableOther.lowerLimit),
            this.lowerLimit.multiply(reachableOther.upperLimit),
            this.upperLimit.multiply(reachableOther.lowerLimit),
            this.upperLimit.multiply(reachableOther.upperLimit)
        );
        var upperBound = InfInt.max(
            this.lowerLimit.multiply(reachableOther.lowerLimit),
            this.lowerLimit.multiply(reachableOther.upperLimit),
            this.upperLimit.multiply(reachableOther.lowerLimit),
            this.upperLimit.multiply(reachableOther.upperLimit)
        );
        return new ReachableInterval(lowerBound, upperBound);
      }
      return new Unreachable();
    }

    @Override
    public Interval multiplyByConstant(long constant) {
      return new ReachableInterval(this.lowerLimit.multiply(constant), this.upperLimit.multiply(constant));
    }

    @Override
    public Interval divide(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var otherLower = reachableOther.lowerLimit;
        var otherUpper = reachableOther.upperLimit;

        if (otherLower.equals(InfInt.of(0)) && otherUpper.equals(InfInt.of(0))) {
          throw new ArithmeticException("Cannot divide by zero interval.");
        }
        otherLower = otherLower.equals(InfInt.of(0)) ? InfInt.of(1) : otherLower;
        otherUpper = otherUpper.equals(InfInt.of(0)) ? InfInt.of(-1) : otherUpper;

        var lower = InfInt.min(
            this.lowerLimit.divide(otherLower),
            this.lowerLimit.divide(otherUpper),
            this.upperLimit.divide(otherLower),
            this.upperLimit.divide(otherUpper)
        );

        var upper = InfInt.max(
            this.lowerLimit.divide(otherLower),
            this.lowerLimit.divide(otherUpper),
            this.upperLimit.divide(otherLower),
            this.upperLimit.divide(otherUpper)
        );
        return new ReachableInterval(lower, upper);
      }
      return new Unreachable();
    }

    @Override
    public Interval divideByConstant(long constant) {
      if (constant == 0) {
        throw new ArithmeticException("Cannot divide by zero.");
      }
      return new ReachableInterval(this.lowerLimit.divide(constant), this.upperLimit.divide(constant));
    }


    /**
     * To simplify implementation, modulo of intervals is implemented
     */
    @Override
    public Interval modulo(Interval other) {
      if (other instanceof ReachableInterval reachableOther) {
        var divisor = reachableOther.absoluteValue();
        divisor = new ReachableInterval(divisor.lowerLimit, divisor.upperLimit.subtract(1));

        if (this.lowerLimit.isGreaterEqualThan(InfInt.of(0))) {
          return new ReachableInterval(InfInt.of(0), InfInt.min(this.upperLimit, divisor.upperLimit));
        }

        if (this.upperLimit.isLessThan(InfInt.of(0))) {
          return new ReachableInterval(InfInt.max(this.lowerLimit, divisor.negate().lowerLimit), InfInt.of(0));
        }

        return new ReachableInterval(InfInt.max(this.lowerLimit, divisor.negate().lowerLimit), InfInt.min(this.upperLimit, divisor.upperLimit));
      }
      return unreachable();
    }

    @Override
    public Interval modulo(long constant) {
      return new ReachableInterval(this.lowerLimit.modulo(constant), this.upperLimit.modulo(constant));
    }

    @Override
    public ReachableInterval absoluteValue() {
      if (this.lowerLimit.isLessEqualThan(InfInt.of(0)) && this.upperLimit.isGreaterEqualThan(InfInt.of(0))) {
        return new ReachableInterval(InfInt.of(0), InfInt.max(this.lowerLimit.negate(), this.upperLimit));
      }
      if (this.lowerLimit.isLessEqualThan(InfInt.of(0)) && this.upperLimit.isLessEqualThan(InfInt.of(0))) {
        return new ReachableInterval(this.upperLimit.negate(), this.lowerLimit.negate());
      }
      return this;
    }

    @Override
    public boolean isGreaterThan(Interval other) {
      Objects.requireNonNull(other);
      if (other instanceof ReachableInterval reachableOther) {
        return this.lowerLimit.isGreaterThan(reachableOther.upperLimit);
      }
      return false;
    }

    @Override
    public boolean mayBeGreaterThan(Interval other) {
      Objects.requireNonNull(other);
      if (other instanceof ReachableInterval reachableOther) {
        return this.upperLimit.isGreaterThan(reachableOther.lowerLimit);
      }
      return false;
    }

    @Override
    public boolean isGreaterEqualThan(Interval other) {
      Objects.requireNonNull(other);
      if (other instanceof ReachableInterval reachableOther) {
        return this.lowerLimit.isGreaterEqualThan(reachableOther.upperLimit);
      }
      return false;
    }

    @Override
    public boolean mayBeGreaterEqualThan(Interval other) {
      Objects.requireNonNull(other);
      if (other instanceof ReachableInterval reachableOther) {
        return this.upperLimit.isGreaterEqualThan(reachableOther.lowerLimit);
      }
      return false;
    }

    @Override
    public boolean isEqual(Interval other) {
      Objects.requireNonNull(other);
      if (other instanceof ReachableInterval reachableOther) {
        return this.upperLimit.equals(reachableOther.upperLimit)
            && this.lowerLimit.equals(reachableOther.lowerLimit)
            && this.lowerLimit.equals(this.upperLimit);
      }
      return false;
    }

    @Override
    public boolean isReachable() {
      return true;
    }
  }

  final static class Unreachable extends Interval {

    @Override
    public Interval join(Interval other) {
      return other;
    }

    @Override
    public Interval meet(Interval other) {
      return this;
    }

    @Override
    public Interval widen(Interval other) {
      return other;
    }

    @Override
    public String toString() {
      return "⊥";
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Unreachable;
    }

    @Override
    public Interval add(Interval value) {
      return this;
    }

    @Override
    public Interval negate() {
      return this;
    }

    @Override
    boolean isGreaterThan(Interval other) {
      return false;
    }

    @Override
    boolean isGreaterEqualThan(Interval other) {
      return false;
    }

    @Override
    boolean isEqual(Interval other) {
      return false;
    }

    @Override
    boolean mayBeGreaterThan(Interval other) {
      return false;
    }

    @Override
    boolean mayBeGreaterEqualThan(Interval other) {
      return false;
    }

    @Override
    public Interval subtract(Interval value) {
      return this;
    }

    @Override
    public Interval addConstant(long constant) {
      return this;
    }

    @Override
    public Interval subtractConstant(long constant) {
      return this;
    }

    @Override
    public Interval multiply(Interval other) {
      return this;
    }

    @Override
    public Interval multiplyByConstant(long constant) {
      return this;
    }

    @Override
    public Interval divide(Interval other) {
      return this;
    }

    @Override
    public Interval divideByConstant(long constant) {
      return this;
    }

    @Override
    public Interval modulo(Interval other) {
      return this;
    }

    @Override
    public Interval modulo(long constant) {
      return this;
    }

    @Override
    public Interval absoluteValue() {
      return this;
    }

    @Override
    public boolean isReachable() {
      return false;
    }
  }
}

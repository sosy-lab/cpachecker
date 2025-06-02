package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Objects;

/**
 * A utility class for integers with infinities.
 */
public abstract sealed class InfInt implements Comparable<InfInt>
        permits InfInt.Infinity, InfInt.FiniteInteger {

  public static InfInt of(long value) {
    return new FiniteInteger(value);
  }

  public static InfInt posInf() {
    return new PositiveInfinity();
  }

  public static InfInt negInf() {
    return new NegativeInfinity();
  }

  /**
   * A utility function returning the minimum of multiple integers with infinity.
   *
   * @param ints the integers.
   * @return the maximum of both integers.
   */
  public static InfInt min(InfInt... ints) {
    if (ints.length == 0) {
      throw new IllegalArgumentException("Cannot find minimum for no InfInts provided.");
    }
    var min = ints[0];
    for (int i = 1; i < ints.length; i++) {
      if (ints[i].compareTo(min) <= 0) {
        min = ints[i];
      }
    }
    return min;
  }

  /**
   * A utility function returning the maximum of multiple integers with infinity.
   *
   * @param ints the integers.
   * @return the maximum of both integers.
   */
  public static InfInt max(InfInt... ints) {
    if (ints.length == 0) {
      throw new IllegalArgumentException("Cannot find maximum for no InfInts provided.");
    }
    var max = ints[0];
    for (int i = 1; i < ints.length; i++) {
      if (ints[i].compareTo(max) >= 0) {
        max = ints[i];
      }
    }
    return max;
  }

  public boolean isLessThan(InfInt other) {
    return this.compareTo(other) < 0;
  }

  public boolean isLessEqualThan(InfInt other) {
    return this.compareTo(other) <= 0;
  }

  public boolean isGreaterThan(InfInt other) {
    return this.compareTo(other) > 0;
  }

  public boolean isGreaterEqualThan(InfInt other) {
    return this.compareTo(other) >= 0;
  }

  public boolean isPosInf() {
    return this instanceof PositiveInfinity;
  }

  public boolean isNegInf() {
    return this instanceof NegativeInfinity;
  }

  public abstract InfInt add(InfInt value);

  public InfInt add(int value) {
    return this.add(InfInt.of(value));
  }

  public InfInt subtract(InfInt value) {
    return add(value.negate());
  }

  public InfInt subtract(long value) {
    return subtract(InfInt.of(value));
  }

  public abstract InfInt multiply(InfInt value);

  public InfInt multiply(long value) {
    return this.multiply(InfInt.of(value));
  }

  public abstract InfInt divide(InfInt value);

  public InfInt divide(long value) {
    return this.divide(InfInt.of(value));
  }

  /*
   * Modulo with negative operands is defined as in the C language:
   * The sign of the divisor is ignored. The operation is conducted as if the dividend was positive
   * and its sign is then applied to the result. This differs from the java implementation where if
   * either operand is negative, the result will also be negative.
   */
  public abstract InfInt modulo(InfInt value);

  public InfInt modulo(long value) {
    return this.modulo(InfInt.of(value));
  }

  public abstract InfInt negate();

  public static abstract sealed class Infinity extends InfInt permits NegativeInfinity, PositiveInfinity {
    @Override
    public InfInt add(InfInt value) {
      return this;
    }

    @Override
    public InfInt modulo(InfInt value) {
      throw new ArithmeticException("Cannot mod infinity");
    }
  }

  /**
   * Negative infinity value in {@link InfInt}.
   */
  public static final class NegativeInfinity extends Infinity {
    @Override
    public String toString() {
      return "-∞";
    }

    @Override
    public InfInt multiply(InfInt value) {
      if (value.isNegInf()) {
        return InfInt.posInf();
      }
      return this;
    }

    @Override
    public InfInt divide(InfInt value) {
      if (value.isGreaterThan(InfInt.of(0))) {
        return InfInt.negInf();
      } else if (value.equals(InfInt.of(0))) {
        throw new ArithmeticException("Cannot divide by zero");
      } else {
        return InfInt.posInf();
      }
    }

    @Override
    public InfInt negate() {
      return new PositiveInfinity();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof NegativeInfinity;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed")
    @Override
    public int compareTo(InfInt other) {
      if (other instanceof NegativeInfinity) {
        return 0;
      }
      return -1;
    }
  }

  /**
   * Positive infinity value in {@link InfInt}.
   */
  public static final class PositiveInfinity extends Infinity {
    @Override
    public String toString() {
      return "∞";
    }

    @Override
    public InfInt multiply(InfInt value) {
      if (value.isNegInf()) {
        return InfInt.negInf();
      }
      return this;
    }

    @Override
    public InfInt divide(InfInt value) {
      if (value.isGreaterThan(InfInt.of(0))) {
        return InfInt.posInf();
      } else if (value.equals(InfInt.of(0))) {
        throw new ArithmeticException("Cannot divide by zero");
      } else {
        return InfInt.negInf();
      }
    }

    @Override
    public InfInt negate() {
      return new NegativeInfinity();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof PositiveInfinity;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed")
    @Override
    public int compareTo(InfInt other) {
      if (other instanceof PositiveInfinity) {
        return 0;
      }
      return 1;
    }
  }

  /**
   * A finite integer.
   */

  public static final class FiniteInteger extends InfInt {

    private final long value;

    public FiniteInteger(long pValue) {
      this.value = pValue;
    }

    Long getValue() {
      return value;
    }

    @Override
    public int compareTo(InfInt other) {
      Objects.requireNonNull(other);
      if (other instanceof NegativeInfinity) {
        return 1;
      } else if (other instanceof PositiveInfinity) {
        return -1;
      } else if (other instanceof FiniteInteger f) {
        return Long.compare(value, f.getValue());
      }
      throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
      return Long.toString(value);
    }

    @Override
    public InfInt add(InfInt pValue) {
      Objects.requireNonNull(pValue);
      if (pValue instanceof FiniteInteger f) {
        return new FiniteInteger(this.value + f.value);
      } else if (pValue instanceof Infinity i) {
        return i;
      }
      throw new IllegalArgumentException();
    }

    @Override
    public InfInt multiply(InfInt pValue) {
      Objects.requireNonNull(pValue);
      if (pValue instanceof FiniteInteger f) {
        return new FiniteInteger(this.value * f.value);
      } else if (pValue instanceof Infinity i) {
        return i.multiply(this);
      }
      throw new IllegalArgumentException();
    }

    @Override
    public InfInt divide(InfInt pValue) {
      Objects.requireNonNull(pValue);
      if (pValue instanceof FiniteInteger f) {
        return new FiniteInteger(this.value / f.value);
      } else if (pValue instanceof Infinity) {
        return InfInt.of(0);
      }
      throw new IllegalArgumentException();
    }

    /*
     * Modulo with negative operands is defined as in the C language:
     * The sign of the divisor is ignored. The operation is conducted as if the dividend was positive
     * and its sign is then applied to the result. This differs from the java implementation where if
     * either operand is negative, the result will also be negative.
     */
    @Override
    public InfInt modulo(InfInt pValue) {
      Objects.requireNonNull(pValue);
      if (pValue instanceof FiniteInteger f) {
        boolean dividendIsPositive = this.value >= 0;
        var dividend = dividendIsPositive ? this.value : -this.value;
        var divisor = f.value >= 0 ? f.value : -f.value;
        var mod = dividend % divisor;
        mod = dividendIsPositive ? mod : -mod;
        return new FiniteInteger(mod);
      } else if (pValue instanceof Infinity i) {
        return i.modulo(this);
      }
      throw new IllegalArgumentException();
    }

    @Override
    public InfInt negate() {
      return new FiniteInteger(-value);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof FiniteInteger f && f.value == this.value;
    }
  }
}


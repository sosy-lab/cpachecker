package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class IntegerInterval implements NumberInterface {
    /**
     * the lower bound of the interval
     */
    private final Long low;

    /**
     * the upper bound of the interval
     */
    private final Long high;

    public static final IntegerInterval EMPTY = new IntegerInterval(null, null);
    public static final IntegerInterval UNBOUND = new IntegerInterval(Long.MIN_VALUE, Long.MAX_VALUE);
    public static final IntegerInterval BOOLEAN_INTERVAL = new IntegerInterval(0L, 1L);
    public static final IntegerInterval ZERO = new IntegerInterval(0L, 0L);
    public static final IntegerInterval ONE = new IntegerInterval(1L, 1L);

    /**
     * This method acts as constructor for a single-value interval.
     *
     * @param value
     *            for the lower and upper bound
     */
    public IntegerInterval(Long value) {
        this.low = value;

        this.high = value;

        isSane();
    }

    /**
     * This method acts as constructor for a long-based interval.
     *
     * @param low
     *            the lower bound
     * @param high
     *            the upper bound
     */
    public IntegerInterval(Long low, Long high) {
        this.low = low;

        this.high = high;

        isSane();
    }

    @Override
    public NumberInterface EMPTY() {
        return EMPTY;
    }

    @Override
    public NumberInterface UNBOUND() {
        return UNBOUND;
    }

    @Override
    public NumberInterface BOOLEAN_INTERVAL() {
        return BOOLEAN_INTERVAL;
    }

    @Override
    public NumberInterface ZERO() {
        return ZERO;
    }

    @Override
    public NumberInterface ONE() {
        return ONE;
    }

    private boolean isSane() {
        if ((low == null) != (high == null)) {
            throw new IllegalStateException("invalid empty interval");
        }
        if (low != null && low > high) {
            throw new IllegalStateException("low cannot be larger than high");
        }

        return true;
    }

    /**
     * This method returns the lower bound of the interval.
     *
     * @return the lower bound
     */
    @Override
    public Long getLow() {
        return low;
    }

    /**
     * This method returns the upper bound of the interval.
     *
     * @return the upper bound
     */
    @Override
    public Long getHigh() {
        return high;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other != null && getClass().equals(other.getClass())) {
            IntegerInterval another = (IntegerInterval) other;
            return Objects.equals(low, another.getLow()) && Objects.equals(high, another.getHigh());
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }

    /**
     * This method creates a new interval instance representing the union of
     * this interval with another interval.
     *
     * The lower bound and upper bound of the new interval is the minimum of
     * both lower bounds and the maximum of both upper bounds, respectively.
     *
     * @param other
     *            the other interval
     * @return the new interval with the respective bounds
     */
    @Override
    public NumberInterface union(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        if (isEmpty() || tempOther.isEmpty()) {
            return EMPTY;
        } else if (low <= tempOther.getLow() && high >= tempOther.getHigh()) {
            return this;
        } else if (low >= tempOther.getLow() && high <= tempOther.getHigh()) {
            return other;
        } else {
            return new IntegerInterval(Math.min(low, tempOther.getLow()), Math.max(high, tempOther.getHigh()));
        }
    }

    /**
     * This method creates a new interval instance representing the intersection
     * of this interval with another interval.
     *
     * The lower bound and upper bound of the new interval is the maximum of
     * both lower bounds and the minimum of both upper bounds, respectively.
     *
     * @param other
     *            the other interval
     * @return the new interval with the respective bounds
     */
    @Override
    public NumberInterface intersect(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        if (this.intersects(other)) {
            return new IntegerInterval(Math.max(low, tempOther.getLow()), Math.min(high, tempOther.getHigh()));
        } else {
            return EMPTY;
        }
    }

    /**
     * This method determines if this interval is definitely greater than the
     * other interval.
     *
     * @param other
     *            interval to compare with
     * @return true if the lower bound of this interval is always strictly
     *         greater than the upper bound of the other interval, else false
     */
    @Override
    public boolean isGreaterThan(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        return !isEmpty() && !tempOther.isEmpty() && low > tempOther.getHigh();
    }

    /**
     * This method determines if this interval is definitely greater or equal
     * than the other interval. The equality is only satisfied for one single
     * value!
     *
     * @param other
     *            interval to compare with
     * @return true if the lower bound of this interval is always strictly
     *         greater or equal than the upper bound of the other interval, else
     *         false
     */
    @Override
    public boolean isGreaterOrEqualThan(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        return !isEmpty() && !tempOther.isEmpty() && low >= tempOther.getHigh();
    }

    /**
     * This method determines if this interval maybe greater than the other
     * interval.
     *
     * @param other
     *            interval to compare with
     * @return true if the upper bound of this interval is strictly greater than
     *         the lower bound of the other interval, else false
     */
    public boolean mayBeGreaterThan(IntegerInterval other) {
        return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high > other.getLow());
    }

    /**
     * This method determines if this interval maybe greater or equal than the
     * other interval.
     *
     * @param other
     *            interval to compare with
     * @return true if the upper bound of this interval is strictly greater than
     *         the lower bound of the other interval, else false
     */
    public boolean mayBeGreaterOrEqualThan(IntegerInterval other) {
        return other.isEmpty() || (!isEmpty() && !other.isEmpty() && high >= other.getLow());
    }

    /**
     * New interval instance after the modulo computation.
     *
     * @param other
     *            the other interval
     * @return the new interval with the respective bounds.
     */
    @Override
    public NumberInterface modulo(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        if (tempOther.contains(ZERO)) {
            return IntegerInterval.UNBOUND;
        }

        // The interval doesn't contain zero, hence low and high has to be of
        // the same sign.
        // In that case we can call an absolute value on both, as "% (-x)" is
        // the same as "% x".
        other = new IntegerInterval(Math.abs(tempOther.getLow()), Math.abs(tempOther.getHigh()));
        tempOther = (IntegerInterval) other;

        long newHigh;
        long newLow;

        // New high of the interval can't be higher than the highest value in
        // the divisor.
        // If the divisible element is positive, it is also bounded by it's
        // highest number,
        // or by the absolute value of the lowest number.
        // (-1 % 6 CAN be either -1 or 5 according to the C standard).
        long top;
        if (low >= 0) {
            top = high;
        } else {
            if (low == Long.MIN_VALUE) {
                top = Long.MAX_VALUE;
            } else {
                top = Math.max(Math.abs(low), high);
            }
        }
        newHigh = Math.min(top, tempOther.getHigh() - 1);

        // Separate consideration for the case where the divisible number can be
        // negative.
        if (low >= 0) { // If the divisible interval is all positive, the lowest
                        // we can ever get is 0.

            // We can only get zero if we include 0 or the number higher than
            // the smallest value of the other interval.
            if (low == 0 || high >= tempOther.getLow()) {
                newLow = 0;
            } else {
                newLow = low;
            }
        } else {
            // The remainder can go negative, but it can not be more negative
            // than the negation of the highest value
            // of the other interval plus 1.
            // (e.g. X mod 14 can not be lower than -13)

            // Remember, <low> is negative in this branch.
            newLow = Math.max(low, 1 - tempOther.getHigh());
        }

        IntegerInterval out = new IntegerInterval(newLow, newHigh);
        return out;
    }

    /**
     * This method returns a new interval with a limited, i.e. higher, lower
     * bound.
     *
     * @param other
     *            the interval to limit this interval
     * @return the new interval with the upper bound of this interval and the
     *         lower bound set to the maximum of this interval's and the other
     *         interval's lower bound or an empty interval if this interval is
     *         less than the other interval.
     */
    @Override
    public NumberInterface limitLowerBoundBy(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        IntegerInterval interval = null;

        if (isEmpty() || tempOther.isEmpty() || high < tempOther.getLow()) {
            interval = EMPTY;
        } else {
            interval = new IntegerInterval(Math.max(low, tempOther.getLow()), high);
        }

        return interval;
    }

    /**
     * This method returns a new interval with a limited, i.e. lower, upper
     * bound.
     *
     * @param other
     *            the interval to limit this interval
     * @return the new interval with the lower bound of this interval and the
     *         upper bound set to the minimum of this interval's and the other
     *         interval's upper bound or an empty interval if this interval is
     *         greater than the other interval.
     */
    @Override
    public NumberInterface limitUpperBoundBy(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        IntegerInterval interval = null;
        if (isEmpty() || tempOther.isEmpty() || low > tempOther.getHigh()) {
            interval = EMPTY;
        } else {
            interval = new IntegerInterval(low, Math.min(high, tempOther.getHigh()));
        }

        return interval;
    }

    /**
     * This method determines if this interval intersects with another interval.
     *
     * @param other
     *            the other interval
     * @return true if the intervals intersect, else false
     */
    @Override
    public boolean intersects(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        if (isEmpty() || tempOther.isEmpty()) {
            return false;
        }

        return (low >= tempOther.getLow() && low <= tempOther.getHigh())
                || (high >= tempOther.getLow() && high <= tempOther.getHigh())
                || (low <= tempOther.getLow() && high >= tempOther.getHigh());
    }

    /**
     * This method determines if this interval contains another interval.
     *
     * The method still returns true, if the borders match. An empty interval
     * does not contain any interval and is not contained in any interval
     * either. So if the callee or parameter is an empty interval, this method
     * will return false.
     *
     * @param other
     *            the other interval
     * @return true if this interval contains the other interval, else false
     */
    @Override
    public boolean contains(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        return (!isEmpty() && !tempOther.isEmpty() && low <= tempOther.getLow() && tempOther.getHigh() <= high);
    }

    /**
     * This method adds an interval from this interval, overflow is handled by
     * setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
     *
     * @param interval
     *            the interval to add
     * @return a new interval with the respective bounds
     */
    @Override
    public NumberInterface plus(NumberInterface interval) {
        IntegerInterval tempInterval = (IntegerInterval) interval.asInteger();
        if (isEmpty() || tempInterval.isEmpty()) {
            return EMPTY;
        }

        return new IntegerInterval(scalarPlus(low, tempInterval.getLow()), scalarPlus(high, tempInterval.getHigh()));
    }

    /**
     * This method adds a constant offset to this interval, overflow is handled
     * by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
     *
     * @param offset
     *            the constant offset to add
     * @return a new interval with the respective bounds
     */
    public NumberInterface plus(Long offset) {
        return plus(new IntegerInterval(offset, offset));
    }

    /**
     * This method subtracts an interval from this interval, overflow is handled
     * by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
     *
     * @param other
     *            interval to subtract
     * @return a new interval with the respective bounds
     */
    @Override
    public NumberInterface minus(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        return plus(tempOther.negate());
    }

    /**
     * This method subtracts a constant offset to this interval, overflow is
     * handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE
     * respectively.
     *
     * @param offset
     *            the constant offset to subtract
     * @return a new interval with the respective bounds
     */
    public NumberInterface minus(Long offset) {
        return plus(-offset);
    }

    /**
     * This method multiplies this interval with another interval. In case of an
     * overflow Long.MAX_VALUE and Long.MIN_VALUE are used instead.
     *
     * @param other
     *            interval to multiply this interval with
     * @return new interval that represents the result of the multiplication of
     *         the two intervals
     */
    @Override
    public NumberInterface times(NumberInterface other) {
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        Long[] values = { scalarTimes(low, tempOther.getLow()), scalarTimes(low, tempOther.getHigh()),
                scalarTimes(high, tempOther.getLow()), scalarTimes(high, tempOther.getHigh()) };

        return new IntegerInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
    }

    /**
     * This method divides this interval by another interval. If the other
     * interval contains "0" an unbound interval is returned.
     *
     * @param other
     *            interval to divide this interval by
     * @return new interval that represents the result of the division of the
     *         two intervals
     */
    @Override
    public NumberInterface divide(NumberInterface other) {
        // other interval contains "0", return unbound interval
        IntegerInterval tempOther = (IntegerInterval) other.asInteger();
        if (other.contains(ZERO)) {
            return UNBOUND;
        } else {
            Long[] values = { low / tempOther.getLow(), low / tempOther.getHigh(), high / tempOther.getLow(),
                    high / tempOther.getHigh() };

            return new IntegerInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
        }
    }

    /**
     * This method performs an arithmetical left shift of the interval bounds.
     *
     * @param offset
     *            Interval offset to perform an arithmetical left shift on the
     *            interval bounds. If the offset maybe less than zero an unbound
     *            interval is returned.
     * @return new interval that represents the result of the arithmetical left
     *         shift
     */
    @Override
    public NumberInterface shiftLeft(NumberInterface offset) {
        // create an unbound interval upon trying to shift by a possibly
        // negative offset
        IntegerInterval tempOffset = (IntegerInterval) offset.asInteger();
        if (ZERO.mayBeGreaterThan(tempOffset)) {
            return UNBOUND;
        } else {
            // if lower bound is negative, shift it by upper bound of offset,
            // else by lower bound of offset
            Long newLow = low << ((low < 0L) ? tempOffset.getHigh() : tempOffset.getLow());

            // if upper bound is negative, shift it by lower bound of offset,
            // else by upper bound of offset
            Long newHigh = high << ((high < 0L) ? tempOffset.getLow() : tempOffset.getHigh());

            if ((low < 0 && newLow > low) || (high > 0 && newHigh < high)) {
                return UNBOUND;
            } else {
                return new IntegerInterval(newLow, newHigh);
            }
        }
    }

    /**
     * This method performs an arithmetical right shift of the interval bounds.
     * If the offset maybe less than zero an unbound interval is returned.
     *
     * @param offset
     *            Interval offset to perform an arithmetical right shift on the
     *            interval bounds
     * @return new interval that represents the result of the arithmetical right
     *         shift
     */
    @Override
    public NumberInterface shiftRight(NumberInterface offset) {
        // create an unbound interval upon trying to shift by a possibly
        // negative offset
        IntegerInterval tempOffset = (IntegerInterval) offset.asInteger();
        if (ZERO.mayBeGreaterThan(tempOffset)) {
            return UNBOUND;
        } else {
            // if lower bound is negative, shift it by lower bound of offset,
            // else by upper bound of offset
            Long newLow = low >> ((low < 0L) ? tempOffset.getLow() : tempOffset.getHigh());

            // if upper bound is negative, shift it by upper bound of offset,
            // else by lower bound of offset
            Long newHigh = high >> ((high < 0L) ? tempOffset.getHigh() : tempOffset.getLow());

            return new IntegerInterval(newLow, newHigh);
        }
    }

    /**
     * This method negates this interval.
     *
     * @return new negated interval
     */
    @Override
    public NumberInterface negate() {
        IntegerInterval i = new IntegerInterval(scalarTimes(high, -1L), scalarTimes(low, -1L));
        return i;
    }

    /**
     * This method determines whether the interval is empty or not.
     *
     * @return true, if the interval is empty, i.e. the lower and upper bounds
     *         are null
     */
    @Override
    public boolean isEmpty() {
        return low == null && high == null;
    }

    @Override
    public boolean isUnbound() {
        return !isEmpty() && low == Long.MIN_VALUE && high == Long.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "[" + (low == null ? "" : low) + "; " + (high == null ? "" : high) + "]";
    }

    /**
     * This method is a factory method for a lower bounded interval.
     *
     * @param lowerBound
     *            the lower bound to set
     * @return a lower bounded interval, i.e. the lower bound is set to the
     *         given lower bound, the upper bound is set to Long.MAX_VALUE
     */

    public static NumberInterface createLowerBoundedInterval(Long lowerBound) {
        return new IntegerInterval(lowerBound, Long.MAX_VALUE);
    }

    /**
     * This method is a factory method for an upper bounded interval.
     *
     * @param upperBound
     *            the upper bound to set
     * @return an upper bounded interval, i.e. the lower bound is set to
     *         Long.MIN_VALUE, the upper bound is set to the given upper bound
     */

    public static NumberInterface createUpperBoundedInterval(Long upperBound) {
        return new IntegerInterval(Long.MIN_VALUE, upperBound);
    }

    /**
     * This method adds two scalar values and returns their sum, or on overflow
     * Long.MAX_VALUE or Long.MIN_VALUE, respectively.
     *
     * @param x
     *            the first scalar operand
     * @param y
     *            the second scalar operand
     * @return the sum of the first and second scalar operand or on overflow
     *         Long.MAX_VALUE and Long.MIN_VALUE, respectively.
     */
    private static Long scalarPlus(Long x, Long y) {
        Long result = x + y;

        // both operands are positive but the result is negative
        if ((Long.signum(x) + Long.signum(y) == 2) && Long.signum(result) == -1) {
            result = Long.MAX_VALUE;
        } else if ((Long.signum(x) + Long.signum(y) == -2) && Long.signum(result) == +1) {
            result = Long.MIN_VALUE;
        }

        return result;
    }

    /**
     * This method multiplies two scalar values and returns their product, or on
     * overflow Long.MAX_VALUE or Long.MIN_VALUE, respectively.
     *
     * @param x
     *            the first scalar operand
     * @param y
     *            the second scalar operand
     * @return the product of the first and second scalar operand or on overflow
     *         Long.MAX_VALUE and Long.MIN_VALUE, respectively.
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

    @Override
    public NumberInterface asDecimal() {
        return new DoubleInterval(this.low.doubleValue(), this.high.doubleValue());
    }

    @Override
    public NumberInterface asInteger() {
        return this;
    }

    @Override
    public Long asLong(CType pType) {
        throw new AssertionError("trying to perform default function");
    }

    @Override
    public NumberInterface binaryAnd(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");
    }

    @Override
    public NumberInterface evaluateLessOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");
    }

    @Override
    public NumberInterface evaluateLessEqualOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");
    }

    @Override
    public NumberInterface evaluateEqualOperator(NumberInterface pRight) {
        throw new AssertionError("trying to perform default function");
    }


}

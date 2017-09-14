
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class DoubleInterval implements NumberInterface {
    /**
     * the lower bound of the interval
     */
    private final Double low;

    /**
     * the upper bound of the interval
     */
    private final Double high;

    public static final DoubleInterval EMPTY = new DoubleInterval(null, null);
    public static final DoubleInterval UNBOUND = new DoubleInterval(-Double.MAX_VALUE, Double.MAX_VALUE);
    public static final IntegerInterval BOOLEAN_INTERVAL = new IntegerInterval(0L, 1L);
    public static final DoubleInterval ZERO = new DoubleInterval(0.0, 0.0);
    public static final DoubleInterval ONE = new DoubleInterval(1.0, 1.0);

    /**
     * This method acts as constructor for a single-value interval.
     *
     * @param value
     *            for the lower and upper bound
     */
    public DoubleInterval(Double value) {
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
    public DoubleInterval(Double low, Double high) {
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
    public Double getLow() {
        return low;
    }

    /**
     * This method returns the upper bound of the interval.
     *
     * @return the upper bound
     */
    @Override
    public Double getHigh() {
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
            DoubleInterval another = (DoubleInterval) other;
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
        DoubleInterval tempOther = (DoubleInterval) other;
        if (isEmpty() || tempOther.isEmpty()) {
            return EMPTY;
        } else if (low <= tempOther.getLow() && high >= tempOther.getHigh()) {
            return this;
        } else if (low >= tempOther.getLow() && high <= tempOther.getHigh()) {
            return other;
        } else {
            return new DoubleInterval(Math.min(low, tempOther.getLow()), Math.max(high, tempOther.getHigh()));
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
        DoubleInterval tempOther = (DoubleInterval) other;
        if (this.intersects(other)) {
            return new DoubleInterval(Math.max(low, tempOther.getLow()), Math.min(high, tempOther.getHigh()));
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
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
    public boolean mayBeGreaterThan(DoubleInterval other) {
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
    public boolean mayBeGreaterOrEqualThan(DoubleInterval other) {
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        if (tempOther.contains(ZERO)) {
            return DoubleInterval.UNBOUND;
        }

        // The interval doesn't contain zero, hence low and high has to be of
        // the same sign.
        // In that case we can call an absolute value on both, as "% (-x)" is
        // the same as "% x".
        other = new DoubleInterval(Math.abs(tempOther.getLow()), Math.abs(tempOther.getHigh()));
        tempOther = (DoubleInterval) other;

        double newHigh;
        double newLow;

        // New high of the interval can't be higher than the highest value in
        // the divisor.
        // If the divisible element is positive, it is also bounded by it's
        // highest number,
        // or by the absolute value of the lowest number.
        // (-1 % 6 CAN be either -1 or 5 according to the C standard).
        double top;
        if (low >= 0) {
            top = high;
        } else {
            if (low == -Double.MAX_VALUE) {
                top = Double.MAX_VALUE;
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

        DoubleInterval out = new DoubleInterval(newLow, newHigh);
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        DoubleInterval interval = null;

        if (isEmpty() || tempOther.isEmpty() || high < tempOther.getLow()) {
            interval = EMPTY;
        } else {
            interval = new DoubleInterval(Math.max(low, tempOther.getLow()), high);
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        DoubleInterval interval = null;
        if (isEmpty() || tempOther.isEmpty() || low > tempOther.getHigh()) {
            interval = EMPTY;
        } else {
            interval = new DoubleInterval(low, Math.min(high, tempOther.getHigh()));
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        return (!isEmpty() && !tempOther.isEmpty() && low <= tempOther.getLow() && tempOther.getHigh() <= high);
    }

    /**
     * This method adds an interval from this interval, overflow is handled by
     * setting the bound to -Double.MAX_VALUE or Double.MAX_VALUE respectively.
     *
     * @param interval
     *            the interval to add
     * @return a new interval with the respective bounds
     */
    @Override
    public NumberInterface plus(NumberInterface interval) {
        DoubleInterval tempInterval = (DoubleInterval) interval.asDecimal();
        if (isEmpty() || tempInterval.isEmpty()) {
            return EMPTY;
        }

        return new DoubleInterval(scalarPlus(low, tempInterval.getLow()), scalarPlus(high, tempInterval.getHigh()));
    }

    /**
     * This method adds a constant offset to this interval, overflow is handled
     * by setting the bound to -Double.MAX_VALUE or Double.MAX_VALUE
     * respectively.
     *
     * @param offset
     *            the constant offset to add
     * @return a new interval with the respective bounds
     */
    public NumberInterface plus(Double offset) {
        return plus(new DoubleInterval(offset, offset));
    }

    /**
     * This method subtracts an interval from this interval, overflow is handled
     * by setting the bound to -Double.MAX_VALUE or Double.MAX_VALUE
     * respectively.
     *
     * @param other
     *            interval to subtract
     * @return a new interval with the respective bounds
     */
    @Override
    public NumberInterface minus(NumberInterface other) {
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        return plus(tempOther.negate());
    }

    /**
     * This method subtracts a constant offset to this interval, overflow is
     * handled by setting the bound to -Double.MAX_VALUE or Double.MAX_VALUE
     * respectively.
     *
     * @param offset
     *            the constant offset to subtract
     * @return a new interval with the respective bounds
     */
    public NumberInterface minus(Double offset) {
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        Double[] values = { scalarTimes(low, tempOther.getLow()), scalarTimes(low, tempOther.getHigh()),
                scalarTimes(high, tempOther.getLow()), scalarTimes(high, tempOther.getHigh()) };

        return new DoubleInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
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
        DoubleInterval tempOther = (DoubleInterval) other.asDecimal();
        if (other.contains(ZERO)) {
            return UNBOUND;
        } else {
            Double[] values = { low / tempOther.getLow(), low / tempOther.getHigh(), high / tempOther.getLow(),
                    high / tempOther.getHigh() };

            return new DoubleInterval(Collections.min(Arrays.asList(values)), Collections.max(Arrays.asList(values)));
        }
    }

    /**
     * This method should not be used for double!!!
     *
     *
     * @param offset
     *            Interval offset to perform an arithmetical left shift on the
     *            interval bounds. If the offset maybe less than zero an unbound
     *            interval is returned.
     * @return assertion error
     */
    @Override
    public NumberInterface shiftLeft(NumberInterface offset) {
        throw new AssertionError("trying to perform ShiftLeft on floating point operands");
    }

    /**
     * This method should not be used for double!!!
     *
     * @param offset
     *            Interval offset to perform an arithmetical right shift on the
     *            interval bounds
     * @return assertion error
     */
    @Override
    public NumberInterface shiftRight(NumberInterface offset) {
        throw new AssertionError("trying to perform ShiftRight on floating point operands");
    }

    /**
     * This method negates this interval.
     *
     * @return new negated interval
     */
    @Override
    public NumberInterface negate() {
        return new DoubleInterval(scalarTimes(high, -1.0), scalarTimes(low, -1.0));
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
        return !isEmpty() && low == -Double.MIN_VALUE && high == Double.MAX_VALUE;
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
     *         given lower bound, the upper bound is set to Double.MAX_VALUE
     */

    public static NumberInterface createLowerBoundedInterval(Double lowerBound) {
        return new DoubleInterval(lowerBound, Double.MAX_VALUE);
    }

    /**
     * This method is a factory method for an upper bounded interval.
     *
     * @param upperBound
     *            the upper bound to set
     * @return an upper bounded interval, i.e. the lower bound is set to
     *         -Double.MAX_VALUE, the upper bound is set to the given upper bound
     */

    public static NumberInterface createUpperBoundedInterval(Double upperBound) {
        return new DoubleInterval(-Double.MAX_VALUE, upperBound);
    }

    /**
     * This method adds two scalar values and returns their sum, or on overflow
     * Double.MAX_VALUE or -Double.MAX_VALUE, respectively.
     *
     * @param x
     *            the first scalar operand
     * @param y
     *            the second scalar operand
     * @return the sum of the first and second scalar operand or on overflow
     *         Double.MAX_VALUE and -Double.MAX_VALUE, respectively.
     */
    private static Double scalarPlus(Double x, Double y) {
        Double result = x + y;
        // both operands are positive but the result is negative
        if ((Math.signum(x) + Math.signum(y) == 2) && Math.signum(result) == -1) {
            result = Double.MAX_VALUE;
        } else if ((Math.signum(x) + Math.signum(y) == -2) && Math.signum(result) == +1) {
            result = -Double.MAX_VALUE;
        }

        return result;
    }

    /**
     * This method multiplies two scalar values and returns their product, or on
     * overflow Double.MAX_VALUE or -Double.MAX_VALUE, respectively.
     *
     * @param x
     *            the first scalar operand
     * @param y
     *            the second scalar operand
     * @return the product of the first and second scalar operand or on overflow
     *         Double.MAX_VALUE and -Double.MAX_VALUE, respectively.
     */
    private static Double scalarTimes(Double x, Double y) {
        Double bound = (Math.signum(x) == Math.signum(y)) ? Double.MAX_VALUE : -Double.MAX_VALUE;

        // if overflow occurs, return the respective bound
        if (x != 0.0 && ((y > 0.0 && y > (bound / x)) || (y < 0.0 && y < (bound / x)))) {
            return bound;
        } else {
            return x * y;
        }
    }

    @Override
    public NumberInterface asDecimal() {
        return this;
    }

    @Override
    public NumberInterface asInteger() {
        return new IntegerInterval(this.low.longValue(), this.high.longValue());
    }

    @Override
    public NumberInterface unsignedDivide(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface unsignedModulo(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface unsignedShiftRight(NumberInterface pOther) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long asLong(CType pType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryAnd(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryOr(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface binaryXor(NumberInterface pRNum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean covers(NumberInterface pSign) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSubsetOf(NumberInterface pSign) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NumberInterface evaluateNonCommutativePlusOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateMulOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateNonCommutativeMulOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateDivideOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateModuloOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateAndOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateLessOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateLessEqualOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NumberInterface evaluateEqualOperator(NumberInterface pRight) {
        // TODO Auto-generated method stub
        return null;
    }
}

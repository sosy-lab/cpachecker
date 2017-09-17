package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

public interface NumberInterface {

    default NumberInterface EMPTY() {
        return null;
    }

    default NumberInterface UNBOUND() {
        return null;
    }

    default NumberInterface BOOLEAN_INTERVAL() {
        return null;
    }

    default NumberInterface ZERO() {
        return null;
    }

    default NumberInterface ONE() {
        return null;
    }

    /**
     * This method determines if this interval intersects with another interval.
     *
     * @param other
     *            the other interval
     * @return true if the intervals intersect, else false
     */
    default boolean intersects(NumberInterface other) {
        return false;
    }

    /**
     * This method returns the lower bound of the interval.
     *
     * @return the lower bound
     */
    default Number getLow() {
        return null;
    }

    /**
     * This method returns the upper bound of the interval.
     *
     * @return the upper bound
     */
    default Number getHigh() {
        return null;
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
    default boolean isGreaterThan(NumberInterface other) {
        return false;
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
    default boolean isGreaterOrEqualThan(NumberInterface other) {
        return false;
    }

    public NumberInterface plus(NumberInterface interval);

    public NumberInterface minus(NumberInterface other);

    public NumberInterface times(NumberInterface other);

    public NumberInterface divide(NumberInterface other);

    public NumberInterface shiftLeft(NumberInterface offset);

    public NumberInterface shiftRight(NumberInterface offset);

    public NumberInterface unsignedDivide(NumberInterface other);

    public NumberInterface unsignedModulo(NumberInterface other);

    public NumberInterface unsignedShiftRight(NumberInterface other);

    /**
     * New interval instance after the modulo computation.
     *
     * @param other
     *            the other interval
     * @return the new interval with the respective bounds.
     */
    default NumberInterface modulo(NumberInterface other) {
        return null;
    }

    default boolean isUnbound() {
        return false;
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
    default NumberInterface union(NumberInterface other) {
        return null;
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
    default boolean contains(NumberInterface other) {
        return false;
    }

    default boolean isEmpty() {
        return false;
    }

    default NumberInterface negate() {
        return null;
    }

    // public NumberInterface createUpperBoundedInterval(Long upperBound);
    // public NumberInterface createLowerBoundedInterval(Long lowerBound);
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
    default NumberInterface intersect(NumberInterface other) {
        return null;
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
    default NumberInterface limitUpperBoundBy(NumberInterface other) {
        return null;
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
    default NumberInterface limitLowerBoundBy(NumberInterface other) {
        return null;
    }

    default NumberInterface asDecimal() {
        return null;
    }

    default NumberInterface asInteger() {
        return null;
    }

    default boolean isNumericValue() {
        return false;
    }

    /**
     * True if we have no idea about this value(can not track it), false
     * otherwise.
     */
    default boolean isUnknown() {
        return false;
    }

    /** True if we deterministically know the actual value, false otherwise. */
    default boolean isExplicitlyKnown() {
        return false;
    }

    public NumberInterface binaryAnd(NumberInterface rNum);

    public NumberInterface binaryOr(NumberInterface rNum);

    public NumberInterface binaryXor(NumberInterface rNum);

    /**
     * Returns the NumericValue if the stored value can be explicitly
     * represented by a numeric value, null otherwise.
     **/

    default NumericValue asNumericValue() {
        return null;
    }

    public Long asLong(CType type);

    default Number getNumber() {
        return null;
    }

    public boolean covers(NumberInterface sign);
    public boolean isSubsetOf(NumberInterface sign);
    default ImmutableSet<NumberInterface> split() {
        return null;
    }
    public NumberInterface evaluateNonCommutativePlusOperator(NumberInterface pRight);
    public NumberInterface evaluateMulOperator(NumberInterface pRight);
    public NumberInterface evaluateNonCommutativeMulOperator(NumberInterface right);
    public NumberInterface evaluateDivideOperator(NumberInterface right);
    public NumberInterface evaluateModuloOperator(NumberInterface pRight);
    // assumes that indicator bit for negative numbers is 1
    public NumberInterface evaluateAndOperator(NumberInterface right);
    public NumberInterface evaluateLessOperator(NumberInterface pRight);
    public NumberInterface evaluateLessEqualOperator(NumberInterface pRight);
    public NumberInterface evaluateEqualOperator(NumberInterface pRight);

    public static final class UnknownValue implements NumberInterface, Serializable {

        private static final long serialVersionUID = -300842115868319184L;
        private static final UnknownValue instance = new UnknownValue();

        @Override
        public String toString() {
            return "UNKNOWN";
        }

        public static UnknownValue getInstance() {
            return instance;
        }

        @Override
        public boolean isNumericValue() {
            return false;
        }

        @Override
        public NumericValue asNumericValue() {
            return null;
        }

        @Override
        public Long asLong(CType type) {
            checkNotNull(type);
            return null;
        }

        @Override
        public boolean isUnknown() {
            return true;
        }

        @Override
        public boolean isExplicitlyKnown() {
            return false;
        }

        protected Object readResolve() {
            return instance;
        }

        @Override
        public NumberInterface plus(NumberInterface pInterval) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface minus(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface times(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface divide(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface shiftLeft(NumberInterface pOffset) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface shiftRight(NumberInterface pOffset) {
            // TODO Auto-generated method stub
            return null;
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
}

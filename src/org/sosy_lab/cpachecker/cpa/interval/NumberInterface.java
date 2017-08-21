package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

public interface NumberInterface {

    public NumberInterface EMPTY();

    public NumberInterface UNBOUND();

    public NumberInterface BOOLEAN_INTERVAL();

    public NumberInterface ZERO();

    public NumberInterface ONE();

    /**
     * This method determines if this interval intersects with another interval.
     *
     * @param other
     *            the other interval
     * @return true if the intervals intersect, else false
     */
    public boolean intersects(NumberInterface other);

    /**
     * This method returns the lower bound of the interval.
     *
     * @return the lower bound
     */
    public Number getLow();

    /**
     * This method returns the upper bound of the interval.
     *
     * @return the upper bound
     */
    public Number getHigh();

    /**
     * This method determines if this interval is definitely greater than the
     * other interval.
     *
     * @param other
     *            interval to compare with
     * @return true if the lower bound of this interval is always strictly
     *         greater than the upper bound of the other interval, else false
     */
    public boolean isGreaterThan(NumberInterface other);

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
    public boolean isGreaterOrEqualThan(NumberInterface other);

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
    public NumberInterface modulo(NumberInterface other);

    public boolean isUnbound();

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
    public NumberInterface union(NumberInterface other);

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
    public boolean contains(NumberInterface other);

    default boolean isEmpty() {
        return false;
    }

    public NumberInterface negate();

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
    public NumberInterface intersect(NumberInterface other);

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
    public NumberInterface limitUpperBoundBy(NumberInterface other);

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
    public NumberInterface limitLowerBoundBy(NumberInterface other);

    public NumberInterface asDecimal();

    public NumberInterface asInteger();

    public boolean isNumericValue();

    /**
     * True if we have no idea about this value(can not track it), false
     * otherwise.
     */
    public boolean isUnknown();

    /** True if we deterministically know the actual value, false otherwise. */
    public boolean isExplicitlyKnown();

    /**
     * Returns the NumericValue if the stored value can be explicitly
     * represented by a numeric value, null otherwise.
     **/

    public NumericValue asNumericValue();

    /** Return the long value if this is a long value, null otherwise. **/
    public Long asLong(CType type);

    public Number getNumber();

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
        public NumberInterface EMPTY() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface UNBOUND() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface BOOLEAN_INTERVAL() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface ZERO() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface ONE() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean intersects(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Number getLow() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Number getHigh() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isGreaterThan(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isGreaterOrEqualThan(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public NumberInterface plus(NumberInterface otherNumberInterface) {
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
        public NumberInterface modulo(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isUnbound() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public NumberInterface union(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean contains(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public NumberInterface negate() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface intersect(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface limitUpperBoundBy(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface limitLowerBoundBy(NumberInterface pOther) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface asDecimal() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NumberInterface asInteger() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Number getNumber() {
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
    }
}

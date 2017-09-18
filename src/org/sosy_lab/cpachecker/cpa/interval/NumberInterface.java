package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

public interface NumberInterface {

    default NumberInterface EMPTY() {
        throw new AssertionError("trying to perform default function");
    }
    default NumberInterface UNBOUND(){
        throw new AssertionError("trying to perform default function");
    }
    default NumberInterface BOOLEAN_INTERVAL() {
        throw new AssertionError("trying to perform default function");
    }
    default NumberInterface ZERO() {
        throw new AssertionError("trying to perform default function");
    }
    default NumberInterface ONE() {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method determines if this element intersects with another element.
     *
     * @param other
     *            the other element
     * @return true if the elements intersect, else false
     */
    default boolean intersects(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method returns the lower bound of the interval.
     *
     * @return the lower bound
     */
    default Number getLow() {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method returns the upper bound of the element.
     *
     * @return the upper bound
     */
    default Number getHigh() {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method determines if this element is definitely greater than the
     * other element.
     *
     * @param other
     *            element to compare with
     * @return true if the lower bound of this element is always strictly
     *         greater than the upper bound of the other element, else false
     */
    default boolean isGreaterThan(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method determines if this element is definitely greater or equal
     * than the other element. The equality is only satisfied for one single
     * value!
     *
     * @param other
     *            element to compare with
     * @return true if the lower bound of this element is always strictly
     *         greater or equal than the upper bound of the other element, else
     *         false
     */
    default boolean isGreaterOrEqualThan(NumberInterface other){
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method adds an element to this element
     * @param element to add with
     * @return result of addition
     */
    default NumberInterface plus(NumberInterface element) {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method extracts an element to this element
     * @param element to extract with
     * @return result of multiplication
     */
    default  NumberInterface minus(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method multiplies an element by this element
     * @param element to multiplying with
     * @return result of multiplication
     */
    default NumberInterface times(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method divides an element by this element
     * @param element to divide by
     * @return result of divide
     */
    default NumberInterface divide(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs an arithmetical left shift of the integral types.
     *
     * @param offset element to perform an arithmetical left shift on the
     *            interval bounds.
     *
     * @return of the arithmetical left shift
     */
    default NumberInterface shiftLeft(NumberInterface offset){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs an arithmetical right shift of the integral types.
     *
     * @param offset element to perform an arithmetical right shift on the
     *            interval bounds.
     *
     * @return of the arithmetical right shift
     */
    default NumberInterface shiftRight(NumberInterface offset){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method divides an unsigned element by this (unsigned) element
     * @param element to divide by
     * @return result of divide
     */
    default NumberInterface unsignedDivide(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method divides an unsigned element by this (unsigned) element
     * @param element to divide by
     * @return result of divide
     */
    default NumberInterface unsignedModulo(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs an arithmetical right shift of the integral types.
     *
     * @param other element to perform an arithmetical right shift on the
     *            interval bounds.
     *
     * @return of the arithmetical right shift
     */
    default NumberInterface unsignedShiftRight(NumberInterface other){
        throw new AssertionError("trying to perform default function");
    }

    /**
     * New element instance after the modulo computation.
     *
     * @param other
     *            the other element
     * @return the new element with the respective bounds.
     */
    default NumberInterface modulo(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    default boolean isUnbound() {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method creates a new element instance representing the union of
     * this element with another element.
     *
     * The lower bound and upper bound of the new element is the minimum of
     * both lower bounds and the maximum of both upper bounds, respectively.
     *
     * @param other
     *            the other element
     * @return the new element with the respective bounds
     */
    default NumberInterface union(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method determines if this element contains another element.
     *
     * The method still returns true, if the borders match. An empty element
     * does not contain any element and is not contained in any element
     * either. So if the callee or parameter is an empty element, this method
     * will return false.
     *
     * @param other
     *            the other element
     * @return true if this element contains the other element, else false
     */
    default boolean contains(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    default boolean isEmpty() {
        throw new AssertionError("trying to perform default function");
    }

    default NumberInterface negate() {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method creates a new element instance representing the intersection
     * of this element with another element.
     *
     * The lower bound and upper bound of the new element is the maximum of
     * both lower bounds and the minimum of both upper bounds, respectively.
     *
     * @param other
     *            the other element
     * @return the new element with the respective bounds
     */
    default NumberInterface intersect(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method returns a new element with a limited, i.e. lower, upper
     * bound.
     *
     * @param other
     *            the element to limit this element
     * @return the new element with the lower bound of this element and the
     *         upper bound set to the minimum of this element's and the other
     *         element's upper bound or an empty element if this element is
     *         greater than the other element.
     */
    default NumberInterface limitUpperBoundBy(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * This method returns a new element with a limited, i.e. higher, lower
     * bound.
     *
     * @param other
     *            the element to limit this element
     * @return the new element with the upper bound of this element and the
     *         lower bound set to the maximum of this element's and the other
     *         element's lower bound or an empty element if this element is
     *         less than the other element.
     */
    default NumberInterface limitLowerBoundBy(NumberInterface other) {
        throw new AssertionError("trying to perform default function");
    }

    default NumberInterface asDecimal() {
        throw new AssertionError("trying to perform default function");
    }

    default NumberInterface asInteger() {
        throw new AssertionError("trying to perform default function");
    }

    default boolean isNumericValue() {
        throw new AssertionError("trying to perform default function");
    }

    /**
     * True if we have no idea about this value(can not track it), false
     * otherwise.
     */
    default boolean isUnknown() {
        throw new AssertionError("trying to perform default function");
    }

    /** True if we deterministically know the actual value, false otherwise. */
    default boolean isExplicitlyKnown() {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs bitwise inclusive AND
     * @param element to calculate with
     * @return result of operation
     */
    default NumberInterface binaryAnd(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs bitwise inclusive OR
     * @param element to calculate with
     * @return result of operation
     */
    default NumberInterface binaryOr(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method performs bitwise inclusive XOR
     * @param element to calculate with
     * @return result of operation
     */
    default NumberInterface binaryXor(NumberInterface element){
        throw new AssertionError("trying to perform default function");
    }

    /**
     * Returns the NumericValue if the stored value can be explicitly
     * represented by a numeric value, null otherwise.
     **/

    default NumericValue asNumericValue() {
        throw new AssertionError("trying to perform default function");
    }

    public Long asLong(CType type);

    default Number getNumber() {
        throw new AssertionError("trying to perform default function");
    }
    /**
     * This method proves if an element covers another element
     * @param sign to prove it
     * @return result
     */
    default boolean covers(NumberInterface sign)
    {
        throw new AssertionError("trying to perform default function");
    }
    default ImmutableSet<NumberInterface> split() {
        throw new AssertionError("trying to perform default function");
    }

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
}

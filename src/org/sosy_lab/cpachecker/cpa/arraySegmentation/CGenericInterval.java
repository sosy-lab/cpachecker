/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CGenericInterval implements Serializable{


  private static final long serialVersionUID = -6301784251123330158L;

  /**
   * the lower bound of the interval
   */
  private CExpression low;

  /**
   * the upper bound of the interval
   */
  private CExpression high;

  // private static final CGenericInterval EMPTY = new CGenericInterval(null, null);
//  public static final GenericInterval UNBOUND = new GenericInterval(new AIntegerLiteralExpression(), Long.MAX_VALUE);
//  public static final GenericInterval BOOLEAN_INTERVAL = new GenericInterval(0L, 1L);
  // public static final CGenericInterval ZERO =
  // new CGenericInterval(CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ZERO);
  // public static final CGenericInterval ONE =
  // new CGenericInterval(CIntegerLiteralExpression.ONE, CIntegerLiteralExpression.ONE);

  /**
   * This method acts as constructor for a single-value interval.
   *
   * @param value for the lower and upper bound
   */
  public CGenericInterval(CExpression value) {
    this.low  = value;

    this.high = value;

    isSane();
  }

  /**
   * This method acts as constructor for a long-based interval.
   *
   * @param low the lower bound
   * @param high the upper bound
   */
  public CGenericInterval(CExpression low, CExpression high) {
    this.low  = low;

    this.high = high;

    isSane();
  }

  private boolean isSane() {
    checkState((low == null) == (high == null), "invalid empty interval");
    // checkState(low == null , "low cannot be larger than high");

    return true;
  }

  /**
   * This method returns the lower bound of the interval.
   *
   * @return the lower bound
   */
  public CExpression getLow() {
    return low;
  }

  /**
   * This method returns the upper bound of the interval.
   *
   * @return the upper bound
   */
  public CExpression getHigh() {
    return high;
  }

  public void setLow(CExpression pLow) {
    low = pLow;
  }

  public void setHigh(CExpression pHigh) {
    high = pHigh;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object other) {
    if (other != null && getClass().equals(other.getClass())) {
      CGenericInterval another = (CGenericInterval)other;
      return Objects.equals(low, another.low) && Objects.equals(high, another.high);
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(low, high);
  }





  /**
   * This method adds an interval from this interval, overflow is handled by setting the bound to
   * Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param interval the interval to add
   * @return a new interval with the respective bounds
   */
  public CGenericInterval plus(CGenericInterval interval) {
    // if (isEmpty() || interval.isEmpty()) {
    // return EMPTY;
    // }

    return new CGenericInterval(
        saturatedAdd(low, interval.low, CBinaryExpression.BinaryOperator.PLUS),
        saturatedAdd(high, interval.high, CBinaryExpression.BinaryOperator.PLUS));
  }

  private CExpression
      saturatedAdd(CExpression op1, CExpression op2, CBinaryExpression.BinaryOperator op) {
    CType t1 = op1.getExpressionType().getCanonicalType();
    CType t2 = op2.getExpressionType().getCanonicalType();

    final CType calculationType;
    final CType resultType;

    // if parser cannot determinate type, we ignore the type
    // TODO do we use the correct CProblemType?
    // TODO in special cases (depending on the operator) we could return the correct type
    if (t1 instanceof CProblemType) {
      calculationType = resultType = t1;
    } else if (t2 instanceof CProblemType) {
      calculationType = resultType = t2;

    } else {
      calculationType = op1.getExpressionType();
      resultType = op1.getExpressionType();
    }

    return new CBinaryExpression(op1.getFileLocation(), resultType, calculationType, op1, op2, op);
  }


  /**
   * This method adds a constant offset to this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to add
   * @return a new interval with the respective bounds
   */
  public CGenericInterval plus(Long offset) {
    return plus(
        new CGenericInterval(
            new CIntegerLiteralExpression(
                FileLocation.DUMMY,
                CNumericTypes.INT,
                BigInteger.valueOf(offset))));
  }

  // /**
  // * This method subtracts an interval from this interval, overflow is handled by setting the
  // bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
  // *
  // * @param other interval to subtract
  // * @return a new interval with the respective bounds
  // */
  // public CGenericInterval minus(CGenericInterval other) {
  // return plus(other.negate());
  // }

  /**
   * This method subtracts a constant offset to this interval, overflow is handled by setting the bound to Long.MIN_VALUE or Long.MAX_VALUE respectively.
   *
   * @param offset the constant offset to subtract
   * @return a new interval with the respective bounds
   */
  public CGenericInterval minus(Long offset) {
    return plus(-offset);
  }

  // /**
  // * This method multiplies this interval with another interval. In case of an overflow
  // Long.MAX_VALUE and Long.MIN_VALUE are used instead.
  // *
  // * @param other interval to multiply this interval with
  // * @return new interval that represents the result of the multiplication of the two intervals
  // */
  // public CGenericInterval times(CGenericInterval other) {
  // long[] values = {
  // saturatedMultiply(low, other.low),
  // saturatedMultiply(low, other.high),
  // saturatedMultiply(high, other.low),
  // saturatedMultiply(high, other.high)
  // };
  //
  // return new CGenericInterval(Longs.min(values), Longs.max(values));
  // }
  //
  // /**
  // * This method divides this interval by another interval. If the other interval contains "0" an
  // unbound interval is returned.
  // *
  // * @param other interval to divide this interval by
  // * @return new interval that represents the result of the division of the two intervals
  // */
  // public CGenericInterval divide(CGenericInterval other) {
  // // other interval contains "0", return unbound interval
  // if (other.contains(ZERO)) {
  // return UNBOUND;
  // } else {
  // long[] values = {
  // low / other.low,
  // low / other.high,
  // high / other.low,
  // high / other.high
  // };
  //
  // return new CGenericInterval(Longs.min(values), Longs.max(values));
  // }
  // }
  //
  // /**
  // * This method performs an arithmetical left shift of the interval bounds.
  // *
  // * @param offset Interval offset to perform an arithmetical left shift on the interval
  // * bounds. If the offset maybe less than zero an unbound interval is returned.
  // * @return new interval that represents the result of the arithmetical left shift
  // */
  // public CGenericInterval shiftLeft(CGenericInterval offset) {
  // // create an unbound interval upon trying to shift by a possibly negative offset
  // if (ZERO.mayBeGreaterThan(offset)) {
  // return UNBOUND;
  // } else {
  // // if lower bound is negative, shift it by upper bound of offset, else by lower bound of offset
  // Long newLow = low << ((low < 0L) ? offset.high : offset.low);
  //
  // // if upper bound is negative, shift it by lower bound of offset, else by upper bound of offset
  // Long newHigh = high << ((high < 0L) ? offset.low : offset.high);
  //
  // if ((low < 0 && newLow > low) || (high > 0 && newHigh < high)) {
  // return UNBOUND;
  // } else {
  // return new CGenericInterval(newLow, newHigh);
  // }
  // }
  // }
  //
  // /**
  // * This method performs an arithmetical right shift of the interval bounds. If the offset maybe
  // less than zero an unbound interval is returned.
  // *
  // * @param offset Interval offset to perform an arithmetical right shift on the interval bounds
  // * @return new interval that represents the result of the arithmetical right shift
  // */
  // public CGenericInterval shiftRight(CGenericInterval offset) {
  // // create an unbound interval upon trying to shift by a possibly negative offset
  // if (ZERO.mayBeGreaterThan(offset)) {
  // return UNBOUND;
  // } else {
  // // if lower bound is negative, shift it by lower bound of offset, else by upper bound of offset
  // Long newLow = low >> ((low < 0L) ? offset.low : offset.high);
  //
  // // if upper bound is negative, shift it by upper bound of offset, else by lower bound of offset
  // Long newHigh = high >> ((high < 0L) ? offset.high : offset.low);
  //
  // return new CGenericInterval(newLow, newHigh);
  // }
  // }

  // /**
  // * This method negates this interval.
  // *
  // * @return new negated interval
  // */
  // public CGenericInterval negate() {
  // return new CGenericInterval(saturatedMultiply(high, -1L), saturatedMultiply(low, -1L));
  // }

  /**
   * This method determines whether the interval is empty or not.
   *
   * @return true, if the interval is empty, i.e. the lower and upper bounds are null
   */
  public boolean isEmpty() {
    return low == null && high == null;
  }

  // public boolean isUnbound() {
  // return !isEmpty() && low == Long.MIN_VALUE && high == Long.MAX_VALUE;
  // }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[" + (low == null ? "" : low) + "; " + (high == null ? "" : high) + "]";
  }

  // /**
  // * This method is a factory method for a lower bounded interval.
  // *
  // * @param lowerBound the lower bound to set
  // * @return a lower bounded interval, i.e. the lower bound is set to the given lower bound, the
  // upper bound is set to Long.MAX_VALUE
  // */
  // public static CGenericInterval createLowerBoundedInterval(Long lowerBound) {
  // return new CGenericInterval(lowerBound, Long.MAX_VALUE);
  // }
  //
  // /**
  // * This method is a factory method for an upper bounded interval.
  // *
  // * @param upperBound the upper bound to set
  // * @return an upper bounded interval, i.e. the lower bound is set to Long.MIN_VALUE, the upper
  // bound is set to the given upper bound
  // */
  // public static CGenericInterval createUpperBoundedInterval(Long upperBound) {
  // return new CGenericInterval(Long.MIN_VALUE, upperBound);
  // }
}

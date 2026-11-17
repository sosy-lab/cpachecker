// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.base.Equivalence;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Equivalence of {@link Value}s for the value mapping of SMG2 (see {@link
 * org.sosy_lab.cpachecker.cpa.smg2.util.ValueToSMGValueMapping}).
 *
 * <p>Two values are equivalent if they are equal, or if both are zero with an integer type (see
 * {@link NumericValue#hasIntegerType()}), regardless of their Java type (e.g., {@code 0}, {@code
 * 0L}, and {@code BigInteger.ZERO}). All integer zeros are thus mapped to the same {@link
 * org.sosy_lab.cpachecker.util.smg.graph.SMGValue}. All other values, including floating-point
 * zeros and rational numbers, are compared with {@link Object#equals(Object)}, so, e.g., {@code
 * 0.0f}, {@code 0.0}, and {@code -0.0} are neither equivalent to integer zero nor to each other.
 */
public final class ValueWrapper extends Equivalence<Value> {

  @Override
  protected boolean doEquivalent(Value pArg0, Value pArg1) {
    return (isIntegerZero(pArg0) && isIntegerZero(pArg1)) || pArg0.equals(pArg1);
  }

  @Override
  protected int doHash(Value pArg0) {
    // All integer zeros are equivalent, so they need the same hash code
    return isIntegerZero(pArg0) ? 0 : pArg0.hashCode();
  }

  private static boolean isIntegerZero(Value pValue) {
    return pValue instanceof NumericValue numericValue
        && numericValue.hasIntegerType()
        && numericValue.bigIntegerValue().signum() == 0;
  }
}

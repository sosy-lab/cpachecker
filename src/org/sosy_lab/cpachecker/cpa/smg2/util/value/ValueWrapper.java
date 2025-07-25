// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.base.Equivalence;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/** Wrapper for {@link Value} to make equal {@link NumericValue}s equal for the valuesMapping. */
public final class ValueWrapper extends Equivalence<Value> {

  @Override
  protected boolean doEquivalent(Value pArg0, Value pArg1) {
    if ((pArg0 instanceof NumericValue && pArg1 instanceof NumericValue)
        && (pArg0.asNumericValue().longValue() == 0 && pArg1.asNumericValue().longValue() == 0)) {
      // 0 has to be split into 3 categories, for non-floating point types, floats and doubles
      Number arg0Num = pArg0.asNumericValue().getNumber();
      Number arg1Num = pArg1.asNumericValue().getNumber();
      if (arg0Num instanceof Float arg0 && arg1Num instanceof Float arg1) {
        return arg0.compareTo(arg1) == 0;
      } else if (arg0Num instanceof Double arg0 && arg1Num instanceof Double arg1) {
        return arg0.compareTo(arg1) == 0;
      } else if (arg0Num instanceof FloatValue || arg1Num instanceof FloatValue) {
        return arg0Num.equals(arg1Num);
      } else if (!(arg0Num instanceof Double)
          && !(arg1Num instanceof Double)
          && !(arg0Num instanceof Float)
          && !(arg1Num instanceof Float)) {
        try {
          return pArg0.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0
              && pArg1.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0;
        } catch (NumberFormatException e) {
          // This happens for Nan, -/+Infinity
          // let equals handle this
        }
      }
    }
    return pArg0.equals(pArg1);
  }

  @Override
  protected int doHash(Value pArg0) {
    return pArg0.hashCode();
  }
}

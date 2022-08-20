// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.base.Equivalence;
import java.math.BigDecimal;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/** Wrapper for {@link Value} to make equal {@link NumericValue}s equal for the valuesMapping. */
public final class ValueWrapper extends Equivalence<Value> {

  @Override
  protected boolean doEquivalent(Value pArg0, Value pArg1) {
    if (pArg0 instanceof NumericValue && pArg1 instanceof NumericValue) {
      if (pArg0.asNumericValue().longValue() == 0 && pArg1.asNumericValue().longValue() == 0) {
        try {
          return pArg0.asNumericValue().bigDecimalValue().compareTo(BigDecimal.ZERO) == 0
              && pArg1.asNumericValue().bigDecimalValue().compareTo(BigDecimal.ZERO) == 0;
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

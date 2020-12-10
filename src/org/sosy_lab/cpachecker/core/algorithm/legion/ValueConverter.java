// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

class ValueConverter {

  /**
   * Convert a value object (something implementing org.sosy_lab.cpachecker.cpa.value.type.Value)
   * into it's concrete implementation.
   */
  public static Value toValue(Object value) {
    if (value instanceof Boolean) {
      return BooleanValue.valueOf((Boolean) value);
    } else if (value instanceof Integer) {
      return new NumericValue((Integer) value);
    } else if (value instanceof Character) {
      char c = (Character) value;
      int i = c;
      return new NumericValue(i);
    } else if (value instanceof Float) {
      return new NumericValue((Float) value);
    } else if (value instanceof Double) {
      return new NumericValue((Double) value);
    } else if (value instanceof BigInteger) {
      BigInteger v = (BigInteger) value;
      return new NumericValue(v);
    } else {
      throw new IllegalArgumentException(
          String.format("Did not recognize value for loadedValues Map: %s.", value.getClass()));
    }
  }
}

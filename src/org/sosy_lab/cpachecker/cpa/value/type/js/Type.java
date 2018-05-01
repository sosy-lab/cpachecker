/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type.js;

import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Type conversion and testing of <code>Value</code>. See
 * https://www.ecma-international.org/ecma-262/5.1/#sec-9
 */
@SuppressWarnings("WeakerAccess")
public final class Type {
  private Type() {}

  public static NumericValue toNumber(final Value pValue) {
    if (pValue instanceof JSUndefinedValue) {
      return toNumber((JSUndefinedValue) pValue);
    } else if (pValue instanceof NullValue) {
      return toNumber((NullValue) pValue);
    } else if (pValue instanceof BooleanValue) {
      return toNumber((BooleanValue) pValue);
    } else if (pValue instanceof NumericValue) {
      return toNumber((NumericValue) pValue);
    }
    throw new AssertionError("Unhandled value type " + pValue.getClass());
  }

  public static NumericValue toNumber(final JSUndefinedValue pValue) {
    return new NumericValue(Double.NaN);
  }

  public static NumericValue toNumber(final NullValue pValue) {
    return new NumericValue(0.0);
  }

  public static NumericValue toNumber(final BooleanValue pValue) {
    return new NumericValue(pValue.isTrue() ? 1 : 0);
  }

  public static NumericValue toNumber(final NumericValue pValue) {
    return pValue;
  }

  public static BooleanValue toBoolean(final Value pValue) {
    if (pValue instanceof JSUndefinedValue) {
      return toBoolean((JSUndefinedValue) pValue);
    } else if (pValue instanceof NullValue) {
      return toBoolean((NullValue) pValue);
    } else if (pValue instanceof BooleanValue) {
      return toBoolean((BooleanValue) pValue);
    } else if (pValue instanceof NumericValue) {
      return toBoolean((NumericValue) pValue);
    }
    throw new AssertionError("Unhandled value type " + pValue.getClass());
  }

  @SuppressWarnings("unused")
  public static BooleanValue toBoolean(final JSUndefinedValue pValue) {
    return BooleanValue.valueOf(false);
  }

  @SuppressWarnings("unused")
  public static BooleanValue toBoolean(final NullValue pValue) {
    return BooleanValue.valueOf(false);
  }

  public static BooleanValue toBoolean(final BooleanValue pValue) {
    return pValue;
  }

  public static BooleanValue toBoolean(final NumericValue pValue) {
    final double doubleValue = pValue.doubleValue();
    return BooleanValue.valueOf(!Double.isNaN(doubleValue) && doubleValue != 0.0);
  }
}

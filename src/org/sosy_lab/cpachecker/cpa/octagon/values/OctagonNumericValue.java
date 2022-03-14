// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon.values;

@SuppressWarnings("rawtypes")
public abstract class OctagonNumericValue<T extends Number>
    implements Comparable<OctagonNumericValue> {

  protected T value;

  protected OctagonNumericValue(T val) {
    assert val instanceof Double || val instanceof Long
        : "currently only doubles and longs are supported";
    value = val;
  }

  public final T getValue() {
    return value;
  }

  public abstract OctagonNumericValue min(OctagonNumericValue val1);

  public abstract OctagonNumericValue max(OctagonNumericValue val1);

  public abstract int signum();

  public abstract boolean isInfinite();

  public abstract boolean isInInterval(double lower, double upper);

  public abstract OctagonNumericValue add(OctagonNumericValue val);

  public abstract OctagonNumericValue add(long val);

  public abstract OctagonNumericValue add(double val);

  public abstract OctagonNumericValue subtract(OctagonNumericValue val);

  public abstract OctagonNumericValue subtract(long val);

  public abstract OctagonNumericValue subtract(double val);

  public abstract OctagonNumericValue mul(OctagonNumericValue val);

  public abstract OctagonNumericValue mul(long val);

  public abstract OctagonNumericValue mul(double val);

  public abstract OctagonNumericValue div(OctagonNumericValue divisor);

  public abstract OctagonNumericValue div(long divisor);

  public abstract OctagonNumericValue div(double divisor);

  public abstract boolean greaterEqual(OctagonNumericValue val);

  public abstract boolean greaterThan(OctagonNumericValue val);

  public abstract boolean lessEqual(OctagonNumericValue val);

  public abstract boolean lessThan(OctagonNumericValue val);

  public abstract boolean isEqual(OctagonNumericValue val);

  public abstract boolean greaterEqual(long pVal);

  public abstract boolean greaterEqual(double pVal);

  public abstract boolean greaterThan(long pVal);

  public abstract boolean greaterThan(double pVal);

  public abstract boolean lessEqual(long pVal);

  public abstract boolean lessEqual(double pVal);

  public abstract boolean lessThan(long pVal);

  public abstract boolean lessThan(double pVal);

  public abstract boolean isEqual(long pVal);

  public abstract boolean isEqual(double pVal);
}

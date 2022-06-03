// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon.values;

import java.util.Objects;

@SuppressWarnings("rawtypes")
public class OctagonDoubleValue extends OctagonNumericValue<Double> {

  public OctagonDoubleValue(double value) {
    super(value);
  }

  @Override
  public OctagonNumericValue min(OctagonNumericValue val1) {
    return val1.lessThan(value) ? val1 : this;
  }

  @Override
  public OctagonNumericValue max(OctagonNumericValue val1) {
    return val1.greaterThan(value) ? val1 : this;
  }

  @Override
  public int signum() {
    if (value.longValue() > 0) {
      return 1;
    } else if (value.longValue() == 0) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public boolean isInfinite() {
    return value.isInfinite();
  }

  @Override
  public boolean isInInterval(double lower, double upper) {
    return lower < value.doubleValue() && upper > value.doubleValue();
  }

  @Override
  public OctagonNumericValue add(OctagonNumericValue pVal) {
    return pVal.add(value);
  }

  @Override
  public OctagonNumericValue add(long pVal) {
    return new OctagonDoubleValue(value.doubleValue() + pVal);
  }

  @Override
  public OctagonNumericValue add(double pVal) {
    return new OctagonDoubleValue(value.doubleValue() + pVal);
  }

  @Override
  public OctagonNumericValue subtract(OctagonNumericValue pVal) {
    if (pVal instanceof OctagonIntValue) {
      return new OctagonDoubleValue(
          value.doubleValue() - ((OctagonIntValue) pVal).value.doubleValue());
    } else if (pVal instanceof OctagonDoubleValue) {
      return new OctagonDoubleValue(
          value.doubleValue() - ((OctagonDoubleValue) pVal).value.doubleValue());
    }
    throw new AssertionError("unknown subtype of octnumericvalue");
  }

  @Override
  public OctagonNumericValue subtract(long pVal) {
    return new OctagonDoubleValue(value.doubleValue() - pVal);
  }

  @Override
  public OctagonNumericValue subtract(double pVal) {
    return new OctagonDoubleValue(value.doubleValue() - pVal);
  }

  @Override
  public OctagonNumericValue mul(OctagonNumericValue pVal) {
    return pVal.mul(value);
  }

  @Override
  public OctagonNumericValue mul(long pVal) {
    return new OctagonDoubleValue(value.doubleValue() * pVal);
  }

  @Override
  public OctagonNumericValue mul(double pVal) {
    return new OctagonDoubleValue(value.doubleValue() * pVal);
  }

  @Override
  public OctagonNumericValue div(OctagonNumericValue pDivisor) {
    if (pDivisor instanceof OctagonIntValue) {
      return new OctagonDoubleValue(
          value.doubleValue() / ((OctagonIntValue) pDivisor).value.doubleValue());
    } else if (pDivisor instanceof OctagonDoubleValue) {
      return new OctagonDoubleValue(
          value.doubleValue() / ((OctagonDoubleValue) pDivisor).value.doubleValue());
    }
    throw new AssertionError("unknown subtype of octnumericvalue");
  }

  @Override
  public OctagonNumericValue div(long pDivisor) {
    return new OctagonDoubleValue(value.doubleValue() / pDivisor);
  }

  @Override
  public OctagonNumericValue div(double pDivisor) {
    return new OctagonDoubleValue(value.doubleValue() / pDivisor);
  }

  @Override
  public boolean greaterEqual(OctagonNumericValue pVal) {
    return pVal.lessEqual(value);
  }

  @Override
  public boolean greaterEqual(long pVal) {
    return value.doubleValue() >= pVal;
  }

  @Override
  public boolean greaterEqual(double pVal) {
    return value.doubleValue() >= pVal;
  }

  @Override
  public boolean greaterThan(OctagonNumericValue pVal) {
    return pVal.lessThan(value);
  }

  @Override
  public boolean greaterThan(long pVal) {
    return value.doubleValue() > pVal;
  }

  @Override
  public boolean greaterThan(double pVal) {
    return value.doubleValue() > pVal;
  }

  @Override
  public boolean lessEqual(OctagonNumericValue pVal) {
    return pVal.greaterEqual(value);
  }

  @Override
  public boolean lessEqual(long pVal) {
    return value.doubleValue() <= pVal;
  }

  @Override
  public boolean lessEqual(double pVal) {
    return value.doubleValue() <= pVal;
  }

  @Override
  public boolean lessThan(OctagonNumericValue pVal) {
    return pVal.greaterThan(value);
  }

  @Override
  public boolean lessThan(long pVal) {
    return value.doubleValue() < pVal;
  }

  @Override
  public boolean lessThan(double pVal) {
    return value.doubleValue() < pVal;
  }

  @Override
  public boolean isEqual(OctagonNumericValue pVal) {
    return pVal.isEqual(value);
  }

  @Override
  public boolean isEqual(long pVal) {
    return value.doubleValue() == pVal;
  }

  @Override
  public boolean isEqual(double pVal) {
    return value.doubleValue() == pVal;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof OctagonDoubleValue)) {
      return false;
    }

    OctagonDoubleValue other = (OctagonDoubleValue) obj;

    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(value);
    return result;
  }

  @Override
  public int compareTo(OctagonNumericValue val) {
    if (val.isEqual(value)) {
      return 0;
    } else if (val.lessThan(value)) {
      return 1;
    } else if (val.greaterThan(value)) {
      return -1;
    }
    throw new AssertionError("implementation fault");
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

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
package org.sosy_lab.cpachecker.cpa.octagon.values;

import java.util.Objects;

@SuppressWarnings("rawtypes")
public class OctDoubleValue extends OctNumericValue<Double> {

  public OctDoubleValue(double value) {
    super (value);
  }

  @Override
  public OctNumericValue min(OctNumericValue val1) {
    return val1.lessThan(value) ? val1 : this;
  }

  @Override
  public OctNumericValue max(OctNumericValue val1) {
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
  public OctNumericValue add(OctNumericValue pVal) {
    return pVal.add(value.doubleValue());
  }

  @Override
  public OctNumericValue add(long pVal) {
    return new OctDoubleValue(value.doubleValue() + pVal);
  }

  @Override
  public OctNumericValue add(double pVal) {
    return new OctDoubleValue(value.doubleValue() + pVal);
  }

  @Override
  public OctNumericValue subtract(OctNumericValue pVal) {
    if (pVal instanceof OctIntValue) {
      return new OctDoubleValue(value.doubleValue() - ((OctIntValue)pVal).value.doubleValue());
    } else if (pVal instanceof OctDoubleValue) {
      return new OctDoubleValue(value.doubleValue() - ((OctDoubleValue)pVal).value.doubleValue());
    }
    throw new AssertionError("unknown subtype of octnumericvalue");
  }

  @Override
  public OctNumericValue subtract(long pVal) {
    return new OctDoubleValue(value.doubleValue() - pVal);
  }

  @Override
  public OctNumericValue subtract(double pVal) {
    return new OctDoubleValue(value.doubleValue() - pVal);
  }

  @Override
  public OctNumericValue mul(OctNumericValue pVal) {
    return pVal.mul(value);
  }

  @Override
  public OctNumericValue mul(long pVal) {
    return new OctDoubleValue(value.doubleValue() * pVal);
  }

  @Override
  public OctNumericValue mul(double pVal) {
    return new OctDoubleValue(value.doubleValue() * pVal);
  }

  @Override
  public OctNumericValue div(OctNumericValue pDivisor) {
    if (pDivisor instanceof OctIntValue) {
      return new OctDoubleValue(value.doubleValue() / ((OctIntValue)pDivisor).value.doubleValue());
    } else if (pDivisor instanceof OctDoubleValue) {
      return new OctDoubleValue(value.doubleValue() / ((OctDoubleValue)pDivisor).value.doubleValue());
    }
    throw new AssertionError("unknown subtype of octnumericvalue");
  }

  @Override
  public OctNumericValue div(long pDivisor) {
    return new OctDoubleValue(value.doubleValue() / pDivisor);
  }

  @Override
  public OctNumericValue div(double pDivisor) {
    return new OctDoubleValue(value.doubleValue() / pDivisor);
  }

  @Override
  public boolean greaterEqual(OctNumericValue pVal) {
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
  public boolean greaterThan(OctNumericValue pVal) {
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
  public boolean lessEqual(OctNumericValue pVal) {
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
  public boolean lessThan(OctNumericValue pVal) {
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
  public boolean isEqual(OctNumericValue pVal) {
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
    if (!(obj instanceof OctDoubleValue)) {
      return false;
    }

    OctDoubleValue other = (OctDoubleValue) obj;

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
  public int compareTo(OctNumericValue val) {
    if (val.isEqual(value)) {
      return 0;
    } else if (val.lessThan(value)) {
      return 1;
    } else if (val.greaterThan(value)){
      return -1;
    }
    throw new AssertionError("implementation fault");
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

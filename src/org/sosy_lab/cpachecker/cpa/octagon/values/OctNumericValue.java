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

@SuppressWarnings("rawtypes")
public abstract class OctNumericValue<T extends Number> implements Comparable<OctNumericValue> {

  protected T value;

  protected OctNumericValue (T val) {
    assert (val instanceof Double || val instanceof Long) : "currently only doubles and longs are supported";
    value = val;
  }

  public final T getValue() {
    return value;
  }

  public abstract OctNumericValue min(OctNumericValue val1);
  public abstract OctNumericValue max(OctNumericValue val1);
  public abstract int signum();
  public abstract boolean isInfinite();

  public abstract boolean isInInterval(double lower, double upper);

  public abstract OctNumericValue add(OctNumericValue val);

  public abstract OctNumericValue add(long val);

  public abstract OctNumericValue add(double val);

  public abstract OctNumericValue subtract(OctNumericValue val);

  public abstract OctNumericValue subtract(long val);

  public abstract OctNumericValue subtract(double val);

  public abstract OctNumericValue mul(OctNumericValue val);

  public abstract OctNumericValue mul(long val);

  public abstract OctNumericValue mul(double val);

  public abstract OctNumericValue div(OctNumericValue divisor);

  public abstract OctNumericValue div(long divisor);

  public abstract OctNumericValue div(double divisor);

  public abstract boolean greaterEqual(OctNumericValue val);

  public abstract boolean greaterThan(OctNumericValue val);

  public abstract boolean lessEqual(OctNumericValue val);

  public abstract boolean lessThan(OctNumericValue val);

  public abstract boolean isEqual(OctNumericValue val);

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

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
package org.sosy_lab.cpachecker.util.octagon;

import java.util.Objects;

import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctNumericValue;


public class InfinityNumericWrapper {

  private boolean isPositiveInfinite;
  private boolean isNegativeInfinite;
  private OctNumericValue value;

  public InfinityNumericWrapper(double d) {
    if (Double.isInfinite(d)) {
      isPositiveInfinite = d > 0;
      isNegativeInfinite = d < 0;
    } else {
      value = new OctNumericValue(d);
    }
  }

  public InfinityNumericWrapper(OctNumericValue num) {
    value = num;
  }

  public boolean isPositiveInfinite() {
    return isPositiveInfinite;
  }

  public boolean isNegativeInfinite() {
    return isNegativeInfinite;
  }

  public OctNumericValue getValue() {
    if (isPositiveInfinite || isNegativeInfinite) {
      throw new UnsupportedOperationException("Infinte numbers have no value!");
    } else {
      return value;
    }
  }

  public InfinityNumericWrapper mul(OctNumericValue val) {
    if (val.greaterEqual(OctNumericValue.ZERO)) {
      if (isPositiveInfinite || isNegativeInfinite) {
        return this;
      }
    } else {
      if (isPositiveInfinite) {
        return new InfinityNumericWrapper(Double.NEGATIVE_INFINITY);
      } else if (isNegativeInfinite) {
        return new InfinityNumericWrapper(Double.POSITIVE_INFINITY);
      }
    }

    return new InfinityNumericWrapper(value.mul(val));
  }

  @Override
  public String toString(){
    if (isPositiveInfinite) {
      return "INFINITY";
    } else if (isNegativeInfinite) {
      return "-INFINITY";
    } else {
      return value.toString();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof InfinityNumericWrapper)) {
      return false;
    }

    InfinityNumericWrapper other = (InfinityNumericWrapper) obj;

    return (isNegativeInfinite && other.isNegativeInfinite)
            || (isPositiveInfinite && other.isPositiveInfinite)
            || (value.equals(other.value));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(isNegativeInfinite);
    result = prime * result + Objects.hash(isPositiveInfinite);
    result = prime * result + Objects.hash(value);
    return result;
  }
}

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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Stores a numeric value that can be tracked by the
 * ValueAnalysisCPA.
 */
public class NumericValue implements Value {
  private Number number;

  /**
   * Creates a new <code>NumericValue</code>, given the type and a
   * <code>BigDecimal</code>.
   * @param pType the inital type of the number.
   * @param pNumber the value of the number (must be a <code>BigDecimal</code>)
   */
  public NumericValue(Number pNumber) {
    number = pNumber;
  }

  /**
   * Returns the number stored in the container.
   *
   * @return the number stored in the container
   */
  public Number getNumber() {
    return number;
  }

  /**
   * Returns the integer stored in the container as long. Before calling this function,
   * it must be ensured using `getType()` that this container contains an integer.
   * @return
   */
  public long longValue() {
    return number.longValue();
  }

  /**
   * Returns the floating point stored in the container as float.
   * @return
   */
  public float floatValue() {
    return number.floatValue();
  }

  /**
   * Returns the floating point stored in the container as double.
   * @return
   */
  public double doubleValue() {
    return number.doubleValue();
  }

  /**
   * Returns a BigDecimal value representing the stored number.
   */
  public BigDecimal bigDecimalValue() {
    return new BigDecimal(number.toString());
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NumericValue [number=" + number + "]";
  }

  @Override
  public boolean equals(Object other) {
    if(other instanceof NumericValue) {
      return this.getNumber().equals(((NumericValue) other).getNumber());
    } else {
      return false;
    }
  }

  @Override
  public boolean isNumericValue() {
    return true;
  }

  public NumericValue negate() {
    // TODO explicitfloat: handle the different implementations of Number properly
    return new NumericValue(this.bigDecimalValue().negate());
  }

  public boolean isNull() {
    return bigDecimalValue().compareTo(new BigDecimal(0)) == 0;
  }

  @Override
  public NumericValue asNumericValue() {
    return this;
  }

  @Override
  public Long asLong(CType type) {
    checkNotNull(type);
    if(!(type instanceof CSimpleType)) {
      return null;
    }

    if(((CSimpleType)type).getType() == CBasicType.INT) {
      return longValue();
    } else {
      return null;
    }
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public int hashCode() {
    // fulfills contract that if this.equals(other),
    // then this.hashCode() == other.hashCode()
    return number.hashCode();
  }

}

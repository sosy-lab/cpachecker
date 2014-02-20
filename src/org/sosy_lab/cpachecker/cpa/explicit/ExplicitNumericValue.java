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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Stores a numeric value that can be tracked by the
 * ExplicitCPA.
 */
public class ExplicitNumericValue implements ExplicitValueBase {
  private Number number;

  /**
   * Creates a new <code>ExplicitNumberValue</code>, given the type and a
   * <code>BigDecimal</code>.
   * @param pType the inital type of the number.
   * @param pNumber the value of the number (must be a <code>BigDecimal</code>)
   */
  public ExplicitNumericValue(Number pNumber) {
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
    return "ExplicitNumericValue [number=" + number + "]";
  }

  @Override
  public boolean equals(Object other) {
    if(other instanceof ExplicitNumericValue) {
      return this.getNumber().equals(((ExplicitNumericValue) other).getNumber());
    } else if(other instanceof Number) {
      return this.getNumber().equals(other);
    } else {
      return false;
    }
  }

  @Override
  public boolean isNumericValue() {
    return true;
  }

  public ExplicitNumericValue negate() {
    // TODO explicitfloat: handle the different implementations of Number properly
    return new ExplicitNumericValue(this.bigDecimalValue().negate());
  }

  public boolean isNull() {
    return bigDecimalValue().compareTo(new BigDecimal(0)) == 0;
  }

  @Override
  public ExplicitNumericValue asNumericValue() {
    return this;
  }

  @Override
  public Long asLong(CType type) {
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

}

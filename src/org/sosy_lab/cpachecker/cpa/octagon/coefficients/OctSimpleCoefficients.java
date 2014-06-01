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
package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import java.util.Arrays;
import java.util.Objects;

import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctDoubleValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctIntValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctInterval;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctNumericValue;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.base.Preconditions;

@SuppressWarnings("rawtypes")
public class OctSimpleCoefficients extends AOctCoefficients {

  protected OctNumericValue[] coefficients;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   */
  public OctSimpleCoefficients(int size, OctState oct) {
    super(size, oct);
    coefficients = new OctNumericValue[size+1];
    Arrays.fill(coefficients, OctIntValue.ZERO);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctSimpleCoefficients(int size, int index, OctNumericValue value, OctState oct) {
    super(size, oct);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new OctNumericValue[size+1];
    Arrays.fill(coefficients, OctIntValue.ZERO);
    coefficients[index] = value;
  }

  /**
   * Create new Coefficients for #size variables. With a constant value
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctSimpleCoefficients(int size, OctNumericValue value, OctState oct) {
    super(size, oct);
    coefficients = new OctNumericValue[size+1];
    Arrays.fill(coefficients, OctIntValue.ZERO);
    coefficients[size] = value;
  }

  @Override
  public OctSimpleCoefficients expandToSize(int size, OctState oct) {
    Preconditions.checkArgument(this.size <= size, "new size too small");

    if (this.size == size) {
      return this;
    }

    OctSimpleCoefficients newCoeffs = new OctSimpleCoefficients(size, oct);

    for (int i = 0; i < coefficients.length-1; i++) {
      newCoeffs.coefficients[i] = coefficients[i];
    }
    newCoeffs.coefficients[size] = coefficients[this.size];

    return newCoeffs;
  }

  public OctIntervalCoefficients convertToInterval(){
    OctIntervalCoefficients octCoeffs = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      octCoeffs.coefficients[i] = new OctInterval(coefficients[i]);
    }
    return octCoeffs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients add(IOctCoefficients other) {
    if (other instanceof OctSimpleCoefficients) {
      return add((OctSimpleCoefficients)other);
    } else if (other instanceof OctIntervalCoefficients) {
      return add((OctIntervalCoefficients)other);
    } else if (other instanceof OctEmptyCoefficients) {
      return OctEmptyCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctCoefficients add(OctSimpleCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctSimpleCoefficients ret = new OctSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].add(other.coefficients[i]);
    }
    return ret;
  }

  private IOctCoefficients add(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = other.coefficients[i].plus(new OctInterval(coefficients[i]));
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients sub(IOctCoefficients other) {
    if (other instanceof OctSimpleCoefficients) {
      return sub((OctSimpleCoefficients)other);
    } else if (other instanceof OctIntervalCoefficients) {
      return sub((OctIntervalCoefficients)other);
    } else if (other instanceof OctEmptyCoefficients) {
      return OctEmptyCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctCoefficients sub(OctSimpleCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctSimpleCoefficients ret = new OctSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].subtract(other.coefficients[i]);
    }
    return ret;
  }

  private IOctCoefficients sub(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = new OctInterval(coefficients[i]).minus(other.coefficients[i]);
    }
    return ret;
  }

  @Override
  protected IOctCoefficients mulInner(IOctCoefficients other) {
    assert hasOnlyOneValue();

    int index = 0;
    OctNumericValue value = null;
    while (index < coefficients.length) {
      value = coefficients[index];
      if (!value.isEqual(OctIntValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index >= oct.sizeOfVariables()) {
      return other.mul(value);
    }

    OctInterval bounds = oct.getVariableBounds(index);
    // TODO make more cases (lower infinite / higher infinite)
    if (bounds.isInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.isSingular()) {
      return other.mul(bounds.getLow().mul(value));
    } else {
      return other.mul(bounds.times(new OctInterval(value)));
    }
  }

  @Override
  public IOctCoefficients mul(OctNumericValue factor) {
    OctSimpleCoefficients newCoeffs = new OctSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      newCoeffs.coefficients[i] = coefficients[i].mul(factor);
    }
    return newCoeffs;
  }

  @Override
  public IOctCoefficients mul(OctInterval interval) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = interval.times(new OctInterval(coefficients[i]));
    }
    return ret;
  }

  @Override
  protected IOctCoefficients divInner(IOctCoefficients coeffs) {
    assert coeffs.hasOnlyOneValue();
    if (coeffs instanceof OctSimpleCoefficients) {
      return divInner((OctSimpleCoefficients)coeffs);
    } else if (coeffs instanceof OctIntervalCoefficients) {
      return divInner((OctIntervalCoefficients)coeffs);
    } else if (coeffs instanceof OctEmptyCoefficients) {
      return OctEmptyCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctCoefficients divInner(OctSimpleCoefficients coeffs) {
    int index = 0;
    OctNumericValue value = null;
    while (index < coeffs.coefficients.length) {
      value = coeffs.coefficients[index];
      if (!value.isEqual(OctIntValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index == coeffs.oct.sizeOfVariables()) {
      return div(value);

      // this is a constant value which is ZERO
    } else if (index > oct.sizeOfVariables()) {
      throw new ArithmeticException("Division by zero");
    }

    OctInterval bounds = coeffs.oct.getVariableBounds(index);
    if (bounds.isInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.contains(OctInterval.FALSE)) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.isSingular()) {
      return div(bounds.getLow().mul(value));
    } else {
      return div(bounds.times(new OctInterval(value)));
    }
  }

  private IOctCoefficients divInner(OctIntervalCoefficients coeffs) {
    assert coeffs.hasOnlyOneValue();

    int index = 0;
    OctInterval bounds = null;
    while (index < coeffs.coefficients.length) {
      bounds = coeffs.coefficients[index];
      if (!bounds.equals(OctInterval.FALSE)) {
        break;
      }
      index++;
    }

    OctInterval infBounds;

    // this is a constant value
    if (index == coeffs.oct.sizeOfVariables()) {
      return div(bounds);

     // this is a constant value which is in the interval [0,0]
    } else if (index > coeffs.oct.sizeOfVariables()) {
      throw new ArithmeticException("Division by zero");

    } else {
      infBounds = coeffs.oct.getVariableBounds(index);
    }



    if (infBounds.isInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    return div(infBounds.times(bounds));
  }

  @Override
  public IOctCoefficients div(OctNumericValue pDivisor) {
    OctSimpleCoefficients newCoeffs = new OctSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      newCoeffs.coefficients[i] = coefficients[i].div(pDivisor);
    }
    return newCoeffs;
  }

  @Override
  public IOctCoefficients div(OctInterval interval) {
    if (interval.isInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    // TODO make configurable
    if (interval.intersects(OctInterval.DELTA)) {
      return OctEmptyCoefficients.INSTANCE;
    }

    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = new OctInterval(coefficients[i]).divide(interval);
    }
    return ret;
  }

  /**
   * Returns the coefficient at the given index.
   */
  public OctNumericValue get(int index) {
    Preconditions.checkArgument(index < size, "Index too big");
    return coefficients[index];
  }

  public OctNumericValue getConstantValue() {
    return coefficients[size];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length - 1; i++) {
      if (!coefficients[i].isEqual(OctIntValue.ZERO)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean hasOnlyOneValue() {
    boolean foundValue = false;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].isEqual(OctIntValue.ZERO)) {
        if (foundValue) {
          return false;
        }
        foundValue = true;
      }
    }
    return true;
  }

  @Override
  public int getVariableIndex() {
    assert hasOnlyOneValue() && !hasOnlyConstantValue() : "is no variable!";
    int counter = 0;
    while (counter < size && coefficients[counter].isEqual(OctIntValue.ZERO)) {
      counter++;
    }
    return counter;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Arrays.hashCode(coefficients);
    result = prime * result + Objects.hash(size);
    return result;
  }

  public static OctSimpleCoefficients getBoolTRUECoeffs(int size, OctState oct) {
    OctSimpleCoefficients result = new OctSimpleCoefficients(size, oct);
    result.coefficients[size] = OctIntValue.ONE;
    return result;
  }

  public static OctSimpleCoefficients getBoolFALSECoeffs(int size, OctState oct) {
    OctSimpleCoefficients result = new OctSimpleCoefficients(size, oct);
    result.coefficients[size] = OctIntValue.ZERO;
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof OctSimpleCoefficients) || !super.equals(other)) {
      return false;
    }

    OctSimpleCoefficients oct = (OctSimpleCoefficients) other;

    return Arrays.equals(coefficients, oct.coefficients) && size == oct.size;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < coefficients.length; i++) {
      String tmp = coefficients[i].toString();
      if (i < coefficients.length-1) {
        builder.append(oct.getVariableToIndexMap().inverse().get(i) + " -> " + tmp + "\n");
      } else {
        builder.append("CONSTANT_VAL -> " + tmp + "\n");
      }
    }
    return builder.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NumArray getNumArray(OctagonManager manager) {
    NumArray arr = manager.init_num_t(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      if (coefficients[i] instanceof OctDoubleValue) {
        manager.num_set_float(arr, i, coefficients[i].getValue().doubleValue());
      } else {
        manager.num_set_int(arr, i, coefficients[i].getValue().longValue());
      }
    }
    return arr;
  }

}

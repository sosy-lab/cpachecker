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

import org.sosy_lab.cpachecker.cpa.octagon.OctagonState;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonDoubleValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonIntValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonInterval;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonNumericValue;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.base.Preconditions;

@SuppressWarnings("rawtypes")
public class OctagonSimpleCoefficients extends AOctagonCoefficients {

  protected OctagonNumericValue[] coefficients;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   */
  public OctagonSimpleCoefficients(int size, OctagonState oct) {
    super(size, oct);
    coefficients = new OctagonNumericValue[size+1];
    Arrays.fill(coefficients, OctagonIntValue.ZERO);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctagonSimpleCoefficients(int size, int index, OctagonNumericValue value, OctagonState oct) {
    super(size, oct);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new OctagonNumericValue[size+1];
    Arrays.fill(coefficients, OctagonIntValue.ZERO);
    coefficients[index] = value;
  }

  /**
   * Create new Coefficients for #size variables. With a constant value
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctagonSimpleCoefficients(int size, OctagonNumericValue value, OctagonState oct) {
    super(size, oct);
    coefficients = new OctagonNumericValue[size+1];
    Arrays.fill(coefficients, OctagonIntValue.ZERO);
    coefficients[size] = value;
  }

  @Override
  public OctagonSimpleCoefficients expandToSize(int size, OctagonState oct) {
    Preconditions.checkArgument(this.size <= size, "new size too small");

    if (this.size == size) {
      return this;
    }

    OctagonSimpleCoefficients newCoeffs = new OctagonSimpleCoefficients(size, oct);

    for (int i = 0; i < coefficients.length-1; i++) {
      newCoeffs.coefficients[i] = coefficients[i];
    }
    newCoeffs.coefficients[size] = coefficients[this.size];

    return newCoeffs;
  }

  public OctagonIntervalCoefficients convertToInterval(){
    OctagonIntervalCoefficients octCoeffs = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      octCoeffs.coefficients[i] = new OctagonInterval(coefficients[i]);
    }
    return octCoeffs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctagonCoefficients add(IOctagonCoefficients other) {
    if (other instanceof OctagonSimpleCoefficients) {
      return add((OctagonSimpleCoefficients)other);
    } else if (other instanceof OctagonIntervalCoefficients) {
      return add((OctagonIntervalCoefficients)other);
    } else if (other instanceof OctagonUniversalCoefficients) {
      return OctagonUniversalCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctagonCoefficients add(OctagonSimpleCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonSimpleCoefficients ret = new OctagonSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].add(other.coefficients[i]);
    }
    return ret;
  }

  private IOctagonCoefficients add(OctagonIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = other.coefficients[i].plus(new OctagonInterval(coefficients[i]));
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctagonCoefficients sub(IOctagonCoefficients other) {
    if (other instanceof OctagonSimpleCoefficients) {
      return sub((OctagonSimpleCoefficients)other);
    } else if (other instanceof OctagonIntervalCoefficients) {
      return sub((OctagonIntervalCoefficients)other);
    } else if (other instanceof OctagonUniversalCoefficients) {
      return OctagonUniversalCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctagonCoefficients sub(OctagonSimpleCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonSimpleCoefficients ret = new OctagonSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].subtract(other.coefficients[i]);
    }
    return ret;
  }

  private IOctagonCoefficients sub(OctagonIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = new OctagonInterval(coefficients[i]).minus(other.coefficients[i]);
    }
    return ret;
  }

  @Override
  protected IOctagonCoefficients mulInner(IOctagonCoefficients other) {
    assert hasOnlyOneValue();

    int index = 0;
    OctagonNumericValue value = null;
    while (index < coefficients.length) {
      value = coefficients[index];
      if (!value.isEqual(OctagonIntValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index >= oct.sizeOfVariables()) {
      return other.mul(value);
    }

    OctagonInterval bounds = oct.getVariableBounds(index);
    // TODO make more cases (lower infinite / higher infinite)
    if (bounds.isInfinite()) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    if (bounds.isSingular()) {
      return other.mul(bounds.getLow().mul(value));
    } else {
      return other.mul(bounds.times(new OctagonInterval(value)));
    }
  }

  @Override
  public IOctagonCoefficients mul(OctagonNumericValue factor) {
    OctagonSimpleCoefficients newCoeffs = new OctagonSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      newCoeffs.coefficients[i] = coefficients[i].mul(factor);
    }
    return newCoeffs;
  }

  @Override
  public IOctagonCoefficients mul(OctagonInterval interval) {
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = interval.times(new OctagonInterval(coefficients[i]));
    }
    return ret;
  }

  @Override
  protected IOctagonCoefficients divInner(IOctagonCoefficients coeffs) {
    assert coeffs.hasOnlyOneValue();
    if (coeffs instanceof OctagonSimpleCoefficients) {
      return divInner((OctagonSimpleCoefficients)coeffs);
    } else if (coeffs instanceof OctagonIntervalCoefficients) {
      return divInner((OctagonIntervalCoefficients)coeffs);
    } else if (coeffs instanceof OctagonUniversalCoefficients) {
      return OctagonUniversalCoefficients.INSTANCE;
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctagonCoefficients divInner(OctagonSimpleCoefficients coeffs) {
    int index = 0;
    OctagonNumericValue value = null;
    while (index < coeffs.coefficients.length) {
      value = coeffs.coefficients[index];
      if (!value.isEqual(OctagonIntValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index == coeffs.oct.sizeOfVariables()) {
      return div(value);

      // this is a constant value which is ZERO
      // divisions through zero should not be possible, thus this state
      // is there because of over-approximation
    } else if (index > oct.sizeOfVariables()) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    OctagonInterval bounds = coeffs.oct.getVariableBounds(index);
    if (bounds.isInfinite()) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    if (bounds.contains(OctagonInterval.FALSE)) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    if (bounds.isSingular()) {
      return div(bounds.getLow().mul(value));
    } else {
      return div(bounds.times(new OctagonInterval(value)));
    }
  }

  private IOctagonCoefficients divInner(OctagonIntervalCoefficients coeffs) {
    assert coeffs.hasOnlyOneValue();

    int index = 0;
    OctagonInterval bounds = null;
    while (index < coeffs.coefficients.length) {
      bounds = coeffs.coefficients[index];
      if (!bounds.equals(OctagonInterval.FALSE)) {
        break;
      }
      index++;
    }

    OctagonInterval infBounds;

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
      return OctagonUniversalCoefficients.INSTANCE;
    }

    return div(infBounds.times(bounds));
  }

  @Override
  public IOctagonCoefficients div(OctagonNumericValue pDivisor) {
    OctagonSimpleCoefficients newCoeffs = new OctagonSimpleCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      newCoeffs.coefficients[i] = coefficients[i].div(pDivisor);
    }
    return newCoeffs;
  }

  @Override
  public IOctagonCoefficients div(OctagonInterval interval) {
    if (interval.isInfinite()) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    // TODO make configurable
    if (interval.intersects(OctagonInterval.DELTA)) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = new OctagonInterval(coefficients[i]).divide(interval);
    }
    return ret;
  }

  /**
   * Returns the coefficient at the given index.
   */
  public OctagonNumericValue get(int index) {
    Preconditions.checkArgument(index < size, "Index too big");
    return coefficients[index];
  }

  public OctagonNumericValue getConstantValue() {
    return coefficients[size];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length - 1; i++) {
      if (!coefficients[i].isEqual(OctagonIntValue.ZERO)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean hasOnlyOneValue() {
    boolean foundValue = false;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].isEqual(OctagonIntValue.ZERO)) {
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
    while (counter < size && coefficients[counter].isEqual(OctagonIntValue.ZERO)) {
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

  public static OctagonSimpleCoefficients getBoolTRUECoeffs(int size, OctagonState oct) {
    OctagonSimpleCoefficients result = new OctagonSimpleCoefficients(size, oct);
    result.coefficients[size] = OctagonIntValue.ONE;
    return result;
  }

  public static OctagonSimpleCoefficients getBoolFALSECoeffs(int size, OctagonState oct) {
    OctagonSimpleCoefficients result = new OctagonSimpleCoefficients(size, oct);
    result.coefficients[size] = OctagonIntValue.ZERO;
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof OctagonSimpleCoefficients) || !super.equals(other)) {
      return false;
    }

    OctagonSimpleCoefficients oct = (OctagonSimpleCoefficients) other;

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
      if (coefficients[i] instanceof OctagonDoubleValue) {
        manager.num_set_float(arr, i, coefficients[i].getValue().doubleValue());
      } else {
        manager.num_set_int(arr, i, coefficients[i].getValue().longValue());
      }
    }
    return arr;
  }

}

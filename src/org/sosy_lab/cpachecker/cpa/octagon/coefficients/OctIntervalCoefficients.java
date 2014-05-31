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
public class OctIntervalCoefficients extends AOctCoefficients {

  protected OctInterval[] coefficients;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   */
  public OctIntervalCoefficients(int size, OctState oct) {
    super(size, oct);
    coefficients = new OctInterval[size+1];
    Arrays.fill(coefficients, OctInterval.FALSE);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctIntervalCoefficients(int size, int index, OctInterval bounds, OctState oct) {
    super(size, oct);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new OctInterval[size+1];
    Arrays.fill(coefficients, OctInterval.FALSE);
    coefficients[index] = bounds;
  }


  /**
   * Create new Coefficients for #size variables. With a constant value.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctIntervalCoefficients(int size, OctInterval bounds, OctState oct) {
    super(size, oct);
    coefficients = new OctInterval[size+1];
    Arrays.fill(coefficients, OctInterval.FALSE);
    coefficients[size] = bounds;
  }

  @Override
  public OctIntervalCoefficients expandToSize(int size, OctState oct) {
    Preconditions.checkArgument(this.size <= size, "new size too small");

    if (this.size == size) {
      return this;
    }

    OctIntervalCoefficients newCoeffs = new OctIntervalCoefficients(size, oct);

    for (int i = 0; i < coefficients.length-1; i++) {
      newCoeffs.coefficients[i] = coefficients[i];
    }
    newCoeffs.coefficients[newCoeffs.coefficients.length-1] = coefficients[coefficients.length-1];

    return newCoeffs;
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
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < other.size; i++) {
      ret.coefficients[i] = coefficients[i].plus(new OctInterval(other.coefficients[i]));
    }
    return ret;
  }

  private IOctCoefficients add(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].plus(other.coefficients[i]);
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
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      ret.coefficients[i] = coefficients[i].minus(new OctInterval(other.coefficients[i]));
    }
    return ret;
  }

  private IOctCoefficients sub(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      ret.coefficients[i] = coefficients[i].minus(other.coefficients[i]);
    }
    return ret;
  }

  @Override
  protected IOctCoefficients mulInner(IOctCoefficients other) {
    assert hasOnlyOneValue();

    int index = 0;
    OctInterval bounds = null;
    while (index < coefficients.length) {
      bounds = coefficients[index];
      if (!bounds.equals(OctInterval.FALSE)) {
        break;
      }
      index++;
    }

    OctInterval infBounds;

    // this is a constant value
    if (index >= oct.sizeOfVariables()) {
      return other.mul(bounds);

    } else {
      infBounds = oct.getVariableBounds(index);
    }

    if (infBounds.isInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    return other.mul(bounds.times(infBounds));
  }

  @Override
  public IOctCoefficients mul(OctNumericValue factor) {
    return mul(new OctInterval(factor));
  }

  @Override
  public IOctCoefficients mul(OctInterval interval) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
        ret.coefficients[i] = coefficients[i].times(interval);
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
      if (!value.equals(OctIntValue.ZERO)) {
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

    return div(bounds.times(new OctInterval(value)));
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
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      ret.coefficients[i] = coefficients[i].divide(new OctInterval(pDivisor));
    }
    return ret;
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
    for (int i = 0; i < size; i++) {
      ret.coefficients[i] = coefficients[i].divide(interval);
    }

    return ret;
  }

  public OctIntervalCoefficients withConstantValue(OctInterval bounds) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    ret.coefficients = Arrays.copyOf(coefficients, coefficients.length);
    ret.coefficients[coefficients.length-1] = bounds;
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length - 1; i++) {
      if (!coefficients[i].equals(OctInterval.FALSE)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int getVariableIndex() {
    assert hasOnlyOneValue() && !hasOnlyConstantValue() : "is no variable!";
    int counter = 0;
    while (counter < size && coefficients[counter].equals(OctInterval.FALSE)) {
      counter++;
    }
    return counter;
  }

  public OctInterval getConstantValue() {
    return coefficients[coefficients.length-1];
  }

  @Override
  public boolean hasOnlyOneValue() {
    boolean foundValue = false;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].equals(OctInterval.FALSE)) {
        if (foundValue) {
          return false;
        }
        foundValue = true;
      }
    }
    return true;
  }

  public static OctIntervalCoefficients getNondetUIntCoeffs(int size, OctState oct) {
    OctIntervalCoefficients result = new OctIntervalCoefficients(size, oct);
    result.coefficients[result.coefficients.length-1] = new OctInterval(0, Double.POSITIVE_INFINITY);
    return result;
  }

  public static OctIntervalCoefficients getNondetBoolCoeffs(int size, OctState oct) {
    OctIntervalCoefficients result = new OctIntervalCoefficients(size, oct);
    result.coefficients[result.coefficients.length-1] = new OctInterval(0, 1);
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(coefficients);
    result = prime * result + Objects.hash(size);
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof OctIntervalCoefficients) && !super.equals(other)) {
      return false;
    }

    OctIntervalCoefficients oct = (OctIntervalCoefficients) other;

    return Arrays.equals(coefficients, oct.coefficients) && size == oct.size;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < coefficients.length - 1; i++) {
      String a, b;
      if (coefficients[i].getLow().isInfinite()) {
        a = "INFINITY";
      } else {
        a = coefficients[i].getLow().toString();
      }
      if (coefficients[i].getHigh().isInfinite()) {
        b = "INFINITY";
      } else {
        b = coefficients[i].getHigh().toString();
      }
      if ((i+1)/2 < size) {
        builder.append(oct.getVariableToIndexMap().inverse().get((i+1)/2) + " -> [" + a + ", " + b + "]\n");
      } else {
        builder.append("CONSTANT_VAL -> [" + a + ", " + b + "]\n");
      }

    }
    return builder.toString();
  }

  @Override
  public NumArray getNumArray(OctagonManager manager) {
    NumArray arr = manager.init_num_t(coefficients.length * 2);
    for (int i = 0; i < coefficients.length; i++) {
      OctNumericValue low = coefficients[i].getLow();
      OctNumericValue high = coefficients[i].getHigh();

      if (low.isInfinite()) {
        manager.num_set_inf(arr, i*2+1);
      } else {
        if (low instanceof OctDoubleValue) {
          manager.num_set_float(arr, i*2+1, low.getValue().doubleValue()*-1);
        } else {
          manager.num_set_int(arr, i*2+1, low.getValue().longValue()*-1);
        }
      }

      if (high.isInfinite()) {
        manager.num_set_inf(arr, i*2);
      } else {
        if (high instanceof OctDoubleValue) {
          manager.num_set_float(arr, i*2, high.getValue().doubleValue());
        } else {
          manager.num_set_int(arr, i*2, high.getValue().longValue());
        }
      }
    }
    return arr;
  }
}
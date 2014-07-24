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
public class OctagonIntervalCoefficients extends AOctagonCoefficients {

  protected OctagonInterval[] coefficients;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   */
  public OctagonIntervalCoefficients(int size, OctagonState oct) {
    super(size, oct);
    coefficients = new OctagonInterval[size+1];
    Arrays.fill(coefficients, OctagonInterval.FALSE);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctagonIntervalCoefficients(int size, int index, OctagonInterval bounds, OctagonState oct) {
    super(size, oct);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new OctagonInterval[size+1];
    Arrays.fill(coefficients, OctagonInterval.FALSE);
    coefficients[index] = bounds;
  }


  /**
   * Create new Coefficients for #size variables. With a constant value.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctagonIntervalCoefficients(int size, OctagonInterval bounds, OctagonState oct) {
    super(size, oct);
    coefficients = new OctagonInterval[size+1];
    Arrays.fill(coefficients, OctagonInterval.FALSE);
    coefficients[size] = bounds;
  }

  @Override
  public OctagonIntervalCoefficients expandToSize(int size, OctagonState oct) {
    Preconditions.checkArgument(this.size <= size, "new size too small");

    if (this.size == size) {
      return this;
    }

    OctagonIntervalCoefficients newCoeffs = new OctagonIntervalCoefficients(size, oct);

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
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].plus(new OctagonInterval(other.coefficients[i]));
    }
    return ret;
  }

  private IOctagonCoefficients add(OctagonIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].plus(other.coefficients[i]);
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
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].minus(new OctagonInterval(other.coefficients[i]));
    }
    return ret;
  }

  private IOctagonCoefficients sub(OctagonIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].minus(other.coefficients[i]);
    }
    return ret;
  }

  @Override
  protected IOctagonCoefficients mulInner(IOctagonCoefficients other) {
    assert hasOnlyOneValue();

    int index = 0;
    OctagonInterval bounds = null;
    while (index < coefficients.length) {
      bounds = coefficients[index];
      if (!bounds.equals(OctagonInterval.FALSE)) {
        break;
      }
      index++;
    }

    OctagonInterval infBounds;

    // this is a constant value
    if (index >= oct.sizeOfVariables()) {
      return other.mul(bounds);

    } else {
      infBounds = oct.getVariableBounds(index);
    }

    if (infBounds.isInfinite()) {
      return OctagonUniversalCoefficients.INSTANCE;
    }

    return other.mul(bounds.times(infBounds));
  }

  @Override
  public IOctagonCoefficients mul(OctagonNumericValue factor) {
    return mul(new OctagonInterval(factor));
  }

  @Override
  public IOctagonCoefficients mul(OctagonInterval interval) {
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
        ret.coefficients[i] = coefficients[i].times(interval);
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

    return div(bounds.times(new OctagonInterval(value)));
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
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].divide(new OctagonInterval(pDivisor));
    }
    return ret;
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
      ret.coefficients[i] = coefficients[i].divide(interval);
    }

    return ret;
  }

  public OctagonIntervalCoefficients withConstantValue(OctagonInterval bounds) {
    OctagonIntervalCoefficients ret = new OctagonIntervalCoefficients(size, oct);
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
      if (!coefficients[i].equals(OctagonInterval.FALSE)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int getVariableIndex() {
    assert hasOnlyOneValue() && !hasOnlyConstantValue() : "is no variable!";
    int counter = 0;
    while (counter < size && coefficients[counter].equals(OctagonInterval.FALSE)) {
      counter++;
    }
    return counter;
  }

  public OctagonInterval getConstantValue() {
    return coefficients[coefficients.length-1];
  }

  @Override
  public boolean hasOnlyOneValue() {
    boolean foundValue = false;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].equals(OctagonInterval.FALSE)) {
        if (foundValue) {
          return false;
        }
        foundValue = true;
      }
    }
    return true;
  }

  public static OctagonIntervalCoefficients getNondetUIntCoeffs(int size, OctagonState oct) {
    OctagonIntervalCoefficients result = new OctagonIntervalCoefficients(size, oct);
    result.coefficients[result.coefficients.length-1] = new OctagonInterval(0, Double.POSITIVE_INFINITY);
    return result;
  }

  public static OctagonIntervalCoefficients getNondetBoolCoeffs(int size, OctagonState oct) {
    OctagonIntervalCoefficients result = new OctagonIntervalCoefficients(size, oct);
    result.coefficients[result.coefficients.length-1] = new OctagonInterval(0, 1);
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

    if (!(other instanceof OctagonIntervalCoefficients) || !super.equals(other)) {
      return false;
    }

    OctagonIntervalCoefficients oct = (OctagonIntervalCoefficients) other;

    return Arrays.equals(coefficients, oct.coefficients) && size == oct.size;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < coefficients.length; i++) {
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
      if (i < size) {
        builder.append(oct.getVariableToIndexMap().inverse().get(i) + " -> [" + a + ", " + b + "]\n");
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
      OctagonNumericValue low = coefficients[i].getLow();
      OctagonNumericValue high = coefficients[i].getHigh();

      if (low.isInfinite()) {
        manager.num_set_inf(arr, i*2+1);
      } else {
        if (low instanceof OctagonDoubleValue) {
          manager.num_set_float(arr, i*2+1, low.getValue().doubleValue()*-1);
        } else {
          manager.num_set_int(arr, i*2+1, low.getValue().longValue()*-1);
        }
      }

      if (high.isInfinite()) {
        manager.num_set_inf(arr, i*2);
      } else {
        if (high instanceof OctagonDoubleValue) {
          manager.num_set_float(arr, i*2, high.getValue().doubleValue());
        } else {
          manager.num_set_int(arr, i*2, high.getValue().longValue());
        }
      }
    }
    return arr;
  }
}
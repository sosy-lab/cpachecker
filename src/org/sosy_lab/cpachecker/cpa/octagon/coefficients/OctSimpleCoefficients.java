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
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.base.Preconditions;


public class OctSimpleCoefficients extends AOctCoefficients {

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   */
  public OctSimpleCoefficients(int size, OctState oct) {
    super(size, oct);
    coefficients = new OctNumericValue[size+1];
    Arrays.fill(coefficients, OctNumericValue.ZERO);
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
    Arrays.fill(coefficients, OctNumericValue.ZERO);
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
    Arrays.fill(coefficients, OctNumericValue.ZERO);
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
    for (int i = 0; i <= size; i++) {
      octCoeffs.coefficients[2*i] = coefficients[i];
      octCoeffs.coefficients[2*i+1] = coefficients[i];
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
      ret.coefficients[i*2] = other.coefficients[i*2].add(coefficients[i]);
      ret.coefficients[i*2+1] = other.coefficients[i*2+1].add(coefficients[i]);
      ret.isInfite[i*2] = other.isInfite[i*2];
      ret.isInfite[i*2+1] = other.isInfite[i*2+1];
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
      ret.coefficients[i*2] = coefficients[i].subtract(other.coefficients[i*2+1]);
      ret.coefficients[i*2+1] = coefficients[i].subtract(other.coefficients[i*2]);
      ret.isInfite[i*2] = other.isInfite[i*2+1];
      ret.isInfite[i*2+1] = other.isInfite[i*2];
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
      if (!coefficients[i].equals(OctNumericValue.ZERO)) { return false; }
    }
    return true;
  }

  public boolean isVariable() {
    int counter = 0;
    while (counter < size && coefficients[counter].equals(OctNumericValue.ZERO)) {
      counter++;
    }
    counter++;
    while (counter < size && coefficients[counter].equals(OctNumericValue.ZERO)) {
      counter++;
    }
    if (counter == size && coefficients[counter].equals(OctNumericValue.ZERO)) {
      return true;
    } else {
      return false;
    }
  }

  public int getVariableIndex() {
    assert isVariable() : "is no variable!";
    int counter = 0;
    while (counter < size && coefficients[counter].equals(OctNumericValue.ZERO)) {
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
    result.coefficients[size] = OctNumericValue.ONE;
    return result;
  }

  public static OctSimpleCoefficients getBoolFALSECoeffs(int size, OctState oct) {
    OctSimpleCoefficients result = new OctSimpleCoefficients(size, oct);
    result.coefficients[size] = OctNumericValue.ZERO;
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof OctSimpleCoefficients) && !super.equals(other)) {
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
      if (coefficients[i].isFloat()) {
        manager.num_set_float(arr, i, coefficients[i].getFloatVal().doubleValue());
      } else {
        manager.num_set_int(arr, i, coefficients[i].getIntVal().intValue());
      }
    }
    return arr;
  }
}

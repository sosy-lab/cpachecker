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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.util.octagon.InfinityNumericWrapper;
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

  @Override
  protected IOctCoefficients mulInner(IOctCoefficients other) {
    assert hasOnlyOneValue();

    int index = 0;
    OctNumericValue value = null;
    while (index < coefficients.length) {
      value = coefficients[index];
      if (!value.equals(OctNumericValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index == oct.sizeOfVariables()) {
      return other.mul(value);

      // this is a constant value which is ZERO
    } else if (index > oct.sizeOfVariables()) {
      return other.mul(value);
    }

    Pair<InfinityNumericWrapper, InfinityNumericWrapper> bounds = oct.getVariableBounds(index);
    if (bounds.getFirst().isNegativeInfinite() || bounds.getSecond().isPositiveInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.getFirst().equals(bounds.getSecond())) {
      return other.mul(bounds.getFirst().getValue().mul(value));
    } else {
      OctNumericValue bound1 = bounds.getFirst().getValue().mul(value);
      OctNumericValue bound2 = bounds.getSecond().getValue().mul(value);
      return other.mul(new InfinityNumericWrapper(bound1), new InfinityNumericWrapper(bound2));
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
  public IOctCoefficients mul(InfinityNumericWrapper bound1, InfinityNumericWrapper bound2) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      InfinityNumericWrapper newBound1 = bound1.mul(coefficients[i]);
      InfinityNumericWrapper newBound2 = bound2.mul(coefficients[i]);
      Pair<InfinityNumericWrapper, InfinityNumericWrapper> maxBounds = getMaxBounds(newBound1,
                                                                                    newBound2);

      if (maxBounds.getSecond().isPositiveInfinite()) {
        ret.coefficients[i*2] = OctNumericValue.ZERO;
        ret.isInfite[i*2] = true;
      } else {
        ret.coefficients[i*2] = maxBounds.getSecond().getValue();
        ret.isInfite[i*2] = false;
      }

      if (maxBounds.getFirst().isNegativeInfinite()) {
        ret.coefficients[i*2+1] = OctNumericValue.ZERO;
        ret.isInfite[i*2] = true;
      } else {
        ret.coefficients[i*2+1] = maxBounds.getFirst().getValue();
        ret.isInfite[i*2] = false;
      }

    }
    return ret;
  }

  private Pair<InfinityNumericWrapper, InfinityNumericWrapper> getMaxBounds(InfinityNumericWrapper... vals) {
    InfinityNumericWrapper lowest = vals[0];
    InfinityNumericWrapper highest = vals[0];
    for (int i = 1; i < vals.length; i++) {
      if (vals[i].isNegativeInfinite()) {
        lowest = vals[i];
      } else if (vals[i].isPositiveInfinite()) {
        highest = vals[i];
      } else if (!lowest.isNegativeInfinite()) {
        if (lowest.getValue().greaterThan(vals[i].getValue())) {
          lowest = vals[i];
        }
        if (highest.getValue().lessThan(vals[i].getValue())) {
          highest = vals[i];
        }
      }
    }
    return Pair.of(lowest, highest);
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
      if (!value.equals(OctNumericValue.ZERO)) {
        break;
      }
      index++;
    }

    // this is a constant value
    if (index == coeffs.oct.sizeOfVariables()) {
      return mul(value);

      // this is a constant value which is ZERO
    } else if (index > oct.sizeOfVariables()) {
      throw new ArithmeticException("Division by zero");
    }

    Pair<InfinityNumericWrapper, InfinityNumericWrapper> bounds = coeffs.oct.getVariableBounds(index);
    if (bounds.getFirst().isNegativeInfinite() || bounds.getSecond().isPositiveInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.getFirst().getValue().equals(OctNumericValue.ZERO) || bounds.getSecond().getValue().equals(OctNumericValue.ZERO)) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (bounds.getFirst().equals(bounds.getSecond())) {
      return div(bounds.getFirst().getValue().mul(value));
    } else {
      OctNumericValue bound1 = bounds.getFirst().getValue().mul(value);
      OctNumericValue bound2 = bounds.getSecond().getValue().mul(value);
      return div(new InfinityNumericWrapper(bound1), new InfinityNumericWrapper(bound2));
    }
  }

  private IOctCoefficients divInner(OctIntervalCoefficients coeffs) {
    assert coeffs.hasOnlyOneValue();

    int index = 0;
    OctNumericValue bound1 = null;
    OctNumericValue bound2 = null;
    while (index * 2 < coeffs.coefficients.length) {
      bound1 = coeffs.coefficients[index * 2];
      bound2 = coeffs.coefficients[(index * 2) + 1];
      if (!coeffs.coefficients[index * 2].equals(OctNumericValue.ZERO)
          || !coeffs.coefficients[(index * 2)+1].equals(OctNumericValue.ZERO)
          || coeffs.isInfite[index * 2] || coeffs.isInfite[index * 2 + 1]) {
        break;
      }
      index++;
    }

    Pair<InfinityNumericWrapper, InfinityNumericWrapper> infBounds;

    // this is a constant value
    if (index == coeffs.oct.sizeOfVariables()) {
      InfinityNumericWrapper lowerBound;
      InfinityNumericWrapper upperBound;

      if (coeffs.isInfite[index * 2 + 1]) {
        lowerBound = new InfinityNumericWrapper(Double.NEGATIVE_INFINITY);
      } else {
        lowerBound = new InfinityNumericWrapper(bound2);
      }

      if (coeffs.isInfite[index * 2]) {
        upperBound = new InfinityNumericWrapper(Double.POSITIVE_INFINITY);
      } else {
        upperBound = new InfinityNumericWrapper(bound1);
      }

      infBounds = Pair.of(lowerBound, upperBound);

     // this is a constant value which is in the interval [0,0]
    } else if (index > coeffs.oct.sizeOfVariables()) {
      throw new ArithmeticException("Division by zero");

    } else {
      infBounds = coeffs.oct.getVariableBounds(index);
    }



    if (infBounds.getFirst().isNegativeInfinite() || infBounds.getSecond().isPositiveInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    boolean varBoundsEqual = bound1.equals(bound2);
    boolean valueBoundsEqual = infBounds.getFirst().equals(infBounds.getSecond());

    // value bounds can only be equal if there is a value and not only infininty
    if (valueBoundsEqual && varBoundsEqual) {
      return div(infBounds.getFirst().getValue().mul(bound1));
    } else if (valueBoundsEqual) {
      return div(infBounds.getFirst().mul(bound2), infBounds.getFirst().mul(bound1));

    } else if (varBoundsEqual) {
      return div(infBounds.getFirst().mul(bound1), infBounds.getSecond().mul(bound1));

    } else {
      InfinityNumericWrapper newBound1 = infBounds.getFirst().mul(bound1);
      InfinityNumericWrapper newBound2 = infBounds.getFirst().mul(bound2);
      InfinityNumericWrapper newBound3 = infBounds.getSecond().mul(bound1);
      InfinityNumericWrapper newBound4 = infBounds.getSecond().mul(bound2);
      Pair<InfinityNumericWrapper, InfinityNumericWrapper> maxBounds = getMaxBounds(newBound1,
                                                                                    newBound2,
                                                                                    newBound3,
                                                                                    newBound4);
      return div(maxBounds.getFirst(), maxBounds.getSecond());
    }
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
  public IOctCoefficients div(InfinityNumericWrapper lowerBound, InfinityNumericWrapper upperBound) {
    if (lowerBound.isNegativeInfinite() || lowerBound.isPositiveInfinite() || upperBound.isNegativeInfinite() || upperBound.isPositiveInfinite()) {
      return OctEmptyCoefficients.INSTANCE;
    }

    OctNumericValue val1 = lowerBound.getValue();
    OctNumericValue val2 = upperBound.getValue();
    assert !val1.equals(val2);

    // TODO make configurable
    if (val1.isInInterval(-0.1, 0.1) || val2.isInInterval(-0.1, 0.1)) {
      return OctEmptyCoefficients.INSTANCE;
    }

    if (!val1.greaterEqual(new OctNumericValue(0.1))
        && !val2.lessEqual(new OctNumericValue(-0.1))) {
      return OctEmptyCoefficients.INSTANCE;
    }

    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      InfinityNumericWrapper newBound1 = new InfinityNumericWrapper(coefficients[i].div(val1));
      InfinityNumericWrapper newBound2 = new InfinityNumericWrapper(coefficients[i].div(val2));
      Pair<InfinityNumericWrapper, InfinityNumericWrapper> maxBounds = getMaxBounds(newBound1,
                                                                                    newBound2);

      if (maxBounds.getSecond().isPositiveInfinite()) {
        ret.coefficients[i*2] = OctNumericValue.ZERO;
        ret.isInfite[i*2] = true;
      } else {
        ret.coefficients[i*2] = maxBounds.getSecond().getValue();
        ret.isInfite[i*2] = false;
      }

      if (maxBounds.getFirst().isNegativeInfinite()) {
        ret.coefficients[i*2+1] = OctNumericValue.ZERO;
        ret.isInfite[i*2] = true;
      } else {
        ret.coefficients[i*2+1] = maxBounds.getFirst().getValue();
        ret.isInfite[i*2] = false;
      }

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

  @Override
  public boolean hasOnlyOneValue() {
    boolean foundValue = false;
    for (int i = 0; i < coefficients.length; i++) {
      if (!coefficients[i].equals(OctNumericValue.ZERO)) {
        if (foundValue) {
          return false;
        }
        foundValue = true;
      }
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
      if (coefficients[i].isFloat()) {
        manager.num_set_float(arr, i, coefficients[i].getFloatVal().doubleValue());
      } else {
        manager.num_set_int(arr, i, coefficients[i].getIntVal().intValue());
      }
    }
    return arr;
  }

}

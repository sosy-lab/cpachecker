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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.base.Preconditions;


public class OctIntervalCoefficients extends AOctCoefficients {


  protected boolean[] isInfite;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   */
  public OctIntervalCoefficients(int size, OctState oct) {
    super(size, oct);
    coefficients = new BigInteger[(size+1)*2];
    isInfite = new boolean[(size+1)*2];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctIntervalCoefficients(int size, int index, long lowerBound, long upperBound, boolean lowerInfinite, boolean upperInfinite, OctState oct) {
    super(size, oct);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new BigInteger[(size+1)*2];
    isInfite = new boolean[(size+1)*2];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
    coefficients[index*2] = BigInteger.valueOf(upperBound);
    coefficients[index*2+1] = BigInteger.valueOf(lowerBound);
    isInfite[index*2] = upperInfinite;
    isInfite[index*2 +1] = lowerInfinite;
  }


  /**
   * Create new Coefficients for #size variables. With a constant value.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctIntervalCoefficients(int size, long lowerBound, long upperBound, boolean lowerInfinite, boolean upperInfinite, OctState oct) {
    super(size, oct);
    coefficients = new BigInteger[(size+1)*2];
    isInfite = new boolean[(size+1)*2];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
    coefficients[size*2] = BigInteger.valueOf(upperBound);
    coefficients[size*2 +1] = BigInteger.valueOf(lowerBound);
    isInfite[size*2] = upperInfinite;
    isInfite[size*2 +1] = lowerInfinite;
  }

  @Override
  public OctIntervalCoefficients expandToSize(int size, OctState oct) {
    Preconditions.checkArgument(this.size <= size, "new size too small");

    if (this.size == size) {
      return this;
    }

    OctIntervalCoefficients newCoeffs = new OctIntervalCoefficients(size, oct);

    for (int i = 0; i < coefficients.length-2; i++) {
      newCoeffs.coefficients[i] = coefficients[i];
      newCoeffs.isInfite[i] = isInfite[i];
    }
    newCoeffs.coefficients[newCoeffs.coefficients.length-2] = coefficients[coefficients.length-2];
    newCoeffs.coefficients[newCoeffs.coefficients.length-1] = coefficients[coefficients.length-1];
    newCoeffs.isInfite[newCoeffs.isInfite.length-2] = isInfite[isInfite.length-2];
    newCoeffs.isInfite[newCoeffs.isInfite.length-1] = isInfite[isInfite.length-1];

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
      ret.coefficients[i*2] = coefficients[i*2].add(other.coefficients[i]);
      ret.coefficients[i*2+2] = coefficients[i*2+1].add(other.coefficients[i]);
      ret.isInfite[i*2] = isInfite[i*2];
      ret.isInfite[i*2+2] = isInfite[i*2+1];
    }
    return ret;
  }

  private IOctCoefficients add(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].add(other.coefficients[i]);
      ret.isInfite[i] = isInfite[i] || other.isInfite[i];
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
      ret.coefficients[i*2] = coefficients[i*2].subtract(other.coefficients[i]);
      ret.coefficients[i*2+2] = coefficients[i*2+1].subtract(other.coefficients[i]);
      ret.isInfite[i*2] = isInfite[i*2];
      ret.isInfite[i*2+1] = isInfite[i*2+1];
    }
    return ret;
  }

  private IOctCoefficients sub(OctIntervalCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    for (int i = 0; i < size; i++) {
      ret.coefficients[i*2] = coefficients[i*2].subtract(other.coefficients[i*2+1]);
      ret.coefficients[i*2+1] = coefficients[i*2+1].subtract(other.coefficients[i*2]);
      ret.isInfite[i*2] = isInfite[i*2] || other.isInfite[i*2+1];
      ret.isInfite[i*2+1] = isInfite[i*2+1] || other.isInfite[i*2];
    }
    return ret;
  }

  /**
   * Returns the coefficients (lowerBount, upperBound) at the given index.
   */
  public Pair<Pair<BigInteger, Boolean>, Pair<BigInteger, Boolean>> get(int index) {
    Preconditions.checkArgument(index < size, "Index too big");
    return Pair.of(Pair.of(coefficients[index+1], isInfite[index+1]), Pair.of(coefficients[index], isInfite[index]));
  }

  public Pair<Pair<BigInteger, Boolean>, Pair<BigInteger, Boolean>> getConstantValue() {
    return Pair.of(Pair.of(coefficients[coefficients.length-1], isInfite[coefficients.length-1]), Pair.of(coefficients[coefficients.length-2], isInfite[coefficients.length-2]));
  }

  public OctIntervalCoefficients withConstantValue(long lowerBound, long upperBound, boolean lowerInfinite, boolean upperInfinite) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size, oct);
    ret.coefficients = Arrays.copyOf(coefficients, coefficients.length);
    ret.isInfite = Arrays.copyOf(isInfite, isInfite.length);
    ret.coefficients[coefficients.length-2] = BigInteger.valueOf(upperBound);
    ret.isInfite[isInfite.length-2] = upperInfinite;
    ret.coefficients[coefficients.length-1] = BigInteger.valueOf(lowerBound);
    ret.isInfite[isInfite.length-1] = lowerInfinite;
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length - 2; i++) {
      if (!coefficients[i].equals(BigInteger.ZERO)) { return false; }
    }
    return true;
  }

  public static OctIntervalCoefficients getNondetUIntCoeffs(int size, OctState oct) {
    OctIntervalCoefficients result = new OctIntervalCoefficients(size, oct);
    result.isInfite[result.coefficients.length-2] = true;
    result.coefficients[result.coefficients.length-1] = BigInteger.ZERO;
    return result;
  }

  public static OctIntervalCoefficients getNondetBoolCoeffs(int size, OctState oct) {
    OctIntervalCoefficients result = new OctIntervalCoefficients(size, oct);
    result.coefficients[result.coefficients.length-2] = BigInteger.ONE;
    result.coefficients[result.coefficients.length-1] = BigInteger.ZERO;
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(coefficients);
    result = prime * result + Objects.hash(isInfite);
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

    return Arrays.equals(coefficients, oct.coefficients) && Arrays.equals(isInfite, oct.isInfite) && size == oct.size;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < coefficients.length - 1; i += 2) {
      String a, b;
      if (isInfite[i+1]) {
        a = "INFINITY";
      } else {
        a = coefficients[i+1].toString();
      }
      if (isInfite[i]) {
        b = "INFINITY";
      } else {
        b = coefficients[i].toString();
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
  public NumArray getNumArray() {
    NumArray arr = OctagonManager.init_num_t(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      if (i % 2 == 0) {
        if (isInfite[i]) {
          OctagonManager.num_set_inf(arr, i);
        } else {
          OctagonManager.num_set_int(arr, i, coefficients[i].intValue());
        }
      } else {
        if (isInfite[i]) {
          OctagonManager.num_set_inf(arr, i);
        } else {
          OctagonManager.num_set_int(arr, i, coefficients[i].intValue()*-1);
        }
      }
    }
    return arr;
  }
}
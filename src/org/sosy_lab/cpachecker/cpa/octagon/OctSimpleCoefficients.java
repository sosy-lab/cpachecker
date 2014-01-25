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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.math.BigInteger;
import java.util.Arrays;

import com.google.common.base.Preconditions;


public class OctSimpleCoefficients extends AOctCoefficients {

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   */
  public OctSimpleCoefficients(int size) {
    super(size);
    coefficients = new BigInteger[size+1];
    isInfite = new boolean[size+1];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctSimpleCoefficients(int size, int index, long value) {
    super(size);
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients = new BigInteger[size+1];
    isInfite = new boolean[size+1];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
    coefficients[index] = BigInteger.valueOf(value);
  }

  /**
   * Create new Coefficients for #size variables. With a constant value
   *
   * @param size The size of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctSimpleCoefficients(int size, long value) {
    super(size);
    coefficients = new BigInteger[size+1];
    isInfite = new boolean[size+1];
    Arrays.fill(coefficients, BigInteger.ZERO);
    Arrays.fill(isInfite, false);
    coefficients[size] = BigInteger.valueOf(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients add(IOctCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    if (other instanceof OctSimpleCoefficients) {
      return add((OctSimpleCoefficients)other);
    } else if (other instanceof OctIntervalCoefficients) {
      return add((OctIntervalCoefficients)other);
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctCoefficients add(OctSimpleCoefficients other) {
    OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].add(other.coefficients[i]);
    }
    return ret;
  }

  private IOctCoefficients add(OctIntervalCoefficients other) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i*2] = other.coefficients[i*2].add(coefficients[i]);
      ret.coefficients[(i*2)+1] = other.coefficients[(i*2)+1].add(coefficients[i]);
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients sub(IOctCoefficients other) {
    Preconditions.checkArgument(other.size() == size, "Different size of coefficients.");
    if (other instanceof OctSimpleCoefficients) {
      return sub((OctSimpleCoefficients)other);
    } else if (other instanceof OctIntervalCoefficients) {
      return sub((OctIntervalCoefficients)other);
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  private IOctCoefficients sub(OctSimpleCoefficients other) {
    OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].subtract(other.coefficients[i]);
    }
    return ret;
  }

  private IOctCoefficients sub(OctIntervalCoefficients other) {
    OctIntervalCoefficients ret = new OctIntervalCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i*2] = coefficients[i].subtract(other.coefficients[i*2]);
      ret.coefficients[(i*2)+1] = coefficients[i].subtract(other.coefficients[(i*2)+1]);
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients mult(long coeff) {
    OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].multiply(BigInteger.valueOf(coeff));
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients div(long coeff) {
    if (coeff == 0) {
      return null;
    }

    OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i].divide(BigInteger.valueOf(coeff));
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients mult(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue()) {
      return oct.mult(coefficients[coefficients.length-1].longValue());
    } else if (oct.hasOnlyConstantValue() && oct instanceof OctSimpleCoefficients) {
      return mult(((OctSimpleCoefficients)oct).coefficients[((OctSimpleCoefficients)oct).coefficients.length-1].longValue());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IOctCoefficients div(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        return div(((OctSimpleCoefficients) oct).getConstantValue().longValue());
      }
    }
    return null;
  }

  @Override
  public IOctCoefficients binAnd(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].and(((OctSimpleCoefficients)oct).coefficients[size-1]);
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients binOr(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].or(((OctSimpleCoefficients)oct).coefficients[coefficients.length-1]);
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients binXOr(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].xor(((OctSimpleCoefficients)oct).coefficients[coefficients.length-1]);
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients shiftLeft(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].shiftLeft(((OctSimpleCoefficients)oct).coefficients[coefficients.length-1].intValue());
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients shiftRight(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].shiftRight(((OctSimpleCoefficients)oct).coefficients[coefficients.length-1].intValue());
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients modulo(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients && hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      OctSimpleCoefficients ret = new OctSimpleCoefficients(size);
      ret.coefficients[coefficients.length-1] =
          coefficients[coefficients.length-1].mod(((OctSimpleCoefficients)oct).coefficients[coefficients.length-1]);
      return ret;
    }
    return null;
  }

  @Override
  public IOctCoefficients greaterEq(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[coefficients.length-1]) >= 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      } else if (oct instanceof OctIntervalCoefficients) {
        OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-2]) >= 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      }
    }
    return null;
  }

  @Override
  public IOctCoefficients greater(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[coefficients.length-1]) > 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      } else if (oct instanceof OctIntervalCoefficients) {
        OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-2]) > 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      }
    }
    return null;
  }

  @Override
  public IOctCoefficients smallerEq(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[coefficients.length-1]) <= 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      } else if (oct instanceof OctIntervalCoefficients) {
        OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-1]) <= 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      }
    }
    return null;
  }

  @Override
  public IOctCoefficients smaller(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[coefficients.length-1]) < 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      } else if (oct instanceof OctIntervalCoefficients) {
        OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-1]) < 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      }
    }
    return null;
  }

  @Override
  public IOctCoefficients eq(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (oct instanceof OctSimpleCoefficients) {
      OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
      int val = 1;
      for (int i = 0; i < coefficients.length && val == 1; i++) {
        val = coefficients[i].compareTo(tmp.coefficients[i]) == 0 ? 1 : 0;
      }
      return new OctSimpleCoefficients(size, val);
    } else if (oct instanceof OctIntervalCoefficients) {
      OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
      int val = 1;
      for (int i = 0; i < coefficients.length && val == 1; i++) {
        val = coefficients[i].compareTo(tmp.coefficients[i*2]) == 0 ? 1 : 0;
        if (val == 1) {
          val = coefficients[i].compareTo(tmp.coefficients[(i*2)+1]) == 0 ? 1 : 0;
        }
      }
      return new OctSimpleCoefficients(size, val);
    }
    return null;
  }

  @Override
  public IOctCoefficients ineq(IOctCoefficients oct) {
    Preconditions.checkArgument(oct.size() == size, "Different size of coefficients.");
    if (hasOnlyConstantValue() && oct.hasOnlyConstantValue()) {
      if (oct instanceof OctSimpleCoefficients) {
        OctSimpleCoefficients tmp = (OctSimpleCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[coefficients.length-1]) != 0 ? 1 : 0;
        return new OctSimpleCoefficients(size, val);
      } else if (oct instanceof OctIntervalCoefficients) {
        OctIntervalCoefficients tmp = (OctIntervalCoefficients) oct;
        int val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-1]) < 0 ? 1 : 0;
        if (val == 0) {
          val = coefficients[coefficients.length-1].compareTo(tmp.coefficients[tmp.coefficients.length-2]) > 0 ? 1 : 0;
        }
        return new OctSimpleCoefficients(size, val);
      }
    }
    return null;
  }

  /**
   * Sets the coefficient to a given value at a certain index.
   */
  public void set(int index, long value) {
    Preconditions.checkArgument(index < size, "Index too big");
    coefficients[index] = BigInteger.valueOf(value);
    isInfite[index] = false;
  }

  public void setConstantValue(long value) {
    coefficients[size] = BigInteger.valueOf(value);
    isInfite[size] = false;
  }

  /**
   * Returns the coefficient at the given index.
   */
  public BigInteger get(int index) {
    Preconditions.checkArgument(index < size, "Index too big");
    return coefficients[index];
  }

  public BigInteger getConstantValue() {
    return coefficients[size];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length - 1; i++) {
      if (!coefficients[i].equals(BigInteger.ZERO)) { return false; }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < coefficients.length; i++) {
      String tmp;
      if (isInfite[i]) {
        tmp ="INFINITY";
      } else {
        tmp = coefficients[i].toString();
      }
      builder.append(i + " -> " + tmp + "\n");
    }
    return builder.toString();
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Arrays;

import org.sosy_lab.cpachecker.util.octagon.NumArray;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

import com.google.common.base.Preconditions;

/**
 * Class for representing Coeffecients which show the value of a variable
 * dependant on all other variables and a constant value.
 */
public class OctCoefficients {

  private int[] coefficients;

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   */
  public OctCoefficients(int size) {
    coefficients = new int[size];
    Arrays.fill(coefficients, 0);
  }

  /**
   * Create new Coefficients for #size variables.
   *
   * @param size The amount of variables for which coefficients should be stored
   * @param index The index of the variable which should be set by default to a given value
   * @param value The value to which the variable should be set
   */
  public OctCoefficients(int size, int index, int value) {
    coefficients = new int[size];
    Arrays.fill(coefficients, 0);
    coefficients[index] = value;
  }

  /**
   * Sets the coefficient to a given value at a certain index.
   */
  public void set(int index, int value) {
    coefficients[index] = value;
  }

  /**
   * Returns the coefficient at the given index.
   */
  public int get(int index) {
    return coefficients[index];
  }

  /**
   * Creates a NumArray out of the coefficient array.
   */
  public NumArray getNumArray() {
    NumArray arr = OctagonManager.init_num_t(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      OctagonManager.num_set_int(arr, i, coefficients[i]);
    }
    return arr;
  }

  /**
   * Returns the size of the coefficient list.
   */
  public int size() {
    return coefficients.length;
  }

  /**
   * Adds two OctCoefficients.
   * @return The new added Coefficient.
   */
  public OctCoefficients add(OctCoefficients other) {
    Preconditions.checkArgument(other.coefficients.length == coefficients.length, "Different amount of coefficients.");
    OctCoefficients ret = new OctCoefficients(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i] + other.coefficients[i];
    }
    return ret;
  }

  /**
   * Substracts two OctCoefficients.
   * @return The new substracted Coefficient.
   */
  public OctCoefficients sub(OctCoefficients other) {
    Preconditions.checkArgument(other.coefficients.length == coefficients.length, "Different amount of coefficients.");
    OctCoefficients ret = new OctCoefficients(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i] - other.coefficients[i];
    }
    return ret;
  }

  /**
   * Multiplies an OctCoefficient with a constant value.
   * @return The new scaled Coefficient.
   */
  public OctCoefficients mult(int coeff) {
    OctCoefficients ret = new OctCoefficients(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i] * coeff;
    }
    return ret;
  }

  /**
   * Divides an OctCoefficient with a constant value.
   * @return The new scaled Coefficient.
   */
  public OctCoefficients div(int coeff) {
    // TODO check if there should be something else than integer divison
    OctCoefficients ret = new OctCoefficients(coefficients.length);
    for (int i = 0; i < coefficients.length; i++) {
      ret.coefficients[i] = coefficients[i] / coeff;
    }
    return ret;
  }

  /**
   * Indicates whether the Coefficient List only consists of a constant value.
   */
  public boolean hasOnlyConstantValue() {
    for (int i = 0; i < coefficients.length; i++) {
      if (coefficients[i] != 0) { return false; }
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < coefficients.length; i++) {
      builder.append(i + " -> " + coefficients[i] + "\n");
    }
    return builder.toString();
  }
}

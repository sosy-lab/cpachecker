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

import org.sosy_lab.cpachecker.util.octagon.NumArray;

/**
 * Class for representing Coeffecients which show the value of a variable
 * dependant on all other variables and a constant value.
 */
interface IOctCoefficients {

  /**
   * Creates a NumArray out of the coefficient array.
   */
  public abstract NumArray getNumArray();

  /**
   * Returns the size of the coefficient list.
   */
  public abstract int size();

  public abstract IOctCoefficients fitToSize(int size);

  /**
   * Adds two OctCoefficients.
   * @return The new added Coefficient.
   */
  public abstract IOctCoefficients add(IOctCoefficients other);

  /**
   * Substracts two OctCoefficients.
   * @return The new substracted Coefficient.
   */
  public abstract IOctCoefficients sub(IOctCoefficients other);

  /**
   * Scales the values of an OctCoefficient.
   *
   * @param scale
   * @return
   */
  public abstract IOctCoefficients mult(long scale);

  /**
   * Scales the values of an OctCoefficient.
   *
   * @param scale
   * @return
   */
  public abstract IOctCoefficients div(long scale);

  /**
   * Multiplies two OctCoefficients if possible (one of them has to be constant)
   * if there is no constant OctCoefficient, null is returned.
   *
   * @return The new scaled Coefficient.
   */
  public abstract IOctCoefficients mult(IOctCoefficients oct);

  /**
   * Divides two OctCoefficients if possible (one of them has to be constant)
   * if there is no constant OctCoefficient, null is returned.
   *
   * @return The new scaled Coefficient.
   */
  public abstract IOctCoefficients div(IOctCoefficients oct);

  public abstract IOctCoefficients binAnd(IOctCoefficients oct);

  public abstract IOctCoefficients binOr(IOctCoefficients oct);

  public abstract IOctCoefficients binXOr(IOctCoefficients oct);

  public abstract IOctCoefficients shiftLeft(IOctCoefficients oct);

  public abstract IOctCoefficients shiftRight(IOctCoefficients oct);

  public abstract IOctCoefficients modulo(IOctCoefficients oct);

  public abstract IOctCoefficients greaterEq(IOctCoefficients oct);

  public abstract IOctCoefficients greater(IOctCoefficients oct);

  public abstract IOctCoefficients smallerEq(IOctCoefficients oct);

  public abstract IOctCoefficients smaller(IOctCoefficients oct);

  public abstract IOctCoefficients eq(IOctCoefficients oct);

  public abstract IOctCoefficients ineq(IOctCoefficients oct);

  /**
   * Indicates whether the Coefficient List only consists of a constant value.
   */
  public abstract boolean hasOnlyConstantValue();

  @Override
  public abstract String toString();

}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

/**
 * This interface should help to cope with the strict requirements
 *    of Object.equals, i.e., where Objects.equals is not adequate.
 *
 * An example would be java.util.regex.Pattern which does not
 * implement "equals", but it might be possible to decide whether two
 * regular expressions are equal or not; if this would not be decidable
 * within specific resource constraints, a method could return UNKNOWN.
 *
 * This interface helps to implement sound verification algorithms.
 *
 */
public interface TrinaryEqualable {

  public enum Equality {
    /** The objects could be either EQUAL, or NOT EQUAL. Overapproximation. */
    UNKNOWN,

    /** The objects are equal (for sure) */
    EQUAL,

    /** The objects are NOT equal (for sure) */
    UNEQUAL
  }

  /**
   * Get the best possible information
   * on the equality of two objects.
   *
   * "Best possible" means, that the method also might
   * return {@link Equality#UNKNOWN}, which would mean that
   * the object could be EQUAL, but could also be NOT EQUAL.
   *
   * @param pOther    Object to compare with
   * @return          Comparison result -- see {@link Equality} for possible values
   */
  public Equality equalityTo(Object pOther);

}

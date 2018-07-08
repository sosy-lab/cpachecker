/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type.js;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;

/** Singleton class for the special JavaScript value <code>undefined</code>. */
public class JSUndefinedValue implements Value {

  private static final long serialVersionUID = 49593725475613735L;

  private static final JSUndefinedValue SINGLETON = new JSUndefinedValue();

  private JSUndefinedValue() {
    // private constructor for singleton pattern
  }

  /**
   * Returns an instance of a <code>JSUndefinedValue</code> object.
   *
   * @return an instance of this object
   */
  public static Value getInstance() {
    return SINGLETON;
  }

  /**
   * Always returns <code>false</code> since <code>undefined</code> is no numeric value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Always returns <code>false</code> since <code>undefined</code> is a specific value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /** Always returns <code>true</code> since <code>undefined</code> is a specific value. */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * This method always returns <code>undefined</code>.
   *
   * <p>This object always represents <code>undefined</code>, which can't be represented by a
   * specific numeric value.
   */
  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  /**
   * This method is not implemented and will lead to an <code>AssertionError</code>. <code>Undefined
   * </code> can't be represented by a specific number.
   */
  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("Undefined cannot be represented as Long");
  }

  @Override
  public <T> T accept(final ValueVisitor<T> pVisitor) {
    throw new RuntimeException("Not implemented"); // TODO
  }

  @Override
  public boolean equals(Object other) {
    // all JSUndefinedValue objects are equal
    return other instanceof JSUndefinedValue;
  }

  @Override
  public int hashCode() {
    return 1; // singleton without any values
  }

  @Override
  public String toString() {
    return "UNDEFINED";
  }
  }

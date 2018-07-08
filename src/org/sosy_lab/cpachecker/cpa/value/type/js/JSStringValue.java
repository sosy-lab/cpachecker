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

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;

/** Singleton class for JavaScript string values. */
public class JSStringValue implements Value {

  private static final long serialVersionUID = -3826687802529504151L;
  private final String value;

  private JSStringValue(final String pValue) {
    value = pValue;
  }

  public static Value of(final String pValue) {
    return new JSStringValue(pValue);
  }

  @Override
  public String toString() {
    return "\"" + value + "\"";
  }

  @Override
  public boolean equals(final Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    final JSStringValue that = (JSStringValue) pO;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  /**
   * Always returns <code>false</code> since string is no numeric value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Always returns <code>false</code> since string is a specific value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /** Always returns <code>true</code> since string is a specific value. */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * This method throws an <code>AssertionError</code> since string is not an instance of {@link
   * NumericValue}.
   */
  @Override
  public NumericValue asNumericValue() {
    throw new AssertionError("String cannot be represented as numeric value");
  }

  /**
   * This method throws an <code>AssertionError</code> since string is not an instance of {@link
   * NumericValue}.
   */
  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("String cannot be represented as Long");
  }

  @Override
  public <T> T accept(final ValueVisitor<T> pVisitor) {
    throw new RuntimeException("Not implemented"); // TODO
  }
}

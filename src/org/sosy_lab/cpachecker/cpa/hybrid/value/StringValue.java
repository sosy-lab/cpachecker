/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.hybrid.value;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;

/**
 * This class represents a value for cases when a value for a char pointer or a char-array is needed
 */
public class StringValue implements Value {

  private static final long serialVersionUID = 342936573L;

  private final String value;

  public StringValue(String pValue) {
    this.value = pValue;
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  @Override
  public NumericValue asNumericValue() {
    throw new AssertionError("A string value cannot be represented numerically.");
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return null;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || !(obj instanceof StringValue)) {
      return false;
    }

    return this.value.equals(((StringValue)obj).value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   *
   * @return The string value the instance represents
   */
  public String getValue() {
    return value;
  }
}

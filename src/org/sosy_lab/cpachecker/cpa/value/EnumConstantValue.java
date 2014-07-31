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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * Stores an enum constant that can be tracked by the ValueAnalysisCPA.
 */
public class EnumConstantValue implements Value {

  private final Type classType;
  private final String fullyQualifiedName;

  /**
   * Creates a new <code>EnumValue</code>.
   *
   * @param enumType the type of the enum constant.
   * @param fullyQualifiedName the fully qualified name of this constant
   */
  public EnumConstantValue(Type pEnumType, String pFullyQualifiedName) {
    assert pEnumType instanceof JClassType || pEnumType instanceof CComplexType;

    classType = pEnumType;
    fullyQualifiedName = pFullyQualifiedName;
  }

  /**
   * Returns the enum type of this value.
   *
   * @return the enum type of this value
   */
  public Type getEnumType() {
    return classType;
  }

  /**
   * Returns the fully qualified name of the stored enum constant.
   *
   * @return the fully qualified name of this value
   */
  public String getName() {
    return fullyQualifiedName;
  }

  /**
   * Always returns <code>false</code> since enum constants are no numbers.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Always returns <code>false</code> since every
   * <code>EnumConstantValue</code> has to represent a specific value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Always returns <code>true</code> since every
   * <code>EnumConstantValue</code> represents one specific value.
   *
   * @return always returns <code>true</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * This method is not implemented and will lead to an <code>AssertionError</code>.
   * Enum constants can't be represented by a number.
   */
  @Override
  public NumericValue asNumericValue() {
    throw new AssertionError("Enum constant cannot be represented as NumericValue");
  }

  /**
   * This method is not implemented and will lead to an <code>AssertionError</code>.
   * Enum constants can't be represented by a number.
   */
  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("Enum constant cannot be represented as Long");
  }

  @Override
  public String toString() {
    return fullyQualifiedName;
  }
}

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

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * Stores an enum constant that can be tracked by the ValueAnalysisCPA.
 */
public class EnumConstantValue implements Value {

  private final JClassType classType;
  private final String fullyQualifiedName;

  /**
   * Creates a new <code>EnumValue</code>.
   *
   * @param enumType the enum type of the constant
   * @param fullyQualifiedName the fully qualified name of this constant
   */
  public EnumConstantValue(JClassType pEnumType, String pFullyQualifiedName) {
    classType = pEnumType;
    this.fullyQualifiedName = pFullyQualifiedName;
  }

  /**
   * Returns the enum type of this value.
   *
   * @return the enum type of this value
   */
  public JClassType getEnumType() {
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
    throw new AssertionError("Enum constant cannot be represented as NumericValue");
  }

  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("Enum constant cannot be represented as Long");
  }

  @Override
  public String toString() {
    return fullyQualifiedName;
  }

}

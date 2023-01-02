// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** Stores an enum constant that can be tracked by the ValueAnalysisCPA. */
public class EnumConstantValue implements Value {

  private static final long serialVersionUID = 2745087444102463717L;

  private final String fullyQualifiedName;

  /**
   * Creates a new <code>EnumValue</code>.
   *
   * @param pFullyQualifiedName the fully qualified name of this constant
   */
  public EnumConstantValue(String pFullyQualifiedName) {
    fullyQualifiedName = pFullyQualifiedName;
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
   * Always returns <code>false</code> since every <code>EnumConstantValue</code> has to represent a
   * specific value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Always returns <code>true</code> since every <code>EnumConstantValue</code> represents one
   * specific value.
   *
   * @return always returns <code>true</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * This method is not implemented and will lead to an <code>AssertionError</code>. Enum constants
   * can't be represented by a number.
   */
  @Override
  public NumericValue asNumericValue() {
    throw new AssertionError("Enum constant cannot be represented as NumericValue");
  }

  /**
   * This method always returns <code>null</code>. Enum constants can't be represented by a number.
   */
  @Override
  public Long asLong(CType pType) {
    return null;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof EnumConstantValue) {
      EnumConstantValue concreteOther = (EnumConstantValue) other;

      return concreteOther.fullyQualifiedName.equals(fullyQualifiedName);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return fullyQualifiedName.hashCode();
  }

  @Override
  public String toString() {
    return fullyQualifiedName;
  }
}

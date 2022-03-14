// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class FunctionValue implements Value, Serializable {

  private static final long serialVersionUID = -3829943575180448170L;

  private final String str;

  /**
   * Creates a new <code>FunctionValue</code>.
   *
   * @param pString the value of the function
   */
  public FunctionValue(String pString) {
    str = checkNotNull(pString);
  }

  @Override
  public String toString() {
    return "FunctionValue [name=" + str + "]";
  }

  public String getName() {
    return str;
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
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType pType) {
    return null;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return str.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    // equals is only called if this object is a function pointer
    // always false when comparing a functional pointer with zero
    // if this object is not a functional pointer, then equals is not called.
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FunctionValue other = (FunctionValue) obj;
    return str.equals(other.str);
  }

  public String getString() {
    return str;
  }
}

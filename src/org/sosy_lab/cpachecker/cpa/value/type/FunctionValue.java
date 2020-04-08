/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class FunctionValue implements Value, Serializable {

  private static final long serialVersionUID = -3829943575180448170L;

  private String str;

  /**
   * Creates a new <code>FunctionValue</code>.
   * @param pString the value of the function
   */
  public FunctionValue(String pString) {
    str = pString;
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((str == null) ? 0 : str.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
  //equals is only called if this object is a function pointer
  //always false when comparing a functional pointer with zero
  //if this object is not a functional pointer, then equals is not called.
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
    if (str == null) {
      if (other.str != null) {
        return false;
      }
    } else if (!str.equals(other.str)) {
      return false;
    }
    return true;
  }

  public String getString() {
    return str;
  }
}

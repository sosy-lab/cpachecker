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
package org.sosy_lab.cpachecker.cfa.ast;


import java.math.BigDecimal;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;


public abstract class AFloatLiteralExpression extends ALiteralExpression {

  private final BigDecimal value;

  public AFloatLiteralExpression(FileLocation pFileLocation, Type pType, BigDecimal pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    // If the value is integral and has no zeroes after the decimal point yet, add one
    if (value.scale() <= 0) {
      return String.format("%.1f", value);
    }
    return value.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(value);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFloatLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    AFloatLiteralExpression other = (AFloatLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}

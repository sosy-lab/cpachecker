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


import java.math.BigInteger;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;


public abstract class AIntegerLiteralExpression extends ALiteralExpression {

  private final BigInteger value;

  public AIntegerLiteralExpression(FileLocation pFileLocation, Type pType, BigInteger pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  public long asLong() {
    // TODO handle values that are bigger than MAX_LONG
    return value.longValue();
  }


   @Override
  public  String toASTString() {
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

    if (!(obj instanceof AIntegerLiteralExpression)
        || !super.equals(obj)) {
      return false;
    }

    AIntegerLiteralExpression other = (AIntegerLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

}
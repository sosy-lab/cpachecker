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

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AUnaryExpression extends AExpression {

  private final IAExpression operand;
  private final AUnaryOperator  operator;

  public AUnaryExpression(FileLocation pFileLocation, Type pType, final IAExpression pOperand,
      final AUnaryOperator pOperator) {
    super(pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public IAExpression getOperand() {
    return operand;
  }

  public AUnaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + operand.toParenthesizedASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hash(operand);
    result = prime * result + Objects.hash(operator);
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

    if (!(obj instanceof AUnaryExpression)
        || !super.equals(obj)) {
      return false;
    }

    AUnaryExpression other = (AUnaryExpression) obj;

    return Objects.equals(other.operand, operand)
            && Objects.equals(other.operator, operator);
  }

  public static  interface AUnaryOperator {
    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator();
  }

}
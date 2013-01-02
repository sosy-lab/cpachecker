/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This is the abstract Class for  Casted Expressions.
 *
 */
public abstract class ACastExpression extends AExpression {

  private final IAExpression operand;
  private final Type     castType;


  public ACastExpression(FileLocation pFileLocation, Type castExpressionType , IAExpression pOperand ) {
    super(pFileLocation, castExpressionType);

    operand = pOperand;
    castType = castExpressionType;
  }

  public IAExpression getOperand() {
    return operand;
  }

  @Override
  public String toASTString() {
    return "(" + getExpressionType().toASTString("") + ")" + operand.toParenthesizedASTString();
  }

  public Type getCastType() {
    return castType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((castType == null) ? 0 : castType.hashCode());
    result = prime * result + ((operand == null) ? 0 : operand.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof ACastExpression)) { return false; }
    ACastExpression other = (ACastExpression) obj;
    if (castType == null) {
      if (other.castType != null) { return false; }
    } else if (!castType.equals(other.castType)) { return false; }
    if (operand == null) {
      if (other.operand != null) { return false; }
    } else if (!operand.equals(other.operand)) { return false; }
    return true;
  }

}

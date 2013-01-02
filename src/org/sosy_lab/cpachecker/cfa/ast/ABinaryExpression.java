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
 * This is the abstract Class for all Expressions with two Operands and one Operator.
 */
public abstract class ABinaryExpression extends AExpression {


  private final IAExpression operand1;
  private final IAExpression operand2;
  private final ABinaryOperator operator;

  public ABinaryExpression(FileLocation pFileLocation, Type pType,
      final IAExpression pOperand1,
      final IAExpression pOperand2,
      final ABinaryOperator pOperator) {
    super(pFileLocation, pType);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
  }



  public IAExpression getOperand1() {
    return operand1;
  }

  public IAExpression getOperand2() {
    return operand2;
  }

  public ABinaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString() {
    return operand1.toParenthesizedASTString() + " "
        + operator.getOperator() + " " + operand2.toParenthesizedASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((operand1 == null) ? 0 : operand1.hashCode());
    result = prime * result + ((operand2 == null) ? 0 : operand2.hashCode());
    result = prime * result + ((operator == null) ? 0 : operator.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }



  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof ABinaryExpression)) { return false; }
    ABinaryExpression other = (ABinaryExpression) obj;
    if (operand1 == null) {
      if (other.operand1 != null) { return false; }
    } else if (!operand1.equals(other.operand1)) { return false; }
    if (operand2 == null) {
      if (other.operand2 != null) { return false; }
    } else if (!operand2.equals(other.operand2)) { return false; }
    if (operator == null) {
      if (other.operator != null) { return false; }
    } else if (!operator.equals(other.operator)) { return false; }

    return super.equals(other);
  }

  public static  interface ABinaryOperator {
    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator();
  }
}
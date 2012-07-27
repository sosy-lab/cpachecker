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


import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;



public class ABinaryExpression extends AExpression implements JExpression{


  protected final IAExpression operand1;
  protected final IAExpression operand2;
  protected final BinaryOperator operator;

  public ABinaryExpression(CFileLocation pFileLocation, Type pType,
      final IAExpression pOperand1,
      final IAExpression pOperand2,
      final BinaryOperator pOperator) {
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

  public BinaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString() {
    return operand1.toParenthesizedASTString() + " "
        + operator.getOperator() + " " + operand2.toParenthesizedASTString();
  }



  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }



  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

}
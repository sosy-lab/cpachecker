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

import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.Type;


public class AUnaryExpression extends AExpression {

  private final IAExpression operand;
  private final AUnaryOperator  operator;

  public AUnaryExpression(CFileLocation pFileLocation, Type pType, final IAExpression pOperand,
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
    if (operator instanceof CUnaryExpression.UnaryOperator && (CUnaryExpression.UnaryOperator)operator == UnaryOperator.SIZEOF) {
      return operator.getOperator() + "(" + operand.toASTString() + ")";
    } else {
      return operator.getOperator() + operand.toParenthesizedASTString();
    }
  }

  public static  interface AUnaryOperator {
    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator();
  }

}
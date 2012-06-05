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

public class IASTUnaryExpression extends IASTExpression {

  private final IASTExpression operand;
  private final UnaryOperator  operator;

  public IASTUnaryExpression(final IASTFileLocation pFileLocation,
                             final IType pType, final IASTExpression pOperand,
                             final UnaryOperator pOperator) {
    super(pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public IASTExpression getOperand() {
    return operand;
  }

  public UnaryOperator getOperator() {
    return operator;
  }

  @Override
  public <R, X extends Exception> R accept(ExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(RightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public static enum UnaryOperator {
    PLUS   ("+"),
    MINUS  ("-"),
    STAR   ("*"),
    AMPER  ("&"),
    TILDE  ("~"),
    NOT    ("!"),
    SIZEOF ("sizeof"),
    ;

    private final String mOp;

    private UnaryOperator(String pOp) {
      mOp = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator() {
      return mOp;
    }
  }

  @Override
  public String toASTString() {
    if (operator == UnaryOperator.SIZEOF) {
      return operator.getOperator() + "(" + operand.toASTString() + ")";
    } else {
      return operator.getOperator() + operand.toParenthesizedASTString();
    }
  }
}

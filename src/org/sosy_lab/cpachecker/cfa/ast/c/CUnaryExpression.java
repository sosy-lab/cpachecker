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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CUnaryExpression extends AUnaryExpression implements CExpression {



  public CUnaryExpression(final FileLocation pFileLocation,
                             final CType pType, final CExpression pOperand,
                             final UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);

  }

  @Override
  public CExpression getOperand() {
    return (CExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public String toASTString() {
    if (getOperator() == UnaryOperator.SIZEOF) {
      return getOperator().getOperator() + "(" + getOperand().toASTString() + ")";
    } else {
      return getOperator().getOperator() + getOperand().toParenthesizedASTString();
    }
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public static enum UnaryOperator implements AUnaryExpression.AUnaryOperator {
    @Deprecated
    PLUS   ("+"),
    MINUS  ("-"),
    AMPER  ("&"),
    TILDE  ("~"),
    @Deprecated
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
    @Override
    public String getOperator() {
      return mOp;
    }
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
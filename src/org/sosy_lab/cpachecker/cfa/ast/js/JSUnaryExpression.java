/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa.ast.js;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;

public class JSUnaryExpression extends AUnaryExpression implements JSExpression {

  public JSUnaryExpression(
      final FileLocation pFileLocation,
      final JSType pType,
      final JSExpression pOperand,
      final UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);
  }

  @Override
  public JSExpression getOperand() {
    return (JSExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public JSType getExpressionType() {
    return (JSType) super.getExpressionType();
  }

  @Override
  public String toASTString() {
    return getOperator().getOperator() + getOperand().toParenthesizedASTString();
  }

  @Override
  public <R, X extends Exception> R accept(final JSExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(final JSAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  public static enum UnaryOperator implements AUnaryOperator {
    INCREMENT("++"),
    DECREMENT("--"),
    PLUS("+"),
    MINUS("-"),
    COMPLEMENT("~"),
    NOT("!"),
    ;

    private final String mOp;

    private UnaryOperator(final String pOp) {
      mOp = pOp;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return mOp;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    final int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JSUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}

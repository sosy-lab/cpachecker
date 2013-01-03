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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;


public class JUnaryExpression extends AUnaryExpression implements JExpression {

  public JUnaryExpression(FileLocation pFileLocation, JType pType, JExpression pOperand, UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);

  }

  @Override
  public JType getExpressionType() {
    return (JType) super.getExpressionType();
  }

  @Override
  public JExpression getOperand() {
    return (JExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public static enum UnaryOperator implements AUnaryExpression.AUnaryOperator {


    NOT      ("!"),
    PLUS        ("+"),
    COMPLEMENT        ("~"),
    MINUS("-")
    ;

    private final String op;

    private UnaryOperator(String pOp) {
      op = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    @Override
    public String getOperator() {
      return op;
    }
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof JUnaryExpression)) { return false; }
    return super.equals(obj);
  }
}

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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CBinaryExpression extends ABinaryExpression implements CExpression {

  public CBinaryExpression(final CFileLocation pFileLocation,
                              final CType pType,
                              final CExpression pOperand1,
                              final CExpression pOperand2,
                              final BinaryOperator pOperator) {
    super(pFileLocation, pType , pOperand1 , pOperand2, pOperator);

  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public CType getExpressionType() {
    return (CType) type;
  }

  @Override
  public CExpression getOperand1() {
    return (CExpression) operand1;
  }

  @Override
  public CExpression getOperand2() {
    return (CExpression)operand2;
  }

  @Override
  public BinaryOperator getOperator() {
    return operator;
  }

  public static enum BinaryOperator {
    MULTIPLY      ("*"),
    DIVIDE        ("/"),
    MODULO        ("%"),
    PLUS          ("+"),
    MINUS         ("-"),
    SHIFT_LEFT    ("<<"),
    SHIFT_RIGHT   (">>"),
    LESS_THAN     ("<"),
    GREATER_THAN  (">"),
    LESS_EQUAL    ("<="),
    GREATER_EQUAL (">="),
    BINARY_AND    ("&"),
    BINARY_XOR    ("^"),
    BINARY_OR     ("|"),
    LOGICAL_AND   ("&&"),
    LOGICAL_OR    ("||"),
    EQUALS        ("=="),
    NOT_EQUALS    ("!="),
    ;

    private final String op;

    private BinaryOperator(String pOp) {
      op = pOp;
    }

    /**
     * Returns the string representation of this operator (e.g. "*", "+").
     */
    public String getOperator() {
      return op;
    }
  }
}

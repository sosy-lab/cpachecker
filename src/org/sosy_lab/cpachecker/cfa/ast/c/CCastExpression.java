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

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CCastExpression extends AExpression implements CExpression {

  private final CExpression operand;
  @SuppressWarnings("hiding")
  private final CType     type;

  public CCastExpression(final CFileLocation pFileLocation,
                            final CType pExpressionType,
                            final CExpression pOperand,
                            final CType pType) {
    super(pFileLocation, pExpressionType);
    operand = pOperand;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return type;
  }

  public CExpression getOperand() {
    return operand;
  }

  public CType getType() {
    return type;
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
  public String toASTString() {
    return "(" + type.toASTString("") + ")" + operand.toParenthesizedASTString();
  }

}

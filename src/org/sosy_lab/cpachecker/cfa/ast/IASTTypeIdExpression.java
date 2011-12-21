/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

public final class IASTTypeIdExpression extends IASTExpression {

  private final TypeIdOperator operator;
  private final IASTTypeId type;

  public IASTTypeIdExpression(final IASTFileLocation pFileLocation,
                              final IType pType, final TypeIdOperator pOperator,
                              final IASTTypeId pTypeId) {
    super(pFileLocation, pType);
    operator = pOperator;
    type = pTypeId;
  }

  public TypeIdOperator getOperator() {
    return operator;
  }

  public IASTTypeId getTypeId() {
    return type;
  }

  @Override
  public <R, X extends Exception> R accept(ExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(RightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum TypeIdOperator {
    SIZEOF,
    TYPEID,
    ALIGNOF,
    TYPEOF,
    ;

    /**
     * Returns the string representation of this operator
     */
    public String getOperator() {
      return toString().toLowerCase();
    }
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + "(" + type.toASTString() + ")";
  }
}

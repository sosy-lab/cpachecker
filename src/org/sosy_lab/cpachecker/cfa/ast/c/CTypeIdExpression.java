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

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Objects;

public final class CTypeIdExpression extends AbstractExpression implements CExpression {

  private final TypeIdOperator operator;
  private final CType type;

  public CTypeIdExpression(final FileLocation pFileLocation,
                              final CType pExpressionType, final TypeIdOperator pOperator,
                              final CType pType) {
    super(pFileLocation, pExpressionType);
    operator = pOperator;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public TypeIdOperator getOperator() {
    return operator;
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
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  public enum TypeIdOperator {
    SIZEOF("sizeof"),
    ALIGNOF("_Alignof"),
    TYPEOF("typeof"),
    ;

    private final String cRepresentation;

    private TypeIdOperator(String pCRepresentation) {
      cRepresentation = pCRepresentation;
    }

    /**
     * Returns the string representation of this operator
     */
    public String getOperator() {
      return cRepresentation;
    }
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + "(" + type.toASTString("") + ")";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(operator);
    result = prime * result + Objects.hashCode(type);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CTypeIdExpression)
        || !super.equals(obj)) {
      return false;
    }

    CTypeIdExpression other = (CTypeIdExpression) obj;

    return Objects.equals(other.operator, operator)
            && Objects.equals(other.type, type);
  }

}

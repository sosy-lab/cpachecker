/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents C code such as
 * (struct s) { .field = value }
 */
public final class CTypeIdInitializerExpression extends AExpression implements CExpression {

  private final CInitializer initializer;
  @SuppressWarnings("hiding")
  private final CType     type;

  public CTypeIdInitializerExpression(final FileLocation pFileLocation,
                            final CType pExpressionType,
                            final CInitializer pInitializer,
                            final CType pType) {
    super(pFileLocation, pExpressionType);
    initializer = pInitializer;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType)super.getExpressionType();
  }

  public CInitializer getInitializer() {
    return initializer;
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
    return "(" + type.toASTString("") + ")" + initializer.toParenthesizedASTString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof CTypeIdInitializerExpression)) { return false; }
    CTypeIdInitializerExpression other = (CTypeIdInitializerExpression) obj;
    if (initializer == null) {
      if (other.initializer != null) { return false; }
    } else if (!initializer.equals(other.initializer)) { return false; }
    if (type == null) {
      if (other.type != null) { return false; }
    } else if (!type.equals(other.type)) { return false; }

    return super.equals(other);
  }

}


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

import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Objects;

public final class CIdExpression extends AIdExpression implements CLeftHandSide {


  public CIdExpression(final FileLocation pFileLocation,
                          final CType pType, final String pName,
                          final CSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }



  @Override
  public CType getExpressionType() {
    return (CType)super.getExpressionType();
  }

  /**
   * Get the declaration of the variable.
   * The result may be null if the variable was not declared.
   */
  @Override
  public CSimpleDeclaration getDeclaration() {
    return  (CSimpleDeclaration) super.getDeclaration();
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
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    if (getDeclaration() != null) {
      result = prime * result + Objects.hashCode(getDeclaration().getQualifiedName());
    }
    return prime * result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CIdExpression)) {
      return false;
    }

    // Don't call super.equals() here,
    // it compares the declaration field.
    // In C, there might be several declarations declaring the same variable,
    // so we sometimes need to return true even with different declarations.

    CIdExpression other = (CIdExpression)obj;

    if (getDeclaration() == null) {
      return other.getDeclaration() == null;
    } else {
      return Objects.equal(getDeclaration().getQualifiedName(), other.getDeclaration().getQualifiedName());
    }
  }
}
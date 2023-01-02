// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CIdExpression extends AIdExpression implements CLeftHandSide {

  private static final long serialVersionUID = -608459029930942264L;

  public CIdExpression(
      final FileLocation pFileLocation,
      final CType pType,
      final String pName,
      final CSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }

  public CIdExpression(final FileLocation pFileLocation, final CSimpleDeclaration pDeclaration) {
    super(pFileLocation, pDeclaration);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  /**
   * Get the declaration of the variable. The result may be null if the variable was not declared.
   */
  @Override
  public CSimpleDeclaration getDeclaration() {
    return (CSimpleDeclaration) super.getDeclaration();
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
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    if (getDeclaration() != null) {
      return Objects.hash(getDeclaration().getQualifiedName());
    }
    return 0;
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

    CIdExpression other = (CIdExpression) obj;

    @Nullable CSimpleDeclaration decl = getDeclaration();
    @Nullable CSimpleDeclaration otherDecl = other.getDeclaration();
    if (decl == null || otherDecl == null) {
      return decl == otherDecl;
    } else {
      return Objects.equals(decl.getQualifiedName(), otherDecl.getQualifiedName());
    }
  }
}

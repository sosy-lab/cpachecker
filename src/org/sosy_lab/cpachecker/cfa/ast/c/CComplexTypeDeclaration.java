// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;

/**
 * This class represents declaration of complex types without declarations of variables at the same
 * time. Typedefs are not represented by this class. Example code:
 *
 * <p>struct s { ... }; struct s; enum e { ... };
 *
 * <p>TODO: As these declarations have no name, they should not be in the hierarchy below {@link
 * CSimpleDeclaration}.
 */
public final class CComplexTypeDeclaration extends CTypeDeclaration {

  private static final long serialVersionUID = -1789123397167943609L;

  public CComplexTypeDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal, CComplexType pType) {
    super(pFileLocation, pIsGlobal, pType, null, null);
  }

  @Override
  public CComplexType getType() {
    return (CComplexType) super.getType();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    return result * prime + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CComplexTypeDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}

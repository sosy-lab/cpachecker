// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represent typedef declarations. Example code:
 *
 * <p>typedef int my_int;
 */
public final class CTypeDefDeclaration extends CTypeDeclaration {

  private static final long serialVersionUID = -8939387997205706731L;

  public CTypeDefDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      CType pType,
      String pName,
      String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, checkNotNull(pName), checkNotNull(pQualifiedName));
  }

  @Override
  public String toASTString(boolean pQualified) {
    return "typedef " + super.toASTString(pQualified);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CTypeDefDeclaration)) {
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

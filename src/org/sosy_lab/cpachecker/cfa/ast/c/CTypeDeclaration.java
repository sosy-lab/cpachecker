// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents declarations that define new types, e.g.
 *
 * <p>struct s { int i; } typedef int my_int;
 */
public abstract sealed class CTypeDeclaration extends AbstractDeclaration implements CDeclaration
    permits CComplexTypeDeclaration, CTypeDefDeclaration {

  @Serial private static final long serialVersionUID = -607383651501118425L;
  private final String qualifiedName;

  protected CTypeDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      CType pType,
      String pName,
      String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, pName, pName);
    qualifiedName = pQualifiedName;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
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

    return obj instanceof CTypeDeclaration && super.equals(obj);
  }
}

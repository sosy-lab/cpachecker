// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslTypeDeclaration extends AbstractDeclaration
    implements AcslDeclaration permits AcslTypeVariableDeclaration {

  @Serial private static final long serialVersionUID = -60738365123118425L;
  private final String qualifiedName;

  protected AcslTypeDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      AcslType pType,
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
  public AcslType getType() {
    return (AcslType) super.getType();
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

    return obj instanceof AcslTypeDeclaration && super.equals(obj);
  }
}

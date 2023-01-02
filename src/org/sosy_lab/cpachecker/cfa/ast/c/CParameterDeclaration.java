// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** This is the declaration of a function parameter. It contains a type and a name. */
public final class CParameterDeclaration extends AParameterDeclaration
    implements CSimpleDeclaration {

  private static final long serialVersionUID = -6856088248264928629L;
  private String qualifiedName;

  public CParameterDeclaration(FileLocation pFileLocation, CType pType, String pName) {
    super(pFileLocation, pType, checkNotNull(pName));
  }

  public void setQualifiedName(String pQualifiedName) {
    checkState(qualifiedName == null);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  public CVariableDeclaration asVariableDeclaration() {
    return new CVariableDeclaration(
        getFileLocation(),
        false,
        CStorageClass.AUTO,
        getType(),
        getName(),
        getOrigName(),
        getQualifiedName(),
        null);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(qualifiedName) * 31 * super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CParameterDeclaration) || !super.equals(obj)) {
      return false;
    }

    CParameterDeclaration other = (CParameterDeclaration) obj;
    return Objects.equals(qualifiedName, other.qualifiedName);
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

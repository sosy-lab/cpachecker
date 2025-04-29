// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslVariableDeclaration extends AVariableDeclaration implements AcslDeclaration {

  @Serial private static final long serialVersionUID = -6073836908176118425L;

  public AcslVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      AcslType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      AcslInitializer pInitializer) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName, pQualifiedName, pInitializer);
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pName);
    checkNotNull(pOrigName);
    checkNotNull(pQualifiedName);
    checkNotNull(pInitializer);
  }

  public AcslVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      AcslType pType,
      String pName,
      String pOrigName,
      String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName, pQualifiedName, null);
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pName);
    checkNotNull(pOrigName);
    checkNotNull(pQualifiedName);
  }

  @Override
  public AcslType getType() {
    return (AcslType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

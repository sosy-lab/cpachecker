// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslTypeVariableDeclaration extends AcslTypeDeclaration {

  @Serial
  private static final long serialVersionUID = -6073836511235L;

  private AcslTypeVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      AcslType pType,
      String pName,
      String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, pName, pQualifiedName);
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

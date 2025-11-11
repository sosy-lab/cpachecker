// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibSortDeclaration extends AVariableDeclaration implements SvLibDeclaration {

  @Serial private static final long serialVersionUID = -4339353958320549395L;

  public SvLibSortDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      SvLibCustomType pType,
      String pName,
      String pOrigName,
      String pQualifiedName) {
    super(
        pFileLocation,
        pIsGlobal,
        pType,
        pName,
        pOrigName,
        pQualifiedName,
        null /* There are no initializers in SV-LIB */);
  }

  @Override
  @Nullable
  public String getProcedureName() {
    return null;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public SvLibCustomType getType() {
    return (SvLibCustomType) super.getType();
  }
}

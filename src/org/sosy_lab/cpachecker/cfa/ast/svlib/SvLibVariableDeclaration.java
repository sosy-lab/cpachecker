// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibVariableDeclaration extends AVariableDeclaration
    implements SvLibDeclaration {

  @Serial private static final long serialVersionUID = -6558791317019048432L;

  public SvLibVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      SvLibType pType,
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
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public SvLibType getType() {
    return (SvLibType) super.getType();
  }
}

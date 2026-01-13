// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;

public final class SvLibVariableDeclarationCommand implements SvLibCommand {

  @Serial private static final long serialVersionUID = -1300405431265108107L;

  private final SvLibParsingVariableDeclaration variableDeclaration;

  private final FileLocation fileLocation;

  public SvLibVariableDeclarationCommand(
      SvLibParsingVariableDeclaration pVariableDeclaration, FileLocation pFileLocation) {
    variableDeclaration = pVariableDeclaration;
    fileLocation = pFileLocation;
  }

  public SvLibParsingVariableDeclaration getVariableDeclaration() {
    return variableDeclaration;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibVariableDeclarationCommand other
        && variableDeclaration.equals(other.variableDeclaration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(variableDeclaration);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(declare-var "
        + variableDeclaration.toASTString()
        + " "
        + variableDeclaration.getType().toASTString()
        + ")";
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

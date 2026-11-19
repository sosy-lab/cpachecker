// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import com.google.common.base.Preconditions;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;

public final class SvLibDeclareConstCommand implements SvLibCommand, SmtLibCommand {

  @Serial private static final long serialVersionUID = 3470911008796570701L;
  private final SvLibParsingVariableDeclaration variable;
  private final FileLocation fileLocation;

  public SvLibDeclareConstCommand(
      SvLibParsingVariableDeclaration pVariable, FileLocation pFileLocation) {
    Preconditions.checkArgument(pVariable.isConstant());
    Preconditions.checkArgument(pVariable.isGlobal());
    variable = pVariable;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(declare-const "
        + variable.toASTString()
        + " "
        + variable.getType().toASTString()
        + ")";
  }

  public SvLibParsingVariableDeclaration getVariable() {
    return variable;
  }

  @Override
  public int hashCode() {
    return variable.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibDeclareConstCommand other && variable.equals(other.variable);
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

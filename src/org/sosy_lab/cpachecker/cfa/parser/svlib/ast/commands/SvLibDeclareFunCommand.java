// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibDeclareFunCommand implements SvLibCommand, SmtLibCommand {

  @Serial private static final long serialVersionUID = -4340667982732887189L;
  private final SvLibSmtFunctionDeclaration functionDeclaration;
  private final FileLocation fileLocation;

  public SvLibDeclareFunCommand(
      SvLibSmtFunctionDeclaration pFunctionDeclaration, FileLocation pFileLocation) {
    functionDeclaration = pFunctionDeclaration;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(declare-fun "
        + functionDeclaration.getName()
        + " ("
        + Joiner.on(" ")
            .join(
                FluentIterable.from(functionDeclaration.getType().getParameters())
                    .transform(SvLibType::toASTString))
        + ") "
        + functionDeclaration.getType().getReturnType().toASTString()
        + ")";
  }

  @Override
  public int hashCode() {
    return functionDeclaration.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibDeclareFunCommand other
        && functionDeclaration.equals(other.functionDeclaration);
  }

  public SvLibSmtFunctionDeclaration getFunctionDeclaration() {
    return functionDeclaration;
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

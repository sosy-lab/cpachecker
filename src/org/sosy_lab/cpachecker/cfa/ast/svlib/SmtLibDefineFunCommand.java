// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SmtLibDefineFunCommand implements SmtLibCommand, SvLibCommand {
  @Serial private static final long serialVersionUID = 3049346957426478591L;
  private final SvLibFunctionDeclaration functionDeclaration;
  private final SvLibTerm body;
  private final FileLocation fileLocation;

  public SmtLibDefineFunCommand(
      SvLibFunctionDeclaration pFunctionDeclaration, SvLibTerm pBody, FileLocation pFileLocation) {
    functionDeclaration = pFunctionDeclaration;
    body = pBody;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(define-fun "
        + functionDeclaration.getProcedureName()
        + functionDeclaration.getType().toPlainString()
        + " "
        + body.toASTString(pAAstNodeRepresentation)
        + ")";
  }

  public SvLibFunctionDeclaration getFunctionDeclaration() {
    return functionDeclaration;
  }

  public SvLibTerm getBody() {
    return body;
  }

  @Override
  public int hashCode() {
    return functionDeclaration.hashCode() * 31 + body.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SmtLibDefineFunCommand other
        && functionDeclaration.equals(other.functionDeclaration)
        && body.equals(other.body);
  }
}

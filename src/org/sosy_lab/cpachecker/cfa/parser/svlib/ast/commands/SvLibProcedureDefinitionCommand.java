// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;

public final class SvLibProcedureDefinitionCommand implements SvLibCommand {

  @Serial private static final long serialVersionUID = -109324745114809038L;
  private final FileLocation fileLocation;
  private final SvLibProcedureDeclaration procedureDeclaration;
  private final SvLibStatement body;

  public SvLibProcedureDefinitionCommand(
      FileLocation pFileLocation,
      SvLibProcedureDeclaration pProcedureDeclaration,
      SvLibStatement pBody) {
    fileLocation = pFileLocation;
    procedureDeclaration = pProcedureDeclaration;
    body = pBody;
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  public SvLibStatement getBody() {
    return body;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibProcedureDefinitionCommand other
        && procedureDeclaration.equals(other.procedureDeclaration)
        && body.equals(other.body);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + procedureDeclaration.hashCode();
    result = prime * result + body.hashCode();
    return result;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(define-proc " + procedureDeclaration.toASTString() + " " + body.toASTString() + ")";
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

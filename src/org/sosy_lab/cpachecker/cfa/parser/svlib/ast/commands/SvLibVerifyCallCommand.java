// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;

public final class SvLibVerifyCallCommand implements SvLibCommand {

  @Serial private static final long serialVersionUID = 3913861771262171193L;
  private final SvLibProcedureDeclaration procedureDeclaration;
  private final ImmutableList<SvLibTerm> terms;
  private final FileLocation fileLocation;

  public SvLibVerifyCallCommand(
      SvLibProcedureDeclaration pProcedureDeclaration,
      List<SvLibTerm> pTerms,
      FileLocation pFileLocation) {
    procedureDeclaration = pProcedureDeclaration;
    terms = ImmutableList.copyOf(pTerms);
    fileLocation = pFileLocation;
  }

  public SvLibProcedureDeclaration getProcedureDeclaration() {
    return procedureDeclaration;
  }

  public ImmutableList<SvLibTerm> getTerms() {
    return terms;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibVerifyCallCommand other
        && procedureDeclaration.equals(other.procedureDeclaration)
        && terms.equals(other.terms);
  }

  @Override
  public int hashCode() {
    return 31 * procedureDeclaration.hashCode() + terms.hashCode();
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(verify-call "
        + procedureDeclaration.getName()
        + " ("
        + terms.stream().map(t -> t.toASTString()).reduce((t1, t2) -> t1 + " " + t2).orElse("")
        + "))";
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

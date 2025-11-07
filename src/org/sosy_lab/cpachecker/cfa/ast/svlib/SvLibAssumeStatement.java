// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibAssumeStatement extends SvLibControlFlowStatement {

  @Serial private static final long serialVersionUID = 3882587379431999910L;
  private final SvLibTerm term;

  public SvLibAssumeStatement(
      FileLocation pFileLocation,
      SvLibTerm pTerm,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    term = pTerm;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(assume " + term.toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(assume " + term.toASTString(pAAstNodeRepresentation) + ")";
  }

  public SvLibTerm getTerm() {
    return term;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibAssumeStatement other
        && super.equals(other)
        && term.equals(other.term);
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 31 * term.hashCode();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslMemoryLocationSetTerm extends AcslMemoryLocationSet {
  private final AcslTerm term;

  AcslMemoryLocationSetTerm(FileLocation pFileLocation, AcslTerm pTerm) {
    super(pFileLocation, new AcslSetType(pTerm.getExpressionType()));
    term = pTerm;
  }

  public AcslTerm getTerm() {
    return term;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "{" + term.toASTString(pAAstNodeRepresentation) + "}";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "({" + term.toParenthesizedASTString(pAAstNodeRepresentation) + "})";
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslMemoryLocationSetVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = -4;
    int result = 8;
    result = prime * result + super.hashCode();
    result = prime * result + term.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslMemoryLocationSetTerm other
        && super.equals(obj)
        && Objects.equals(other.term, term);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNodeVisitor;

public final class SvLibInvariantTag implements SvLibTagProperty {

  @Serial private static final long serialVersionUID = 1135747516635566858L;
  private final SvLibRelationalTerm term;
  private final FileLocation fileLocation;

  public SvLibInvariantTag(SvLibRelationalTerm pTerm, FileLocation pFileLocation) {
    term = pTerm;
    fileLocation = pFileLocation;
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
    return ":invariant " + term.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":invariant " + term.toParenthesizedASTString(pAAstNodeRepresentation);
  }

  public SvLibRelationalTerm getTerm() {
    return term;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibInvariantTag other && term.equals(other.term);
  }

  @Override
  public int hashCode() {
    return term.hashCode();
  }
}

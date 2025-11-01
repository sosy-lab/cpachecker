// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3AssertTag implements K3TagProperty {

  @Serial private static final long serialVersionUID = 1135747516635566858L;
  private final K3FinalRelationalTerm term;
  private final FileLocation fileLocation;

  public K3AssertTag(K3FinalRelationalTerm pTerm, FileLocation pFileLocation) {
    term = pTerm;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":assert " + term.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return ":assert " + term.toParenthesizedASTString(pAAstNodeRepresentation);
  }

  public K3FinalRelationalTerm getTerm() {
    return term;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3AssertTag other && term.equals(other.term);
  }

  @Override
  public int hashCode() {
    return term.hashCode();
  }
}

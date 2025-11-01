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

public final class K3AssertCommand implements K3Command, SMTLibCommand {

  @Serial private static final long serialVersionUID = -7594945035224979628L;
  private final K3Term term;
  private final FileLocation fileLocation;

  public K3AssertCommand(K3Term pTerm, FileLocation pFileLocation) {
    term = pTerm;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(assert " + term.toASTString(pAAstNodeRepresentation) + ")";
  }

  public K3Term getTerm() {
    return term;
  }

  @Override
  public int hashCode() {
    return term.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3AssertCommand other && term.equals(other.term);
  }

  @Override
  public <R, X extends Exception> R accept(K3CommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

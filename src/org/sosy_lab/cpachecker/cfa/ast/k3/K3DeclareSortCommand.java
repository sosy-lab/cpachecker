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

public final class K3DeclareSortCommand implements K3Command, SMTLibCommand {

  @Serial private static final long serialVersionUID = -7401678598804609690L;
  private final K3SortDeclaration typeDelaration;
  private final FileLocation fileLocation;

  public K3DeclareSortCommand(K3SortDeclaration pDeclaration, FileLocation pFileLocation) {
    typeDelaration = pDeclaration;
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(declare-sort "
        + typeDelaration.getType().getType()
        + " "
        + typeDelaration.getType().getArity()
        + ")";
  }

  public K3SortDeclaration getTypeDelaration() {
    return typeDelaration;
  }

  @Override
  public int hashCode() {
    return typeDelaration.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3DeclareSortCommand other && typeDelaration.equals(other.typeDelaration);
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

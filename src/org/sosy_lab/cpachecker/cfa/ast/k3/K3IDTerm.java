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
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class K3IDTerm implements K3Term {
  @Serial private static final long serialVersionUID = 5782817996036730363L;

  private final K3SimpleDeclaration variable;
  private final FileLocation fileLocation;

  public K3IDTerm(K3SimpleDeclaration pVariable, FileLocation pFileLocation) {
    variable = pVariable;

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
    return variable.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return variable.toParenthesizedASTString(pAAstNodeRepresentation);
  }

  public K3SimpleDeclaration getVariable() {
    return variable;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3IDTerm other && variable.equals(other.variable);
  }

  @Override
  public int hashCode() {
    return variable.hashCode();
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public Type getExpressionType() {
    return variable.getType();
  }
}

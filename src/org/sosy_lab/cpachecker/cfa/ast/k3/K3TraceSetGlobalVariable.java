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

public final class K3TraceSetGlobalVariable extends K3SelectTraceComponent {
  @Serial private static final long serialVersionUID = 5543731065650175240L;
  private final K3IdTerm declaration;
  private final K3ConstantTerm constantTerm;

  public K3TraceSetGlobalVariable(
      K3IdTerm pDeclaration, K3ConstantTerm pConstantTerm, FileLocation pFileLocation) {
    super(pFileLocation);
    declaration = pDeclaration;
    constantTerm = pConstantTerm;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(global " + declaration.getName() + " " + constantTerm.toASTString() + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + declaration.hashCode();
    result = prime * result + constantTerm.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3TraceSetGlobalVariable other
        && declaration.equals(other.declaration)
        && constantTerm.equals(other.constantTerm);
  }
}

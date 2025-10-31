// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3HavocStatement extends K3CfaEdgeStatement {
  @Serial private static final long serialVersionUID = 3102375106958425786L;
  private final List<K3SimpleDeclaration> variables;

  public K3HavocStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      List<K3SimpleDeclaration> pVariables) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    variables = pVariables;
  }

  public List<K3SimpleDeclaration> getVariables() {
    return variables;
  }

  @Override
  public <R, X extends Exception> R accept(K3StatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(havoc (" + Joiner.on(" ").join(variables) + "))";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(havoc (" + Joiner.on(" ").join(variables) + "))";
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + variables.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3HavocStatement other
        && super.equals(other)
        && variables.equals(other.variables);
  }
}

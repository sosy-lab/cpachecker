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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SequenceStatement extends K3ControlFlowStatement {
  @Serial private static final long serialVersionUID = 8121014592707608414L;
  private final List<K3Statement> statements;
  private final FileLocation fileLocation;

  public K3SequenceStatement(
      List<K3Statement> pStatements,
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    statements = pStatements;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(K3ControlFlowStatementVisitor<R, X> v) throws X {
    return v.accept(this);
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
    return "(" + Joiner.on(" ) ( ").join(statements) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3SequenceStatement other
        && super.equals(other)
        && statements.equals(other.statements);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + statements.hashCode();
    return result;
  }
}

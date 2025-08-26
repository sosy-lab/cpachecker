// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3IfStatement extends K3ControlFlowStatement {

  @Serial private static final long serialVersionUID = 8786709909853416125L;
  private final K3Term condition;
  private final K3Statement thenBranch;
  private final Optional<K3Statement> elseBranch;

  public K3IfStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      K3Term pCondition,
      K3Statement pThenBranch) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    thenBranch = pThenBranch;
    elseBranch = Optional.empty();
  }

  public K3IfStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      K3Term pCondition,
      K3Statement pThenBranch,
      K3Statement pElseBranch) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    thenBranch = pThenBranch;
    elseBranch = Optional.of(pElseBranch);
  }

  public K3Statement getThenBranch() {
    return thenBranch;
  }

  public Optional<K3Statement> getElseBranch() {
    return elseBranch;
  }

  @Override
  <R, X extends Exception> R accept(K3ControlFlowStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(if "
        + condition.toASTString(pAAstNodeRepresentation)
        + " "
        + thenBranch.toASTString(pAAstNodeRepresentation)
        + " "
        + (elseBranch.isPresent()
            ? elseBranch.orElseThrow().toASTString(pAAstNodeRepresentation)
            : "")
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(while "
        + condition.toParenthesizedASTString(pAAstNodeRepresentation)
        + " "
        + thenBranch.toParenthesizedASTString(pAAstNodeRepresentation)
        + " "
        + (elseBranch.isPresent()
            ? elseBranch.orElseThrow().toParenthesizedASTString(pAAstNodeRepresentation)
            : "")
        + ")";
  }

  public K3Term getCondition() {
    return condition;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + condition.hashCode();
    result = prime * result + thenBranch.hashCode();
    result = prime * result + (elseBranch.isPresent() ? elseBranch.orElseThrow().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3IfStatement other
        && super.equals(other)
        && condition.equals(other.condition)
        && thenBranch.equals(other.thenBranch)
        && elseBranch.equals(other.elseBranch);
  }
}

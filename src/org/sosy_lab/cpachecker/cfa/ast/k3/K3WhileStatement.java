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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3WhileStatement extends K3ControlFlowStatement {

  @Serial private static final long serialVersionUID = 8317989637505431967L;
  private final K3Term condition;
  private final K3Statement body;

  public K3WhileStatement(
      K3Term pCondition,
      K3Statement pBody,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      FileLocation pFileLocation) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    condition = pCondition;
    body = pBody;
  }

  @Override
  public <R, X extends Exception> R accept(K3StatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(while "
        + condition.toASTString(pAAstNodeRepresentation)
        + " "
        + body.toASTString(pAAstNodeRepresentation)
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(while "
        + condition.toParenthesizedASTString(pAAstNodeRepresentation)
        + " "
        + body.toParenthesizedASTString(pAAstNodeRepresentation)
        + ")";
  }

  public K3Term getCondition() {
    return condition;
  }

  public K3Statement getBody() {
    return body;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + condition.hashCode();
    result = prime * result + body.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3WhileStatement other
        && super.equals(other)
        && condition.equals(other.condition)
        && body.equals(other.body);
  }
}

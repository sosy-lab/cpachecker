// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3AssignmentStatement extends K3CfaEdgeStatement {
  @Serial private static final long serialVersionUID = 5878865332404007544L;
  private final Map<K3SimpleDeclaration, K3Term> assignments;

  public K3AssignmentStatement(
      Map<K3SimpleDeclaration, K3Term> pAssignments,
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    assignments = pAssignments;
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
    return "(assign ("
        + ("("
            + Joiner.on(")(")
                .join(
                    transformedImmutableListCopy(
                        assignments.entrySet(), entry -> entry.getKey() + " " + entry.getValue()))
            + ")")
        + "))";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public Map<K3SimpleDeclaration, K3Term> getAssignments() {
    return assignments;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3AssignmentStatement other
        && assignments.equals(other.assignments)
        && super.equals(pO);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = result * prime + assignments.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

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
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3HavocVariablesStep extends K3TraceStep {
  @Serial private static final long serialVersionUID = -1341873304472826329L;
  private final Map<K3IdTerm, K3ConstantTerm> assignments;

  public K3HavocVariablesStep(Map<K3IdTerm, K3ConstantTerm> pValues, FileLocation pFileLocation) {
    super(pFileLocation);
    assignments = pValues;
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(havoc "
        + Joiner.on(" ")
            .join(
                assignments.entrySet().stream()
                    .map(
                        entry ->
                            "("
                                + entry.getKey().toASTString()
                                + " "
                                + entry.getValue().toASTString()
                                + ")")
                    .toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public Map<K3IdTerm, K3ConstantTerm> getAssignments() {
    return assignments;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + assignments.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3HavocVariablesStep other && assignments.equals(other.assignments);
  }
}

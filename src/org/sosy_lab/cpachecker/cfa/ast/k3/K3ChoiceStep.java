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

public final class K3ChoiceStep extends K3TraceStep {

  @Serial private static final long serialVersionUID = 3030434235034450950L;
  private final int statementToFollow;

  public K3ChoiceStep(int pStatementToFollow, FileLocation pFileLocation) {
    super(pFileLocation);
    statementToFollow = pStatementToFollow;
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
    return "(choice " + statementToFollow + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public int getStatementToFollow() {
    return statementToFollow;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + statementToFollow;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3ChoiceStep other && statementToFollow == other.statementToFollow;
  }
}

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

public final class K3IncorrectTagProperty extends K3ViolatedProperty {
  @Serial private static final long serialVersionUID = 5489687141447266694L;
  private final String tagName;
  private final K3FinalRelationalTerm violatedTerm;

  K3IncorrectTagProperty(
      FileLocation pFileLocation, String pTagName, K3FinalRelationalTerm pViolatedTerm) {
    super(pFileLocation);
    tagName = pTagName;
    violatedTerm = pViolatedTerm;
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
    return "(incorrect-tag " + tagName + " " + violatedTerm.toASTString() + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public String getTagName() {
    return tagName;
  }

  public K3FinalRelationalTerm getViolatedTerm() {
    return violatedTerm;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + tagName.hashCode();
    result = prime * result + violatedTerm.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3IncorrectTagProperty other
        && tagName.equals(other.tagName)
        && violatedTerm.equals(other.violatedTerm);
  }
}

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

public final class K3ChoiceStatement extends K3ControlFlowStatement {
  @Serial private static final long serialVersionUID = 6120083959916315980L;
  private final List<K3Statement> choices;

  public K3ChoiceStatement(
      List<K3Statement> pChoices,
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    choices = pChoices;
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
    return "(choice"
        + Joiner.on(" ").join(choices.stream().map(K3Statement::toASTString).toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  public List<K3Statement> getChoices() {
    return choices;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + choices.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3ChoiceStatement other
        && choices.equals(other.choices)
        && super.equals(other);
  }
}

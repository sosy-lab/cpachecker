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

public final class K3GotoStatement extends K3ControlFlowStatement {
  @Serial private static final long serialVersionUID = -4425367471857709362L;

  private final String label;

  public K3GotoStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences,
      String pLabel) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    label = pLabel;
  }

  public String getLabel() {
    return label;
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
    return "(goto " + label + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(goto " + label + ")";
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 31 * label.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3GotoStatement other && super.equals(other) && label.equals(other.label);
  }
}

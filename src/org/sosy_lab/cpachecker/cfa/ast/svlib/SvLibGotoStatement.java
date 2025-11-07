// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibGotoStatement extends SvLibControlFlowStatement {
  @Serial private static final long serialVersionUID = -4425367471857709362L;

  private final String label;

  public SvLibGotoStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences,
      String pLabel) {
    super(pFileLocation, pTagAttributes, pTagReferences);
    label = pLabel;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
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

    return pO instanceof SvLibGotoStatement other
        && super.equals(other)
        && label.equals(other.label);
  }
}

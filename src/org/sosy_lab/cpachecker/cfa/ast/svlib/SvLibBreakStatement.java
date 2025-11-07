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

public final class SvLibBreakStatement extends SvLibControlFlowStatement {
  @Serial private static final long serialVersionUID = -3464107498908941097L;

  public SvLibBreakStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
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
    return "(break)";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(break)";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof SvLibBreakStatement && super.equals(pO);
  }
}

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

public abstract sealed class K3ControlFlowStatement extends K3Statement
    permits K3AssumeStatement,
        K3BreakStatement,
        K3ContinueStatement,
        K3GotoStatement,
        K3IfStatement,
        K3LabelStatement,
        K3ReturnStatement,
        K3SequenceStatement,
        K3WhileStatement {

  @Serial private static final long serialVersionUID = -5733006204625200260L;

  protected K3ControlFlowStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
  }

  abstract <R, X extends Exception> R accept(K3ControlFlowStatementVisitor<R, X> v) throws X;

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3ControlFlowStatement && super.equals(pO);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}

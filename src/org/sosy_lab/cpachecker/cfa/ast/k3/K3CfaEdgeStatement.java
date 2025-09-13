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
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class K3CfaEdgeStatement extends K3Statement implements AStatement
    permits K3AssignmentStatement, K3HavocStatement, K3ProcedureCallStatement {
  @Serial private static final long serialVersionUID = 5250154309306501123L;

  protected K3CfaEdgeStatement(
      FileLocation pFileLocation,
      List<K3TagProperty> pTagAttributes,
      List<K3TagReference> pTagReferences) {
    super(pFileLocation, pTagAttributes, pTagReferences);
  }

  abstract <R, X extends Exception> R accept(K3CfaEdgeStatementVisitor<R, X> v) throws X;

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3CfaEdgeStatement && super.equals(pO);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}

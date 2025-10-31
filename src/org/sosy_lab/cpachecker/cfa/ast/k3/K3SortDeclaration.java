// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SortDeclaration extends AVariableDeclaration implements K3Declaration {

  @Serial private static final long serialVersionUID = -4339353958320549395L;

  public K3SortDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      K3Type pType,
      String pName,
      String pOrigName,
      String pQualifiedName) {
    super(
        pFileLocation,
        pIsGlobal,
        pType,
        pName,
        pOrigName,
        pQualifiedName,
        null /* There are no initializers in K3 */);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public K3Type getType() {
    return (K3Type) super.getType();
  }
}

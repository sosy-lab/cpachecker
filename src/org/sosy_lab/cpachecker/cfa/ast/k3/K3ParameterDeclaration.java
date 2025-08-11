// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3ParameterDeclaration extends AParameterDeclaration
    implements K3SimpleDeclaration {
  @Serial private static final long serialVersionUID = -720428046149807846L;

  public K3ParameterDeclaration(FileLocation pFileLocation, K3Type pType, String pName) {
    super(pFileLocation, pType, pName);
  }

  @Override
  public K3Type getType() {
    return (K3Type) super.getType();
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof K3ParameterDeclaration other && super.equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}

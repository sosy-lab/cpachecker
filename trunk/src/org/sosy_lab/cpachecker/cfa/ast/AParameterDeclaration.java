// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AParameterDeclaration extends AbstractSimpleDeclaration {

  private static final long serialVersionUID = 7623251138394648617L;

  protected AParameterDeclaration(FileLocation pFileLocation, Type pType, String pName) {
    super(pFileLocation, pType, checkNotNull(pName));
  }

  @Override
  public String toASTString(boolean pQualified) {
    if (pQualified) {
      return getType().toASTString(getQualifiedName().replace("::", "__"));
    } else {
      return getType().toASTString(getName());
    }
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AParameterDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }
}

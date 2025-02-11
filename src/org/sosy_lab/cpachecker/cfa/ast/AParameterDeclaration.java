// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AParameterDeclaration extends AbstractSimpleDeclaration {

  @Serial private static final long serialVersionUID = 7623251138394648617L;

  private final Type type;

  protected AParameterDeclaration(FileLocation pFileLocation, Type pType, String pName) {
    super(pFileLocation, checkNotNull(pName));
    type = pType;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return switch (pAAstNodeRepresentation) {
      case DEFAULT -> getType().toASTString(getName());
      case QUALIFIED -> getType().toASTString(getQualifiedName().replace("::", "__"));
      case ORIGINAL_NAMES -> getType().toASTString(getOrigName());
    };
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

    return obj instanceof AParameterDeclaration && super.equals(obj);
  }
}

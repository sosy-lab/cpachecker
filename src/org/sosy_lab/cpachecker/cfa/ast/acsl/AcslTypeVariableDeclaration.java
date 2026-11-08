// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

/** Declaration of a type variable of a polymorphic logic function or predicate. */
public final class AcslTypeVariableDeclaration extends AbstractDeclaration
    implements AcslSimpleDeclaration {

  @Serial private static final long serialVersionUID = -6073836511235L;
  private final String qualifiedName;

  public AcslTypeVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      AcslType pType,
      String pName,
      String pQualifiedName) {
    super(checkNotNull(pFileLocation), pIsGlobal, checkNotNull(pType), checkNotNull(pName), pName);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public AcslType getType() {
    return (AcslType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
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

    return obj instanceof AcslTypeVariableDeclaration && super.equals(obj);
  }
}

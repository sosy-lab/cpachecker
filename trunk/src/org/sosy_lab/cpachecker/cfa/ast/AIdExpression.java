// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AIdExpression extends AbstractLeftHandSide {

  private static final long serialVersionUID = -2534849615394054260L;
  private final String name;
  private final ASimpleDeclaration declaration;

  protected AIdExpression(
      FileLocation pFileLocation,
      Type pType,
      final String pName,
      final ASimpleDeclaration pDeclaration) {
    super(pFileLocation, pType);
    name = pName.intern();
    declaration = pDeclaration;
  }

  protected AIdExpression(FileLocation pFileLocation, ASimpleDeclaration pDeclaration) {
    this(pFileLocation, pDeclaration.getType(), pDeclaration.getName(), pDeclaration);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    return toASTString(pQualified);
  }

  @Override
  public String toASTString(boolean pQualified) {
    if (pQualified) {
      ASimpleDeclaration decl = getDeclaration();
      if (decl != null) {
        String qualName = decl.getQualifiedName();
        if (qualName != null) {
          return qualName.replace("::", "__");
        }
      }
    }
    return name;
  }

  public ASimpleDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(declaration);
    result = prime * result + Objects.hashCode(name);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AIdExpression) || !super.equals(obj)) {
      return false;
    }

    AIdExpression other = (AIdExpression) obj;

    return Objects.equals(other.declaration, declaration) && Objects.equals(other.name, name);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslIdTerm extends AcslTerm {

  @Serial private static final long serialVersionUID = -81455024312376L;

  private final String name;
  private final AcslSimpleDeclaration declaration;

  private AcslIdTerm(
      FileLocation pFileLocation,
      AcslType pType,
      final String pName,
      final AcslSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType);
    name = pName.intern();
    declaration = pDeclaration;
  }

  public AcslIdTerm(FileLocation pFileLocation, AcslSimpleDeclaration pDeclaration) {
    this(pFileLocation, pDeclaration.getType(), pDeclaration.getName(), pDeclaration);
  }

  public AcslSimpleDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return switch (pAAstNodeRepresentation) {
      case QUALIFIED -> {
        ASimpleDeclaration decl = getDeclaration();
        if (decl != null) {
          String qualName = decl.getQualifiedName();
          if (qualName != null) {
            yield qualName.replace("::", "__");
          }
        }
        yield name;
      }
      case ORIGINAL_NAMES -> {
        ASimpleDeclaration decl = getDeclaration();
        if (decl != null) {
          String origName = decl.getOrigName();
          if (origName != null) {
            yield origName;
          }
        }
        yield name;
      }
      case DEFAULT -> name;
    };
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 9;
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

    return obj instanceof AcslIdTerm other
        && super.equals(other)
        && Objects.equals(other.declaration, declaration)
        && Objects.equals(other.name, name);
  }
}

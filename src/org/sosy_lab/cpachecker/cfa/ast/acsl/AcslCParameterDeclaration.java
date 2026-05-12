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
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

/**
 * Easy way to lift C parameter declarations to ACSL. Note: try to avoid working on this level if
 * possible. But sometimes it is easier to use existing infrastructure for C.
 */
public final class AcslCParameterDeclaration extends AParameterDeclaration
    implements AcslSimpleDeclaration {

  @Serial private static final long serialVersionUID = -6179734345761871856L;

  @Nullable private final CParameterDeclaration cDeclaration;

  public AcslCParameterDeclaration(
      final FileLocation pFileLocation,
      final AcslCType pType,
      final String pName,
      @Nullable final CParameterDeclaration pDeclaration) {
    super(pFileLocation, pType, pName);
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pName);
    cDeclaration = pDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String getQualifiedName() {
    // Problematic due to the different kinds of declarations that are merged in this class
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public AcslCType getType() {
    return (AcslCType) super.getType();
  }

  public @Nullable CParameterDeclaration getDeclaration() {
    return cDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof AcslCParameterDeclaration other) {
      // Don't call super.equals() here,
      // it compares the declaration field.
      // In C, there might be several declarations declaring the same variable,
      // so we sometimes need to return true even with different declarations.

      @Nullable CSimpleDeclaration decl = getDeclaration();
      @Nullable CSimpleDeclaration otherDecl = other.getDeclaration();
      if (decl == null || otherDecl == null) {
        return decl == otherDecl;
      } else {
        return Objects.equals(decl.getQualifiedName(), otherDecl.getQualifiedName());
      }
    }

    return false;
  }
}

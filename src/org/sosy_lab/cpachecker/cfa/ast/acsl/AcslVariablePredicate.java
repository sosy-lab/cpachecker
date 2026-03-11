// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslVariablePredicate implements AcslPredicate {
  @Serial private static final long serialVersionUID = -3031077988792550124L;

  private final FileLocation fileLocation;
  private final AcslSimpleDeclaration declaration;

  public AcslVariablePredicate(FileLocation pFileLocation, AcslSimpleDeclaration pDeclaration) {
    Preconditions.checkNotNull(pFileLocation);
    Preconditions.checkNotNull(pDeclaration);

    Verify.verify(
        pDeclaration instanceof AcslParameterDeclaration
            || pDeclaration instanceof AcslVariableDeclaration,
        "The declaration should be an acsl parameter declaration or an acsl variable declaration"
            + " but was: %s",
        pDeclaration.getType());
    fileLocation = pFileLocation;
    declaration = pDeclaration;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public Type getExpressionType() {
    return declaration.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return declaration.getName();
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + declaration.getName() + ")";
  }

  @Override
  public boolean equals(Object p0) {
    if (this == p0) {
      return true;
    }
    return p0 instanceof AcslVariablePredicate other && declaration.equals(other.declaration);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(declaration);
    return hash;
  }
}

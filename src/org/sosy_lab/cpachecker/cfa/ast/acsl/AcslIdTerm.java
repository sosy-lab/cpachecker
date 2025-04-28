// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslIdTerm extends AIdExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = -81455024312376L;

  private AcslIdTerm(
      FileLocation pFileLocation,
      AcslType pType,
      final String pName,
      final AcslSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }

  public AcslIdTerm(FileLocation pFileLocation, AcslSimpleDeclaration pDeclaration) {
    this(pFileLocation, pDeclaration.getType(), pDeclaration.getName(), pDeclaration);
  }

  public AcslSimpleDeclaration getDeclaration() {
    return (AcslSimpleDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public final class AcslCIdExpression extends AIdExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = -4055747714336245906L;

  CIdExpression idExpr;

  public AcslCIdExpression(
      final FileLocation pFileLocation, final AcslType pType, final CIdExpression pCIdExpr) {
    super(
        checkNotNull(pFileLocation),
        checkNotNull(pType),
        checkNotNull(pCIdExpr).getName(),
        checkNotNull(pCIdExpr.getDeclaration()));
    idExpr = pCIdExpr;
  }

  public CIdExpression getCIdExpression() {
    return idExpr;
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

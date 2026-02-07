// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

/**
 * Lifts a {@link CExpression} to ACSL. The expression is to be evaluated in its programming
 * language, while the result is then used logically.
 */
public final class AcslCExpressionTerm extends AbstractExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = -8779021790333490934L;

  private final CExpression expr;

  /**
   * Lifts a {@link CExpression} to ACSL. The expression is to be evaluated in its programming
   * language, while the result is then used logically.
   *
   * @param pCExpression arguments of type {@link CExpression} that do not have a more specialized
   *     ACSL representation.
   */
  public AcslCExpressionTerm(FileLocation pFileLocation, AcslType pType, CExpression pCExpression) {
    super(checkNotNull(pFileLocation), checkNotNull(pType));
    checkArgument(
        !(pCExpression instanceof CLeftHandSide),
        "Arguments of type CLeftHandSide should be created using AcslCLeftHandSideTerm");
    expr = checkNotNull(pCExpression);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public CExpression getCExpression() {
    return expr;
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
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return expr.toASTString(pAAstNodeRepresentation);
  }
}

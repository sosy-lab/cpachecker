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
import org.sosy_lab.cpachecker.cfa.ast.AbstractLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

// TODO: build AcslCExpressionTerm that is the super implementation of this
/**
 * Used to lift an arbitrary {@link CLeftHandSide} (i.e. side-effect free C expression) that can not
 * be expressed using one of the existing ACSL term classes (e.g. {@link AcslBinaryTerm} or {@link
 * AcslIdTerm} already exist, but {@link org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference} does not
 * and can not be expressed using ACSL)
 */
public final class AcslCLeftHandSideTerm extends AbstractLeftHandSide implements AcslTerm {

  @Serial private static final long serialVersionUID = 4506265338118081722L;

  private final CLeftHandSide leftHandSide;

  /**
   * Lifts a {@link CLeftHandSide} to ACSL. The expression is to be evaluated in its programming
   * language, while the result is then used logically.
   *
   * @param pCLeftHandSideExpr arguments of type {@link CLeftHandSide} that do not have a more
   *     specialized ACSL representation.
   */
  public AcslCLeftHandSideTerm(
      FileLocation pFileLocation, AcslType pType, CLeftHandSide pCLeftHandSideExpr) {
    super(checkNotNull(pFileLocation), checkNotNull(pType));
    leftHandSide = checkNotNull(pCLeftHandSideExpr);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public CLeftHandSide getCLeftHandSideExpression() {
    return leftHandSide;
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
    return leftHandSide.toASTString(pAAstNodeRepresentation);
  }
}

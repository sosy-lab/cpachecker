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
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslAtTerm extends AbstractExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 8041058356265538956L;
  private final AcslTerm term;
  private final AcslLabel label;

  public AcslAtTerm(FileLocation pLocation, AcslTerm pTerm, AcslLabel pLabel) {
    super(pLocation, checkNotNull(pTerm).getExpressionType());
    term = pTerm;
    label = checkNotNull(pLabel);
  }

  public AcslLabel getLabel() {
    return label;
  }

  public AcslTerm getTerm() {
    return term;
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
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\at("
        + term.toASTString(pAAstNodeRepresentation)
        + ", "
        + label.toASTString(pAAstNodeRepresentation)
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 2;
    result = prime * result + super.hashCode();
    result = prime * result + term.hashCode();
    result = prime * result + label.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslAtTerm other
        && super.equals(other)
        && other.term.equals(term)
        && other.label.equals(label);
  }
}

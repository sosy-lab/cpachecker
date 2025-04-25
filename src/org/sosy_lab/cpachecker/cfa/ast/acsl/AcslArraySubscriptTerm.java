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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslArraySubscriptTerm extends AcslTerm {

  @Serial private static final long serialVersionUID = 8359800949073538182L;
  private final AcslTerm arrayTerm;
  private final AcslTerm subscriptTerm;

  public AcslArraySubscriptTerm(
      FileLocation pLocation, AcslType pType, AcslTerm pArrayTerm, AcslTerm pSubscriptTerm) {
    super(pLocation, pType);
    arrayTerm = pArrayTerm;
    subscriptTerm = pSubscriptTerm;
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
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return arrayTerm.toParenthesizedASTString(pAAstNodeRepresentation)
        + "["
        + subscriptTerm.toASTString(pAAstNodeRepresentation)
        + "]";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + arrayTerm.toParenthesizedASTString(pAAstNodeRepresentation)
        + "["
        + subscriptTerm.toParenthesizedASTString(pAAstNodeRepresentation)
        + "])";
  }

  @Override
  public int hashCode() {
    final int prime = 7;
    int result = 7;
    result = prime * result + super.hashCode();
    result = prime * result + Objects.hashCode(arrayTerm);
    result = prime * result + Objects.hashCode(subscriptTerm);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslArraySubscriptTerm other
        && super.equals(obj)
        && Objects.equals(arrayTerm, other.arrayTerm)
        && Objects.equals(subscriptTerm, other.subscriptTerm);
  }
}

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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslBooleanLiteralPredicate extends AcslLiteralPredicate {

  @Serial private static final long serialVersionUID = 7019912361956900L;

  private final boolean value;

  public AcslBooleanLiteralPredicate(FileLocation pFileLocation, boolean pValue) {
    super(pFileLocation);
    value = pValue;
    checkNotNull(pFileLocation);
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    return value ? "\\true" : "\\false";
  }

  @Override
  public String toQualifiedASTString() {
    return toASTString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslBooleanLiteralPredicate other
        && super.equals(other)
        && other.value == value;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

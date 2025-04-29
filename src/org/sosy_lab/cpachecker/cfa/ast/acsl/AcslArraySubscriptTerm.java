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
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslArraySubscriptTerm extends AArraySubscriptExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 8359800949073538182L;

  public AcslArraySubscriptTerm(
      FileLocation pLocation, AcslType pType, AcslTerm pArrayTerm, AcslTerm pSubscriptTerm) {
    super(pLocation, pType, pArrayTerm, pSubscriptTerm);
    checkNotNull(pLocation);
    checkNotNull(pType);
    checkNotNull(pArrayTerm);
    checkNotNull(pSubscriptTerm);
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
  public AcslTerm getArrayExpression() {
    return (AcslTerm) super.getArrayExpression();
  }

  @Override
  public AcslTerm getSubscriptExpression() {
    return (AcslTerm) super.getSubscriptExpression();
  }
}

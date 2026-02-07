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

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslFunctionCallPredicate extends AFunctionCallExpression
    implements AcslPredicate {

  @Serial private static final long serialVersionUID = -3695619915134937160L;

  public AcslFunctionCallPredicate(
      FileLocation pLocation,
      AcslTerm pFunctionName,
      List<AcslTerm> pParameters,
      AcslPredicateDeclaration pDeclaration) {
    super(
        checkNotNull(pLocation),
        AcslBuiltinLogicType.BOOLEAN,
        checkNotNull(pFunctionName),
        checkNotNull(pParameters),
        checkNotNull(pDeclaration));
    checkArgument(pDeclaration.getType().getReturnType().equals(AcslBuiltinLogicType.BOOLEAN));
  }

  @Override
  public AcslTerm getFunctionNameExpression() {
    return (AcslTerm) super.getFunctionNameExpression();
  }

  @Override
  @SuppressWarnings(
      "unchecked") // This is always correct per construction, but checkstyle complains
  public ImmutableList<AcslTerm> getParameterExpressions() {
    return (ImmutableList<AcslTerm>) super.getParameterExpressions();
  }

  @Override
  public AcslFunctionDeclaration getDeclaration() {
    return (AcslFunctionDeclaration) super.getDeclaration();
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) getDeclaration().getType().getReturnType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

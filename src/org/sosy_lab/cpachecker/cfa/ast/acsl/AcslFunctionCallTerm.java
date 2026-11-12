// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

public final class AcslFunctionCallTerm extends AFunctionCallExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 300878915220884911L;

  public AcslFunctionCallTerm(
      FileLocation pLocation,
      AcslType pType,
      AcslTerm pFunctionName,
      List<AcslTerm> pParameters,
      AcslFunctionDeclaration pDeclaration) {
    super(
        checkNotNull(pLocation),
        checkNotNull(pType),
        checkNotNull(pFunctionName),
        checkNotNull(pParameters),
        checkNotNull(pDeclaration));
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
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslType getExpressionType() {
    return getDeclaration().getType().getReturnType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return null;
  }
}

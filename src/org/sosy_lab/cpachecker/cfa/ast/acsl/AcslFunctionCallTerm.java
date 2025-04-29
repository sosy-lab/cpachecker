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
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslFunctionCallTerm extends AFunctionCallExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = -612040123327639887L;

  public AcslFunctionCallTerm(
      FileLocation pLocation,
      AcslType pType,
      AcslTerm pFunctionName,
      List<AcslTerm> pParameters,
      AcslFunctionDeclaration pDeclaration) {
    super(pLocation, pType, pFunctionName, pParameters, pDeclaration);
    checkNotNull(pLocation);
    checkNotNull(pType);
    checkNotNull(pFunctionName);
    checkNotNull(pParameters);
    checkNotNull(pDeclaration);
  }

  @Override
  public AcslTerm getFunctionNameExpression() {
    return (AcslTerm) super.getFunctionNameExpression();
  }

  @Override
  @SuppressWarnings(
      "unchecked") // This is always correct per construction, but checkstyle complains
  public List<AcslTerm> getParameterExpressions() {
    return (List<AcslTerm>) super.getParameterExpressions();
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
    return (AcslType) getDeclaration().getType().getReturnType();
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

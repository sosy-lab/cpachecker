// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
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
    checkArgument(
        pType.equals(pDeclaration.getType().getReturnType()),
        "The type %s of the call is not the return type of %s",
        pType,
        pDeclaration);
    checkArgument(
        pFunctionName instanceof AcslIdTerm idTerm && idTerm.getDeclaration().equals(pDeclaration),
        "%s does not name the function %s",
        pFunctionName,
        pDeclaration);
    checkArgument(
        argumentsFit(pParameters, pDeclaration.getType().getParameters()),
        "%s cannot be applied to %s",
        pDeclaration,
        pParameters);
  }

  static boolean argumentsFit(List<AcslTerm> pArguments, List<AcslType> pParameterTypes) {
    if (pArguments.size() != pParameterTypes.size()) {
      return false;
    }
    for (int i = 0; i < pArguments.size(); i++) {
      if (!AcslType.areCompatible(pArguments.get(i).getExpressionType(), pParameterTypes.get(i))) {
        return false;
      }
    }
    return true;
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
}

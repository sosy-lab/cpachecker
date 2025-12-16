// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public class SvLibFunctionCallExpression extends AFunctionCallExpression
    implements SvLibRightHandSide, SvLibAstNode {
  @Serial private static final long serialVersionUID = -6970061545000762188L;

  public SvLibFunctionCallExpression(
      FileLocation pFileLocation,
      SvLibType pType,
      SvLibIdTerm pFunctionName,
      List<SvLibTerm> pParameters,
      SvLibFunctionDeclaration pDeclaration) {
    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<SvLibTerm> getParameterExpressions() {
    return (ImmutableList<SvLibTerm>) super.getParameterExpressions();
  }

  @Override
  public SvLibFunctionDeclaration getDeclaration() {
    return (SvLibFunctionDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CFunctionCallExpression extends AFunctionCallExpression implements CRightHandSide {

  private static final long serialVersionUID = -3958452761163446674L;

  public CFunctionCallExpression(
      final FileLocation pFileLocation,
      final CType pType,
      final CExpression pFunctionName,
      final List<CExpression> pParameters,
      final CFunctionDeclaration pDeclaration) {

    super(pFileLocation, pType, pFunctionName, pParameters, pDeclaration);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public CExpression getFunctionNameExpression() {
    return (CExpression) super.getFunctionNameExpression();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CExpression> getParameterExpressions() {
    return (List<CExpression>) super.getParameterExpressions();
  }

  /**
   * Get the declaration of the function. A function may have several declarations in a C file
   * (several forward declarations without a body, and one with it). In this case, it is not defined
   * which declaration is returned.
   *
   * <p>The result may be null if the function was not declared, or if a complex function name
   * expression is used (i.e., a function pointer).
   */
  @Override
  public CFunctionDeclaration getDeclaration() {

    return (CFunctionDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionCallExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}

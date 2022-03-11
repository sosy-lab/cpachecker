// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AFunctionCallExpression extends AbstractRightHandSide {

  private static final long serialVersionUID = -6120400526327639887L;
  private final AExpression functionName;
  private final List<? extends AExpression> parameters;
  private final AFunctionDeclaration declaration;

  protected AFunctionCallExpression(
      FileLocation pFileLocation,
      Type pType,
      final AExpression pFunctionName,
      final List<? extends AExpression> pParameters,
      final AFunctionDeclaration pDeclaration) {
    super(pFileLocation, pType);
    functionName = pFunctionName;
    parameters = ImmutableList.copyOf(pParameters);
    declaration = pDeclaration;
  }

  public AExpression getFunctionNameExpression() {
    return functionName;
  }

  public List<? extends AExpression> getParameterExpressions() {
    return parameters;
  }

  /**
   * Get the declaration of the function. A function may have several declarations in a C file
   * (several forward declarations without a body, and one with it). In this case, it is not defined
   * which declaration is returned.
   *
   * <p>The result may be null if the function was not declared, or if a complex function name
   * expression is used (i.e., a function pointer).
   */
  public AFunctionDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public String toASTString(final boolean pQualified) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(functionName.toParenthesizedASTString(pQualified));
    lASTString.append("(");
    Joiner.on(", ")
        .appendTo(lASTString, transform(parameters, aexpr -> aexpr.toASTString(pQualified)));
    lASTString.append(")");

    return lASTString.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(declaration);
    result = prime * result + Objects.hashCode(functionName);
    result = prime * result + Objects.hashCode(parameters);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionCallExpression) || !super.equals(obj)) {
      return false;
    }

    AFunctionCallExpression other = (AFunctionCallExpression) obj;

    return Objects.equals(other.declaration, declaration)
        && Objects.equals(other.functionName, functionName)
        && Objects.equals(other.parameters, parameters);
  }
}

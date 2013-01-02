/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.collect.Iterables.transform;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.types.Type;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


public abstract class AFunctionCallExpression extends ARightHandSide {

  private final IAExpression functionName;
  private final List< ? extends IAExpression> parameters;
  private final IASimpleDeclaration declaration;


  public AFunctionCallExpression(FileLocation pFileLocation, Type pType, final IAExpression pFunctionName,
      final List<? extends IAExpression> pParameters,
      final IASimpleDeclaration pDeclaration) {
    super(pFileLocation, pType);
    functionName = pFunctionName;
    parameters = ImmutableList.copyOf(pParameters);
    declaration = pDeclaration;
  }

  public IAExpression getFunctionNameExpression() {
    return functionName;
  }

  public List<? extends IAExpression> getParameterExpressions() {
    return parameters;
  }

  /**
   * Get the declaration of the function.
   * A function may have several declarations in a C file (several forward
   * declarations without a body, and one with it). In this case, it is not
   * defined which declaration is returned.
   *
   * The result may be null if the function was not declared, or if a complex
   * function name expression is used (i.e., a function pointer).
   */
  public IASimpleDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(functionName.toParenthesizedASTString());
    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, transform(parameters, AstNode.TO_AST_STRING));
    lASTString.append(")");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((declaration == null) ? 0 : declaration.hashCode());
    result = prime * result + ((functionName == null) ? 0 : functionName.hashCode());
    result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof AFunctionCallExpression)) { return false; }
    AFunctionCallExpression other = (AFunctionCallExpression) obj;
    if (declaration == null) {
      if (other.declaration != null) { return false; }
    } else if (!declaration.equals(other.declaration)) { return false; }
    if (functionName == null) {
      if (other.functionName != null) { return false; }
    } else if (!functionName.equals(other.functionName)) { return false; }
    if (parameters == null) {
      if (other.parameters != null) { return false; }
    } else if (!parameters.equals(other.parameters)) { return false; }

    return super.equals(other);
  }

}
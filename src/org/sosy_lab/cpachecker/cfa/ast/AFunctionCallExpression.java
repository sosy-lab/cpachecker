/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


public abstract class AFunctionCallExpression extends ARightHandSide {

  private final IAExpression functionName;
  private final List<? extends IAExpression> parameters;
  private final AFunctionDeclaration declaration;


  public AFunctionCallExpression(FileLocation pFileLocation, Type pType, final IAExpression pFunctionName,
      final List<? extends IAExpression> pParameters,
      final AFunctionDeclaration pDeclaration) {
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
  public AFunctionDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(functionName.toParenthesizedASTString());
    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, transform(parameters, IAExpression.TO_AST_STRING));
    lASTString.append(")");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionCallExpression)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionCallExpression other = (AFunctionCallExpression) obj;

    return Objects.equals(other.declaration, declaration)
            && Objects.equals(other.functionName, functionName)
            && Objects.equals(other.parameters, parameters);
  }

}
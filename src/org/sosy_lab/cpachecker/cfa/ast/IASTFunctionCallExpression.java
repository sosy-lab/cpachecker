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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class IASTFunctionCallExpression extends IASTRightHandSide {

  private final IASTExpression functionName;
  private final List<IASTExpression> parameters;
  private final IASTSimpleDeclaration declaration;

  public IASTFunctionCallExpression(final IASTFileLocation pFileLocation,
                                    final IType pType,
                                    final IASTExpression pFunctionName,
                                    final List<IASTExpression> pParameters,
                                    final IASTSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType);
    functionName = pFunctionName;
    parameters = ImmutableList.copyOf(pParameters);
    declaration = pDeclaration;
  }

  public IASTExpression getFunctionNameExpression() {
    return functionName;
  }

  public List<IASTExpression> getParameterExpressions() {
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
  public IASTSimpleDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public <R, X extends Exception> R accept(RightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(functionName.toParenthesizedASTString());
    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, new ASTStringIterable(parameters));
    lASTString.append(")");

    return lASTString.toString();
  }
}

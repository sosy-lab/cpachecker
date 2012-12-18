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


  protected final IAExpression functionName;
  protected final List< ? extends IAExpression> parameters;
  protected final IASimpleDeclaration declaration;


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

}
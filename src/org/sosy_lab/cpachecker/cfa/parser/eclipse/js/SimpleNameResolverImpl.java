/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.core.dom.binding.FunctionBinding;
import org.eclipse.wst.jsdt.internal.core.dom.binding.VariableBinding;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

class SimpleNameResolverImpl implements SimpleNameResolver {
  private final VariableDeclarationRegistry variableDeclarationRegistry;
  private final FunctionDeclarationRegistry functionDeclarationRegistry;

  SimpleNameResolverImpl(
      final VariableDeclarationRegistry pVariableDeclarationRegistry,
      final FunctionDeclarationRegistry pFunctionDeclarationRegistry) {
    variableDeclarationRegistry = pVariableDeclarationRegistry;
    functionDeclarationRegistry = pFunctionDeclarationRegistry;
  }

  @Override
  public JSIdExpression resolve(final JavaScriptCFABuilder pBuilder, final SimpleName pSimpleName) {
    // undefined is writable in ES3, but not writable in ES5, see:
    // https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/undefined#Description
    // The used parser of Eclipse JSDT 3.9 does only support ES3.
    // Thereby, it creates a SimpleName for undefined instead of an UndefinedLiteral.
    // We like to be conformant to ES5.
    // That's why we convert it to an JSUndefinedLiteralExpression if it is used as an Expression.
    // However, it can not be resolved to an JSIdExpression.
    assert !pSimpleName.getIdentifier().equals("undefined")
        : "Can not resolve undefined"; // unsupported use of undefined
    final IBinding binding = pSimpleName.resolveBinding();
    assert binding != null;
    return new JSIdExpression(
        pBuilder.getFileLocation(pSimpleName),
        JSAnyType.ANY,
        pSimpleName.getIdentifier(),
        resolve(binding));
  }

  public JSSimpleDeclaration resolve(final IBinding pBinding) {
    if (pBinding instanceof VariableBinding) {
      return resolve((VariableBinding) pBinding);
    } else if (pBinding instanceof FunctionBinding) {
      return resolve((FunctionBinding) pBinding);
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of binding (not handled yet): " + pBinding.toString());
  }

  public JSVariableDeclaration resolve(final VariableBinding pBinding) {
    return resolve((VariableDeclarationFragment) pBinding.getDeclaration().getNode().getParent());
  }

  public JSFunctionDeclaration resolve(final FunctionBinding pBinding) {
    return resolve((FunctionDeclaration) pBinding.getDeclaration().getNode().getParent());
  }

  public JSFunctionDeclaration resolve(final FunctionDeclaration pDeclaration) {
    return functionDeclarationRegistry.get(pDeclaration);
  }

  public JSVariableDeclaration resolve(
      final VariableDeclarationFragment pVariableDeclarationFragment) {
    return variableDeclarationRegistry.get(pVariableDeclarationFragment);
  }
}

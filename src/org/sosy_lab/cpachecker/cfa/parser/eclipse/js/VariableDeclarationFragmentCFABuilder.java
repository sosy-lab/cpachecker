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

import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class VariableDeclarationFragmentCFABuilder implements VariableDeclarationFragmentAppendable {

  @Override
  public JSVariableDeclaration append(
      final JavaScriptCFABuilder pBuilder,
      final VariableDeclarationFragment pVariableDeclarationFragment) {
    final String variableIdentifier = pVariableDeclarationFragment.getName().getIdentifier();
    final JSExpression expression =
        pVariableDeclarationFragment.getInitializer() == null
          ? new JSUndefinedLiteralExpression(FileLocation.DUMMY)
          : pBuilder.append(pVariableDeclarationFragment.getInitializer());

    final Optional<? extends JSSimpleDeclaration> hoistedDeclaration =
        pBuilder.getScope().findDeclaration(variableIdentifier);
    final FunctionScope functionScope = pBuilder.getScope().getScope(FunctionScope.class);
    final Optional<? extends JSSimpleDeclaration> outerDeclaration =
        functionScope == null
            ? Optional.empty()
            : functionScope.getParentScope().findDeclaration(variableIdentifier);

    final JSVariableDeclaration variableDeclaration;
    // check if found declaration is hoisted declaration and not another (shadowed) declaration
    // outside of the function (scope
    if (hoistedDeclaration.isPresent() && !hoistedDeclaration.equals(outerDeclaration)) {
      // variable declared by a hoisted declaration using 'var' or a function declaration
      final JSSimpleDeclaration d = hoistedDeclaration.get();
      variableDeclaration =
          new JSVariableDeclaration(
              pBuilder.getFileLocation(pVariableDeclarationFragment),
              ScopeConverter.toCFAScope(pBuilder.getScope()),
              d.getName(),
              d.getOrigName(),
              d.getQualifiedName(),
              new JSInitializerExpression(expression.getFileLocation(), expression));
    } else {
      // variable declared using 'let' or 'const'
      variableDeclaration =
          new JSVariableDeclaration(
              pBuilder.getFileLocation(pVariableDeclarationFragment),
              ScopeConverter.toCFAScope(pBuilder.getScope()),
              variableIdentifier,
              variableIdentifier,
              pBuilder.getScope().qualifiedVariableNameOf(variableIdentifier),
              new JSInitializerExpression(expression.getFileLocation(), expression));
    }

    pBuilder.appendEdge(JSDeclarationEdge.of(variableDeclaration));
    return variableDeclaration;
  }
}

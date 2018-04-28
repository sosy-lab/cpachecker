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

import static org.sosy_lab.cpachecker.cfa.ast.java.QualifiedNameBuilder.qualifiedNameOf;

import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class VariableDeclarationFragmentCFABuilder implements VariableDeclarationFragmentAppendable {

  @Override
  public JSVariableDeclaration append(
      final JavaScriptCFABuilder pBuilder,
      final VariableDeclarationFragment pVariableDeclarationFragment) {
    final String variableIdentifier = pVariableDeclarationFragment.getName().getIdentifier();
    final JSExpression expression = pBuilder.append(pVariableDeclarationFragment.getInitializer());
    final JSVariableDeclaration variableDeclaration =
        new JSVariableDeclaration(
            pBuilder.getFileLocation(pVariableDeclarationFragment),
            false,
            variableIdentifier,
            variableIdentifier,
            qualifiedNameOf(pBuilder.getFunctionName(), variableIdentifier),
            new JSInitializerExpression(expression.getFileLocation(), expression));
    pBuilder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSDeclarationEdge(
                variableDeclaration.toASTString(),
                variableDeclaration.getFileLocation(),
                pPredecessor,
                pSuccessor,
                variableDeclaration));
    return variableDeclaration;
  }
}

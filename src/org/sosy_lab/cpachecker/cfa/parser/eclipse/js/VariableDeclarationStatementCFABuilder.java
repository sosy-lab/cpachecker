/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.List;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

class VariableDeclarationStatementCFABuilder {

  private final CFABuilder builder;

  VariableDeclarationStatementCFABuilder(final CFABuilder pBuilder) {
    builder = pBuilder;
  }

  public void append(final VariableDeclarationStatement node) {
    @SuppressWarnings("unchecked")
    final List<VariableDeclarationFragment> variableDeclarationFragments = node.fragments();
    for (final VariableDeclarationFragment variableDeclarationFragment :
        variableDeclarationFragments) {
      append(variableDeclarationFragment);
    }
  }

  private void append(final VariableDeclarationFragment pVariableDeclarationFragment) {
    final JSVariableDeclaration variableDeclaration =
        builder.getAstConverter().convert(pVariableDeclarationFragment);
    builder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSDeclarationEdge(
                variableDeclaration.toASTString(),
                builder.getAstConverter().getFileLocation(pVariableDeclarationFragment),
                pPredecessor,
                pSuccessor,
                variableDeclaration));
  }

  public CFABuilder getBuilder() {
    return builder;
  }
}

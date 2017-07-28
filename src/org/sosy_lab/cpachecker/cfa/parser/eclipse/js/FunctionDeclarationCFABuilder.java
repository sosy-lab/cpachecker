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

import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

class FunctionDeclarationCFABuilder
    implements CFABuilderWrapperOfType<FunctionDeclarationCFABuilder> {

  private final CFABuilder builder;

  FunctionDeclarationCFABuilder(final CFABuilder pBuilder) {
    builder = pBuilder;
  }

  public FunctionDeclarationCFABuilder append(final FunctionDeclaration pFunctionDeclaration) {
    final ASTConverter astConverter = builder.getAstConverter();
    final JSFunctionDeclaration jsFunctionDeclaration = astConverter.convert(pFunctionDeclaration);
    final String functionName = jsFunctionDeclaration.getName();
    final FunctionExitNode exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY, jsFunctionDeclaration, exitNode, Optional.empty());
    exitNode.setEntryNode(entryNode);
    final CFABuilder functionCFABuilder =
        new CFABuilder(builder.getLogger(), astConverter, entryNode);

    addFunctionEntryNode();

    final StatementCFABuilder bodyBuilder = new StatementCFABuilder(functionCFABuilder);
    bodyBuilder.append(pFunctionDeclaration.getBody());

    functionCFABuilder.appendEdge(
        exitNode,
        (pPredecessor, pSuccessor) ->
            new BlankEdge("", FileLocation.DUMMY, pPredecessor, pSuccessor, "default return"));

    builder.append(functionCFABuilder);
    return this;
  }

  /**
   * Add a dummy edge to allow a function call as first statement. Without this edge {@link
   * CFASecondPassBuilder#insertCallEdgesRecursively()} would consider the function call as
   * unreachable.
   */
  private void addFunctionEntryNode() {
    builder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new BlankEdge(
                "", FileLocation.DUMMY, pPredecessor, pSuccessor, "Function start dummy edge"));
  }

  @Override
  public CFABuilder getBuilder() {
    return builder;
  }
}

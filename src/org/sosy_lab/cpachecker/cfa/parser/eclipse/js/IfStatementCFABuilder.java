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

import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;

class IfStatementCFABuilder {

  private final CFABuilder builder;

  IfStatementCFABuilder(final CFABuilder pBuilder) {
    builder = pBuilder;
  }

  public void append(final IfStatement node) {
    final String conditionNodeDescription = builder.getExitNode().toString();
    final CFANode exitNode = builder.createNode();
    final ASTConverter converter = builder.getAstConverter();
    final JSExpression condition = converter.convert(node.getExpression());

    final StatementCFABuilder thenStatementBuilder =
        new StatementCFABuilder(
            new CFABuilder(
                builder.getLogger(), converter, builder.getFunctionName(), builder.getExitNode()));
    thenStatementBuilder
        .getBuilder()
        .appendEdge(
            (pPredecessor, pSuccessor) ->
                new JSAssumeEdge(
                    condition.toASTString(),
                    condition.getFileLocation(),
                    pPredecessor,
                    pSuccessor,
                    condition,
                    true));
    thenStatementBuilder.append(node.getThenStatement());
    thenStatementBuilder
        .getBuilder()
        .appendEdge(
            exitNode,
            (pPredecessor, pSuccessor) ->
                new BlankEdge(
                    "",
                    FileLocation.DUMMY,
                    pPredecessor,
                    pSuccessor,
                    "end if of node " + conditionNodeDescription));
    builder.addParseResult(thenStatementBuilder.getBuilder().getParseResult());

    builder.appendEdge(
        exitNode,
        (pPredecessor, pSuccessor) ->
            new JSAssumeEdge(
                condition.toASTString(),
                condition.getFileLocation(),
                pPredecessor,
                pSuccessor,
                condition,
                false));
  }

  public CFABuilder getBuilder() {
    return builder;
  }
}

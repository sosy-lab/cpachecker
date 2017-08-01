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
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;

class IfStatementCFABuilder implements IfStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder builder, final IfStatement node) {
    final String conditionNodeDescription = builder.getExitNode().toString();
    final CFANode exitNode = builder.createNode();
    final JSExpression condition = builder.append(node.getExpression());

    builder.addParseResult(
        buildConditionBranch(
                builder,
                true,
                condition,
                node.getThenStatement(),
                "end if of node " + conditionNodeDescription,
                exitNode)
            .getParseResult());

    final Statement elseStatement = node.getElseStatement();
    if (elseStatement == null) {
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
    } else {
      builder.append(
          buildConditionBranch(
                  builder,
                  false,
                  condition,
                  elseStatement,
                  "end else of node " + conditionNodeDescription,
                  exitNode)
              .getBuilder());
    }
  }

  /**
   * Build CFA of then- or else-branch.
   *
   * @param pTruthAssumption <code>true</code> if then-branch is built and <code>false</code> if
   *     else-branch is built.
   * @param pCondition The condition of the if-statement.
   * @param pStatement The then- or else-statement.
   * @param pExitEdgeDescription The description of the edge that will point to the exit node.
   * @param pExitNode The node after the if-statement, where then- and else-branch flow together.
   * @return The builder that contains the parse result of the branch.
   */
  private JavaScriptCFABuilder buildConditionBranch(
      final JavaScriptCFABuilder builder,
      final boolean pTruthAssumption,
      final JSExpression pCondition,
      final Statement pStatement,
      final String pExitEdgeDescription,
      final CFANode pExitNode) {
    return builder
        .copy()
        .appendEdge(
            (pPredecessor, pSuccessor) ->
                new JSAssumeEdge(
                    pCondition.toASTString(),
                    pCondition.getFileLocation(),
                    pPredecessor,
                    pSuccessor,
                    pCondition,
                    pTruthAssumption))
        .append(pStatement)
        .appendEdge(pExitNode, DummyEdge.withDescription(pExitEdgeDescription));
  }

}

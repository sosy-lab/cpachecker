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

import java.util.function.BiFunction;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;

class IfStatementCFABuilder implements IfStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final IfStatement pNode) {
    final String conditionNodeDescription = pBuilder.getExitNode().toString();
    final CFANode exitNode = pBuilder.createNode();
    final JSExpression condition = pBuilder.append(pNode.getExpression());

    pBuilder.addParseResult(
        buildConditionBranch(
            pBuilder,
                true,
                condition,
                pNode.getThenStatement(),
                "end if of node " + conditionNodeDescription,
                exitNode)
            .getParseResult());

    final Statement elseStatement = pNode.getElseStatement();
    if (elseStatement == null) {
      pBuilder.appendEdge(exitNode, assume(condition, false));
    } else {
      pBuilder.append(
          buildConditionBranch(
              pBuilder,
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
   * @return The pBuilder that contains the parse result of the branch.
   */
  private JavaScriptCFABuilder buildConditionBranch(
      final JavaScriptCFABuilder pBuilder,
      final boolean pTruthAssumption,
      final JSExpression pCondition,
      final Statement pStatement,
      final String pExitEdgeDescription,
      final CFANode pExitNode) {
    return pBuilder
        .copy()
        .appendEdge(assume(pCondition, pTruthAssumption))
        .append(pStatement)
        .appendEdge(pExitNode, DummyEdge.withDescription(pExitEdgeDescription));
  }

  private BiFunction<CFANode, CFANode, AbstractCFAEdge> assume(
      final JSExpression pCondition, final boolean pTruthAssumption) {
    return (pPredecessor, pSuccessor) ->
        new JSAssumeEdge(
            pCondition.toASTString(),
            pCondition.getFileLocation(),
            pPredecessor,
            pSuccessor,
            pCondition,
            pTruthAssumption);
  }

}

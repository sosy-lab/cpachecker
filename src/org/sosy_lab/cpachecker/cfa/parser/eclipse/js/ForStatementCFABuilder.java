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

import static org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge.assume;

import java.util.List;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

@SuppressWarnings("ResultOfMethodCallIgnored")
class ForStatementCFABuilder implements ForStatementAppendable {

  @SuppressWarnings("unchecked")
  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final ForStatement pNode) {
    final LoopScopeImpl loopScope = new LoopScopeImpl(pBuilder.getScope());
    final JavaScriptCFABuilder loopBuilder = pBuilder.copyWith(loopScope);
    for (final Expression initializer : (List<Expression>) pNode.initializers()) {
      loopBuilder.append(initializer);
    }
    final CFANode loopStartNode = loopBuilder.getExitNode();
    loopStartNode.setLoopStart();
    loopScope.setLoopStartNode(loopStartNode);
    final JSExpression condition = loopBuilder.append(pNode.getExpression());
    final JavaScriptCFABuilder loopEdgeBuilder = loopBuilder.copy();
    loopEdgeBuilder.appendEdge(assume(condition, true)).append(pNode.getBody());
    for (final Expression updater : (List<Expression>) pNode.updaters()) {
      loopEdgeBuilder.append(updater);
    }
    loopEdgeBuilder.appendEdge(
        loopStartNode, DummyEdge.withDescription("check for-loop condition after updaters"));
    loopBuilder.addParseResult(loopEdgeBuilder.getParseResult());
    final CFANode exitNode = loopBuilder.createNode();
    loopScope.setLoopExitNode(exitNode);
    loopBuilder.appendEdge(exitNode, assume(condition, false));
    loopBuilder.appendTo(pBuilder.getBuilder());
  }
}

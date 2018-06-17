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

import static org.mockito.Mockito.mock;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.util.test.ReturnValueCaptor;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class WhileStatementCFABuilderTest extends CFABuilderTestBase {

  private ReturnValueCaptor<AbstractCFAEdge> bodyStatementEdgeCaptor;
  private JavaScriptCFABuilder loopBuilder;

  @Override
  public void init() throws InvalidConfigurationException {
    super.init();
    bodyStatementEdgeCaptor = new ReturnValueCaptor<>();
  }

  @Test
  public final void testWhile() {
    final WhileStatement whileStatement =
        parseStatement(WhileStatement.class, "while (condition) { doSomething() }");
    // expected CFA: ------------------------------------------->((entryNode)) --[!condition]--> ()
    //                 |                                               |
    //                 \<-- () <--{doSomething()}-- () <--[condition]--/

    final JSExpression condition =
        new JSIdExpression(FileLocation.DUMMY, "condition", mock(JSSimpleDeclaration.class));
    final StatementAppendable statementAppendable =
        (pStatementBuilder, pStatement) ->
        {
          Truth.assertThat(pStatementBuilder.getScope()).isEqualTo(loopBuilder.getScope());
          pStatementBuilder.appendEdge(
              bodyStatementEdgeCaptor.captureReturn(
                  DummyEdge.withDescription("dummy statement edge")));
        };
    builder.setStatementAppendable(statementAppendable);
    builder.setExpressionAppendable((pExpressionBuilder, pExpression) -> {
      loopBuilder = pExpressionBuilder;
      return condition;
    });

    new WhileStatementCFABuilder().append(builder, whileStatement);

    Truth.assertThat(loopBuilder.getScope()).isInstanceOf(LoopScope.class);
    final LoopScope loopBuilderScope = (LoopScope) loopBuilder.getScope();
    Truth.assertThat(loopBuilderScope.getParentScope()).isEqualTo(builder.getScope());

    final CFANode loopStartNode = this.entryNode;
    Truth.assertThat(loopStartNode.isLoopStart()).isTrue();
    Truth.assertThat(loopBuilderScope.getLoopStartNode()).isEqualTo(loopStartNode);
    Truth.assertThat(loopStartNode.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) loopStartNode.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) loopStartNode.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge loopEntryEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge loopExitEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    Truth.assertThat(loopEntryEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(loopExitEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(loopEntryEdge.getExpression()).isEqualTo(condition);
    Truth.assertThat(loopExitEdge.getExpression()).isEqualTo(condition);

    final CFANode bodyNode = loopEntryEdge.getSuccessor();
    Truth.assertThat(bodyNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(bodyNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(bodyStatementEdgeCaptor.getTimesCalled()).isEqualTo(1);
    final AbstractCFAEdge bodyStatementEdge = bodyStatementEdgeCaptor.getReturnValue(0);
    Truth.assertThat(bodyNode.getLeavingEdge(0)).isEqualTo(bodyStatementEdge);
    Truth.assertThat(bodyStatementEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(bodyStatementEdge.getSuccessor().getLeavingEdge(0).getSuccessor())
        .isEqualTo(loopStartNode);

    final CFANode loopExitNode = loopExitEdge.getSuccessor();
    Truth.assertThat(loopExitNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(loopExitNode.getNumLeavingEdges()).isEqualTo(0);
    Truth.assertThat(loopExitNode).isEqualTo(builder.getExitNode());
    Truth.assertThat(loopBuilderScope.getLoopExitNode()).isEqualTo(loopExitNode);
  }
}

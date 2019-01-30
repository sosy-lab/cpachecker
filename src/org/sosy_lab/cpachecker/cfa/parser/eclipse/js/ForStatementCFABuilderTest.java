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
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.util.test.ReturnValueCaptor;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class ForStatementCFABuilderTest extends CFABuilderTestBase {

  private ReturnValueCaptor<CFAEdge> bodyStatementEdgeCaptor;
  private ReturnValueCaptor<CFAEdge> expressionEdgeCaptor;
  private JavaScriptCFABuilder loopBuilder;

  @Override
  public void init() {
    super.init();
    bodyStatementEdgeCaptor = new ReturnValueCaptor<>();
    expressionEdgeCaptor = new ReturnValueCaptor<>();
    loopBuilder = null;
  }

  @Test
  public final void testFor() {
    final ForStatement forStatement =
        parseStatement(ForStatement.class, "for (initializers; condition; updaters) { body }");
    // expected CFA:
    // (entryNode)               /----{condition}-------------------------> () --[!condition]--> ()
    //   |                      /                                            |
    //   \--{initializers}--> (()) <--{updaters}--{body}-- () <--[condition]--/

    final JSExpression condition =
        new JSIdExpression(FileLocation.DUMMY, "condition", mock(JSSimpleDeclaration.class));
    final JSExpression initializers =
        new JSIdExpression(FileLocation.DUMMY, "initializers", mock(JSSimpleDeclaration.class));
    final JSExpression updaters =
        new JSIdExpression(FileLocation.DUMMY, "updaters", mock(JSSimpleDeclaration.class));
    final StatementAppendable statementAppendable =
        (pBuilder, pStatement) -> {
          Truth.assertThat(pBuilder.getScope()).isEqualTo(loopBuilder.getScope());
          pBuilder.appendEdge(
              bodyStatementEdgeCaptor.captureReturn(
                  DummyEdge.withDescription("dummy statement edge")));
        };
    builder.setStatementAppendable(statementAppendable);

    final ExpressionAppendable expressionAppendable =
        (pBuilder, pExpression) -> {
          pBuilder.appendEdge(
              expressionEdgeCaptor.captureReturn(
                  DummyEdge.withDescription("dummy expression edge")));
          switch (((SimpleName) pExpression).getIdentifier()) {
            case "condition":
              Truth.assertThat(pBuilder.getScope()).isEqualTo(loopBuilder.getScope());
              return condition;
            case "initializers":
              loopBuilder = pBuilder;
              return initializers;
            case "updaters":
              Truth.assertThat(pBuilder.getScope()).isEqualTo(loopBuilder.getScope());
              return updaters;
            default:
              throw new CFAGenerationRuntimeException("Unexpected SimpleName expression");
          }
        };
    builder.setExpressionAppendable(expressionAppendable);

    new ForStatementCFABuilder().append(builder, forStatement);

    Truth.assertThat(loopBuilder.getScope()).isInstanceOf(LoopScope.class);
    final LoopScope loopBuilderScope = (LoopScope) loopBuilder.getScope();
    Truth.assertThat(loopBuilderScope.getParentScope()).isEqualTo(builder.getScope());

    Truth.assertThat(expressionEdgeCaptor.getTimesCalled()).isEqualTo(3);
    final CFAEdge initializersEdge = expressionEdgeCaptor.getReturnValue(0);
    final CFAEdge conditionEdge = expressionEdgeCaptor.getReturnValue(1);
    final CFAEdge updatersEdge = expressionEdgeCaptor.getReturnValue(2);

    Truth.assertThat(entryNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(entryNode.getLeavingEdge(0)).isEqualTo(initializersEdge);

    final CFANode loopStartNode = initializersEdge.getSuccessor();
    Truth.assertThat(loopStartNode.isLoopStart()).isTrue();
    Truth.assertThat(loopBuilderScope.getLoopStartNode()).isEqualTo(loopStartNode);
    Truth.assertThat(loopStartNode.getNumEnteringEdges()).isEqualTo(2);
    Truth.assertThat(loopStartNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(loopStartNode.getLeavingEdge(0)).isEqualTo(conditionEdge);

    final CFANode checkConditionNode = conditionEdge.getSuccessor();
    final JSAssumeEdge firstEdge = (JSAssumeEdge) checkConditionNode.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) checkConditionNode.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge loopEntryEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge loopExitEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    final CFANode loopExitNode = loopExitEdge.getSuccessor();
    Truth.assertThat(loopExitNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(loopExitNode.getNumLeavingEdges()).isEqualTo(0);
    Truth.assertThat(loopExitNode).isEqualTo(builder.getExitNode());
    Truth.assertThat(loopBuilderScope.getLoopExitNode()).isEqualTo(loopExitNode);

    Truth.assertThat(loopEntryEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(loopExitEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(loopEntryEdge.getExpression()).isEqualTo(condition);
    Truth.assertThat(loopExitEdge.getExpression()).isEqualTo(condition);

    final CFANode bodyNode = loopEntryEdge.getSuccessor();
    Truth.assertThat(bodyNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(bodyNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(bodyStatementEdgeCaptor.getTimesCalled()).isEqualTo(1);
    final CFAEdge bodyStatementEdge = bodyStatementEdgeCaptor.getReturnValue(0);
    Truth.assertThat(bodyNode.getLeavingEdge(0)).isEqualTo(bodyStatementEdge);

    final CFANode beforeUpdatersNode = bodyStatementEdge.getSuccessor();
    Truth.assertThat(beforeUpdatersNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(beforeUpdatersNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(beforeUpdatersNode.getLeavingEdge(0)).isInstanceOf(BlankEdge.class);
    final CFAEdge updatersDescriptionEdge = beforeUpdatersNode.getLeavingEdge(0);
    Truth.assertThat(updatersDescriptionEdge.getDescription()).isEqualTo("run for loop updaters");
    final CFANode afterUpdatersDescriptionNode = updatersDescriptionEdge.getSuccessor();
    Truth.assertThat(afterUpdatersDescriptionNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(afterUpdatersDescriptionNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(afterUpdatersDescriptionNode.getLeavingEdge(0)).isEqualTo(updatersEdge);

    final CFANode afterUpdatersNode = updatersEdge.getSuccessor();
    Truth.assertThat(afterUpdatersNode.getNumEnteringEdges()).isEqualTo(1);
    Truth.assertThat(afterUpdatersNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(afterUpdatersNode.getLeavingEdge(0)).isInstanceOf(BlankEdge.class);
    Truth.assertThat(afterUpdatersNode.getLeavingEdge(0).getSuccessor()).isEqualTo(loopStartNode);

    Truth.assertThat(afterUpdatersNode.getLeavingEdge(0).getSuccessor()).isEqualTo(loopStartNode);

    Truth.assertThat(getAllCFANodes())
        .containsExactly(
            afterUpdatersNode,
            afterUpdatersDescriptionNode,
            beforeUpdatersNode,
            bodyNode,
            checkConditionNode,
            entryNode,
            loopExitNode,
            loopStartNode);
  }
}

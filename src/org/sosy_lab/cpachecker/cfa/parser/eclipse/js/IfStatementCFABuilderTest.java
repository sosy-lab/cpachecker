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

import static org.mockito.Mockito.*;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

public class IfStatementCFABuilderTest extends CFABuilderTestBase {

  @Test
  public final void testIfWithoutElse() {
    final IfStatement ifStatement =
        parseStatement(IfStatement.class, "if (condition) { doSomething() }");
    // expected CFA: <entryNode> --[condition]--> () -{doSomething()}-> () --\
    //                    \                                                   }--> ()
    //                     \------[!condition]-------------------------------/

    final JSExpression condition =
        new JSIdExpression(
            FileLocation.DUMMY, JSAnyType.ANY, "condition", mock(JSSimpleDeclaration.class));
    final StatementAppendable statementAppendable =
        (builder, pStatement) ->
            builder.appendEdge(DummyEdge.withDescription("dummy statement edge"));
    builder.setStatementAppendable(statementAppendable);
    builder.setExpressionAppendable((pBuilder, pExpression) -> condition);

    new IfStatementCFABuilder().append(builder, ifStatement);

    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) entryNode.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) entryNode.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge thenEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge elseEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    Truth.assertThat(thenEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(elseEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(thenEdge.getExpression()).isEqualTo(condition);
    Truth.assertThat(elseEdge.getExpression()).isEqualTo(condition);
  }

  @Test
  public final void testIfWithElse() {
    final IfStatement ifStatement =
        parseStatement(IfStatement.class, "if (condition) var thenCase; else var elseCase;");
    // expected CFA: <entryNode> --[condition]--> () -{var thenCase}-> () --\
    //                    \                                                  }--> ()
    //                     \------[!condition]--> () -{var elseCase}-> () --/

    final JSExpression condition =
        new JSIdExpression(
            FileLocation.DUMMY, JSAnyType.ANY, "condition", mock(JSSimpleDeclaration.class));
    final StatementAppendable statementAppendable =
        (builder, pStatement) ->
            builder.appendEdge(DummyEdge.withDescription(pStatement.toString()));
    builder.setStatementAppendable(statementAppendable);
    builder.setExpressionAppendable((pBuilder, pExpression) -> condition);

    new IfStatementCFABuilder().append(builder, ifStatement);

    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) entryNode.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) entryNode.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge thenEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge elseEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    Truth.assertThat(thenEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(elseEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(thenEdge.getExpression()).isEqualTo(condition);
    Truth.assertThat(elseEdge.getExpression()).isEqualTo(condition);

    final CFAEdge thenStatementEdge = thenEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(thenStatementEdge.getDescription()).isEqualTo("var thenCase");
    final CFAEdge elseStatementEdge = elseEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(elseStatementEdge.getDescription()).isEqualTo("var elseCase");
    final CFANode exitNode = builder.getExitNode();
    Truth.assertThat(exitNode.getNumEnteringEdges()).isEqualTo(2);
    Truth.assertThat(thenStatementEdge.getSuccessor().getLeavingEdge(0).getSuccessor())
        .isEqualTo(exitNode);
    Truth.assertThat(elseStatementEdge.getSuccessor().getLeavingEdge(0).getSuccessor())
        .isEqualTo(exitNode);
  }
}

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

public class ConditionalExpressionCFABuilderTest {

  private EclipseJavaScriptParser parser;
  private ConfigurableJavaScriptCFABuilder builder;
  private CFANode entryNode;

  @Before
  public void init() throws InvalidConfigurationException {
    builder = JavaScriptCFABuilderFactory.createTestJavaScriptCFABuilder();
    parser = new EclipseJavaScriptParser(builder.getLogger());
    entryNode = builder.getExitNode();
  }

  private JavaScriptUnit createAST(final String pCode) {
    return (JavaScriptUnit) parser.createAST(builder.getBuilder().getFilename(), pCode);
  }

  @SuppressWarnings({"unchecked", "SameParameterValue"})
  private ConditionalExpression parseConditionalExpression(final String pCode) {
    final ExpressionStatement expressionStatement =
        (ExpressionStatement) createAST(pCode).statements().get(0);
    return (ConditionalExpression) expressionStatement.getExpression();
  }

  @Test
  public void testConditionalExpression() {
    final ConditionalExpression conditionalExpression =
        parseConditionalExpression("condition ? thenExpr : elseExpr");
    // conditional expression:
    //    condition ? thenExpr : elseExpr
    // expected side effect:
    //    var result
    //    if (condition) {
    //      result = thenExpr
    //    } elseExpr {
    //      result = elseExpr
    //    }
    // expected result:
    //    result
    // expected CFA:   <entryNode> --{var result}--> () ----\
    //                                                      |
    //                /-----{result = elseExpr}-- () <--[thenExpr]--/
    //   (result) <--{                                     /
    //                \--{result = thenExpr}-- () <--[!thenExpr]--/

    final JSExpression condition = mock(JSExpression.class, "condition");
    when(condition.getFileLocation()).thenReturn(FileLocation.DUMMY);
    final JSExpression thenExpr = mock(JSExpression.class, "thenExpr");
    final JSExpression elseExpr = mock(JSExpression.class, "elseExpr");
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any(SimpleName.class)))
        .thenAnswer(
            pInvocationOnMock -> {
              final SimpleName name = pInvocationOnMock.getArgument(1);
              switch (name.getIdentifier()) {
                case "condition":
                  return condition;
                case "thenExpr":
                  return thenExpr;
                case "elseExpr":
                  return elseExpr;
              }
              throw new RuntimeException("Unexpected SimpleName expression");
            });
    builder.setExpressionAppendable(expressionAppendable);

    final JSIdExpression result =
        (JSIdExpression)
            new ConditionalExpressionCFABuilder().append(builder, conditionalExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    // assert expected side effect: var result
    final JSDeclarationEdge resultDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration resultDeclaration =
        (JSVariableDeclaration) resultDeclarationEdge.getDeclaration();
    Truth.assertThat(resultDeclaration.getInitializer()).isEqualTo(null);

    // assert expected side effect:
    //    if (condition) {
    //      result = thenExpr
    //    } elseExpr {
    //      result = elseExpr
    //    }
    final CFANode resultDeclarationEdgeSuccessor = resultDeclarationEdge.getSuccessor();
    Truth.assertThat(resultDeclarationEdgeSuccessor.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge thenEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge elseEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    // assert expected side effect:
    //    if (condition) { ... } else { ... }
    Truth.assertThat(thenEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(elseEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(thenEdge.getExpression()).isEqualTo(condition);
    Truth.assertThat(elseEdge.getExpression()).isEqualTo(condition);

    // assert expected side effect:
    //      result = thenExpr
    final JSStatementEdge thenStatementEdge =
        (JSStatementEdge) thenEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment thenStatement = (JSAssignment) thenStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) thenStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(thenStatement.getRightHandSide()).isEqualTo(thenExpr);

    // assert expected side effect:
    //      result = elseExpr
    final JSStatementEdge elseStatementEdge =
        (JSStatementEdge) elseEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment elseStatement = (JSAssignment) elseStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) elseStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(elseStatement.getRightHandSide()).isEqualTo(elseExpr);

    // assert that conditional branches are joined in exit node
    final CFANode exitNode = builder.getExitNode();
    Truth.assertThat(exitNode.getNumEnteringEdges()).isEqualTo(2);
    final CFAEdge thenStatementExitEdge = thenStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(thenStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(thenStatementExitEdge.getDescription())
        .isEqualTo("end true ? thenExpr : elseExpr");
    final CFAEdge elseStatementExitEdge = elseStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(elseStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(elseStatementExitEdge.getDescription())
        .isEqualTo("end false ? thenExpr : elseExpr");

    // assert expected result
    Truth.assertThat(result.getDeclaration()).isEqualTo(resultDeclaration);
  }
}

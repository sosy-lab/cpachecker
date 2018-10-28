/*
 *  CPAchecker is a tool for configurable software verification.
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

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.util.test.ReturnValueCaptor;

public class ExpressionListCFABuilderTest extends CFABuilderTestBase {

  private ReturnValueCaptor<CFAEdge> sideEffectEdgeCaptor;

  @Override
  public void init() throws InvalidConfigurationException {
    super.init();
    sideEffectEdgeCaptor = new ReturnValueCaptor<>();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void appendSideEffectEdge(final JavaScriptCFABuilder pBuilder) {
    pBuilder.appendEdge(
        sideEffectEdgeCaptor.captureReturn(
            DummyEdge.withDescription("side effect " + sideEffectEdgeCaptor.getTimesCalled())));
  }

  @Test
  public final void testSingleExpression() {
    // single expression should be appended without temporary assignment
    final Expression expression = parseExpression(PrefixExpression.class, "++x");
    final JSIdExpression x = mock(JSIdExpression.class, "x");
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> {
          appendSideEffectEdge(pBuilder);
          return x;
        });

    final List<JSExpression> result =
        new ExpressionListCFABuilder().append(builder, ImmutableList.of(expression));

    Truth.assertThat(result).isEqualTo(ImmutableList.of(x));
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(sideEffectEdgeCaptor.getValues()).contains(entryNode.getLeavingEdge(0));
    Truth.assertThat(entryNode.getLeavingEdge(0).getSuccessor()).isEqualTo(builder.getExitNode());
  }

  @Test
  public final void testLastExpressionHasSideEffect() {
    // single expression should be appended without temporary assignment
    final Expression first = parseExpression(SimpleName.class, "x");
    final Expression second = parseExpression(PrefixExpression.class, "++x");
    final JSIdExpression x = mock(JSIdExpression.class, "x");
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> {
          if (pExpression == first) {
            return x;
          } else if (pExpression == second) {
            appendSideEffectEdge(pBuilder);
            return x;
          } else {
            throw new RuntimeException("unexpected expression");
          }
        });

    final List<JSExpression> result =
        new ExpressionListCFABuilder().append(builder, ImmutableList.of(first, second));

    // TODO check result
    //    Truth.assertThat(result).isEqualTo(ImmutableList.of(x, x));

    Truth.assertThat(sideEffectEdgeCaptor.getTimesCalled()).isGreaterThan(0);

    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(entryNode.getLeavingEdge(0)).isInstanceOf(JSDeclarationEdge.class);
    final JSDeclarationEdge tmpAssignmentEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration tmpVariableDeclaration =
        (JSVariableDeclaration) tmpAssignmentEdge.getDeclaration();
    Truth.assertThat(
            ((JSInitializerExpression) tmpVariableDeclaration.getInitializer()).getExpression())
        .isEqualTo(x);

    Truth.assertThat(tmpAssignmentEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(1);
    final CFAEdge sideEffectEdge = tmpAssignmentEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(sideEffectEdgeCaptor.getValues()).contains(sideEffectEdge);
    // no temporary variable assignment is required for the last expression.
    // That's why there should be no further (declaration) edge.
    Truth.assertThat(sideEffectEdge.getSuccessor()).isEqualTo(builder.getExitNode());
  }

  @Test
  public final void testExpressionsWithoutSideEffectAreNotTemporaryAssigned() {
    final Expression first = parseExpression(SimpleName.class, "x");
    final Expression second = parseExpression(SimpleName.class, "y");
    final Expression third = parseExpression(SimpleName.class, "z");
    final JSExpression firstResult = mock(JSIdExpression.class, "x");
    final JSExpression secondResult = mock(JSIdExpression.class, "y");
    final JSExpression thirdResult = mock(JSIdExpression.class, "z");
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> {
          if (pExpression == first) {
            return firstResult;
          } else if (pExpression == second) {
            return secondResult;
          } else if (pExpression == third) {
            return thirdResult;
          } else {
            throw new RuntimeException("unexpected expression");
          }
        });

    final List<JSExpression> result =
        new ExpressionListCFABuilder().append(builder, ImmutableList.of(first, second, third));

    Truth.assertThat(result).isEqualTo(ImmutableList.of(firstResult, secondResult, thirdResult));
    Truth.assertThat(builder.getExitNode()).isEqualTo(entryNode);
  }

}

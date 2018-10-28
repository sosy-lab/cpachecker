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
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
    Truth.assertThat(sideEffectEdgeCaptor.getTimesCalled()).isEqualTo(1);
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    Truth.assertThat(entryNode.getLeavingEdge(0)).isEqualTo(sideEffectEdgeCaptor.getReturnValue(0));
    Truth.assertThat(entryNode.getLeavingEdge(0).getSuccessor()).isEqualTo(builder.getExitNode());
  }
}

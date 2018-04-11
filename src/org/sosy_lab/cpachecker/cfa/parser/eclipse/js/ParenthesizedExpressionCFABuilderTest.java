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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;

public class ParenthesizedExpressionCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testParenthesizedExpression() {
    final ParenthesizedExpression parenthesizedExpression =
        parseExpression(ParenthesizedExpression.class, "(expr)");
    // expected CFA: <entryNode>

    final JSExpression expr = mock(JSExpression.class);
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(eq(builder), any(Expression.class))).thenReturn(expr);
    builder.setExpressionAppendable(expressionAppendable);

    final JSExpression result =
        new ParenthesizedExpressionCFABuilder().append(builder, parenthesizedExpression);

    Truth.assertThat(result).isEqualTo(expr);
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(0);
  }
}

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
import java.util.Collections;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;

public class FunctionExpressionCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testAnonymousFunctionExpression() {
    final FunctionExpression functionExpression =
        (FunctionExpression)
            parseExpression(ParenthesizedExpression.class, "(function () {})").getExpression();

    final JSFunctionDeclaration jsFunctionDeclaration =
        new JSFunctionDeclaration(
            FileLocation.DUMMY,
            "",
            Collections.emptyList());

    final FunctionDeclarationAppendable functionDeclarationAppendable =
        mock(FunctionDeclarationAppendable.class);
    when(functionDeclarationAppendable.append(any(), any(FunctionDeclaration.class)))
        .thenReturn(jsFunctionDeclaration);
    builder.setFunctionDeclarationAppendable(functionDeclarationAppendable);

    final JSIdExpression result =
        (JSIdExpression) new FunctionExpressionCFABuilder().append(builder, functionExpression);

    Truth.assertThat(result).isNotNull();
  }

}

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
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class FunctionDeclarationCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testNamedFunctionDeclaration() {
    final FunctionDeclaration declaration =
        parseStatement(FunctionDeclaration.class, "function foo() {}");

    builder.setStatementAppendable(mock(StatementAppendable.class));

    final JSFunctionDeclaration result =
        new FunctionDeclarationCFABuilder().append(builder, declaration);

    Truth.assertThat(result).isNotNull();
    final ParseResult parseResult = builder.getParseResult();
    Truth.assertThat(parseResult.getFunctions()).isNotEmpty();
    final FunctionEntryNode functionEntryNode = parseResult.getFunctions().get("foo");
    Truth.assertThat(functionEntryNode).isNotNull();
    Truth.assertThat(functionEntryNode.getFunctionDefinition()).isEqualTo(result);
  }

  @Test
  public void testAnonymousFunctionDeclaration() {
    final FunctionDeclaration declaration =
        parseExpression(FunctionExpression.class, "(function () {})").getMethod();

    builder.setStatementAppendable(mock(StatementAppendable.class));

    final JSFunctionDeclaration result =
        new FunctionDeclarationCFABuilder().append(builder, declaration);

    Truth.assertThat(result).isNotNull();
    final ParseResult parseResult = builder.getParseResult();
    Truth.assertThat(parseResult.getFunctions()).hasSize(1);
    final String functionKey = parseResult.getFunctions().firstKey();
    Truth.assertThat(functionKey).startsWith("__CPAChecker_ANONYMOUS_FUNCTION_");
    final FunctionEntryNode functionEntryNode = parseResult.getFunctions().get(functionKey);
    Truth.assertThat(functionEntryNode).isNotNull();
    Truth.assertThat(functionEntryNode.getFunctionDefinition()).isEqualTo(result);
  }
}

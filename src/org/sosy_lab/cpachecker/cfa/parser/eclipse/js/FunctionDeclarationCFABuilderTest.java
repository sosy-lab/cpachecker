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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.truth.Truth;
import java.util.List;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class FunctionDeclarationCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testNamedFunctionDeclaration() {
    // TODO check default return
    final FunctionDeclaration declaration =
        parseStatement(FunctionDeclaration.class, "function foo(a,b) {}");
    final String expectedFunctionName = "foo";

    final StatementAppendable statementAppendable = mock(StatementAppendable.class);
    builder.setStatementAppendable(statementAppendable);

    final JSFunctionDeclaration result =
        new FunctionDeclarationCFABuilder().append(builder, declaration);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(result.getName()).isEqualTo(expectedFunctionName);
    final List<JSParameterDeclaration> parameters = result.getParameters();
    Truth.assertThat(parameters).hasSize(2);
    Truth.assertThat(parameters.get(0).getQualifiedName()).isEqualTo("foo::a");
    Truth.assertThat(parameters.get(1).getQualifiedName()).isEqualTo("foo::b");
    final ParseResult parseResult = builder.getParseResult();
    Truth.assertThat(parseResult.getFunctions()).isNotEmpty();
    final FunctionEntryNode functionEntryNode =
        parseResult.getFunctions().get(expectedFunctionName);
    Truth.assertThat(functionEntryNode).isNotNull();
    Truth.assertThat(functionEntryNode.getFunctionDefinition()).isEqualTo(result);

    // check that statements of function body are appended using FunctionScope
    final ArgumentCaptor<JavaScriptCFABuilder> statementAppendableBuilderArgumentCaptor =
        ArgumentCaptor.forClass(JavaScriptCFABuilder.class);
    verify(statementAppendable, times(1))
        .append(statementAppendableBuilderArgumentCaptor.capture(), any());
    final JavaScriptCFABuilder functionBodyBuilder =
        statementAppendableBuilderArgumentCaptor.getValue();
    Truth.assertThat(functionBodyBuilder.getScope()).isInstanceOf(FunctionScope.class);
    final FunctionScope functionScope = (FunctionScope) functionBodyBuilder.getScope();
    Truth.assertThat(functionScope.getParentScope()).isEqualTo(builder.getScope());
  }

  @Test
  public void testAnonymousFunctionDeclaration() {
    final FunctionDeclaration declaration =
        ((FunctionExpression)
                parseExpression(ParenthesizedExpression.class, "(function () {})").getExpression())
            .getMethod();

    builder.setStatementAppendable(mock(StatementAppendable.class));

    final JSFunctionDeclaration result =
        new FunctionDeclarationCFABuilder().append(builder, declaration);

    Truth.assertThat(result).isNotNull();
    final ParseResult parseResult = builder.getParseResult();
    Truth.assertThat(parseResult.getFunctions()).hasSize(1);
    final String functionKey = parseResult.getFunctions().firstKey();
    Truth.assertThat(functionKey).startsWith("CPAchecker_FunctionExpression_");
    final FunctionEntryNode functionEntryNode = parseResult.getFunctions().get(functionKey);
    Truth.assertThat(functionEntryNode).isNotNull();
    Truth.assertThat(functionEntryNode.getFunctionDefinition()).isEqualTo(result);
  }

  @Test
  public void testNestedFunctionDeclaration() {
    final FunctionDeclaration declaration =
        parseStatement(FunctionDeclaration.class, "function outer(o) { function inner(i) {} }");

    final StatementCFABuilder statementAppendable = new StatementCFABuilder();
    statementAppendable.setFunctionDeclarationStatementAppendable(
        new FunctionDeclarationStatementCFABuilder());
    statementAppendable.setBlockStatementAppendable(new BlockStatementCFABuilder());
    builder.setStatementAppendable(statementAppendable);
    final FunctionDeclarationCFABuilder functionDeclarationCFABuilder =
        new FunctionDeclarationCFABuilder();
    final JSFunctionDeclaration[] innerDeclarationCaptor = {null};
    builder.setFunctionDeclarationAppendable(
        (pBuilder, pFunctionDeclaration) -> {
          innerDeclarationCaptor[0] =
              functionDeclarationCFABuilder.append(pBuilder, pFunctionDeclaration);
          return innerDeclarationCaptor[0];
        });
    final JSFunctionDeclaration outerDeclaration =
        functionDeclarationCFABuilder.append(builder, declaration);

    Truth.assertThat(outerDeclaration).isNotNull();
    Truth.assertThat(outerDeclaration.getName()).isEqualTo("outer");
    final List<JSParameterDeclaration> outerParameters = outerDeclaration.getParameters();
    Truth.assertThat(outerParameters).hasSize(1);
    Truth.assertThat(outerParameters.get(0).getQualifiedName()).isEqualTo("outer::o");
    final ParseResult parseResult = builder.getParseResult();
    Truth.assertThat(parseResult.getFunctions()).isNotEmpty();
    final FunctionEntryNode outerFunctionEntryNode = parseResult.getFunctions().get("outer");
    Truth.assertThat(outerFunctionEntryNode).isNotNull();
    Truth.assertThat(outerFunctionEntryNode.getFunctionDefinition()).isEqualTo(outerDeclaration);

    final JSFunctionDeclaration innerDeclaration = innerDeclarationCaptor[0];
    Truth.assertThat(innerDeclaration).isNotNull();
    Truth.assertThat(innerDeclaration.getName()).isEqualTo("inner");
    Truth.assertThat(innerDeclaration.getQualifiedName()).isEqualTo("outer.inner");
    final List<JSParameterDeclaration> innerParameters = innerDeclaration.getParameters();
    Truth.assertThat(innerParameters).hasSize(1);
    Truth.assertThat(innerParameters.get(0).getQualifiedName()).isEqualTo("outer.inner::i");
    Truth.assertThat(parseResult.getFunctions()).isNotEmpty();
    Truth.assertThat(parseResult.getFunctions().get("inner")).isNull();
    final FunctionEntryNode innerFunctionEntryNode = parseResult.getFunctions().get("outer.inner");
    Truth.assertThat(innerFunctionEntryNode).isNotNull();
    Truth.assertThat(innerFunctionEntryNode.getFunctionDefinition()).isEqualTo(innerDeclaration);
  }
}

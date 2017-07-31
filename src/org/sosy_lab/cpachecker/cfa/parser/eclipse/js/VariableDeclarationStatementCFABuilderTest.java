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

import com.google.common.truth.Truth;
import java.math.BigInteger;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class VariableDeclarationStatementCFABuilderTest {

  private EclipseJavaScriptParser parser;
  private JavaScriptCFABuilder builder;
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

  @SuppressWarnings("unchecked")
  private VariableDeclarationStatement parseStatement(final String pCode) {
    return (VariableDeclarationStatement) createAST(pCode).statements().get(0);
  }

  @Test
  public final void testSingleVariableDeclaration() throws ParserException {
    final VariableDeclarationStatement variableDeclarationStatement = parseStatement("var x = 42");
    final String expectedVariableName = "x";
    final int expectedVariableValue = 42;
    // expected CFA: entryNode -{var x = 42}-> ()

    new VariableDeclarationStatementCFABuilder().append(builder, variableDeclarationStatement);

    final JSDeclarationEdge declarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration variableDeclaration =
        (JSVariableDeclaration) declarationEdge.getDeclaration();
    Truth.assertThat(variableDeclaration.getName()).isEqualTo(expectedVariableName);
    Truth.assertThat(
            ((JSInitializerExpression) variableDeclaration.getInitializer()).getExpression())
        .isEqualTo(
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(expectedVariableValue)));
    Truth.assertThat(declarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public final void testMultiVariableDeclaration() throws ParserException {
    final VariableDeclarationStatement variableDeclarationStatement =
        parseStatement("var x = 123, y = 456");
    final String expectedFirstVariableName = "x";
    final int expectedFirstVariableValue = 123;
    final String expectedSecondVariableName = "y";
    final int expectedSecondVariableValue = 456;
    // expected CFA: entryNode -{var x = 123}-> () -{var y = 456}-> ()

    new VariableDeclarationStatementCFABuilder().append(builder, variableDeclarationStatement);

    final JSDeclarationEdge xDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration xVariableDeclaration =
        (JSVariableDeclaration) xDeclarationEdge.getDeclaration();
    Truth.assertThat(xVariableDeclaration.getName()).isEqualTo(expectedFirstVariableName);
    Truth.assertThat(
            ((JSInitializerExpression) xVariableDeclaration.getInitializer()).getExpression())
        .isEqualTo(
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(expectedFirstVariableValue)));

    final JSDeclarationEdge yDeclarationEdge = (JSDeclarationEdge) xDeclarationEdge.getSuccessor
        ().getLeavingEdge(0);
    final JSVariableDeclaration yVariableDeclaration =
        (JSVariableDeclaration) yDeclarationEdge.getDeclaration();
    Truth.assertThat(yVariableDeclaration.getName()).isEqualTo(expectedSecondVariableName);
    Truth.assertThat(
            ((JSInitializerExpression) yVariableDeclaration.getInitializer()).getExpression())
        .isEqualTo(
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(expectedSecondVariableValue)));
    Truth.assertThat(yDeclarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }
}

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
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

public class VariableDeclarationStatementCFABuilderTest extends CFABuilderTestBase {

  @Test
  public final void testSingleVariableDeclaration() {
    final VariableDeclarationStatement variableDeclarationStatement =
        parseStatement(VariableDeclarationStatement.class, "var x = 42");
    final String expectedVariableName = "x";
    final JSIntegerLiteralExpression expectedInitializerExpression =
        new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.valueOf(42));
    // expected CFA: entryNode -{var x = 42}-> ()

    builder.setExpressionAppendable((pBuilder, pExpression) -> expectedInitializerExpression);

    // TODO test VariableDeclarationFragmentCFABuilder and VariableDeclarationStatementCFABuilder separately
    builder.setVariableDeclarationFragmentAppendable(new VariableDeclarationFragmentCFABuilder());
    new VariableDeclarationStatementCFABuilder().append(builder, variableDeclarationStatement);

    final JSDeclarationEdge declarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration variableDeclaration =
        (JSVariableDeclaration) declarationEdge.getDeclaration();
    Truth.assertThat(variableDeclaration.getName()).isEqualTo(expectedVariableName);
    Truth.assertThat(
            ((JSInitializerExpression) variableDeclaration.getInitializer()).getExpression())
        .isEqualTo(expectedInitializerExpression);
    Truth.assertThat(declarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public final void testMultiVariableDeclaration() {
    final VariableDeclarationStatement variableDeclarationStatement =
        parseStatement(VariableDeclarationStatement.class, "var x = 123, y = 456");
    final String expectedFirstVariableName = "x";
    final int expectedFirstVariableValue = 123;
    final String expectedSecondVariableName = "y";
    final int expectedSecondVariableValue = 456;
    // expected CFA: entryNode -{var x = 123}-> () -{var y = 456}-> ()

    // TODO is there a better solution than relying on the ASTConverter.convert implementation?
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> pBuilder.getAstConverter().convert((NumberLiteral) pExpression));

    // TODO test VariableDeclarationFragmentCFABuilder and VariableDeclarationStatementCFABuilder separately
    builder.setVariableDeclarationFragmentAppendable(new VariableDeclarationFragmentCFABuilder());
    new VariableDeclarationStatementCFABuilder().append(builder, variableDeclarationStatement);

    final JSDeclarationEdge xDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration xVariableDeclaration =
        (JSVariableDeclaration) xDeclarationEdge.getDeclaration();
    Truth.assertThat(xVariableDeclaration.getName()).isEqualTo(expectedFirstVariableName);
    Truth.assertThat(getInitializerExpressionValue(xVariableDeclaration))
        .isEqualTo(BigInteger.valueOf(expectedFirstVariableValue));

    final JSDeclarationEdge yDeclarationEdge =
        (JSDeclarationEdge) xDeclarationEdge.getSuccessor().getLeavingEdge(0);
    final JSVariableDeclaration yVariableDeclaration =
        (JSVariableDeclaration) yDeclarationEdge.getDeclaration();
    Truth.assertThat(yVariableDeclaration.getName()).isEqualTo(expectedSecondVariableName);
    Truth.assertThat(getInitializerExpressionValue(yVariableDeclaration))
        .isEqualTo(BigInteger.valueOf(expectedSecondVariableValue));
    Truth.assertThat(yDeclarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }

  private BigInteger getInitializerExpressionValue(
      final JSVariableDeclaration pVariableDeclaration) {
    final JSInitializerExpression initializer =
        (JSInitializerExpression) pVariableDeclaration.getInitializer();
    final JSIntegerLiteralExpression initializerExpression =
        (JSIntegerLiteralExpression) initializer.getExpression();
    return initializerExpression.getValue();
  }
}

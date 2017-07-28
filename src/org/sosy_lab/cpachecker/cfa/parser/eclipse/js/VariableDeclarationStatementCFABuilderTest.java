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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class VariableDeclarationStatementCFABuilderTest {

  private EclipseJavaScriptParser parser;
  private CFABuilder builder;
  private CFANode entryNode;
  private final String filename = "dummy.js";

  @Before
  public void init() throws InvalidConfigurationException {
    final LogManager logger = LogManager.createTestLogManager();
    parser = new EclipseJavaScriptParser(logger);
    final String functionName = "dummy";
    // Make entryNode reachable by adding an entry edge.
    // Otherwise, the builder can not add edges.
    final CFANode dummyEntryNode = new CFANode(functionName);
    entryNode = new CFANode(functionName);
    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new BlankEdge("", FileLocation.DUMMY, dummyEntryNode, entryNode, "start dummy edge"));
    builder =
        new CFABuilder(
            logger, new ASTConverter(new Scope(filename), logger), functionName, entryNode);
  }

  private JavaScriptUnit createAST(final String pCode) {
    return (JavaScriptUnit) parser.createAST(filename, pCode);
  }

  @Test
  public final void testSingleVariableDeclaration() throws ParserException {
    final JavaScriptUnit ast = createAST("var x = 42");
    new VariableDeclarationStatementCFABuilder(builder)
        .append((VariableDeclarationStatement) ast.statements().get(0));
    final JSDeclarationEdge declarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration variableDeclaration =
        (JSVariableDeclaration) declarationEdge.getDeclaration();
    Truth.assertThat(variableDeclaration.getName()).isEqualTo("x");
    Truth.assertThat(
            ((JSInitializerExpression) variableDeclaration.getInitializer()).getExpression())
        .isEqualTo(new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.valueOf(42)));
    Truth.assertThat(declarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }
}

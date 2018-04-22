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

import static org.mockito.Mockito.spy;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

final class JavaScriptCFABuilderFactory {

  static JavaScriptCFABuilder withAllFeatures(final Scope pScope, final LogManager pLogger) {
    final FileCFABuilder fileBuilder =
        new FileCFABuilder(pScope, pLogger, new JavaScriptUnitCFABuilder());
    final JavaScriptCFABuilderImpl builder = new JavaScriptCFABuilderImpl(fileBuilder.getBuilder());
    final VariableDeclarationRegistryImpl variableDeclarationRegistry =
        new VariableDeclarationRegistryImpl();
    final FunctionDeclarationRegistryImpl functionDeclarationRegistry =
        new FunctionDeclarationRegistryImpl();
    final SimpleNameResolver simpleNameResolver =
        new SimpleNameResolverImpl(variableDeclarationRegistry, functionDeclarationRegistry);
    builder.setExpressionAppendable(
        ExpressionAppendableFactory.withAllFeatures(simpleNameResolver));
    builder.setFunctionDeclarationAppendable(
        new RegisteredFunctionDeclarationCFABuilder(
            functionDeclarationRegistry, new FunctionDeclarationCFABuilder()));
    builder.setJavaScriptUnitAppendable(fileBuilder);
    builder.setStatementAppendable(StatementAppendableFactory.withAllFeatures());
    builder.setVariableDeclarationFragmentAppendable(
        new RegisteredVariableDeclarationFragmentCFABuilder(
            variableDeclarationRegistry, new VariableDeclarationFragmentCFABuilder()));
    return builder;
  }

  // TODO move to CFABuilder?
  private static CFABuilder createTestCFABuilder() {
    final String filename = "dummy.js";
    final String functionName = "dummy";
    // Make entryNode reachable by adding an entry edge.
    // Otherwise, the builder can not add edges.
    final CFANode dummyEntryNode = new CFANode(functionName);
    final CFANode entryNode = new CFANode(functionName);
    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new BlankEdge("", FileLocation.DUMMY, dummyEntryNode, entryNode, "start dummy edge"));
    final LogManager logger = LogManager.createTestLogManager();
    final Scope scope = new Scope(filename);
    return new CFABuilder(
        scope, logger, spy(new ASTConverter(scope, logger)), functionName, entryNode);
  }

  static ConfigurableJavaScriptCFABuilder createTestJavaScriptCFABuilder() {
    return new JavaScriptCFABuilderImpl(createTestCFABuilder());
  }
}

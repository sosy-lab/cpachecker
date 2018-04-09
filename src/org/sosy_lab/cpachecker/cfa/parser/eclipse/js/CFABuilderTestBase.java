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

import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.junit.Before;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

abstract class CFABuilderTestBase {

  protected EclipseJavaScriptParser parser;
  protected ConfigurableJavaScriptCFABuilder builder;
  protected CFANode entryNode;

  @Before
  public void init() throws InvalidConfigurationException {
    builder = JavaScriptCFABuilderFactory.createTestJavaScriptCFABuilder();
    parser = new EclipseJavaScriptParser(builder.getLogger());
    entryNode = builder.getExitNode();
  }

  protected JavaScriptUnit createAST(final String pCode) {
    return (JavaScriptUnit) parser.createAST(builder.getBuilder().getFilename(), pCode);
  }

  <S> S parseStatement(final Class<S> statementClass, final String pCode) {
    return parseStatement(statementClass, pCode, 0);
  }

  @SuppressWarnings("unchecked")
  <S> S parseStatement(final Class<S> statementClass, final String pCode, final int pIndex) {
    return (S) createAST(pCode).statements().get(pIndex);
  }

  <E> E parseExpression(final Class<E> expressionClass, final String pCode) {
    return parseExpression(expressionClass, pCode, 0);
  }

  @SuppressWarnings({"unchecked", "SameParameterValue"})
  <E> E parseExpression(final Class<E> expressionClass, final String pCode, final int pIndex) {
    final ExpressionStatement expressionStatement =
        (ExpressionStatement) createAST(pCode).statements().get(pIndex);
    return (E) expressionStatement.getExpression();
  }
}

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

import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

final class JavaScriptCFABuilderImpl implements ConfigurableJavaScriptCFABuilder {

  private final CFABuilder builder;

  // When another appendable field is added, it has to be set in copyWith(CFABuilder) and
  // JavaScriptCFABuilderFactory.withAllFeatures
  private ExpressionAppendable expressionAppendable;
  private FunctionDeclarationAppendable functionDeclarationAppendable;
  private JavaScriptUnitAppendable javaScriptUnitAppendable;
  private StatementAppendable statementAppendable;
  private VariableNameGenerator variableNameGenerator;

  JavaScriptCFABuilderImpl(final CFABuilder pBuilder) {
    builder = pBuilder;
    variableNameGenerator = new VariableNameGeneratorImpl();
  }

  private JavaScriptCFABuilderImpl(final CFABuilder pBuilder, final VariableNameGenerator pVariableNameGenerator) {
    builder = pBuilder;
    variableNameGenerator = pVariableNameGenerator;
  }

  @Override
  public JavaScriptCFABuilder copy() {
    return copyWith(
        new CFABuilder(
            builder.getScope(),
            builder.getLogger(),
            builder.getAstConverter(),
            builder.getFunctionName(),
            builder.getExitNode()));
  }

  @Override
  public JavaScriptCFABuilder copyWith(final JSFunctionEntryNode pEntryNode) {
    return copyWith(
        new CFABuilder(
            builder.getScope(), builder.getLogger(), builder.getAstConverter(), pEntryNode));
  }

  @Override
  public JavaScriptCFABuilder copyWith(final CFABuilder pBuilder) {
    final JavaScriptCFABuilderImpl duplicate = new JavaScriptCFABuilderImpl(pBuilder, variableNameGenerator);
    duplicate.setExpressionAppendable(expressionAppendable);
    duplicate.setFunctionDeclarationAppendable(functionDeclarationAppendable);
    duplicate.setJavaScriptUnitAppendable(javaScriptUnitAppendable);
    duplicate.setStatementAppendable(statementAppendable);
    return duplicate;
  }

  @Override
  public JSExpression append(final Expression pExpression) {
    return expressionAppendable.append(this, pExpression);
  }

  @Override
  public CFABuilder getBuilder() {
    return builder;
  }

  @Override
  public JSFunctionDeclaration append(final FunctionDeclaration pDeclaration) {
    return functionDeclarationAppendable.append(this, pDeclaration);
  }

  @Override
  public JavaScriptCFABuilder append(final JavaScriptUnit pJavaScriptUnit) {
    javaScriptUnitAppendable.append(this, pJavaScriptUnit);
    return this;
  }

  @Override
  public JavaScriptCFABuilder append(final Statement pStatement) {
    statementAppendable.append(this, pStatement);
    return this;
  }

  @Override
  public void setStatementAppendable(final StatementAppendable pStatementAppendable) {
    statementAppendable = pStatementAppendable;
  }

  @Override
  public void setExpressionAppendable(final ExpressionAppendable pExpressionAppendable) {
    expressionAppendable = pExpressionAppendable;
  }

  @Override
  public void setFunctionDeclarationAppendable(
      final FunctionDeclarationAppendable pFunctionDeclarationAppendable) {
    functionDeclarationAppendable = pFunctionDeclarationAppendable;
  }

  @Override
  public void setJavaScriptUnitAppendable(
      final JavaScriptUnitAppendable pJavaScriptUnitAppendable) {
    javaScriptUnitAppendable = pJavaScriptUnitAppendable;
  }

  @Override
  public String generateVariableName() {
    return variableNameGenerator.generateVariableName();
  }
}

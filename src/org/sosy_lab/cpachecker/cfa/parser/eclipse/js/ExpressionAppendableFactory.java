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

import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;

final class ExpressionAppendableFactory {

  static ExpressionAppendable withAllFeatures(
      final JSFunctionDeclaration pUnknownFunctionCallerDeclaration) {
    final ExpressionCFABuilder builder = new ExpressionCFABuilder();
    builder.setAssignmentAppendable(new AssignmentCFABuilder());
    builder.setBooleanLiteralConverter(new BooleanLiteralConverterImpl());
    builder.setConditionalExpressionAppendable(new ConditionalExpressionCFABuilder());
    builder.setFunctionExpressionAppendable(new FunctionExpressionCFABuilder());
    builder.setFunctionInvocationAppendable(
        new FunctionInvocationCFABuilder(pUnknownFunctionCallerDeclaration));
    builder.setInfixExpressionAppendable(new InfixExpressionCFABuilder());
    builder.setNullLiteralConverter(new NullLiteralConverterImpl());
    builder.setNumberLiteralConverter(new NumberLiteralConverterImpl());
    builder.setObjectLiteralAppendable(new ObjectLiteralCFABuilder());
    builder.setParenthesizedExpressionAppendable(new ParenthesizedExpressionCFABuilder());
    builder.setPrefixExpressionAppendable(new PrefixExpressionCFABuilder());
    builder.setPostfixExpressionAppendable(new PostfixExpressionCFABuilder());
    builder.setSimpleNameResolver(new SimpleNameResolverImpl());
    builder.setStringLiteralConverter(new StringLiteralConverterImpl());
    builder.setUndefinedLiteralConverter(new UndefinedLiteralConverterImpl());
    builder.setVariableDeclarationExpressionAppendable(new VariableDeclarationExpressionCFABuilder());
    return builder;
  }
}

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
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

interface JavaScriptCFABuilder extends CFABuilderWrapperOfType<JavaScriptCFABuilder>, VariableNameGenerator {

  @Override
  default ParseResult getParseResult() {
    return getBuilder().getParseResult();
  }

  @Override
  default ASTConverter getAstConverter() {
    return getBuilder().getAstConverter();
  }

  JavaScriptCFABuilder append(Statement pStatement);

  JSFunctionDeclaration append(FunctionDeclaration pDeclaration);

  JavaScriptCFABuilder copyWith(CFABuilder pBuilder);

  JavaScriptCFABuilder append(JavaScriptUnit pJavaScriptUnit);

  JavaScriptCFABuilder copy();

  JavaScriptCFABuilder copyWith(JSFunctionEntryNode pEntryNode);

  JSExpression append(Expression pExpression);

  JSVariableDeclaration append(VariableDeclarationFragment pVariableDeclarationFragment);

  JSIdExpression resolve(final SimpleName pSimpleName);
}

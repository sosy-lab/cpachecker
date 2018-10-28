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

import java.util.List;
import java.util.function.BiFunction;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.sosy_lab.cpachecker.cfa.CFARemoveUnreachable;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.JumpExitEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

interface JavaScriptCFABuilder
    extends CFABuilderWrapperOfType<JavaScriptCFABuilder>,
        VariableNameGenerator,
        FileLocationProvider {

  @Override
  default ParseResult getParseResult() {
    return getBuilder().getParseResult();
  }

  JavaScriptCFABuilder append(Statement pStatement);

  JSFunctionDeclaration append(FunctionDeclaration pDeclaration);

  JavaScriptCFABuilder copyWith(CFABuilder pBuilder);

  JavaScriptCFABuilder append(JavaScriptUnit pJavaScriptUnit);

  JavaScriptCFABuilder copy();

  JavaScriptCFABuilder copyWith(Scope pScope);

  JavaScriptCFABuilder copyWith(JSFunctionEntryNode pEntryNode, FunctionScope pScope);

  JSExpression append(Expression pExpression);

  List<JSExpression> append(Expression pFirst, Expression pSecond);

  JSVariableDeclaration append(VariableDeclarationFragment pVariableDeclarationFragment);

  JSIdExpression resolve(final SimpleName pSimpleName);

  FunctionExitNode getFunctionExitNode();

  JSIdExpression getReturnVariableId();

  Scope getScope();

  /**
   * Add a {@link JumpExitEdge} and continue this builder in an unreachable node after the statement
   * that jumped out of the regular control flow. This unreachable path is removed later by {@link
   * CFARemoveUnreachable}. However, not the whole path might be unreachable since another reachable
   * entering edge might be added to one of the nodes that are appended to this builder.
   *
   * @param pJumpExitNode The node to which the {@link JumpExitEdge} should exit.
   * @param pCreateEdge Create the {@link JumpExitEdge} from the current exit edge of this builder
   *     to {@code pJumpExitNode}.
   * @return This builder.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  default CFABuilderWrapper appendJumpExitEdge(
      final CFANode pJumpExitNode, final BiFunction<CFANode, CFANode, JumpExitEdge> pCreateEdge) {
    copy()
        .appendEdge(
            pJumpExitNode,
            (pPredecessor, pSuccessor) -> pCreateEdge.apply(pPredecessor, pSuccessor));
    appendEdge(DummyEdge.withDescription("unreachable due to JumpExitEdge"));
    return this;
  }

  default JSVariableDeclaration declareVariable() {
    return declareVariable(null);
  }

  default JSVariableDeclaration declareVariable(final JSInitializerExpression pInitializer) {
    final String variableName = generateVariableName();
    return new JSVariableDeclaration(
        FileLocation.DUMMY,
        ScopeConverter.toCFAScope(getScope()),
        variableName,
        variableName,
        getScope().qualifiedVariableNameOf(variableName),
        pInitializer);
  }
}

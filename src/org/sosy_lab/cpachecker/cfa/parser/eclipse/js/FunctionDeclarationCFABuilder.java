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

import com.google.common.base.Optional;
import java.util.Collections;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSFunctionType;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FunctionDeclarationCFABuilder implements FunctionDeclarationAppendable {

  @Override
  public JSFunctionDeclaration append(
      final JavaScriptCFABuilder pBuilder, final FunctionDeclaration pFunctionDeclaration) {
    final JSFunctionDeclaration jsFunctionDeclaration =
        new JSFunctionDeclaration(
            pBuilder.getFileLocation(pFunctionDeclaration),
            new JSFunctionType(JSAnyType.ANY, Collections.emptyList()),
            getFunctionName(pFunctionDeclaration),
            Collections.emptyList());
    final String returnVariableName = "__retval__";
    final JSVariableDeclaration returnVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            false,
            returnVariableName,
            returnVariableName,
            returnVariableName,
            new JSInitializerExpression(
                FileLocation.DUMMY, new JSUndefinedLiteralExpression(FileLocation.DUMMY)));

    final String functionName = jsFunctionDeclaration.getName();
    final FunctionExitNode exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY,
            jsFunctionDeclaration,
            exitNode,
            Optional.of(returnVariableDeclaration));
    exitNode.setEntryNode(entryNode);
    final JavaScriptCFABuilder functionCFABuilder = pBuilder.copyWith(entryNode);

    addFunctionEntryNode(pBuilder);

    functionCFABuilder
        .append(pFunctionDeclaration.getBody())
        .appendEdge(
            exitNode,
            (pPredecessor, pSuccessor) -> {
              final JSUndefinedLiteralExpression returnValue =
                  new JSUndefinedLiteralExpression(FileLocation.DUMMY);
              return new JSReturnStatementEdge(
                  "return;",
                  new JSReturnStatement(
                      FileLocation.DUMMY,
                      Optional.of(returnValue),
                      Optional.of(
                          new JSExpressionAssignmentStatement(
                              FileLocation.DUMMY,
                              new JSIdExpression(
                                  FileLocation.DUMMY,
                                  returnVariableName,
                                  returnVariableDeclaration),
                              returnValue))),
                  FileLocation.DUMMY,
                  pPredecessor,
                  exitNode);
            })
        .appendTo(pBuilder.getBuilder());

    return jsFunctionDeclaration;
  }

  /**
   * Add a dummy edge to allow a function call as first statement. Without this edge {@link
   * CFASecondPassBuilder#insertCallEdgesRecursively()} would consider the function call as
   * unreachable.
   *
   * @param pBuilder Builder to which the edge is added.
   */
  private void addFunctionEntryNode(final JavaScriptCFABuilder pBuilder) {
    pBuilder.appendEdge(DummyEdge.withDescription("Function start dummy edge"));
  }

  public static String getFunctionName(final FunctionDeclaration node) {
    return node.getMethodName() == null
        ? "__CPAChecker_ANONYMOUS_FUNCTION_" + node.hashCode()
        : ((SimpleName) node.getMethodName()).getIdentifier();
  }
}

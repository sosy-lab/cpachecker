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
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSReturnStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FunctionDeclarationCFABuilder implements FunctionDeclarationAppendable {

  @Override
  public JSFunctionDeclaration append(
      final JavaScriptCFABuilder pBuilder, final FunctionDeclaration pFunctionDeclaration) {
    final Scope currentScope = pBuilder.getScope();
    final List<JSParameterDeclaration> parameters =
        convertParameters(pBuilder, pFunctionDeclaration);
    final String originalFunctionName = getFunctionName(pFunctionDeclaration);
    final String functionName = currentScope.uniquifyName(originalFunctionName);
    final String functionQualifiedName = currentScope.qualifiedFunctionNameOf(functionName);
    final JSFunctionDeclaration jsFunctionDeclaration =
        new JSFunctionDeclaration(
            pBuilder.getFileLocation(pFunctionDeclaration),
            ScopeConverter.toCFAScope(pBuilder.getScope()),
            functionName,
            originalFunctionName,
            functionQualifiedName,
            parameters);
    currentScope.addDeclaration(jsFunctionDeclaration);
    pBuilder.appendEdge(JSDeclarationEdge.of(jsFunctionDeclaration));
    final FunctionScopeImpl functionScope =
        new FunctionScopeImpl(currentScope, jsFunctionDeclaration, pBuilder.getLogger());
    final String returnVariableName = "__retval__";
    final JSVariableDeclaration returnVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            ScopeConverter.toCFAScope(functionScope),
            returnVariableName,
            returnVariableName,
            returnVariableName,
            new JSInitializerExpression(
                FileLocation.DUMMY, new JSUndefinedLiteralExpression(FileLocation.DUMMY)));

    final FunctionExitNode exitNode = new FunctionExitNode(functionQualifiedName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY,
            jsFunctionDeclaration,
            exitNode,
            Optional.of(returnVariableDeclaration));
    exitNode.setEntryNode(entryNode);
    final JavaScriptCFABuilder functionCFABuilder = pBuilder.copyWith(entryNode, functionScope);

    addFunctionEntryNode(functionCFABuilder);

    functionCFABuilder.append(pFunctionDeclaration.getBody());
    if (!functionCFABuilder.getExitNode().equals(exitNode)) {
      functionCFABuilder.appendEdge(
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
                                FileLocation.DUMMY, returnVariableName, returnVariableDeclaration),
                            returnValue))),
                FileLocation.DUMMY,
                pPredecessor,
                exitNode);
          });
    }
    functionCFABuilder.appendTo(pBuilder.getBuilder());

    return jsFunctionDeclaration;
  }

  @SuppressWarnings("unchecked")
  private List<JSParameterDeclaration> convertParameters(
      final JavaScriptCFABuilder pBuilder, final FunctionDeclaration pFunctionDeclaration) {
    final List<SingleVariableDeclaration> parameterDeclarations = pFunctionDeclaration.parameters();
    return parameterDeclarations
        .stream()
        .map(
            parameterDeclaration ->
                new JSParameterDeclaration(
                    pBuilder.getFileLocation(parameterDeclaration),
                    parameterDeclaration.getName().getIdentifier()))
        .collect(Collectors.toList());
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
    final String originalName =
        node.getMethodName() == null ? "" : ((SimpleName) node.getMethodName()).getIdentifier();
    // Declarations of function expressions get a prefix added to their name.
    // Thereby, they do not override functions that are declared in the scope with the same name;
    // like in:
    //   function f() {}
    //   var g = function f() {};
    return node.getParent() instanceof FunctionExpression
        ? "CPAchecker_FunctionExpression_" + originalName + "_" + node.hashCode()
        : originalName;
  }
}

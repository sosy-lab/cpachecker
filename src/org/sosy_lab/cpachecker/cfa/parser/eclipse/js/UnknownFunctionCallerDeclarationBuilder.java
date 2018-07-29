/*
 *  CPAchecker is a tool for configurable software verification.
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

import static org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge.assume;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

public class UnknownFunctionCallerDeclarationBuilder implements FunctionDeclarationAppendable {
  private static final String returnVariableName = "__retval__";
  public static final int maxParameterCount = 10;
  private final FunctionDeclarationAppendable functionDeclarationAppendable;
  private final JSFunctionDeclaration unknownFunctionCallerDeclaration;
  private final JSFunctionEntryNode entryNode;
  private final FunctionExitNode exitNode;
  private final JSParameterDeclaration functionObjectParameter;
  private final ArrayList<JSExpression> parameterIds;
  private CFAEdge lastDefaultReturnEdge;
  private JavaScriptCFABuilder unknownFunctionCallerCFABuilder;
  private JSIdExpression returnVariableId;

  @SuppressWarnings("UnnecessaryLocalVariable")
  UnknownFunctionCallerDeclarationBuilder(
      final FunctionDeclarationAppendable pFunctionDeclarationAppendable) {
    functionDeclarationAppendable = pFunctionDeclarationAppendable;

    final String originalFunctionName = "__CPAchecker_callUnknownFunction";
    final String functionName = originalFunctionName;
    final String functionQualifiedName = originalFunctionName;
    functionObjectParameter = new JSParameterDeclaration(FileLocation.DUMMY, "functionObject");
    parameterIds = new ArrayList<>(maxParameterCount);
    final ArrayList<JSParameterDeclaration> parameters =
        Lists.newArrayList(functionObjectParameter);
    for (int i = 0; i < maxParameterCount; i++) {
      final JSParameterDeclaration p = new JSParameterDeclaration(FileLocation.DUMMY, "p" + i);
      parameters.add(p);
      parameterIds.add(new JSIdExpression(FileLocation.DUMMY, p));
    }
    unknownFunctionCallerDeclaration =
        new JSFunctionDeclaration(
            FileLocation.DUMMY,
            functionName,
            originalFunctionName,
            functionQualifiedName,
            parameters);
    final JSVariableDeclaration returnVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            false,
            returnVariableName,
            returnVariableName,
            returnVariableName,
            new JSInitializerExpression(
                FileLocation.DUMMY, new JSUndefinedLiteralExpression(FileLocation.DUMMY)));
    returnVariableId =
        new JSIdExpression(FileLocation.DUMMY, returnVariableName, returnVariableDeclaration);
    exitNode = new FunctionExitNode(functionQualifiedName);
    entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY,
            getUnknownFunctionCallerDeclaration(),
            exitNode,
            Optional.of(returnVariableDeclaration));
    exitNode.setEntryNode(entryNode);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public JSFunctionDeclaration append(
      final JavaScriptCFABuilder pBuilder, final FunctionDeclaration pFunctionDeclaration) {
    final JSFunctionDeclaration jsFunctionDeclaration =
        functionDeclarationAppendable.append(pBuilder, pFunctionDeclaration);
    initUnknownFunctionCallerCFABuilder(pBuilder);
    final JSExpression condition =
        new JSDeclaredByExpression(
            new JSIdExpression(FileLocation.DUMMY, functionObjectParameter), jsFunctionDeclaration);
    if (lastDefaultReturnEdge != null) {
      CFACreationUtils.removeEdgeFromNodes(lastDefaultReturnEdge);
    }

    final JSVariableDeclaration resultVariableDeclaration = pBuilder.declareVariable();
    final JSIdExpression resultVariableId =
        new JSIdExpression(FileLocation.DUMMY, resultVariableDeclaration);
    final FileLocation functionDeclarationFileLocation =
        pBuilder.getFileLocation(pFunctionDeclaration);
    unknownFunctionCallerCFABuilder.addParseResult(
        unknownFunctionCallerCFABuilder
            .copy()
            .appendEdge(assume(condition, true))
            .appendEdge(
                JSStatementEdge.of(
                    new JSFunctionCallAssignmentStatement(
                        functionDeclarationFileLocation,
                        resultVariableId,
                        new JSFunctionCallExpression(
                            functionDeclarationFileLocation,
                            new JSIdExpression(FileLocation.DUMMY, jsFunctionDeclaration),
                            getParameterIds(jsFunctionDeclaration),
                            jsFunctionDeclaration))))
            .appendEdge(exitNode, returnEdgeWithValue(resultVariableId))
            .getParseResult());
    unknownFunctionCallerCFABuilder.appendEdge(assume(condition, false));
    lastDefaultReturnEdge = returnEdge(unknownFunctionCallerCFABuilder.getExitNode());
    CFACreationUtils.addEdgeToCFA(lastDefaultReturnEdge, pBuilder.getLogger());
    pBuilder.addParseResult(unknownFunctionCallerCFABuilder.getParseResult());
    return jsFunctionDeclaration;
  }

  @Nonnull
  private List<JSExpression> getParameterIds(final JSFunctionDeclaration pJsFunctionDeclaration) {
    final int parameterCount = pJsFunctionDeclaration.getParameters().size();
    assert parameterCount <= maxParameterCount;
    return parameterIds.subList(0, parameterCount);
  }

  private void initUnknownFunctionCallerCFABuilder(final JavaScriptCFABuilder pBuilder) {
    if (unknownFunctionCallerCFABuilder == null) {
      unknownFunctionCallerCFABuilder =
          pBuilder.copyWith(
              entryNode,
              new FunctionScopeImpl(DummyScope.instance, unknownFunctionCallerDeclaration));
    }
  }

  JSFunctionDeclaration getUnknownFunctionCallerDeclaration() {
    return unknownFunctionCallerDeclaration;
  }

  private BiFunction<CFANode, CFANode, CFAEdge> returnEdgeWithValue(
      final JSExpression pReturnValue) {
    return (final CFANode pPredecessor, final CFANode pSuccessor) ->
        new JSReturnStatementEdge(
            "return " + pReturnValue.toASTString(),
            new JSReturnStatement(
                FileLocation.DUMMY,
                Optional.of(pReturnValue),
                Optional.of(
                    new JSExpressionAssignmentStatement(
                        FileLocation.DUMMY, returnVariableId, pReturnValue))),
            FileLocation.DUMMY,
            pPredecessor,
            exitNode);
  }

  private CFAEdge returnEdge(final CFANode pPredecessor) {
    final JSUndefinedLiteralExpression returnValue =
        new JSUndefinedLiteralExpression(FileLocation.DUMMY);
    return new JSReturnStatementEdge(
        "return;",
        new JSReturnStatement(
            FileLocation.DUMMY,
            Optional.of(returnValue),
            Optional.of(
                new JSExpressionAssignmentStatement(
                    FileLocation.DUMMY, returnVariableId, returnValue))),
        FileLocation.DUMMY,
        pPredecessor,
        exitNode);
  }
}

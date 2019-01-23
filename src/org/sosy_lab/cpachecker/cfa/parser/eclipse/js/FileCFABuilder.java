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
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FileCFABuilder implements JavaScriptUnitAppendable {
  private static final String functionName = "main";

  private final CFABuilder builder;
  private final FunctionExitNode exitNode;
  private final JavaScriptUnitAppendable javaScriptUnitAppendable;
  private final FunctionDeclarationCFABuilder knownFunctionDeclarationBuilder;
  private final FunctionScope scope;

  FileCFABuilder(
      final Scope pScope,
      final LogManager pLogger,
      final JavaScriptUnitAppendable pJavaScriptUnitAppendable,
      final FunctionDeclarationCFABuilder pKnownFunctionDeclarationBuilder) {
    javaScriptUnitAppendable = pJavaScriptUnitAppendable;
    knownFunctionDeclarationBuilder = pKnownFunctionDeclarationBuilder;
    final JSFunctionDeclaration functionDeclaration =
        new JSFunctionDeclaration(
            FileLocation.DUMMY,
            org.sosy_lab.cpachecker.cfa.ast.js.Scope.GLOBAL,
            functionName,
            functionName,
            functionName,
            Collections.emptyList());
    exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY, functionDeclaration, exitNode, Optional.absent());
    exitNode.setEntryNode(entryNode);
    scope = new FunctionScopeImpl(pScope, functionDeclaration, pLogger);
    builder = new CFABuilder(scope, pLogger, entryNode);
  }

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final JavaScriptUnit pUnit) {
    pBuilder.appendEdge(DummyEdge.withDescription("File start dummy edge"));
    new Hoisting(scope, knownFunctionDeclarationBuilder, pBuilder).append(pUnit);
    javaScriptUnitAppendable.append(pBuilder, pUnit);
    pBuilder.appendEdge(exitNode, DummyEdge.withDescription("File end dummy edge"));
  }

  public CFABuilder getBuilder() {
    return builder;
  }
}

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

import java.util.Collections;
import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSFunctionType;

class FileCFABuilder {
  private static final String functionName = "main";

  private final CFABuilder builder;
  private final StatementCFABuilder statementBuilder;
  private final FunctionExitNode exitNode;

  FileCFABuilder(final Scope pScope, final LogManager pLogger) {
    final JSFunctionDeclaration functionDeclaration =
        new JSFunctionDeclaration(
            FileLocation.DUMMY,
            new JSFunctionType(JSAnyType.ANY, Collections.emptyList()),
            functionName,
            Collections.emptyList());
    exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY, functionDeclaration, exitNode, Optional.empty());
    exitNode.setEntryNode(entryNode);
    builder = new CFABuilder(pLogger, new ASTConverter(pScope, pLogger), entryNode);
    statementBuilder = new StatementCFABuilder(builder);
  }

  public void append(final JavaScriptUnit unit) {
    unit.accept(statementBuilder);
    builder.appendEdge(
        exitNode,
        (pPredecessor, pSuccessor) ->
            new BlankEdge("", FileLocation.DUMMY, pPredecessor, pSuccessor, "File end dummy edge"));
  }

  public CFABuilder getBuilder() {
    return builder;
  }
}

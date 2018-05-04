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

import com.google.common.base.Optional;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSReturnStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class ReturnStatementCFABuilder implements ReturnStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final ReturnStatement pStatement) {
    final Expression returnValueExpression = pStatement.getExpression();
    // TODO reconsider if implicitly declared undefined-literal really has to be returned in CFA
    final JSExpression returnValue =
        returnValueExpression != null
            ? pBuilder.append(returnValueExpression)
            : new JSUndefinedLiteralExpression(FileLocation.DUMMY);
    final FunctionExitNode exitNode = pBuilder.getFunctionExitNode();
    final JSIdExpression returnVariableId = pBuilder.getReturnVariableId();
    pBuilder.appendEdge(
        exitNode,
        (pPredecessor, pSuccessor) ->
            new JSReturnStatementEdge(
                "return;",
                new JSReturnStatement(
                    FileLocation.DUMMY,
                    Optional.of(returnValue),
                    Optional.of(
                        new JSExpressionAssignmentStatement(
                            FileLocation.DUMMY, returnVariableId, returnValue))),
                FileLocation.DUMMY,
                pPredecessor,
                exitNode));
  }
}

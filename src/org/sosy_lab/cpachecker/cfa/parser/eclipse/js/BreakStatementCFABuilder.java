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

import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSBreakEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class BreakStatementCFABuilder implements BreakStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final BreakStatement pBreakStatement) {
    final BreakExitScope breakExitScope = findBreakExitScope(pBuilder, pBreakStatement);
    assert breakExitScope != null
        : "BreakStatement has to be in a loop, switch or labeled statement";
    pBuilder.appendJumpExitEdge(
        breakExitScope.getBreakExitNode(),
        (final CFANode pPredecessor, final CFANode pSuccessor) ->
            new JSBreakEdge(
                pBreakStatement.toString(),
                pBuilder.getFileLocation(pBreakStatement),
                pPredecessor,
                pSuccessor));
  }

  private BreakExitScope findBreakExitScope(
      final JavaScriptCFABuilder pBuilder, final BreakStatement pBreakStatement) {
    if (pBreakStatement.getLabel() == null) {
      return pBuilder.getScope().getScope(BreakExitScope.class);
    }
    final String labelName = pBreakStatement.getLabel().getIdentifier();
    LabeledStatementScope current = pBuilder.getScope().getScope(LabeledStatementScope.class);
    assert current != null : "BreakStatement has to be in a loop, switch or labeled statement";
    while (current != null && !current.getLabelName().equals(labelName)) {
      current = current.getParentScope(LabeledStatementScope.class);
    }
    assert current != null && current.getLabelName().equals(labelName)
        : "label \"" + labelName + "\" not found in " + pBuilder.getFileLocation(pBreakStatement);
    return current;
  }
}

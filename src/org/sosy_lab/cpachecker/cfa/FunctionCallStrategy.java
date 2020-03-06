/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class FunctionCallStrategy extends SingleNodeStrategy {

  public FunctionCallStrategy(LogManager pLogger) {
    super(pLogger);
    nodesAtATime = 8;
  }

  @Override
  protected boolean canDeleteNode(CFANode pNode) {
    if (!super.canDeleteNode(pNode)) {
      return false;
    }
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    if (!(leavingEdge instanceof AStatementEdge)) {
      return false;
    }
    return (((AStatementEdge) leavingEdge).getStatement() instanceof AFunctionCall);
  }

  //  private void clearUnusedFunctions() {
  //    ImmutableSet<FunctionEntryNode> functionEntries =
  //        ImmutableSet.copyOf(parseResult.getFunctions().values());
  //    for (FunctionEntryNode entry : functionEntries) {
  //      System.out.print("function " + entry.getFunctionName());
  //      if (entry.getNumEnteringEdges() == 0) {
  //        System.out.println(" is not needed");
  //        parseResult.getCFANodes().removeAll(entry.getFunctionName());
  //        parseResult.getFunctions().remove(entry.getFunctionName());
  //      } else {
  //        System.out.printf(" has %d calls\n", entry.getNumEnteringEdges());
  //      }
  //    }
  //  }
}

// TODO FunctionBodyStrategy
// 2. add o this

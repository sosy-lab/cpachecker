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
package org.sosy_lab.cpachecker.cfa.mutation.strategy;

import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class NodeWithLoopStrategy extends GenericCFAMutationStrategy<CFANode, CFANode> {

  public NodeWithLoopStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger, pRate, pStartDepth, "Nodes with loops");
  }

  @Override
  protected boolean canRemove(ParseResult pParseResult, CFANode pNode) {
    return super.canRemove(pParseResult, pNode)
        && pNode.getNumLeavingEdges() == 1
        && pNode.hasEdgeTo(pNode);
  }

  @Override
  protected Collection<CFANode> getAllObjects(ParseResult pParseResult) {
    Collection<CFANode> answer = new ArrayList<>();
    for (CFANode node : pParseResult.getCFANodes().values()) {
      if (canRemove(pParseResult, node)) {
        answer.add(node);
      }
    }
    return answer;
  }

  @Override
  protected CFANode getRollbackInfo(ParseResult pParseResult, CFANode pObject) {
    return pObject;
  }

  @Override
  protected void removeObject(ParseResult pParseResult, CFANode pObject) {
    CFAEdge loopEdge = pObject.getLeavingEdge(0);
    // if it's a loop on exit node, insert node before
    if (pObject instanceof FunctionExitNode) {
      CFANode newNode = new CFANode(pObject.getFunctionName());
      CFAEdge newLeavingEdge = dupEdge(loopEdge, newNode, pObject);
      disconnectEdge(loopEdge);
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(pObject)) {
        CFAEdge newEnteringEdge = dupEdge(enteringEdge, newNode);
        disconnectEdge(enteringEdge);
        connectEdge(newEnteringEdge);
      }
      connectEdge(newLeavingEdge);
      addNodeToParseResult(pParseResult, newNode);

    } else { // else insert node after
      CFANode newNode = new CFATerminationNode(pObject.getFunctionName());
      CFAEdge newEdge = dupEdge(loopEdge, newNode);
      disconnectEdge(loopEdge);
      connectEdge(newEdge);
      addNodeToParseResult(pParseResult, newNode);
    }
  }

  @Override
  protected void returnObject(ParseResult pParseResult, CFANode pRollbackInfo) {
    if (pRollbackInfo instanceof FunctionExitNode) {
      assert pRollbackInfo.getNumEnteringEdges() == 1;
      CFAEdge wasLoopEdge = pRollbackInfo.getEnteringEdge(0);
      CFANode insertedNode = wasLoopEdge.getPredecessor();
      disconnectEdge(wasLoopEdge);
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(insertedNode)) {
        disconnectEdge(enteringEdge);
        connectEdge(dupEdge(enteringEdge, pRollbackInfo));
      }
      connectEdge(dupEdge(wasLoopEdge, pRollbackInfo, pRollbackInfo));
      removeNodeFromParseResult(pParseResult, insertedNode);
    } else {
      assert pRollbackInfo.getNumLeavingEdges() == 1;
      CFAEdge insertedEdge = pRollbackInfo.getLeavingEdge(0);
      removeNodeFromParseResult(pParseResult, insertedEdge.getSuccessor());
      disconnectEdge(insertedEdge);
      CFAEdge loopEdge = dupEdge(insertedEdge, pRollbackInfo);
      connectEdge(loopEdge);
    }
  }
}

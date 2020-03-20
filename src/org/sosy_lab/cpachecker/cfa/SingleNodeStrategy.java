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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SingleNodeStrategy extends GenericCFAMutationStrategy<CFANode, CFANode> {

  public SingleNodeStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger, pRate, pStartDepth);
  }

  // can delete node with its only leaving edge and reconnect entering edge instead
  @Override
  protected boolean canRemove(ParseResult pParseResult, CFANode pNode) {
    if (!super.canRemove(pParseResult, pNode)) {
      return false;
    }

    if (pNode instanceof FunctionEntryNode
        || pNode instanceof FunctionExitNode
        || pNode instanceof CFATerminationNode) {
      return false;
    }
    if (pNode.getNumLeavingEdges() != 1) {
      return false;
    }
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    for (CFANode predecessor : CFAUtils.allPredecessorsOf(pNode)) {
      if (predecessor.hasEdgeTo(successor)) {
        return false;
      }
    }

    return true;
  }

  @Override
  protected Collection<CFANode> getAllObjects(ParseResult parseResult) {
    List<CFANode> answer = new ArrayList<>();
    for (CFANode node : parseResult.getCFANodes().values()) {
      if (canRemove(parseResult, node)) {
        answer.add(node);
      }
    }
    return answer;
  }

  @Override
  protected Collection<CFANode> getObjects(ParseResult parseResult, int pCount) {
    List<CFANode> result = new ArrayList<>();
    Set<CFANode> succs = new HashSet<>();

    int found = 0;
    for (CFANode node : getAllObjects(parseResult)) {
      if (!canRemove(parseResult, node) || succs.contains(node)) {
        continue;
      }

      CFANode successor = node.getLeavingEdge(0).getSuccessor();
      if (succs.contains(successor)) {
        continue;
      }
      succs.add(successor);
      succs.add(node);

      logger.logf(Level.FINER, "Choosing %s:%s (s: %s)", node.getFunctionName(), node, successor);
      result.add(node);

      if (++found >= pCount) {
        break;
      }
    }

    return ImmutableList.copyOf(result);
  }

  // remove the node with its only leaving and entering edges
  // and insert new edge similar to entering edge.
  @Override
  protected void removeObject(ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    logger.logf(Level.FINE, "removing node %s with edge %s", pNode, leavingEdge);
    logger.logf(Level.FINEST, "entering edges: %s", CFAUtils.allEnteringEdges(pNode));
    logger.logf(
        Level.FINEST, "successor's entering edges: %s", CFAUtils.allEnteringEdges(successor));

    disconnectEdgeFromNode(leavingEdge, successor);

    for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(pNode)) {
      CFANode predecessor = enteringEdge.getPredecessor();
      disconnectEdgeFromNode(enteringEdge, predecessor);

      CFAEdge newEdge = dupEdge(enteringEdge, successor);
      connectEdge(newEdge);
    }

    removeNodeFromParseResult(parseResult, pNode);
  }

  // undo removing a node with leaving edge:
  // insert node, delete inserted edge, reconnect edges
  @Override
  protected void returnObject(ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    logger.logf(Level.FINE, "returning node %s with edge %s", pNode, leavingEdge);

    logger.logf(Level.FINEST, "entering edges: %s", CFAUtils.allEnteringEdges(pNode));
    logger.logf(
        Level.FINEST, "successor's entering edges: %s", CFAUtils.allEnteringEdges(successor));

    for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(pNode)) {
      CFANode predecessor = enteringEdge.getPredecessor();

      CFAEdge insertedEdge = predecessor.getEdgeTo(successor);
      disconnectEdge(insertedEdge);
      connectEdgeToNode(enteringEdge, predecessor);
    }

    connectEdgeToNode(leavingEdge, successor);
    addNodeToParseResult(parseResult, pNode);

  }

  @Override
  protected CFANode getRollbackInfo(ParseResult pParseResult, CFANode pNode) {
    return pNode;
  }
}

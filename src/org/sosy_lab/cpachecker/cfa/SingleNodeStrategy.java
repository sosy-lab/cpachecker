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
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SingleNodeStrategy extends AbstractCFAMutationStrategy {

  private final Set<CFANode> previousMutations = new TreeSet<>();
  private List<CFANode> currentMutation = new ArrayList<>();
  private Set<CFANode> rollbackedNodes = new TreeSet<>();
  protected int nodesAtATime;

  private Set<CFANode> deletedNodes = new HashSet<>();

  public SingleNodeStrategy(LogManager pLogger) {
    this(pLogger, 0);
  }

  public SingleNodeStrategy(LogManager pLogger, int pNodesAtATime) {
    super(pLogger);
    nodesAtATime = pNodesAtATime;
  }

  @Override
  public long countPossibleMutations(final ParseResult parseResult) {
    long possibleMutations = 0;

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (canDeleteNode(node)) {
        possibleMutations += 1;
      }
    }

    if (nodesAtATime == 0) {
      nodesAtATime = (int) Math.round(Math.sqrt(nodesAtATime));
    }

    return possibleMutations;
  }

  // can delete node with its only leaving edge and reconnect entering edge instead
  protected boolean canDeleteNode(CFANode pNode) {
    if (pNode instanceof FunctionEntryNode
        || pNode instanceof FunctionExitNode
        || pNode instanceof CFATerminationNode) {
      return false;
    }
    if (pNode.getNumLeavingEdges() != 1) {
      return false;
    }

    CFANode successor = pNode.getLeavingEdge(0).getSuccessor();
    for (CFANode predecessor : CFAUtils.predecessorsOf(pNode)) {
      if (predecessor.hasEdgeTo(successor)) {
        return false;
      }
    }

    return true;
  }

  public Collection<CFANode> chooseNodesToRemove(ParseResult parseResult) {
    List<CFANode> result = new ArrayList<>();
    Set<CFANode> succs = new HashSet<>();

    int nodesFound = 0;
    for (CFANode node : parseResult.getCFANodes().values()) {
      if (previousMutations.contains(node) || !canDeleteNode(node) || succs.contains(node)) {
        continue;
      }

      CFANode successor = node.getLeavingEdge(0).getSuccessor();
      if (succs.contains(successor)) {
        continue;
      }
      succs.add(successor);

      logger.logf(
          Level.FINER,
          "Choosing (p: %s) %s:%s (s: %s)",
          null,
          node.getFunctionName(),
          node,
          successor);
      result.add(node);

      if (++nodesFound >= nodesAtATime) {
        break;
      }
    }

    previousMutations.addAll(result);
    currentMutation = result;
    return ImmutableList.copyOf(result);
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    deletedNodes.clear();

    Collection<CFANode> chosenNodes = chooseNodesToRemove(parseResult);
    if (chosenNodes.isEmpty()) {
      return false;
    }

    for (CFANode node : chosenNodes) {
      removeLeavingEdgeAndConnectEnteringEdgeAround(parseResult, node);
    }

    return true;
  }

  @Override
  public void rollback(ParseResult parseResult) {
    rollbackedNodes.addAll(currentMutation);
    for (CFANode node : deletedNodes) {
      returnNodeWithLeavingEdge(parseResult, node);
    }
  }

  // remove the node with its only leaving and entering edges
  // and insert new edge similar to entering edge.
  private void removeLeavingEdgeAndConnectEnteringEdgeAround(
      ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    disconnectEdgeFromNode(leavingEdge, successor);

    for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(pNode)) {
      CFANode predecessor = enteringEdge.getPredecessor();
      disconnectEdgeFromNode(enteringEdge, predecessor);

      CFAEdge newEdge = dupEdge(enteringEdge, successor);
      connectEdge(newEdge);
    }

    removeNodeFromParseResult(parseResult, pNode);

    deletedNodes.add(pNode);
    logger.logf(Level.INFO, "removing node %s with edge %s", pNode, leavingEdge);
  }

  // undo removing a node with leaving edge:
  // insert node, delete inserted edge, reconnect edges
  private void returnNodeWithLeavingEdge(ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();

    for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(pNode)) {
      CFANode predecessor = enteringEdge.getPredecessor();

      CFAEdge insertedEdge = predecessor.getEdgeTo(successor);
      disconnectEdge(insertedEdge);
      connectEdgeToNode(enteringEdge, predecessor);
    }

    connectEdgeToNode(leavingEdge, successor);
    addNodeToParseResult(parseResult, pNode);

    logger.logf(Level.FINE, "returning node %s with edge %s", pNode, leavingEdge);
  }
}

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

class Chain extends ArrayDeque<CFANode> {

  private static final long serialVersionUID = -1849261707800370541L;

  public CFAEdge getEnteringEdge() {
    CFANode firstNode = peekFirst();
    if (firstNode == null) {
      return null;
    }
    assert firstNode.getNumEnteringEdges() == 1;
    return firstNode.getEnteringEdge(0);
  }

  public CFANode getPredecessor() {
    CFAEdge e = getEnteringEdge();
    if (e == null) {
      return null;
    }
    return e.getPredecessor();
  }

  public CFAEdge getLeavingEdge() {
    CFANode lastNode = peekLast();
    if (lastNode == null) {
      return null;
    }
    assert lastNode.getNumLeavingEdges() == 1;
    return lastNode.getLeavingEdge(0);
  }

  public CFANode getSuccessor() {
    CFAEdge e = getLeavingEdge();
    if (e == null) {
      return null;
    }
    return e.getSuccessor();
  }

  public String getDescription() {
    String desc;
    StringBuilder sb = new StringBuilder();
    for (CFANode n : this) {
      desc = n.getEnteringEdge(0).getDescription();
      if (!desc.isBlank()) {
        sb.append("\n").append(desc);
      }
    }
    desc = getLeavingEdge().getDescription();
    if (!desc.isBlank()) {
      sb.append("\n").append(desc);
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(0);
    }
    return sb.toString();
  }
}

public class ChainStrategy extends GenericCFAMutationStrategy<Chain, Chain> {

  private Set<CFANode> previousChainsNodes = new HashSet<>();

  public ChainStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger, pRate, pStartDepth, "Chains");
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
    if (pNode.getNumEnteringEdges() != 1) {
      return false;
    }

    CFANode successor = pNode.getLeavingEdge(0).getSuccessor();
    CFANode predecessor = pNode.getEnteringEdge(0).getPredecessor();
    if (predecessor.hasEdgeTo(successor)) {
      return false; // and chains from with predecessor and successor are checked in getObjects
    }

    return true;
  }

  @Override
  protected Collection<Chain> getAllObjects(ParseResult parseResult) {
    List<Chain> chosenChains = new ArrayList<>();
    Set<CFANode> seenNodes = new HashSet<>();

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (previousChainsNodes.contains(node) || !canDeleteNode(node) || seenNodes.contains(node)) {
        continue;
      }
      Chain chain = getChainWith(node);
      seenNodes.addAll(chain);
      chosenChains.add(chain);
    }
    return chosenChains;
  }

  @Override
  protected Collection<Chain> getObjects(ParseResult parseResult, int count) {
    int found = 0;
    List<Chain> chosenChains = new ArrayList<>();
    Set<CFANode> seenNodes = new HashSet<>();

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (previousChainsNodes.contains(node) || !canDeleteNode(node) || seenNodes.contains(node)) {
        continue;
      }
      Chain chain = getChainWith(node);
      seenNodes.addAll(chain);

      // get chain one node less
      // in case predecessor is already connected directly to successor
      // or will be because of other chain deleted
      if (chain.getPredecessor().getNumLeavingEdges() > 1) {
        if (chain.getPredecessor().hasEdgeTo(chain.getSuccessor())) {
          chain.pollFirst();
        } else {
          for (Chain cc : chosenChains) {
            if (cc.getPredecessor() == chain.getPredecessor()
                && cc.getSuccessor() == chain.getSuccessor()) {
              chain.pollFirst();
              break; // next node has only one leaving edge, so no other chain
            }
          }
        }
      }

      if (chain.peekFirst() == null) {
        continue;
      }

      chosenChains.add(chain);
      previousChainsNodes.addAll(chain);
      if (found++ > count) {
        break;
      }
    }
    return chosenChains;
  }

  private class NodePollingChainVisitor extends CFATraversal.DefaultCFAVisitor {
    private Chain chainNodes = new Chain();
    public boolean forwards;

    public NodePollingChainVisitor(boolean pForwards) {
      forwards = pForwards;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      assert !chainNodes.contains(pNode) : pNode.toString() + " is already in chain " + chainNodes;

      if (!canDeleteNode(pNode)) {
        return TraversalProcess.SKIP;
      }

      if (forwards) {
        chainNodes.addLast(pNode);
      } else {
        chainNodes.addFirst(pNode);
      }
      return TraversalProcess.CONTINUE;
    }

    public void changeDirection() {
      forwards = !forwards;
    }

    public Chain getChain() {
      return chainNodes;
    }
  }

  public Chain getChainWith(CFANode pNode) {
    NodePollingChainVisitor oneWayChainVisitor = new NodePollingChainVisitor(false);
    CFATraversal.dfs().backwards().traverse(pNode, oneWayChainVisitor);
    oneWayChainVisitor.changeDirection();
    CFATraversal.dfs().traverse(pNode.getLeavingEdge(0).getSuccessor(), oneWayChainVisitor);
    return oneWayChainVisitor.getChain();
  }

  @Override
  protected void removeObject(ParseResult parseResult, Chain pChain) {
    CFAEdge leavingEdge = pChain.getLeavingEdge();
    CFANode successor = leavingEdge.getSuccessor();
    disconnectEdgeFromNode(leavingEdge, successor);

    CFAEdge enteringEdge = pChain.getEnteringEdge();
    CFANode predecessor = enteringEdge.getPredecessor();
    disconnectEdgeFromNode(enteringEdge, predecessor);

    for (CFANode node : pChain) {
      removeNodeFromParseResult(parseResult, node);
    }

    CFAEdge newEdge = dupEdge(enteringEdge, successor);
    logger.logf(Level.FINE, "replacing chain %s with edge %s", pChain, newEdge);
    connectEdge(newEdge);
  }

  @Override
  protected void returnObject(ParseResult parseResult, Chain pChain) {
    CFAEdge leavingEdge = pChain.getLeavingEdge();
    CFANode successor = leavingEdge.getSuccessor();

    CFAEdge enteringEdge = pChain.getEnteringEdge();
    CFANode predecessor = enteringEdge.getPredecessor();

    CFAEdge insertedEdge = predecessor.getEdgeTo(successor);
    disconnectEdge(insertedEdge);

    connectEdgeToNode(enteringEdge, predecessor);
    connectEdgeToNode(leavingEdge, successor);

    for (CFANode node : pChain) {
      addNodeToParseResult(parseResult, node);
    }

    logger.logf(Level.FINE, "returning chain %s with edge %s", pChain, leavingEdge);
  }

  @Override
  protected Chain getRollbackInfo(ParseResult pParseResult, Chain pChain) {
    return pChain;
  }
}

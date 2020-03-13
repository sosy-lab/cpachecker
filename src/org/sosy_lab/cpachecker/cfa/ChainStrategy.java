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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class ChainStrategy extends AbstractCFAMutationStrategy {

  private class Chain extends ArrayDeque<CFANode> {

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

  }

  private final Set<CFANode> answered = new TreeSet<>();
  private final Set<CFANode> lastAnswer = new TreeSet<>();
  private Set<CFANode> rollbackedNodes = new TreeSet<>();

  private Set<Chain> deletedChains = new HashSet<>();
  private int chainsAtATime;

  public ChainStrategy(LogManager pLogger) {
    super(pLogger);
    chainsAtATime = 1;
  }

  public ChainStrategy(LogManager pLogger, int pChainsAtATime) {
    super(pLogger);
    chainsAtATime = pChainsAtATime;
  }

  @Override
  public long countPossibleMutations(final ParseResult parseResult) {

    int result = chooseChains(parseResult, true).size();

    return result;
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
      return false;
      // TODO actually we could delete chain but one edge and so get double edge....
      // redo?..
    }

    return true;
  }

  private Collection<Chain> chooseChains(ParseResult parseResult, boolean counting) {
    int found = 0;
    List<Chain> chosenChains = new ArrayList<>();
    Set<CFANode> seenNodes = new HashSet<>();

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (answered.contains(node) || !canDeleteNode(node) || seenNodes.contains(node)) {
        continue;
      }
      Chain chain = pollChainFrom(parseResult, node);
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
            }
          }
        }
      }

      if (chain.peekFirst() == null) {
        continue;
      }

      chosenChains.add(chain);
      if (!counting) {
        answered.addAll(chain);
        if (found++ > chainsAtATime) {
          break;
        }
      }
    }
    return chosenChains;
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    deletedChains.clear();
    Collection<Chain> chosenChains = chooseChains(parseResult, false);
    if (chosenChains.isEmpty()) {
      return false;
    }

    for (Chain chain : chosenChains) {
      removeChain(parseResult, chain);
      deletedChains.add(chain);
    }

    return !deletedChains.isEmpty();
  }

  @Override
  public void rollback(ParseResult parseResult) {
    rollbackedNodes.addAll(lastAnswer);
    for (final Chain chain : deletedChains) {
      returnChain(parseResult, chain);
    }
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

  private Chain pollChainFrom(ParseResult pr, CFANode pNode) {
    SortedSet<CFANode> pNodes = pr.getCFANodes().get(pNode.getFunctionName());
    assert pNodes.contains(pNode);

    NodePollingChainVisitor oneWayChainVisitor = new NodePollingChainVisitor(false);
    CFATraversal.dfs().backwards().traverse(pNode, oneWayChainVisitor);
    oneWayChainVisitor.changeDirection();
    CFATraversal.dfs().traverse(pNode.getLeavingEdge(0).getSuccessor(), oneWayChainVisitor);
    return oneWayChainVisitor.getChain();
  }

  private void removeChain(ParseResult parseResult, Chain pChain) {

    CFAEdge leavingEdge = pChain.getLeavingEdge();
    CFANode successor = leavingEdge.getSuccessor();
    disconnectEdgeFromNode(leavingEdge, successor);

    CFAEdge enteringEdge = pChain.getEnteringEdge();
    CFANode predecessor = enteringEdge.getPredecessor();
    disconnectEdgeFromNode(enteringEdge, predecessor);

    for (CFANode node : pChain) {
      removeNodeFromParseResult(parseResult, node);
    }

    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    connectEdge(newEdge);

    deletedChains.add(pChain);
    logger.logf(Level.INFO, "replacing chain %s with edge %s", pChain, newEdge);
  }

  private void returnChain(ParseResult parseResult, final Chain pChain) {

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
}

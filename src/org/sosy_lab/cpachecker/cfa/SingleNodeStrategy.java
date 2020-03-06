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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class SingleNodeStrategy extends AbstractCFAMutationStrategy {

  private final Set<CFANode> answered = new TreeSet<>();
  private final Set<CFANode> lastAnswer = new TreeSet<>();
  private Set<CFANode> rollbackedNodes = new TreeSet<>();
  protected int nodesAtATime;

  private Set<CFANode> deletedNodes = new HashSet<>();
  private Set<Deque<CFANode>> deletedChains = new HashSet<>();

  public SingleNodeStrategy(LogManager pLogger) {
    super(pLogger);
    nodesAtATime = 2;
  }

  @Override
  public long countPossibleMutations(final ParseResult parseResult) {
    long possibleMutations = 0;

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (canDeleteNode(node)) {
        possibleMutations += 1;
      }
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
    if (pNode.getNumEnteringEdges() != 1) {
      return false;
    }

    CFANode successor = pNode.getLeavingEdge(0).getSuccessor();
    if (successor.getNumEnteringEdges() != 1) {
      return false;
    }
    CFANode predecessor = pNode.getEnteringEdge(0).getPredecessor();
    if (predecessor.getNumLeavingEdges() != 1) {
      return false;
    }

    return true;
  }

  public Collection<CFANode> chooseNodesToRemove(ParseResult parseResult) {
    List<CFANode> answer = new ArrayList<>();
    lastAnswer.clear();

    int nodesFound = 0;
    for (CFANode node : parseResult.getCFANodes().values()) {
      if (answered.contains(node)) {
        continue;
      }
      if (!canDeleteNode(node)) {
        continue;
      }

      CFANode predecessor = node.getEnteringEdge(0).getPredecessor();
      CFANode successor = node.getLeavingEdge(0).getSuccessor();
      assert predecessor.getNumLeavingEdges() == 1;

      System.out.println("Choosing (p:" + predecessor + ") " + node + " (s:" + successor + ")");
      answer.add(node);

      if (++nodesFound >= nodesAtATime) {
        break;
      }
    }

    answered.addAll(answer);
    lastAnswer.addAll(answer);
    return ImmutableList.copyOf(answer);
  }

  @Override
  public boolean mutate(ParseResult parseResult) {
    deletedChains.clear();
    deletedNodes.clear();

    Collection<CFANode> chosenNodes = chooseNodesToRemove(parseResult);
    if (chosenNodes.isEmpty()) {
      return false;
    }
    Set<CFANode> nodesRemained = new HashSet<>(chosenNodes);

    for (CFANode node : chosenNodes) {
      if (!nodesRemained.contains(node)) {
        continue;
      }

      Deque<CFANode> chain = pollChainFrom(nodesRemained, node);
      if (chain.size() > 1) {
        removeChain(parseResult, chain);
      } else {
        removeLeavingEdgeAndConnectEnteringEdgeAround(parseResult, node);
      }
    }

    if (chosenNodes.equals(nodesRemained)) {
      return false;
    }

    return true;
  }

  @Override
  public void rollback(ParseResult parseResult) {
    rollbackedNodes.addAll(lastAnswer);
    for (CFANode node : deletedNodes) {
      returnNodeWithLeavingEdge(parseResult, node);
    }
    for (final Deque<CFANode> chain : deletedChains) {
      returnChainWithLastEdge(parseResult, chain);
    }
  }

  private class NodePollingChainVisitor extends CFATraversal.DefaultCFAVisitor {
    private Deque<CFANode> chainNodes = new ArrayDeque<>();
    private final Collection<CFANode> nodes;
    public boolean forwards;

    public NodePollingChainVisitor(Collection<CFANode> pNodes, boolean pForwards) {
      nodes = pNodes;
      forwards = pForwards;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      assert !chainNodes.contains(pNode) : pNode.toString() + " is already in chain " + chainNodes;

      if (!nodes.remove(pNode)) {
        return TraversalProcess.SKIP;
      }

      assert pNode.getNumEnteringEdges() == 1 && pNode.getNumLeavingEdges() == 1
          : pNode.toString() + " is in nodes-to-delete but has ill count of edges";

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

    public Deque<CFANode> getChain() {
      return chainNodes;
    }
  }

  public Deque<CFANode> pollChainFrom(Set<CFANode> pNodes, CFANode pNode) {
    assert pNodes.contains(pNode);

    NodePollingChainVisitor oneWayChainVisitor = new NodePollingChainVisitor(pNodes, false);
    CFATraversal.dfs().backwards().traverse(pNode, oneWayChainVisitor);
    oneWayChainVisitor.changeDirection();
    CFATraversal.dfs().traverse(pNode.getLeavingEdge(0).getSuccessor(), oneWayChainVisitor);
    return oneWayChainVisitor.getChain();
  }

  private void removeChain(ParseResult parseResult, Deque<CFANode> pChain) {
    CFANode firstNode = pChain.getFirst();
    CFANode lastNode = pChain.getLast();

    assert lastNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = lastNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    assert successor.getNumEnteringEdges() == 1;
    disconnectEdgeFromNode(leavingEdge, successor);

    assert firstNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = firstNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    disconnectEdgeFromNode(enteringEdge, predecessor);

    for (CFANode node : pChain) {
      removeNodeFromParseResult(parseResult, node);
    }

    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    connectEdge(newEdge);

    deletedChains.add(pChain);
    logger.logf(Level.INFO, "replacing chain %s with edge %s", pChain, newEdge);
  }

  private void returnChainWithLastEdge(ParseResult parseResult, final Deque<CFANode> pChain) {
    CFANode firstNode = pChain.getFirst();
    CFANode lastNode = pChain.getLast();

    assert firstNode.getNumEnteringEdges() == 1
        : firstNode.getFunctionName()
            + ":"
            + firstNode
            + " has "
            + firstNode.getNumEnteringEdges()
            + (firstNode.getNumEnteringEdges() > 1
                ? "\nfirst two are\n"
                    + firstNode.getEnteringEdge(0)
                    + "\n"
                    + firstNode.getEnteringEdge(1)
                : "(((((");
    CFAEdge enteringEdge = firstNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    CFAEdge insertedEdge = predecessor.getLeavingEdge(0);
    disconnectEdge(insertedEdge);
    connectEdgeToNode(enteringEdge, predecessor);

    assert lastNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = lastNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    connectEdgeToNode(leavingEdge, successor);

    for (CFANode node : pChain) {
      addNodeToParseResult(parseResult, node);
    }

    logger.logf(Level.INFO, "returning chain %s with edge %s", pChain, leavingEdge);
  }

  // remove the node with its only leaving and entering edges
  // and insert new edge similar to entering edge.
  private void removeLeavingEdgeAndConnectEnteringEdgeAround(
      ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    assert successor.getNumEnteringEdges() == 1;
    disconnectEdgeFromNode(leavingEdge, successor);

    assert pNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = pNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    disconnectEdgeFromNode(enteringEdge, predecessor);

    removeNodeFromParseResult(parseResult, pNode);
    CFAEdge newEdge = dupEdge(enteringEdge, null, successor);
    connectEdge(newEdge);

    deletedNodes.add(pNode);
    logger.logf(Level.INFO, "removing node %s with edge %s", pNode, leavingEdge);
  }

  // undo removing a node with leaving edge:
  // insert node, delete inserted edge, reconnect edges
  private void returnNodeWithLeavingEdge(ParseResult parseResult, CFANode pNode) {
    assert pNode.getNumEnteringEdges() == 1;
    CFAEdge enteringEdge = pNode.getEnteringEdge(0);
    CFANode predecessor = enteringEdge.getPredecessor();
    assert predecessor.getNumLeavingEdges() == 1;
    CFAEdge insertedEdge = predecessor.getLeavingEdge(0);
    disconnectEdge(insertedEdge);
    connectEdgeToNode(enteringEdge, predecessor);

    assert pNode.getNumLeavingEdges() == 1;
    CFAEdge leavingEdge = pNode.getLeavingEdge(0);
    CFANode successor = leavingEdge.getSuccessor();
    connectEdgeToNode(leavingEdge, successor);
    addNodeToParseResult(parseResult, pNode);

    logger.logf(Level.INFO, "returning node %s with edge %s", pNode, leavingEdge);
  }
}

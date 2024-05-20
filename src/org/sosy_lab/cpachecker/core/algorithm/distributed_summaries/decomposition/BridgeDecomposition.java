// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.STBridges.BridgeComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BridgeDecomposition implements BlockSummaryCFADecomposer {

  private int id = 1;

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    SingleBlockDecomposition singleBlockDecomposer = new SingleBlockDecomposition();
    List<BlockNodeWithoutGraphInformation> cutNodes = new ArrayList<>();
    List<BlockNodeWithoutGraphInformation> singleBlockNode =
        new ArrayList<>(singleBlockDecomposer.decompose(cfa).getNodes());    
    
    List<BlockNodeWithoutGraphInformation> listOfAllNodes = new ArrayList<>(singleBlockNode);
    char idPrefixChar = 'B';
    
    
    while (true) {
      for (BlockNodeWithoutGraphInformation blockNode : listOfAllNodes) {

        if (shouldAddBlockNodeDirectly(blockNode)) {    
          cutNodes.add(blockNode);
  
        } else if (shouldDecomposeBlockNode(blockNode)) {
          List<BlockNodeWithoutGraphInformation> newBlockNodes =
              getMoreBlockNodes(idPrefixChar + "", blockNode);
          cutNodes.addAll(newBlockNodes);
  
          idPrefixChar++;
        } else if (shouldComputeBridges(blockNode)) {
          BridgeComponents components = STBridges.computeBridges(blockNode);
          cutNodes.addAll(components.connectionsWithEdges(idPrefixChar + ""));
          idPrefixChar++;
        } else {
          cutNodes.add(blockNode);
        }
      }
     
      if (cutNodes.equals(listOfAllNodes)) {
        break;
      }
      listOfAllNodes = new ArrayList<>(cutNodes);
      
      cutNodes.clear();
    }
    BlockGraph blockGraph = BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, listOfAllNodes);

    return blockGraph;
  }

  

  private List<BlockNodeWithoutGraphInformation> getMoreBlockNodes(
      String idPrefix, BlockNodeWithoutGraphInformation blockNode) {
    List<BlockNodeWithoutGraphInformation> newBlockNodes = new ArrayList<>();
    CFANode startNode = blockNode.getFirst();
    CFANode endNode = blockNode.getLast();

    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(startNode)) {
      Set<CFANode> allNodesOfNewBlockNode = new HashSet<>();
      Set<CFAEdge> allEdgesOfNewBlockNode = new HashSet<>();
      allNodesOfNewBlockNode.add(startNode);
      allEdgesOfNewBlockNode.add(leavingEdge);

      explorePaths(
          leavingEdge.getSuccessor(),
          endNode,
          new HashSet<>(),
          allNodesOfNewBlockNode,
          allEdgesOfNewBlockNode);

      BlockNodeWithoutGraphInformation newBlockNode =
          new BlockNodeWithoutGraphInformation(
              idPrefix + id++,
              startNode,
              blockNode.getLast(),
              ImmutableSet.copyOf(allNodesOfNewBlockNode),
              ImmutableSet.copyOf(allEdgesOfNewBlockNode));

      newBlockNodes.add(newBlockNode);
    }
    return newBlockNodes;
  }

  private void explorePaths(
      CFANode currentNode,
      CFANode endNode,
      Set<CFANode> visitedNodes,
      Set<CFANode> allNodesOfNewBlockNode,
      Set<CFAEdge> allEdgesOfNewBlockNode) {

    visitedNodes.add(currentNode);
    allNodesOfNewBlockNode.add(currentNode);

    if (currentNode.equals(endNode)) {
      return;
    }

    for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
      CFAEdge edge = currentNode.getLeavingEdge(i);
      allEdgesOfNewBlockNode.add(edge);
      CFANode successor = edge.getSuccessor();
      if (!visitedNodes.contains(successor)) {
        explorePaths(
            successor, endNode, visitedNodes, allNodesOfNewBlockNode, allEdgesOfNewBlockNode);
      }
    }
  }

  private boolean shouldAddBlockNodeDirectly(BlockNodeWithoutGraphInformation blockNode) {
    return !hasNodeWithMoreThanOneLeavingEdge(blockNode) || blockNode.getNodes().size() <= 2;
  }

  private boolean shouldDecomposeBlockNode(BlockNodeWithoutGraphInformation blockNode) {
    return blockNode.getFirst().getNumLeavingEdges() > 1
        && !onlyOneLeavingEdgeIsPartOfBlockNode(blockNode);
  }

  private boolean onlyOneLeavingEdgeIsPartOfBlockNode(BlockNodeWithoutGraphInformation blockNode) {
    int counter = 0;
    for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(blockNode.getFirst())) {
      if (blockNode.getEdges().contains(leavingEdge)) {
        counter++;
      }
    }
    return counter == 1;
  }

  private boolean shouldComputeBridges(BlockNodeWithoutGraphInformation blockNode) {
    for (CFANode node : blockNode.getNodes()) {
      if (node.getNumLeavingEdges() > 1 && !node.equals(blockNode.getFirst())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasNodeWithMoreThanOneLeavingEdge(BlockNodeWithoutGraphInformation blockNode) {
    for (CFANode node : blockNode.getNodes()) {
      if (node.getNumLeavingEdges() > 1) {
        return true;
      }
    }
    return false;
  }
}

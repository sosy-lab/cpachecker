// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph.BlockGraphFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

/** Decomposer for linear coherent subgraphs of a given CFA. */
public class BlockOperatorDecomposer implements CFADecomposer {

  private final BlockOperator operator;
  private final ShutdownNotifier shutdownNotifier;

  /**
   * Partitions the complete CFA into small linear subgraphs. The union of the subgraphs equals the
   * initial CFA.
   *
   * @param pConfiguration user-provided configuration
   * @throws InvalidConfigurationException thrown if config contains wrong values
   */
  public BlockOperatorDecomposer(Configuration pConfiguration, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    operator = new BlockOperator();
    pConfiguration.inject(operator);
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public BlockGraph cut(CFA cfa) throws InterruptedException {
    operator.setCFA(cfa);
    // start with the first node of the CFA
    CFANode startNode = cfa.getMainFunction();

    BlockGraphFactory factory = new BlockGraphFactory(cfa, shutdownNotifier);

    // create the root node of the tree consisting of the entry node only
    BlockNodeMetaData root =
        factory.makeBlock(startNode, startNode, ImmutableSet.of(startNode), ImmutableSet.of());

    factory.setRoot(root);

    // the stack stores all block ends (i.e., operator.isBlockEnd(node) == true)
    ArrayDeque<CFANode> blockEnds = new ArrayDeque<>();
    // the first block end is our start node since it is the end of the first BlockNode
    blockEnds.push(startNode);

    // map a CFANode (that is the last node of a BlockNode) to its BlockNode
    // we use multimap since one CFANode can have multiple successors
    Multimap<CFANode, BlockNodeMetaData> lastNodeMap = ArrayListMultimap.create();
    // the first entry node maps to the root node of the BlockTree
    lastNodeMap.put(startNode, root);

    // map a CFANode (that is the start node of a BlockNode) to its BlockNode
    // we use multimap since one CFANode can have multiple successors
    Multimap<CFANode, BlockNodeMetaData> startNodeMap = ArrayListMultimap.create();
    // the first entry node maps to the root node of the BlockTree
    startNodeMap.put(startNode, root);

    Set<CFANode> coveredBlockEnds = new LinkedHashSet<>();
    // stores the currently popped node (blockEnds.pop())
    CFANode lastCFANode;
    // contains nodes that are successors of lastCFANode AND belong to the same block
    ArrayDeque<Entry> toSearch = new ArrayDeque<>();
    // if blockEnds is empty, we are at the end of the CFA
    while (!blockEnds.isEmpty()) {
      lastCFANode = blockEnds.pop();
      if (coveredBlockEnds.contains(lastCFANode)) {
        continue;
      } else {
        coveredBlockEnds.add(lastCFANode);
      }
      // add all successors of lastCFANode to toSearch.
      // additionally, we want to store any node that is between start -> end of a BlockNode,
      // so we use the class Entry to track the current node and the nodes in between.
      final CFANode currentRootNode = lastCFANode;
      CFAUtils.leavingEdges(lastCFANode)
          .transform(
              edge ->
                  new Entry(
                      edge.getSuccessor(),
                      new LinkedHashSet<>(ImmutableList.of(currentRootNode, edge.getSuccessor())),
                      new LinkedHashSet<>(ImmutableList.of(edge)),
                      0))
          .copyInto(toSearch);

      // if toSearch is empty, all successor nodes of lastCFANode have reached the block end
      while (!toSearch.isEmpty()) {
        // look at the top entry
        Entry nodeEntry = toSearch.pop();
        // successorOfCurrNode is a successor of lastCFANode
        CFANode successorOfCurrNode = nodeEntry.getNode();
        if (operator.isBlockEnd(successorOfCurrNode, nodeEntry.getThreshold())) {
          // alternative: if (successorOfCurrNode.getNumEnteringEdges() > 1 ||
          // successorOfCurrNode.getNumLeavingEdges() > 1) {
          // if successorOfCurrNode is a block end, create a new BlockNode that starts with
          // lastCFANode and ends with
          // successorOfCurrNode. Additionally, tell the BlockNode which nodes are part of the
          // block.
          blockEnds.push(successorOfCurrNode);
          BlockNodeMetaData childBlockNode =
              factory.makeBlock(
                  lastCFANode, successorOfCurrNode, nodeEntry.getSeen(), nodeEntry.getSeenEdges());

          // every previous block that ends with lastCFANode is now linked to the new BlockNode that
          // starts with lastCFANode.
          lastNodeMap
              .get(lastCFANode)
              .forEach(contained -> factory.linkSuccessor(contained, childBlockNode));
          // every previous block that starts with the current end node is now linked to the new
          // BlockNode
          startNodeMap
              .get(successorOfCurrNode)
              .forEach(contained -> factory.linkSuccessor(childBlockNode, contained));

          // current BlockNode is stored nowhere -> link to self if start and end are equal.
          if (childBlockNode.getStartNode().equals(childBlockNode.getLastNode())) {
            factory.linkSuccessor(childBlockNode, childBlockNode);
          }
          // successorOfCurrNode is the lastNode of childBlockNode
          // note that other BlockNodes can have successorOfCurrNode as their last nodes.
          lastNodeMap.put(successorOfCurrNode, childBlockNode);
          startNodeMap.put(lastCFANode, childBlockNode);
        } else {
          // if successorOfCurrNode is not a block end, all successors must be added to the block
          for (CFANode successor : CFAUtils.successorsOf(successorOfCurrNode)) {
            Set<CFANode> seen = new LinkedHashSet<>(nodeEntry.getSeen());
            Set<CFAEdge> seenEdge = new LinkedHashSet<>(nodeEntry.getSeenEdges());
            seen.add(successor);
            for (CFAEdge leavingEdge : CFAUtils.leavingEdges(successorOfCurrNode)) {
              if (leavingEdge.getSuccessor().equals(successor)) {
                seenEdge.add(leavingEdge);
                break;
              }
            }
            toSearch.add(new Entry(successor, seen, seenEdge, nodeEntry.getThreshold() + 1));
          }
        }
      }
    }
    return factory.build();
  }

  private static class Entry {
    private final CFANode node;
    private final Set<CFANode> seen;
    private final Set<CFAEdge> seenEdges;
    private final int threshold;

    public Entry(CFANode pNode, Set<CFANode> pSeen, Set<CFAEdge> pSeenEdges, int pThreshold) {
      node = pNode;
      seen = new LinkedHashSet<>(pSeen);
      seenEdges = new LinkedHashSet<>(pSeenEdges);
      threshold = pThreshold;
    }

    @Override
    public boolean equals(Object pO) {
      if (pO instanceof Entry) {
        Entry entry = (Entry) pO;
        return Objects.equals(node, entry.getNode()) && Objects.equals(seen, entry.getSeen());
      }
      return false;
    }

    public Set<CFANode> getSeen() {
      return seen;
    }

    public CFANode getNode() {
      return node;
    }

    public int getThreshold() {
      return threshold;
    }

    public Set<CFAEdge> getSeenEdges() {
      return seenEdges;
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, seen);
    }
  }
}

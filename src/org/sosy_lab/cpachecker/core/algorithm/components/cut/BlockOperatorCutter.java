// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.cut;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockTree;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class BlockOperatorCutter implements CFACutter {

  private final BlockOperator operator;

  public BlockOperatorCutter(Configuration pConfiguration) throws InvalidConfigurationException {
    operator = new BlockOperator();
    pConfiguration.inject(operator);
  }

  @Override
  public BlockTree cut(CFA cfa) {
    operator.setCFA(cfa);
    // start with the first node of the CFA
    CFANode startNode = cfa.getMainFunction();

    // create the root node of the tree consisting of the entry node only
    BlockNode root = new BlockNode(startNode, startNode, new LinkedHashSet<>());

    // the stack stores all block ends (i.e., operator.isBlockEnd(node) == true)
    ArrayDeque<CFANode> blockEnds = new ArrayDeque<>();
    // the first block end is our start node since it is the end of the first BlockNode
    blockEnds.push(startNode);

    // map a CFANode (that is the last node of a BlockNode) to its BlockNode
    // we use multimap since one CFANode can have multiple successors
    ArrayListMultimap<CFANode, BlockNode> nodeMap = ArrayListMultimap.create();
    // the first entry node maps to the root node of the BlockTree
    nodeMap.put(startNode, root);


    Set<CFANode> coveredBlockEnds = new HashSet<>();
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
      // additionally we want to store any node that is between start -> end of a BlockNode
      // so we use the class Entry to track the current node and the nodes in between.
      toSearch.addAll(
          CFAUtils.successorsOf(lastCFANode).stream()
              .map(succ -> new Entry(succ, ImmutableSet.of(succ)))
              .collect(Collectors.toList()));

      // if toSearch is empty, all successor nodes of lastCFANode have reached the block end
      while (!toSearch.isEmpty()) {
        // look at the top entry
        Entry nodeEntry = toSearch.pop();
        // successorOfCurrNode is a successor of lastCFANode
        CFANode successorOfCurrNode = nodeEntry.getNode();
        if (operator.isBlockEnd(successorOfCurrNode, 0)) {
          // if successorOfCurrNode is a block end, create a new BlockNode that starts with
          // lastCFANode and ends with
          // successorOfCurrNode. Additionally, tell the BlockNode which nodes are part of the
          // block.
          blockEnds.push(successorOfCurrNode);
          BlockNode childBlockNode =
              new BlockNode(lastCFANode, successorOfCurrNode, nodeEntry.getSeen());

          // every previous block that ends with lastCFANode is now linked to the new BlockNode that
          // starts with lastCFANode.
          nodeMap.get(lastCFANode).forEach(contained -> contained.linkSuccessor(childBlockNode));
          // successorOfCurrNode is the lastNode of childBlockNode
          // note that other BlockNodes can have successorOfCurrNode as their last nodes.
          nodeMap.put(successorOfCurrNode, childBlockNode);
        } else {
          // if successorOfCurrNode is not a block end, all successors must be added to the block
          for (CFANode successor : CFAUtils.successorsOf(successorOfCurrNode)) {
            Set<CFANode> seen = new LinkedHashSet<>(nodeEntry.getSeen());
            if (!seen.contains(successor)) {
              seen.add(successor);
              toSearch.add(new Entry(successor, seen));
            }
          }
        }
      }
    }
    return new BlockTree(root);
  }

  private static class Entry {
    private final CFANode node;
    private final Set<CFANode> seen;

    public Entry(CFANode pNode, Set<CFANode> pSeen) {
      node = pNode;
      seen = new LinkedHashSet<>(pSeen);
    }

    public CFANode getNode() {
      return node;
    }

    public Set<CFANode> getSeen() {
      return seen;
    }

    @Override
    public boolean equals(Object pO) {
      if (pO instanceof Entry) {
        Entry entry = (Entry) pO;
        return Objects.equals(node, entry.node) && Objects.equals(seen, entry.seen);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, seen);
    }
  }

}

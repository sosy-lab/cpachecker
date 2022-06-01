// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Manages a given partition of a program's CFA into a set of blocks. */
public class BlockPartitioning {
  private final Block mainBlock;
  private final ImmutableMap<CFANode, Block> callNodeToBlock;
  private final ImmutableListMultimap<CFANode, Block> returnNodeToBlock;
  private final ImmutableSet<Block> blocks;

  public BlockPartitioning(Collection<Block> subtrees, CFANode mainFunction) {
    final ImmutableMap.Builder<CFANode, Block> callNodeToSubtree = ImmutableMap.builder();
    final ImmutableListMultimap.Builder<CFANode, Block> returnNodeToSubtree =
        ImmutableListMultimap.builder();

    for (Block subtree : subtrees) {
      for (CFANode callNode : subtree.getCallNodes()) {
        callNodeToSubtree.put(callNode, subtree);
      }

      for (CFANode returnNode : subtree.getReturnNodes()) {
        returnNodeToSubtree.put(returnNode, subtree);
      }
    }

    callNodeToBlock = callNodeToSubtree.buildOrThrow();
    returnNodeToBlock = returnNodeToSubtree.build();
    blocks = ImmutableSet.copyOf(subtrees);
    mainBlock = callNodeToBlock.get(mainFunction);
  }

  /**
   * Returns true, if there is a <code>Block</code> such that <code>node</code> is a callnode of the
   * subtree.
   *
   * @param node the node to be checked
   */
  public boolean isCallNode(CFANode node) {
    return callNodeToBlock.containsKey(node);
  }

  /**
   * Requires <code>isCallNode(node)</code> to be <code>true</code>.
   *
   * @param node call node of some cached subtree
   * @return Block for given call node
   */
  public Block getBlockForCallNode(CFANode node) {
    return callNodeToBlock.get(node);
  }

  public Block getMainBlock() {
    return mainBlock;
  }

  public boolean isReturnNode(CFANode node) {
    return returnNodeToBlock.containsKey(node);
  }

  public ImmutableList<Block> getBlocksForReturnNode(CFANode pCurrentNode) {
    return returnNodeToBlock.get(pCurrentNode);
  }

  public Set<Block> getBlocks() {
    return blocks;
  }
}

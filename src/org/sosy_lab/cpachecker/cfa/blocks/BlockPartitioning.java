/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.blocks;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Manages a given partition of a program's CFA into a set of blocks.
 */
public class BlockPartitioning {
  private final Block mainBlock;
  private final Map<CFANode, Block> callNodeToBlock;
  private final ImmutableMultimap<CFANode, Block> returnNodeToBlock;
  private final ImmutableSet<Block> blocks;

  public BlockPartitioning(Collection<Block> subtrees, CFANode mainFunction) {
    final ImmutableMap.Builder<CFANode, Block> callNodeToSubtree = ImmutableMap.builder();
    final ImmutableMultimap.Builder<CFANode, Block> returnNodeToBlock = ImmutableMultimap.builder();

    for (Block subtree : subtrees) {
      for (CFANode callNode : subtree.getCallNodes()) {
        callNodeToSubtree.put(callNode, subtree);
      }

      for (CFANode returnNode : subtree.getReturnNodes()) {
        returnNodeToBlock.put(returnNode, subtree);
      }
    }

    this.callNodeToBlock = callNodeToSubtree.build();
    this.returnNodeToBlock = returnNodeToBlock.build();
    this.blocks = ImmutableSet.copyOf(subtrees);
    this.mainBlock = callNodeToBlock.get(mainFunction);
  }

  /**
   * @param node the node to be checked
   * @return true, if there is a <code>Block</code> such that <code>node</code> is a callnode of the subtree.
   */
  public boolean isCallNode(CFANode node) {
    return callNodeToBlock.containsKey(node);
  }

  /**
   * Requires <code>isCallNode(node)</code> to be <code>true</code>.
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

  @Deprecated
  // reason for deprecation: there can be several blocks for the same return-node
  public Block getBlockForReturnNode(CFANode pCurrentNode) {
    return Iterables.getFirst(returnNodeToBlock.get(pCurrentNode), null);
  }

  public ImmutableCollection<Block> getBlocksForReturnNode(CFANode pCurrentNode) {
    return returnNodeToBlock.get(pCurrentNode);
  }

  public Set<Block> getBlocks() {
    return blocks;
  }
}

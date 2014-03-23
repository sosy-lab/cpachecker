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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import com.google.common.collect.ImmutableMap;

/**
 * Manages a given partition of a program's CFA into a set of blocks.
 */
public class BlockPartitioning {
  private final Block mainBlock;
  private final Map<CFANode, Block> callNodeToBlock;
  private final Map<CFANode, Block> returnNodeToBlock;
  private final Set<Block> blocks;

  public BlockPartitioning(Collection<Block> subtrees, CFANode mainFunction) {
    Block mainBlock = null;
    final ImmutableMap.Builder<CFANode, Block> callNodeToSubtree = new ImmutableMap.Builder<>();
    final ImmutableMap.Builder<CFANode, Block> returnNodeToBlock = new ImmutableMap.Builder<>();
    final ImmutableSet.Builder<Block> blocks = new ImmutableSet.Builder<>();

    for (Block subtree : subtrees) {
      blocks.add(subtree);
      for (CFANode callNode : subtree.getCallNodes()) {
        if (callNode instanceof FunctionEntryNode &&
           callNode.getFunctionName().equalsIgnoreCase(mainFunction.getFunctionName())) {
          assert mainBlock == null;
          mainBlock = subtree;
        }
        callNodeToSubtree.put(callNode, subtree);
      }

      for (CFANode returnNode : subtree.getReturnNodes()) {
        returnNodeToBlock.put(returnNode, subtree);
      }
    }

    assert mainBlock != null;
    this.mainBlock = mainBlock;

    this.callNodeToBlock = callNodeToSubtree.build();
    this.returnNodeToBlock = returnNodeToBlock.build();
    this.blocks = blocks.build();
  }

  /**
   * @param node
   * @return true, if there is a <code>Block</code> such that <code>node</node> is a callnode of the subtree.
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

  public Block getBlockForReturnNode(CFANode pCurrentNode) {
    return returnNodeToBlock.get(pCurrentNode);
  }

  public Set<Block> getBlocks() {
    return blocks;
  }

}

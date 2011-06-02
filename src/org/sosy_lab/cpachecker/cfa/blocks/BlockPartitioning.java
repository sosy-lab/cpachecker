package org.sosy_lab.cpachecker.cfa.blocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Manages a given partition of a program's CFA into a set of blocks.
 * @author dwonisch
 *
 */
public class BlockPartitioning {
  private final Block mainBlock;
  private final Map<CFANode, Block> callNodeToBlock;
  private final Map<CFANode, Block> nodeToBlock;
  private final Set<CFANode> returnNodes;

  public BlockPartitioning(Collection<Block> subtrees) {
    Block mainBlock = null;
    Map<CFANode, Block> callNodeToSubtree = new HashMap<CFANode, Block>();
    Map<CFANode, Block> nodeToSubtree = new HashMap<CFANode, Block>();
    Set<CFANode> returnNodes = new HashSet<CFANode>();

    for(Block subtree : subtrees) {
      for(CFANode callNode : subtree.getCallNodes()) {
        if(callNode instanceof CFAFunctionDefinitionNode && callNode.getFunctionName().equalsIgnoreCase("main")) {
          assert mainBlock == null;
          mainBlock = subtree;
        }
        callNodeToSubtree.put(callNode, subtree);
      }
      for(CFANode uniqueNode : subtree.getUniqueNodes()) {
        nodeToSubtree.put(uniqueNode, subtree);
      }

      returnNodes.addAll(subtree.getReturnNodes());
    }

    assert mainBlock != null;
    this.mainBlock = mainBlock;

    this.callNodeToBlock = ImmutableMap.copyOf(callNodeToSubtree);
    this.nodeToBlock = ImmutableMap.copyOf(nodeToSubtree);
    this.returnNodes = ImmutableSet.copyOf(returnNodes);
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

  public Block getBlockForNode(CFANode node) {
    return nodeToBlock.get(node);
  }

  public Block getMainBlock() {
    return mainBlock;
  }

  public boolean isReturnNode(CFANode node) {
   return returnNodes.contains(node);
  }
}

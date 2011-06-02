package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFA;


/**
 * Defines an interface for heuristics for the partition of a program's CFA into blocks.
 * @author dwonisch
 *
 */
public abstract class PartitioningHeuristic {
  /**
   * Creates a <code>BlockPartitioning</code> using the represented heuristic.
   * @param mainFunction CFANode at which the main-function is defined
   * @return BlockPartitioning
   * @see org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning
   */
  public final BlockPartitioning buildPartitioning(CFANode mainFunction) {
    Set<CFANode> mainFunctionBody = CFA.exploreSubgraph(mainFunction, null);
    BlockPartitioningBuilder builder = new BlockPartitioningBuilder(mainFunctionBody);

    //traverse CFG
    Set<CFANode> seen = new HashSet<CFANode>();
    Deque<CFANode> stack = new ArrayDeque<CFANode>();

    seen.add(mainFunction);
    stack.push(mainFunction);

    while(!stack.isEmpty()) {
      CFANode node = stack.pop();

      if(shouldBeCached(node)) {
        Set<CFANode> subtree = getCachedSubtree(node);
        if(subtree != null) {
          builder.addBlock(subtree);
        }
      }

      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFANode nextNode = node.getLeavingEdge(i).getSuccessor();
        if(!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
    }

    return builder.build();
  }

  /**
   *
   * @param pNode
   * @return <code>true</code>, if for the given node a new <code>Block</code> should be created; <code>false</code> otherwise
   */
  protected abstract boolean shouldBeCached(CFANode pNode);

  /**
   *
   * @param pNode CFANode that should be cached.
   * @return set of nodes that represent a <code>Block</code>.
   */
  protected abstract Set<CFANode> getCachedSubtree(CFANode pNode);
}

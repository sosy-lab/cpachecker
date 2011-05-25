package de.upb.agw.cpachecker.cpa.abm.util;

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
public class CachedSubtreeManager {
  private final CachedSubtree mainCachedSubtree; 
  private final Map<CFANode, CachedSubtree> callNodeToSubtree;
  private final Map<CFANode, CachedSubtree> nodeToSubtree;
  private final Set<CFANode> returnNodes;
  
  public CachedSubtreeManager(Collection<CachedSubtree> subtrees) {
    CachedSubtree mainCachedSubtree = null;
    Map<CFANode, CachedSubtree> callNodeToSubtree = new HashMap<CFANode, CachedSubtree>();
    Map<CFANode, CachedSubtree> nodeToSubtree = new HashMap<CFANode, CachedSubtree>(); 
    Set<CFANode> returnNodes = new HashSet<CFANode>();
    
    for(CachedSubtree subtree : subtrees) {
      for(CFANode callNode : subtree.getCallNodes()) {
        if(callNode instanceof CFAFunctionDefinitionNode && callNode.getFunctionName().equalsIgnoreCase("main")) {
          assert mainCachedSubtree == null;
          mainCachedSubtree = subtree;
        }
        callNodeToSubtree.put(callNode, subtree);
      }
      for(CFANode uniqueNode : subtree.getUniqueNodes()) {
        nodeToSubtree.put(uniqueNode, subtree);
      }
      
      returnNodes.addAll(subtree.getReturnNodes());      
    }
    
    assert mainCachedSubtree != null;
    this.mainCachedSubtree = mainCachedSubtree;
    
    this.callNodeToSubtree = ImmutableMap.copyOf(callNodeToSubtree);
    this.nodeToSubtree = ImmutableMap.copyOf(nodeToSubtree);
    this.returnNodes = ImmutableSet.copyOf(returnNodes);
  }
  
  /**
   * @param node
   * @return true, if there is a <code>CachedSubtree</code> such that <code>node</node> is a callnode of the subtree.
   */
  public boolean isCallNode(CFANode node) {
    return callNodeToSubtree.containsKey(node);
  }
  
  /**
   * Requires <code>isCallNode(node)</code> to be <code>true</code>.
   * @param node call node of some cached subtree
   * @return CachedSubtree for given call node
   */
  public CachedSubtree getCachedSubtreeForCallNode(CFANode node) {
    return callNodeToSubtree.get(node);
  }
  
  public CachedSubtree getCachedSubtreeForNode(CFANode node) {
    return nodeToSubtree.get(node);
  }

  public CachedSubtree getMainSubtree() {
    return mainCachedSubtree;
  }

  public boolean isReturnNode(CFANode node) {
   return returnNodes.contains(node);
  }
}

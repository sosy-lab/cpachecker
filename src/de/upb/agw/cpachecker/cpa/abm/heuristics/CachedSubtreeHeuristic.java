package de.upb.agw.cpachecker.cpa.abm.heuristics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFA;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManagerBuilder;

/**
 * Defines an interface for heuristics for the partition of a program's CFA into blocks.
 * @author dwonisch
 *
 */
public abstract class CachedSubtreeHeuristic {
  /**
   * Creates a <code>CachedSubtreeManager</code> using the represented heuristic.  
   * @param mainFunction CFANode at which the main-function is defined
   * @return CachedSubtreeManager
   * @see de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager
   */
  public final CachedSubtreeManager buildMananger(CFANode mainFunction) {
    Set<CFANode> mainFunctionBody = CFA.exploreSubgraph(mainFunction, null);
    CachedSubtreeManagerBuilder builder = new CachedSubtreeManagerBuilder(mainFunctionBody);
    
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
          builder.addCachedSubtree(subtree);
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
   * @return <code>true</code>, if for the given node a new <code>CachedSubtree</code> should be created; <code>false</code> otherwise
   */
  protected abstract boolean shouldBeCached(CFANode pNode);
  
  /**
   * 
   * @param pNode CFANode that should be cached.
   * @return set of nodes that represent a <code>CachedSubtree</code>.
   */
  protected abstract Set<CFANode> getCachedSubtree(CFANode pNode); 
}

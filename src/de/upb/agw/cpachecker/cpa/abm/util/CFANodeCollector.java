package de.upb.agw.cpachecker.cpa.abm.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;

/**
 * Helper class for the exploration of CFAs.
 * @author dwonisch
 *
 */
public class CFANodeCollector {
  public static Set<CFANode> exploreSubgraph(CFANode startNode, CFANode endNode) {
    Set<CFANode> seen = new HashSet<CFANode>();
    Deque<CFANode> stack = new ArrayDeque<CFANode>();
    
    seen.add(startNode);
    if(endNode != null)
      seen.add(endNode);
    
    for(int i = 0; i < startNode.getNumLeavingEdges(); i++) {
      CFANode nextNode = startNode.getLeavingEdge(i).getSuccessor();
      if(startNode.getLeavingEdge(i) instanceof FunctionCallEdge) {
        nextNode = startNode.getLeavingSummaryEdge().getSuccessor();
      }
      if(!seen.contains(nextNode)) {
        stack.push(nextNode);
        seen.add(nextNode);
      }
    }
    
    while(!stack.isEmpty()) {
      CFANode node = stack.pop();
      if(node instanceof CFAFunctionExitNode) {
        continue;
      }
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFANode nextNode = node.getLeavingEdge(i).getSuccessor();        
        if(node.getLeavingEdge(i) instanceof FunctionCallEdge) {
          nextNode = node.getLeavingSummaryEdge().getSuccessor();
        }
        if(!seen.contains(nextNode)) {
          stack.push(nextNode);
          seen.add(nextNode);
        }
      }
      if(node.getEnteringSummaryEdge() != null) {
        CFANode prevNode = node.getEnteringSummaryEdge().getPredecessor();
        if(!seen.contains(prevNode)) {
          stack.add(prevNode);
          seen.add(prevNode);
        }
        continue;
      }
      for(int i = 0; i < node.getNumEnteringEdges(); i++) {
        CFANode prevNode = node.getEnteringEdge(i).getPredecessor();
        if(!seen.contains(prevNode)) {
          stack.add(prevNode);
          seen.add(prevNode);
        }
      }   
    }    
    
    return seen;
  }
}

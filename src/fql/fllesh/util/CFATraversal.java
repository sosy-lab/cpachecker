package fql.fllesh.util;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

public abstract class CFATraversal {
  
  public static void traverse(CFANode pInitialNode, CFAVisitor pVisitor) {
    assert(pVisitor != null);
    assert(pInitialNode != null);
    
    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();
    
    lWorklist.add(pInitialNode);
    
    pVisitor.init(pInitialNode);
    
    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);
      
      if (lVisitedNodes.contains(lCFANode)) {
        continue;
      }
      
      lVisitedNodes.add(lCFANode);
      
      // determine successors
      CallToReturnEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();
      
      if (lCallToReturnEdge != null) {
        
        pVisitor.visit(lCallToReturnEdge);
        
        CFANode lSuccessor = lCallToReturnEdge.getSuccessor();
        lWorklist.add(lSuccessor);
      }
      
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();
      
      for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
        CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
        
        pVisitor.visit(lEdge);
        
        CFANode lSuccessor = lEdge.getSuccessor();
        lWorklist.add(lSuccessor);
      }
    }
  }
  
}

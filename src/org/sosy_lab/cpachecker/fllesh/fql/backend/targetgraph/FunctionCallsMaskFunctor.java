package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class FunctionCallsMaskFunctor implements MaskFunctor<Node, Edge> {
  
  private static FunctionCallsMaskFunctor mInstance = new FunctionCallsMaskFunctor();
  
  private FunctionCallsMaskFunctor() {
    
  }
  
  public static FunctionCallsMaskFunctor getInstance() {
    return mInstance;
  }
  
  private boolean isFunctionCallEdge(CFAEdge lEdge) {
    assert(lEdge != null);
    
    if (lEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
      return true;
    }
    
    return false;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);
    
    return !isFunctionCallEdge(pArg0.getCFAEdge());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);
    
    CFANode lCFANode = pArg0.getCFANode();
    
    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      if (isFunctionCallEdge(lCFANode.getEnteringEdge(lIndex))) {
        return false;
      }
    }
    
    for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
      if (isFunctionCallEdge(lCFANode.getLeavingEdge(lIndex))) {
        return false;
      }
    }
    
    return true;
  }

}

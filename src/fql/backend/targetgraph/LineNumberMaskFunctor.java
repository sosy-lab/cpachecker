package fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

public class LineNumberMaskFunctor implements MaskFunctor<Node, Edge> {

  private int mLineNumber;
  
  public LineNumberMaskFunctor(int pLineNumber) {
    assert(pLineNumber > 0);
    
    mLineNumber = pLineNumber;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);
    
    return pArg0.getSource().getCFANode().getLineNumber() != mLineNumber;
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);
    
    CFANode lCFANode = pArg0.getCFANode();
    
    if (pArg0.getCFANode().getLineNumber() != mLineNumber) {
      for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
        CFAEdge lCFAEdge = lCFANode.getEnteringEdge(lIndex);
        
        if (lCFAEdge.getPredecessor().getLineNumber() == mLineNumber) {
          // predecessor has correct line number and thus we have to keep this 
          // vertex to preserve the edge
          return false;
        }
      }  
      
      return true;
    }
    else {
      return false;
    }
  }

}

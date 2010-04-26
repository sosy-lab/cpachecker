package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class LineNumberMaskFunctor implements MaskFunctor<Node, Edge> {

  private int mLineNumber;
  
  public LineNumberMaskFunctor(int pLineNumber) {
    assert(pLineNumber > 0);
    
    mLineNumber = pLineNumber;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);
    
    return pArg0.getTarget().getCFANode().getLineNumber() != mLineNumber;
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);
    
    CFANode lCFANode = pArg0.getCFANode();
    
    if (pArg0.getCFANode().getLineNumber() != mLineNumber) {
      for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lCFAEdge = lCFANode.getLeavingEdge(lIndex);
        
        if (lCFAEdge.getSuccessor().getLineNumber() == mLineNumber) {
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
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      LineNumberMaskFunctor lFunctor = (LineNumberMaskFunctor)pOther;
      
      return (mLineNumber == lFunctor.mLineNumber);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 234677 + mLineNumber;  
  }

}

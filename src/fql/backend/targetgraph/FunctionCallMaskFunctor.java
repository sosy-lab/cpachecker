package fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFANode;

public class FunctionCallMaskFunctor implements MaskFunctor<Node, Edge> {

  private String mFunctionName;
  
  public FunctionCallMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);
    
    mFunctionName = pFunctionName;
  }
  
  public String getFunctionName() {
    return mFunctionName;
  }
  
  private boolean isProperFunctionCallEdge(CFAEdge lEdge) {
    assert(lEdge != null);
    
    if (lEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
      return lEdge.getSuccessor().getFunctionName().equals(mFunctionName);
    }
    
    return false;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);
    
    return !isProperFunctionCallEdge(pArg0.getCFAEdge());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);
    
    CFANode lCFANode = pArg0.getCFANode();
    
    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      if (isProperFunctionCallEdge(lCFANode.getEnteringEdge(lIndex))) {
        return false;
      }
    }
    
    for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
      if (isProperFunctionCallEdge(lCFANode.getLeavingEdge(lIndex))) {
        return false;
      }
    }
    
    return true;
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
      FunctionCallMaskFunctor lFunctor = (FunctionCallMaskFunctor)pOther;
      
      return mFunctionName.equals(lFunctor.mFunctionName);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 87237737 + mFunctionName.hashCode();
  }

}

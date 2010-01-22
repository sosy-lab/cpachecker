package fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

import cfa.objectmodel.CFANode;

public class FunctionNameMaskFunctor implements MaskFunctor<Node, Edge> {

  private String mFunctionName;
  
  public FunctionNameMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);
    
    mFunctionName = pFunctionName;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    assert(pArg0 != null);
    
    return isVertexMasked(pArg0.getSource()) || isVertexMasked(pArg0.getTarget());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    assert(pArg0 != null);
    
    CFANode lCFANode = pArg0.getCFANode();
    
    return !lCFANode.getFunctionName().equals(mFunctionName);
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
      FunctionNameMaskFunctor lFunctor = (FunctionNameMaskFunctor)pOther;
      
      return mFunctionName.equals(lFunctor.mFunctionName);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 23477723 + mFunctionName.hashCode();
  }

}

package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

public class FunctionEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private MaskFunctor<Node, Edge> mFunctionNameMaskFunctor;
  private MaskFunctor<Node, Edge> mFunctionEntriesMaskFunctor;
  
  public FunctionEntryMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);
    
    mFunctionNameMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);
    mFunctionEntriesMaskFunctor = FunctionEntriesMaskFunctor.getInstance();
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    return mFunctionNameMaskFunctor.isEdgeMasked(pArg0) || mFunctionEntriesMaskFunctor.isEdgeMasked(pArg0);
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    return mFunctionNameMaskFunctor.isVertexMasked(pArg0) || mFunctionEntriesMaskFunctor.isVertexMasked(pArg0);
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
      FunctionEntryMaskFunctor lFunctor = (FunctionEntryMaskFunctor)pOther;
      
      return mFunctionNameMaskFunctor.equals(lFunctor.mFunctionNameMaskFunctor) && mFunctionEntriesMaskFunctor.equals(lFunctor.mFunctionEntriesMaskFunctor);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 2391767 + mFunctionNameMaskFunctor.hashCode() + mFunctionEntriesMaskFunctor.hashCode();
  }

}

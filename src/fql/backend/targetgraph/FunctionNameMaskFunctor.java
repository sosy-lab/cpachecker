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
    return isVertexMasked(pArg0.getSource()) || isVertexMasked(pArg0.getTarget());
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    CFANode lCFANode = pArg0.getCFANode();
    
    return !lCFANode.getFunctionName().equals(mFunctionName);
  }

}

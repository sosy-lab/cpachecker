package fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;

public class FunctionEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private MaskFunctor<Node, Edge> lFunctionNameMaskFunctor;
  private MaskFunctor<Node, Edge> lFunctionEntriesMaskFunctor;
  
  public FunctionEntryMaskFunctor(String pFunctionName) {
    assert(pFunctionName != null);
    
    lFunctionNameMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);
    lFunctionEntriesMaskFunctor = new FunctionEntriesMaskFunctor();
  }
  
  @Override
  public boolean isEdgeMasked(Edge pArg0) {
    return lFunctionNameMaskFunctor.isEdgeMasked(pArg0) || lFunctionEntriesMaskFunctor.isEdgeMasked(pArg0);
  }

  @Override
  public boolean isVertexMasked(Node pArg0) {
    return lFunctionNameMaskFunctor.isVertexMasked(pArg0) || lFunctionEntriesMaskFunctor.isVertexMasked(pArg0);
  }

}

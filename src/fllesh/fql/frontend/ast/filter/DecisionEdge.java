package fllesh.fql.frontend.ast.filter;

import fllesh.fql.frontend.ast.ASTVisitor;

public class DecisionEdge implements Filter {
  private static DecisionEdge mInstance = new DecisionEdge();
  
  private DecisionEdge() {
    
  }
  
  public static DecisionEdge getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "@DECISIONEDGE";
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

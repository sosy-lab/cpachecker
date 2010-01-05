package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

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
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);

    pVisitor.visit(this);
  }

}

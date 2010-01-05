package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class ConditionEdge implements Filter {
  private static ConditionEdge mInstance = new ConditionEdge();
  
  private ConditionEdge() {
    
  }
  
  public static ConditionEdge getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "@CONDITIONEDGE";
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);

    pVisitor.visit(this);
  }

}

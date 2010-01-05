package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class ConditionGraph implements Filter {
  private static ConditionGraph mInstance = new ConditionGraph();
  
  private ConditionGraph() {
    
  }
  
  public static ConditionGraph getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "@CONDITIONGRAPH";
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);

    pVisitor.visit(this);
  }

}

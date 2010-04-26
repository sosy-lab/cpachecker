package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

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
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

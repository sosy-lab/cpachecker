package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.filter;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

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
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

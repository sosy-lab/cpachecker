package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Identity implements Filter {
  private static Identity mInstance = new Identity();
  
  private Identity() {
    
  }
  
  public static Identity getInstance() {
    return mInstance;
  }

  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "ID";
  }
  
}

package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class FunctionCalls implements Filter {
  private static FunctionCalls mInstance = new FunctionCalls();
  
  private FunctionCalls() {
    
  }
  
  public static FunctionCalls getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "@CALLS";
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);

    pVisitor.visit(this);
  }

}

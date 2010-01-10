package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class BasicBlockEntry implements Filter {
  private static BasicBlockEntry mInstance = new BasicBlockEntry();
  
  private BasicBlockEntry() {
    
  }
  
  public static BasicBlockEntry getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "@BASICBLOCKENTRY";
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

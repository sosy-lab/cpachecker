package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Function implements Filter {

  String mFuncName;
  
  public Function(String pFuncName) {
    assert(pFuncName != null);
    
    mFuncName = pFuncName;
  }
  
  public String getFunctionName() {
    return mFuncName;
  }
  
  @Override
  public String toString() {
    return "@FUNC(" + mFuncName + ")";
  }
  
  @Override
  public int hashCode() {
    return 123411 + mFuncName.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof Function) {
      Function mFuncFilter = (Function)pOther;
      
      return mFuncName.equals(mFuncFilter.mFuncName);
    }
    
    return false;
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    pVisitor.visit(this);
  }

}

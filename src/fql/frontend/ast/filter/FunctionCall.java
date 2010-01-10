package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class FunctionCall implements Filter {

  String mFuncName;
  
  public FunctionCall(String pFuncName) {
    assert(pFuncName != null);
    
    mFuncName = pFuncName;
  }
  
  public String getFunctionName() {
    return mFuncName;
  }
  
  @Override
  public String toString() {
    return "@CALL(" + mFuncName + ")";
  }
  
  @Override
  public int hashCode() {
    return 1324 + mFuncName.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof FunctionCall) {
      FunctionCall mCallFilter = (FunctionCall)pOther;
      
      return mFuncName.equals(mCallFilter.mFuncName);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

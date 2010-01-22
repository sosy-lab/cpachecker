package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class FunctionEntry implements Filter {

  String mFuncName;
  
  public FunctionEntry(String pFuncName) {
    assert(pFuncName != null);
    
    mFuncName = pFuncName;
  }
  
  public String getFunctionName() {
    return mFuncName;
  }
  
  @Override
  public String toString() {
    return "@ENTRY(" + mFuncName + ")";
  }
  
  @Override
  public int hashCode() {
    return 832726 + mFuncName.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther == this) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      FunctionEntry mEntryFilter = (FunctionEntry)pOther;
      
      return mFuncName.equals(mEntryFilter.mFuncName);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

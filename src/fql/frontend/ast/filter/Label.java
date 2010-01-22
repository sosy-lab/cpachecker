package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Label implements Filter {

  String mLabel;
  
  public Label(String pLabel) {
    assert(pLabel != null);
    
    mLabel = pLabel;
  }
  
  public String getFileName() {
    return mLabel;
  }
  
  @Override
  public String toString() {
    return "@LABEL(" + mLabel + ")";
  }
  
  @Override
  public int hashCode() {
    return 35411 + mLabel.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Label mLabelFilter = (Label)pOther;
      
      return mLabel.equals(mLabelFilter.mLabel);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

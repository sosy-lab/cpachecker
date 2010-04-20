package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

public class Line implements Filter {
  private int mLine;
  
  public Line(Integer pLine) {
    assert(pLine != null);
    
    mLine = pLine.intValue();
  }
  
  public int getLine() {
    return mLine;
  }
  
  @Override
  public String toString() {
    return "@LINE(" + mLine + ")";
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
      Line mOtherLine = (Line)pOther;
      
      return (mOtherLine.getLine() == mLine);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mLine;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

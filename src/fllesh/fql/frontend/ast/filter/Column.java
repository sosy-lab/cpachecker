package fllesh.fql.frontend.ast.filter;

import fllesh.fql.frontend.ast.ASTVisitor;

public class Column implements Filter {
  private int mColumn;
  
  public Column(Integer pColumn) {
    assert(pColumn != null);
    
    mColumn = pColumn.intValue();
  }
  
  public int getColumn() {
    return mColumn;
  }
  
  @Override
  public String toString() {
    return "@COLUMN(" + mColumn + ")";
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
      Column mOtherColumn = (Column)pOther;
      
      return (mOtherColumn.getColumn() == mColumn);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mColumn;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

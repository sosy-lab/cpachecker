package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

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
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof Column) {
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
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }

}

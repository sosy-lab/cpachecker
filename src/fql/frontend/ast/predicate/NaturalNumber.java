package fql.frontend.ast.predicate;

import fql.frontend.ast.ASTVisitor;

public class NaturalNumber implements Term {
  private int mValue;
  
  public NaturalNumber(int pValue) {
    mValue = pValue;
  }
  
  public int getValue() {
    return mValue;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == this.getClass()) {
      NaturalNumber mOther = (NaturalNumber)pOther;
      
      return (mValue == mOther.mValue);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mValue;
  }
  
  @Override
  public String toString() {
    return "" + mValue;
  }

  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }
  
}

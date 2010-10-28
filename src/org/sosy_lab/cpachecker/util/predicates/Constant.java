package org.sosy_lab.cpachecker.util.predicates;

public class Constant implements Term {

  private int mValue;
  
  public Constant(int pValue) {
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
    
    if (!(pOther.getClass().equals(getClass()))) {
      return false;
    }
    
    Constant lConstant = (Constant)pOther;
    
    return (mValue == lConstant.mValue);
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
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}

package org.sosy_lab.cpachecker.fllesh.ecp;

public class ECPPredicate implements ECPGuard {

  private String mPredicate;
  
  public ECPPredicate(String pPredicate) {
    mPredicate = pPredicate;
  }
  
  /** copy constructor */
  public ECPPredicate(ECPPredicate pPredicate) {
    this(pPredicate.toString());
  }
  
  @Override
  public int hashCode() {
    return mPredicate.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther instanceof ECPPredicate) {
      ECPPredicate lOther = (ECPPredicate)pOther;
      
      return mPredicate.equals(lOther.mPredicate);
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return mPredicate;
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}

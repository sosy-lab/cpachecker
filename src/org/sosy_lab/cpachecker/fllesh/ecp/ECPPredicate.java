package org.sosy_lab.cpachecker.fllesh.ecp;

import org.sosy_lab.cpachecker.util.predicates.Predicate;

public class ECPPredicate implements ECPGuard {

  private Predicate mPredicate;
  
  public ECPPredicate(Predicate pPredicate) {
    mPredicate = pPredicate;
  }
  
  /** copy constructor */
  public ECPPredicate(ECPPredicate pPredicate) {
    this(pPredicate.mPredicate);
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
    
    if (pOther.getClass().equals(getClass())) {
      ECPPredicate lOther = (ECPPredicate)pOther;
      
      return mPredicate.equals(lOther.mPredicate);
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return mPredicate.toString();
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  public Predicate getPredicate() {
    return mPredicate;
  }
  
}

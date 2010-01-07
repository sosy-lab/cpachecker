package fql.frontend.ast.predicate;

import fql.frontend.ast.ASTVisitor;

public class CIdentifier implements Term {
  
  private String mCIdentifier;
  
  public CIdentifier(String pCIdentifier) {
    assert(pCIdentifier != null);
    
    mCIdentifier = pCIdentifier;
  }
  
  @Override
  public String toString() {
    return mCIdentifier;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther.getClass() == getClass()) {
      CIdentifier lOther = (CIdentifier)pOther;
      
      return mCIdentifier.equals(lOther.mCIdentifier);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 34095 + mCIdentifier.hashCode();
  }

  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }
  
}

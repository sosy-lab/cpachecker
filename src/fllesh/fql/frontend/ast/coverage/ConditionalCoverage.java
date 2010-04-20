package fllesh.fql.frontend.ast.coverage;

import fllesh.fql.frontend.ast.ASTVisitor;
import fllesh.fql.frontend.ast.predicate.Predicates;

public class ConditionalCoverage implements Coverage {

  private Coverage mCoverage;
  private Predicates mPreconditions;
  private Predicates mPostconditions;
  
  public ConditionalCoverage(Predicates pPreconditions, Coverage pCoverage, Predicates pPostconditions) {
    assert(pPreconditions != null);
    assert(pCoverage != null);
    assert(pPostconditions != null);
    
    mPreconditions = pPreconditions;
    mCoverage = pCoverage;
    mPostconditions = pPostconditions;
  }
  
  public Predicates getPreconditions() {
    return mPreconditions;
  }
  
  public Predicates getPostconditions() {
    return mPostconditions;
  }
  
  public Coverage getCoverage() {
    return mCoverage;
  }
  
  @Override
  public String toString() {
    return mPreconditions.toString() + " " + mCoverage.toString() + " " + mPostconditions.toString();
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
      ConditionalCoverage lOther = (ConditionalCoverage)pOther;
      
      return (lOther.mCoverage.equals(mCoverage) && lOther.mPreconditions.equals(mPreconditions) && lOther.mPostconditions.equals(mPostconditions));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 712803 + mCoverage.hashCode() + mPreconditions.hashCode() + mPostconditions.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

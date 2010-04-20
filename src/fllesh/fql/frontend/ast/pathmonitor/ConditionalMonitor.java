package fql.frontend.ast.pathmonitor;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.predicate.Predicates;

public class ConditionalMonitor implements PathMonitor {

  private Predicates mPreconditions;
  private PathMonitor mSubmonitor;
  private Predicates mPostconditions;
  
  public ConditionalMonitor(Predicates pPreconditions, PathMonitor pSubmonitor, Predicates pPostconditions) {
    assert(pPreconditions != null);
    assert(pSubmonitor != null);
    assert(pPostconditions != null);
    
    mPreconditions = pPreconditions;
    mSubmonitor = pSubmonitor;
    mPostconditions = pPostconditions;
  }
  
  public Predicates getPreconditions() {
    return mPreconditions;
  }
  
  public Predicates getPostconditions() {
    return mPostconditions;
  }
  
  public PathMonitor getSubmonitor() {
    return mSubmonitor;
  }
  
  @Override
  public String toString() {
    return mPreconditions.toString() + " " + mSubmonitor.toString() + " " + mPostconditions.toString();
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
      ConditionalMonitor lMonitor = (ConditionalMonitor)pOther;
      
      return lMonitor.mPreconditions.equals(mPreconditions) 
              && lMonitor.mSubmonitor.equals(mSubmonitor)
              && lMonitor.mPostconditions.equals(mPostconditions);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 20393 + mPreconditions.hashCode() + mSubmonitor.hashCode() + mPostconditions.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

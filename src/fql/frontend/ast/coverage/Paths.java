package fql.frontend.ast.coverage;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.predicate.Predicates;

public class Paths implements Coverage {

  private Filter mFilter;
  private Predicates mPredicates;
  private int mBound;
  
  public Paths(Filter pFilter, int pBound, Predicates pPredicates) {
    assert(pFilter != null);
    assert(pPredicates != null);
    assert(pBound >= 0);
    
    mFilter = pFilter;
    mBound = pBound;
    mPredicates = pPredicates;
  }
  
  public int getBound() {
    return mBound;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  public Predicates getPredicates() {
    return mPredicates;
  }
  
  @Override
  public String toString() {
    return "PATHS(" + mFilter.toString() + ", " + mBound + ", " + mPredicates.toString() + ")";
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
      Paths lOther = (Paths)pOther;
      
      return (lOther.mFilter.equals(mFilter) && lOther.mPredicates.equals(mPredicates));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 32233 + mFilter.hashCode() + mPredicates.hashCode() + mBound;
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    assert(pVisitor != null);
    
    pVisitor.visit(this);
  }

}

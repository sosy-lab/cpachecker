package fql.frontend.ast.coverage;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.predicate.Predicates;

public class States implements Coverage {

  private Filter mFilter;
  private Predicates mPredicates;
  
  public States(Filter pFilter) {
    this(pFilter, new Predicates());
  }
  
  public States(Filter pFilter, Predicates pPredicates) {
    assert(pFilter != null);
    assert(pPredicates != null);
    
    mFilter = pFilter;
    mPredicates = pPredicates;
  }
  
  public Filter getFilter() {
    return mFilter;
  }
  
  public Predicates getPredicates() {
    return mPredicates;
  }
  
  @Override
  public String toString() {
    return "STATES(" + mFilter.toString() + ", " + mPredicates.toString() + ")";
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
      States lOther = (States)pOther;
      
      return (lOther.mFilter.equals(mFilter) && lOther.mPredicates.equals(mPredicates));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 34532 + mFilter.hashCode() + mPredicates.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}

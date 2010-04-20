package fql.frontend.ast.coverage;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.predicate.Predicates;

public class Edges implements Coverage {

  private Filter mFilter;
  private Predicates mPredicates;
  
  public Edges(Filter pFilter) {
    this(pFilter, new Predicates());
  }
  
  public Edges(Filter pFilter, Predicates pPredicates) {
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
    return "EDGES(" + mFilter.toString() + ", " + mPredicates.toString() + ")";
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
      Edges lOther = (Edges)pOther;
      
      return (lOther.mFilter.equals(mFilter) && lOther.mPredicates.equals(mPredicates));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 712804 + mFilter.hashCode() + mPredicates.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
